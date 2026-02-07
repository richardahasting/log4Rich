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

package com.log4rich.core;

import com.log4rich.appenders.Appender;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.config.ConfigLoader;
import com.log4rich.config.Configuration;
import com.log4rich.layouts.StandardLayout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Factory and manager for Logger instances.
 * Maintains a registry of loggers and provides thread-safe access.
 *
 * <p>On first use, LogManager automatically loads configuration from
 * {@link ConfigLoader} and creates default appenders (console, file) based
 * on the configuration settings. All loggers created through this manager
 * inherit the default appenders.</p>
 */
public class LogManager {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private LogManager() {
        // Utility class - no instantiation
    }

    private static final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static volatile boolean shutdownHookAdded = false;
    private static volatile boolean initialized = false;
    private static final List<Appender> defaultAppenders = new CopyOnWriteArrayList<>();
    private static volatile Configuration configuration;

    /**
     * Initialize the logging system from configuration.
     * Creates default appenders based on configuration settings.
     * This method is idempotent and thread-safe.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (LogManager.class) {
            if (initialized) {
                return;
            }

            configuration = ConfigLoader.loadConfiguration();

            // Create console appender if enabled (default: true)
            if (configuration.isConsoleEnabled()) {
                ConsoleAppender consoleAppender = new ConsoleAppender(
                    "STDERR".equalsIgnoreCase(configuration.getConsoleTarget())
                        ? ConsoleAppender.Target.STDERR
                        : ConsoleAppender.Target.STDOUT
                );
                consoleAppender.setName("console");
                consoleAppender.setLevel(configuration.getConsoleLevel());

                String pattern = configuration.getConsolePattern();
                if (pattern != null && !pattern.isEmpty()) {
                    consoleAppender.setLayout(new StandardLayout(pattern));
                }

                defaultAppenders.add(consoleAppender);
            }

            // Apply default appenders to any loggers that were already created
            for (Logger logger : loggers.values()) {
                configureLogger(logger);
            }

            initialized = true;
        }
    }

    /**
     * Configures a logger with default appenders and the appropriate log level.
     */
    private static void configureLogger(Logger logger) {
        // Add default appenders if the logger has none
        if (logger.getAppenders().isEmpty()) {
            for (Appender appender : defaultAppenders) {
                logger.addAppender(appender);
            }
        }

        // Apply logger-specific level if configured, otherwise root level
        if (configuration != null) {
            LogLevel specificLevel = configuration.getLoggerLevel(logger.getName());
            if (specificLevel != null) {
                logger.setLevel(specificLevel);
            } else {
                logger.setLevel(configuration.getRootLevel());
            }
        }
    }

    /**
     * Get a logger for the specified name.
     * Triggers auto-initialization on first call.
     * @param name The logger name
     * @return Logger instance
     */
    public static Logger getLogger(String name) {
        if (name == null) {
            name = "root";
        }

        // Ensure initialization has occurred
        if (!initialized) {
            initialize();
        }

        // Use computeIfAbsent for thread-safe logger creation
        final String loggerName = name;
        Logger logger = loggers.computeIfAbsent(loggerName, n -> {
            Logger newLogger = new Logger(n);
            configureLogger(newLogger);
            return newLogger;
        });

        // Add shutdown hook if not already added
        addShutdownHookIfNeeded();

        return logger;
    }

    /**
     * Get a logger for the specified class.
     * @param clazz The class to get logger for
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            return getLogger("root");
        }
        return getLogger(clazz.getName());
    }

    /**
     * Get the root logger.
     * @return Root logger instance
     */
    public static Logger getRootLogger() {
        return getLogger("root");
    }

    /**
     * Check if a logger exists with the given name.
     * @param name The logger name
     * @return true if logger exists
     */
    public static boolean exists(String name) {
        return loggers.containsKey(name);
    }

    /**
     * Get all existing logger names.
     * @return Array of logger names
     */
    public static String[] getLoggerNames() {
        return loggers.keySet().toArray(new String[0]);
    }

    /**
     * Get the current configuration.
     * @return the active Configuration, or null if not yet initialized
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Shutdown all loggers and their appenders.
     */
    public static void shutdown() {
        for (Logger logger : loggers.values()) {
            try {
                logger.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down logger " + logger.getName() + ": " + e.getMessage());
            }
        }
        loggers.clear();
    }

    /**
     * Reset the logging system by removing all loggers.
     */
    public static void reset() {
        shutdown();
        defaultAppenders.clear();
        configuration = null;
        initialized = false;
        ConfigLoader.clearCache();
    }

    /**
     * Add shutdown hook to ensure proper cleanup.
     */
    private static void addShutdownHookIfNeeded() {
        if (!shutdownHookAdded) {
            synchronized (LogManager.class) {
                if (!shutdownHookAdded) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            shutdown();
                        } catch (Exception e) {
                            System.err.println("Error during log4Rich shutdown: " + e.getMessage());
                        }
                    }, "log4Rich-shutdown"));
                    shutdownHookAdded = true;
                }
            }
        }
    }
}
