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

import com.log4rich.appenders.Appender;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.CompressionManager;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages runtime configuration changes for the log4Rich framework.
 * Provides thread-safe methods to modify configuration at runtime.
 * This class serves as the central point for all configuration management
 * and automatically applies changes to the logging system.
 */
public class ConfigurationManager {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConfigurationManager() {
        // Utility class - no instantiation
    }
    
    private static final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();
    private static volatile Configuration currentConfig;
    private static final Map<String, Appender> managedAppenders = new ConcurrentHashMap<>();
    private static volatile boolean autoApplyConfiguration = true;
    
    /**
     * Initializes the configuration manager with the current configuration.
     * This method sets up the configuration and applies it to all loggers.
     * 
     * @param config the configuration to use
     */
    public static void initialize(Configuration config) {
        configLock.writeLock().lock();
        try {
            currentConfig = config;
            applyConfiguration();
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current configuration.
     * This method is thread-safe and returns a reference to the current configuration.
     * 
     * @return the current configuration, or null if not initialized
     */
    public static Configuration getCurrentConfiguration() {
        configLock.readLock().lock();
        try {
            return currentConfig;
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Sets the root logger level.
     * This affects all loggers that don't have specific levels configured.
     * Changes take effect immediately if auto-apply is enabled.
     * 
     * @param level the new root level to set
     */
    public static void setRootLevel(LogLevel level) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.rootLevel", level.name());
                if (autoApplyConfiguration) {
                    LogManager.getRootLogger().setLevel(level);
                    // Update all loggers that don't have a specific level set
                    for (String loggerName : LogManager.getLoggerNames()) {
                        if (!loggerName.equals("root") && currentConfig.getLoggerLevel(loggerName) == null) {
                            Logger logger = LogManager.getLogger(loggerName);
                            logger.setLevel(level);
                        }
                    }
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the location capture setting globally.
     * When enabled, the framework will capture and include class name,
     * method name, and line number information in log events.
     * 
     * @param enabled true to enable location capture, false to disable
     */
    public static void setLocationCapture(boolean enabled) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.location.capture", String.valueOf(enabled));
                if (autoApplyConfiguration) {
                    // Apply to all existing loggers
                    for (String loggerName : LogManager.getLoggerNames()) {
                        Logger logger = LogManager.getLogger(loggerName);
                        logger.setLocationCapture(enabled);
                    }
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets a specific logger's level.
     * This overrides the root level for the specified logger.
     * 
     * @param loggerName the name of the logger to configure
     * @param level the level to set for this logger
     */
    public static void setLoggerLevel(String loggerName, LogLevel level) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.logger." + loggerName, level.name());
                // Also update the loggerLevels map
                currentConfig.setLoggerLevel(loggerName, level);
                if (autoApplyConfiguration) {
                    Logger logger = LogManager.getLogger(loggerName);
                    logger.setLevel(level);
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Enables or disables console logging.
     * When enabled, log messages will be written to the console.
     * 
     * @param enabled true to enable console logging, false to disable
     */
    public static void setConsoleEnabled(boolean enabled) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.console.enabled", String.valueOf(enabled));
                if (autoApplyConfiguration) {
                    applyConsoleConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the console target stream.
     * 
     * @param target the target stream ("STDOUT" or "STDERR")
     */
    public static void setConsoleTarget(String target) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.console.target", target);
                if (autoApplyConfiguration) {
                    applyConsoleConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the console logging pattern.
     * The pattern defines how log messages are formatted for console output.
     * 
     * @param pattern the pattern string (e.g., "[%level] %date %message%n")
     */
    public static void setConsolePattern(String pattern) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.console.pattern", pattern);
                if (autoApplyConfiguration) {
                    applyConsoleConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Enables or disables file logging.
     * When enabled, log messages will be written to the configured file.
     * 
     * @param enabled true to enable file logging, false to disable
     */
    public static void setFileEnabled(boolean enabled) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.enabled", String.valueOf(enabled));
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the file path for logging.
     * This changes where log messages are written.
     * 
     * @param filePath the path to the log file
     */
    public static void setFilePath(String filePath) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.path", filePath);
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the file logging pattern.
     * The pattern defines how log messages are formatted for file output.
     * 
     * @param pattern the pattern string (e.g., "[%level] %date %message%n")
     */
    public static void setFilePattern(String pattern) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.pattern", pattern);
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the maximum file size for rolling.
     * When the log file reaches this size, it will be rolled over.
     * 
     * @param maxSize the maximum size (e.g., "10M", "100K", "1G")
     */
    public static void setMaxFileSize(String maxSize) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.maxSize", maxSize);
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets the maximum number of backup files.
     * When rolling over, this many backup files will be kept.
     * 
     * @param maxBackups the maximum number of backup files to keep
     */
    public static void setMaxBackups(int maxBackups) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.maxBackups", String.valueOf(maxBackups));
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Sets compression settings for rolled log files.
     * When enabled, rolled log files will be compressed using the specified program.
     * 
     * @param enabled true to enable compression, false to disable
     * @param program the compression program to use (e.g., "gzip", "bzip2")
     * @param args the arguments for the compression program
     */
    public static void setCompression(boolean enabled, String program, String args) {
        configLock.writeLock().lock();
        try {
            if (currentConfig != null) {
                currentConfig.getProperties().setProperty("log4rich.file.compress", String.valueOf(enabled));
                if (program != null) {
                    currentConfig.getProperties().setProperty("log4rich.file.compress.program", program);
                }
                if (args != null) {
                    currentConfig.getProperties().setProperty("log4rich.file.compress.args", args);
                }
                if (autoApplyConfiguration) {
                    applyFileConfiguration();
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Creates a new console appender with the specified settings.
     * The appender will be managed by the framework and can be retrieved by name.
     * 
     * @param name the name of the appender
     * @param target the target stream ("STDOUT" or "STDERR")
     * @param pattern the pattern to use for formatting, or null for default
     * @return the created console appender
     */
    public static ConsoleAppender createConsoleAppender(String name, String target, String pattern) {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setName(name);
        
        if ("STDERR".equalsIgnoreCase(target)) {
            appender.setTarget(ConsoleAppender.Target.STDERR);
        } else {
            appender.setTarget(ConsoleAppender.Target.STDOUT);
        }
        
        if (pattern != null) {
            appender.setLayout(new StandardLayout(pattern));
        }
        
        managedAppenders.put(name, appender);
        return appender;
    }
    
    /**
     * Creates a new rolling file appender with the specified settings.
     * The appender will be managed by the framework and can be retrieved by name.
     * 
     * @param name the name of the appender
     * @param filePath the path to the log file
     * @param maxSize the maximum file size (e.g., "10M", "100K")
     * @param maxBackups the maximum number of backup files
     * @param pattern the pattern to use for formatting, or null for default
     * @return the created rolling file appender
     */
    public static RollingFileAppender createRollingFileAppender(String name, String filePath, 
                                                               String maxSize, int maxBackups, String pattern) {
        RollingFileAppender appender = new RollingFileAppender(filePath);
        appender.setName(name);
        appender.setMaxFileSize(maxSize);
        appender.setMaxBackups(maxBackups);
        
        if (pattern != null) {
            appender.setLayout(new StandardLayout(pattern));
        }
        
        managedAppenders.put(name, appender);
        return appender;
    }
    
    /**
     * Gets a managed appender by name.
     * 
     * @param name the name of the appender
     * @return the appender, or null if not found
     */
    public static Appender getManagedAppender(String name) {
        return managedAppenders.get(name);
    }
    
    /**
     * Removes a managed appender.
     * The appender will be closed and can no longer be used.
     * 
     * @param name the name of the appender to remove
     * @return the removed appender, or null if not found
     */
    public static Appender removeManagedAppender(String name) {
        Appender appender = managedAppenders.remove(name);
        if (appender != null) {
            appender.close();
        }
        return appender;
    }
    
    /**
     * Reloads configuration from the original source.
     * This re-reads the configuration file and applies any changes.
     * 
     * @throws Exception if configuration cannot be reloaded
     */
    public static void reloadConfiguration() throws Exception {
        configLock.writeLock().lock();
        try {
            ConfigLoader.clearCache();
            Configuration newConfig = ConfigLoader.loadConfiguration();
            currentConfig = newConfig;
            applyConfiguration();
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Applies the current configuration to all loggers and appenders.
     * This method is called automatically when configuration changes.
     */
    private static void applyConfiguration() {
        if (currentConfig == null) {
            return;
        }
        
        // Apply to root logger
        LogManager.getRootLogger().setLevel(currentConfig.getRootLevel());
        LogManager.getRootLogger().setLocationCapture(currentConfig.isLocationCapture());
        
        // Apply logger-specific levels
        for (Map.Entry<String, LogLevel> entry : currentConfig.getLoggerLevels().entrySet()) {
            Logger logger = LogManager.getLogger(entry.getKey());
            logger.setLevel(entry.getValue());
        }
        
        // Apply console configuration
        applyConsoleConfiguration();
        
        // Apply file configuration
        applyFileConfiguration();
    }
    
    /**
     * Applies console configuration to the root logger.
     * Removes existing console appenders and creates new ones if enabled.
     */
    private static void applyConsoleConfiguration() {
        if (currentConfig == null) {
            return;
        }
        
        Logger rootLogger = LogManager.getRootLogger();
        
        // Remove existing console appenders
        rootLogger.getAppenders().stream()
            .filter(appender -> appender instanceof ConsoleAppender)
            .forEach(rootLogger::removeAppender);
        
        // Add new console appender if enabled
        if (currentConfig.isConsoleEnabled()) {
            ConsoleAppender consoleAppender = createConsoleAppender(
                "Console",
                currentConfig.getConsoleTarget(),
                currentConfig.getConsolePattern()
            );
            consoleAppender.setLevel(currentConfig.getConsoleLevel());
            rootLogger.addAppender(consoleAppender);
        }
    }
    
    /**
     * Applies file configuration to the root logger.
     * Removes existing file appenders and creates new ones if enabled.
     */
    private static void applyFileConfiguration() {
        if (currentConfig == null) {
            return;
        }
        
        Logger rootLogger = LogManager.getRootLogger();
        
        // Remove existing file appenders
        rootLogger.getAppenders().stream()
            .filter(appender -> appender instanceof RollingFileAppender)
            .forEach(rootLogger::removeAppender);
        
        // Add new file appender if enabled
        if (currentConfig.isFileEnabled()) {
            RollingFileAppender fileAppender = createRollingFileAppender(
                "File",
                currentConfig.getFilePath(),
                currentConfig.getMaxSize(),
                currentConfig.getMaxBackups(),
                currentConfig.getFilePattern()
            );
            fileAppender.setLevel(currentConfig.getFileLevel());
            
            // Configure compression
            if (currentConfig.isCompressionEnabled()) {
                CompressionManager compressionManager = new CompressionManager(
                    currentConfig.getCompressionProgram(),
                    currentConfig.getCompressionArgs(),
                    currentConfig.getLockTimeout()
                );
                fileAppender.setCompressionManager(compressionManager);
            }
            
            // Configure other settings
            fileAppender.setEncoding(Charset.forName(currentConfig.getFileEncoding()));
            fileAppender.setImmediateFlush(currentConfig.isImmediateFlush());
            fileAppender.setBufferSize(currentConfig.getBufferSize());
            fileAppender.setDatePattern(currentConfig.getDatePattern());
            
            rootLogger.addAppender(fileAppender);
        }
    }
    
    /**
     * Sets whether configuration changes should be automatically applied.
     * When disabled, changes must be applied manually using applyCurrentConfiguration().
     * 
     * @param autoApply true to automatically apply configuration changes, false otherwise
     */
    public static void setAutoApplyConfiguration(boolean autoApply) {
        configLock.writeLock().lock();
        try {
            autoApplyConfiguration = autoApply;
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Manually applies the current configuration.
     * This method is useful when auto-apply is disabled.
     */
    public static void applyCurrentConfiguration() {
        configLock.writeLock().lock();
        try {
            applyConfiguration();
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets statistics about the current configuration.
     * Provides information about the current state of the logging system.
     * 
     * @return configuration statistics including logger and appender counts
     */
    public static ConfigurationStats getStats() {
        configLock.readLock().lock();
        try {
            return new ConfigurationStats(
                LogManager.getLoggerNames().length,
                managedAppenders.size(),
                currentConfig != null ? currentConfig.getLoggerLevels().size() : 0
            );
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Shuts down the configuration manager and cleans up all managed appenders.
     * This method closes all managed appenders and clears the configuration.
     */
    public static void shutdown() {
        configLock.writeLock().lock();
        try {
            // Close and clear all managed appenders
            for (Appender appender : managedAppenders.values()) {
                try {
                    appender.close();
                } catch (Exception e) {
                    System.err.println("Error closing appender " + appender.getName() + ": " + e.getMessage());
                }
            }
            managedAppenders.clear();
            currentConfig = null;
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Configuration statistics class.
     * Provides information about the current state of the logging configuration.
     */
    public static class ConfigurationStats {
        private final int loggerCount;
        private final int appenderCount;
        private final int configuredLoggerCount;
        
        /**
         * Creates a new ConfigurationStats instance.
         * 
         * @param loggerCount the total number of loggers
         * @param appenderCount the total number of managed appenders
         * @param configuredLoggerCount the number of loggers with specific levels configured
         */
        public ConfigurationStats(int loggerCount, int appenderCount, int configuredLoggerCount) {
            this.loggerCount = loggerCount;
            this.appenderCount = appenderCount;
            this.configuredLoggerCount = configuredLoggerCount;
        }
        
        /**
         * Gets the total number of loggers.
         * 
         * @return the total number of loggers
         */
        public int getLoggerCount() { return loggerCount; }
        
        /**
         * Gets the total number of managed appenders.
         * 
         * @return the total number of managed appenders
         */
        public int getAppenderCount() { return appenderCount; }
        
        /**
         * Gets the number of loggers with specific levels configured.
         * 
         * @return the number of configured loggers
         */
        public int getConfiguredLoggerCount() { return configuredLoggerCount; }
        
        /**
         * Returns a string representation of the configuration statistics.
         * 
         * @return a formatted string with logger and appender counts
         */
        @Override
        public String toString() {
            return String.format("ConfigurationStats{loggers=%d, appenders=%d, configured=%d}", 
                               loggerCount, appenderCount, configuredLoggerCount);
        }
    }
}