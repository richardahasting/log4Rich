/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.log4rich.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches a configuration file for changes and triggers reload when modifications are detected.
 *
 * <p>This class uses Java NIO's WatchService to efficiently monitor file changes using
 * OS-level notifications (inotify on Linux, FSEvents on macOS, etc.) rather than polling.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Start watching configuration file
 * ConfigurationWatcher watcher = new ConfigurationWatcher(Paths.get("log4Rich.config"));
 * watcher.start();
 *
 * // Later, stop watching
 * watcher.stop();
 * }</pre>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Automatic configuration reload on file changes</li>
 *   <li>Debouncing to prevent multiple rapid reloads</li>
 *   <li>Graceful error handling for invalid configurations</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class ConfigurationWatcher {

    /** Minimum time between configuration reloads to prevent rapid-fire updates (in milliseconds) */
    private static final long DEBOUNCE_INTERVAL_MS = 500;

    private final Path configPath;
    private final Path watchDir;
    private final String fileName;

    private WatchService watchService;
    private Thread watchThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastReloadTime = new AtomicLong(0);

    private ConfigurationReloadListener reloadListener;

    /**
     * Creates a new ConfigurationWatcher for the specified configuration file.
     *
     * @param configPath path to the configuration file to watch
     * @throws IllegalArgumentException if the path is null or doesn't exist
     */
    public ConfigurationWatcher(Path configPath) {
        if (configPath == null) {
            throw new IllegalArgumentException("Configuration path cannot be null");
        }

        this.configPath = configPath.toAbsolutePath().normalize();
        this.watchDir = this.configPath.getParent();
        this.fileName = this.configPath.getFileName().toString();

        if (this.watchDir == null) {
            throw new IllegalArgumentException("Configuration path must have a parent directory");
        }
    }

    /**
     * Creates a new ConfigurationWatcher for the specified configuration file path.
     *
     * @param configPath path string to the configuration file to watch
     */
    public ConfigurationWatcher(String configPath) {
        this(Paths.get(configPath));
    }

    /**
     * Sets a listener to be notified when configuration reloads occur.
     *
     * @param listener the listener to notify
     */
    public void setReloadListener(ConfigurationReloadListener listener) {
        this.reloadListener = listener;
    }

    /**
     * Starts watching the configuration file for changes.
     * Creates a daemon thread that monitors the file's parent directory.
     *
     * @throws IOException if unable to create the watch service
     */
    public void start() throws IOException {
        if (running.get()) {
            logInfo("Configuration watcher already running");
            return;
        }

        // Verify the directory exists
        if (!Files.isDirectory(watchDir)) {
            throw new IOException("Watch directory does not exist: " + watchDir);
        }

        watchService = FileSystems.getDefault().newWatchService();
        watchDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        running.set(true);

        watchThread = new Thread(this::watchLoop, "log4Rich-config-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        logInfo("Configuration watcher started for: " + configPath);
    }

    /**
     * Stops watching the configuration file.
     * Interrupts the watch thread and closes the watch service.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        logInfo("Stopping configuration watcher...");

        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logWarning("Error closing watch service: " + e.getMessage());
            }
        }

        logInfo("Configuration watcher stopped");
    }

    /**
     * Checks if the watcher is currently running.
     *
     * @return true if watching, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Gets the path being watched.
     *
     * @return the configuration file path
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Manually triggers a configuration reload.
     * Useful for programmatic reload without waiting for file changes.
     *
     * @return true if reload was successful, false otherwise
     */
    public boolean triggerReload() {
        return reloadConfiguration();
    }

    /**
     * Main watch loop that monitors for file changes.
     */
    private void watchLoop() {
        logInfo("Watch loop started for directory: " + watchDir);

        while (running.get()) {
            WatchKey key;
            try {
                // Wait for events with a timeout so we can check the running flag
                key = watchService.poll(1, TimeUnit.SECONDS);

                if (key == null) {
                    continue;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Handle overflow events
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    logWarning("Watch event overflow - some events may have been lost");
                    continue;
                }

                // Get the file that was modified
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path changedFile = pathEvent.context();

                // Check if it's our configuration file
                if (fileName.equals(changedFile.toString())) {
                    handleConfigurationChange();
                }
            }

            // Reset the key to receive further events
            boolean valid = key.reset();
            if (!valid) {
                logWarning("Watch key became invalid - directory may have been deleted");
                break;
            }
        }

        logInfo("Watch loop ended");
    }

    /**
     * Handles a configuration file change event.
     * Implements debouncing to prevent multiple rapid reloads.
     */
    private void handleConfigurationChange() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastReloadTime.get();

        // Debounce: ignore if we recently reloaded
        if (currentTime - lastTime < DEBOUNCE_INTERVAL_MS) {
            return;
        }

        // Update last reload time
        if (!lastReloadTime.compareAndSet(lastTime, currentTime)) {
            // Another thread beat us to it
            return;
        }

        logInfo("Configuration file change detected: " + configPath);
        reloadConfiguration();
    }

    /**
     * Reloads the configuration from the file.
     *
     * @return true if reload was successful, false otherwise
     */
    private boolean reloadConfiguration() {
        try {
            logInfo("Reloading configuration...");

            // Reload configuration through ConfigurationManager
            ConfigurationManager.reloadConfiguration();

            logInfo("Configuration reloaded successfully");

            // Notify listener
            if (reloadListener != null) {
                reloadListener.onConfigurationReloaded(true, null);
            }

            return true;

        } catch (Exception e) {
            logError("Failed to reload configuration: " + e.getMessage());

            // Notify listener of failure
            if (reloadListener != null) {
                reloadListener.onConfigurationReloaded(false, e);
            }

            return false;
        }
    }

    // Logging methods that avoid circular dependencies
    private void logInfo(String message) {
        System.out.println("[log4Rich] ConfigWatcher: " + message);
    }

    private void logWarning(String message) {
        System.err.println("[log4Rich] ConfigWatcher WARNING: " + message);
    }

    private void logError(String message) {
        System.err.println("[log4Rich] ConfigWatcher ERROR: " + message);
    }

    /**
     * Listener interface for configuration reload events.
     */
    public interface ConfigurationReloadListener {
        /**
         * Called when a configuration reload is attempted.
         *
         * @param success true if the reload was successful, false otherwise
         * @param error the exception if reload failed, null if successful
         */
        void onConfigurationReloaded(boolean success, Exception error);
    }
}
