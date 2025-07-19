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
package com.log4rich;

import com.log4rich.appenders.Appender;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.config.Configuration;
import com.log4rich.config.ConfigLoader;
import com.log4rich.config.ConfigurationManager;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;
import com.log4rich.layouts.StandardLayout;

/**
 * Main entry point and facade for the log4Rich logging framework.
 * 
 * This class provides static methods for easy logger access and configuration,
 * supporting ultra-high performance logging with asynchronous compression,
 * adaptive file size management, and advanced I/O optimizations.
 * 
 * <p><strong>Version:</strong> 1.0.0</p>
 * <p><strong>Performance:</strong> Up to 2.3 million messages/second</p>
 * <p><strong>Features:</strong> Async compression, memory-mapped I/O, lock-free ring buffers</p>
 * 
 * @author log4Rich Contributors
 * @since 1.0.0
 * @version 1.0.0
 */
public class Log4Rich {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Log4Rich() {
        // Utility class - no instantiation
    }
    
    private static String configPath = null;
    private static volatile boolean initialized = false;
    
    /**
     * Initializes the logging framework.
     * This method is called automatically when needed and ensures thread-safe initialization.
     * If configuration loading fails, a default configuration is used.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (Log4Rich.class) {
                if (!initialized) {
                    try {
                        Configuration config = ConfigLoader.loadConfiguration();
                        ConfigurationManager.initialize(config);
                        initialized = true;
                    } catch (Exception e) {
                        System.err.println("Failed to initialize log4Rich: " + e.getMessage());
                        // Use default configuration
                        ConfigurationManager.initialize(new Configuration());
                        initialized = true;
                    }
                }
            }
        }
    }
    
    /**
     * Gets a logger for the specified class.
     * This is the preferred method for getting loggers in most cases.
     * 
     * @param clazz the class to get logger for
     * @return logger instance for the specified class
     */
    public static Logger getLogger(Class<?> clazz) {
        ensureInitialized();
        return LogManager.getLogger(clazz);
    }
    
    /**
     * Gets a logger for the specified name.
     * Use this method when you need a logger with a specific name.
     * 
     * @param name the logger name
     * @return logger instance for the specified name
     */
    public static Logger getLogger(String name) {
        ensureInitialized();
        return LogManager.getLogger(name);
    }
    
    /**
     * Gets the root logger.
     * The root logger is the parent of all loggers in the hierarchy.
     * 
     * @return the root logger instance
     */
    public static Logger getRootLogger() {
        ensureInitialized();
        return LogManager.getRootLogger();
    }
    
    // ========== Configuration Management ==========
    
    /**
     * Sets the configuration file path and reloads the configuration.
     * This method allows you to specify a custom configuration file location
     * and immediately load the configuration from that file.
     * 
     * @param path the path to the configuration file, or null to use default search
     * @throws Exception if the configuration cannot be loaded from the specified path
     */
    public static void setConfigPath(String path) throws Exception {
        configPath = path;
        if (path != null) {
            Configuration config = ConfigLoader.loadConfiguration(path);
            ConfigurationManager.initialize(config);
        } else {
            ConfigurationManager.reloadConfiguration();
        }
        initialized = true;
    }
    
    /**
     * Gets the current configuration file path.
     * 
     * @return the configuration file path, or null if not set
     */
    public static String getConfigPath() {
        return configPath;
    }
    
    /**
     * Sets the root logger level.
     * This affects all loggers that don't have specific levels configured.
     * Changes take effect immediately.
     * 
     * @param level the new root level to set
     */
    public static void setRootLevel(LogLevel level) {
        ensureInitialized();
        ConfigurationManager.setRootLevel(level);
    }
    
    /**
     * Sets the location capture setting globally.
     * When enabled, the framework will capture and include class name,
     * method name, and line number information in log events.
     * 
     * @param enabled true to enable location capture, false to disable
     */
    public static void setLocationCapture(boolean enabled) {
        ensureInitialized();
        ConfigurationManager.setLocationCapture(enabled);
    }
    
    /**
     * Sets a specific logger's level.
     * This overrides the root level for the specified logger.
     * 
     * @param loggerName the name of the logger to configure
     * @param level the level to set for this logger
     */
    public static void setLoggerLevel(String loggerName, LogLevel level) {
        ensureInitialized();
        ConfigurationManager.setLoggerLevel(loggerName, level);
    }
    
    /**
     * Enables or disables console logging.
     * When enabled, log messages will be written to the console.
     * 
     * @param enabled true to enable console logging, false to disable
     */
    public static void setConsoleEnabled(boolean enabled) {
        ensureInitialized();
        ConfigurationManager.setConsoleEnabled(enabled);
    }
    
    /**
     * Sets the console target stream.
     * 
     * @param target the target stream ("STDOUT" or "STDERR")
     */
    public static void setConsoleTarget(String target) {
        ensureInitialized();
        ConfigurationManager.setConsoleTarget(target);
    }
    
    /**
     * Sets the console logging pattern.
     * The pattern defines how log messages are formatted for console output.
     * 
     * @param pattern the pattern string (e.g., "[%level] %date %message%n")
     */
    public static void setConsolePattern(String pattern) {
        ensureInitialized();
        ConfigurationManager.setConsolePattern(pattern);
    }
    
    /**
     * Enables or disables file logging.
     * When enabled, log messages will be written to the configured file.
     * 
     * @param enabled true to enable file logging, false to disable
     */
    public static void setFileEnabled(boolean enabled) {
        ensureInitialized();
        ConfigurationManager.setFileEnabled(enabled);
    }
    
    /**
     * Sets the file path for logging.
     * This changes where log messages are written.
     * 
     * @param filePath the path to the log file
     */
    public static void setFilePath(String filePath) {
        ensureInitialized();
        ConfigurationManager.setFilePath(filePath);
    }
    
    /**
     * Sets the file logging pattern.
     * The pattern defines how log messages are formatted for file output.
     * 
     * @param pattern the pattern string (e.g., "[%level] %date %message%n")
     */
    public static void setFilePattern(String pattern) {
        ensureInitialized();
        ConfigurationManager.setFilePattern(pattern);
    }
    
    /**
     * Sets the maximum file size for rolling.
     * When the log file reaches this size, it will be rolled over.
     * 
     * @param maxSize the maximum size (e.g., "10M", "100K", "1G")
     */
    public static void setMaxFileSize(String maxSize) {
        ensureInitialized();
        ConfigurationManager.setMaxFileSize(maxSize);
    }
    
    /**
     * Sets the maximum number of backup files.
     * When rolling over, this many backup files will be kept.
     * 
     * @param maxBackups the maximum number of backup files to keep
     */
    public static void setMaxBackups(int maxBackups) {
        ensureInitialized();
        ConfigurationManager.setMaxBackups(maxBackups);
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
        ensureInitialized();
        ConfigurationManager.setCompression(enabled, program, args);
    }
    
    /**
     * Creates a new console appender.
     * The appender will be managed by the framework and can be retrieved by name.
     * 
     * @param name the name of the appender
     * @param target the target stream ("STDOUT" or "STDERR")
     * @param pattern the pattern to use for formatting, or null for default
     * @return the created console appender
     */
    public static ConsoleAppender createConsoleAppender(String name, String target, String pattern) {
        ensureInitialized();
        return ConfigurationManager.createConsoleAppender(name, target, pattern);
    }
    
    /**
     * Creates a new rolling file appender.
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
        ensureInitialized();
        return ConfigurationManager.createRollingFileAppender(name, filePath, maxSize, maxBackups, pattern);
    }
    
    /**
     * Creates a simple rolling file appender with default settings.
     * Uses default settings: 10MB max size, 10 backup files, default pattern.
     * 
     * @param filePath the path to the log file
     * @return the created rolling file appender
     */
    public static RollingFileAppender createRollingFileAppender(String filePath) {
        ensureInitialized();
        return ConfigurationManager.createRollingFileAppender(
            "RollingFile-" + System.currentTimeMillis(),
            filePath,
            "10M",
            10,
            null
        );
    }
    
    /**
     * Gets a managed appender by name.
     * 
     * @param name the name of the appender
     * @return the appender, or null if not found
     */
    public static Appender getManagedAppender(String name) {
        ensureInitialized();
        return ConfigurationManager.getManagedAppender(name);
    }
    
    /**
     * Removes a managed appender.
     * The appender will be closed and can no longer be used.
     * 
     * @param name the name of the appender to remove
     * @return the removed appender, or null if not found
     */
    public static Appender removeManagedAppender(String name) {
        ensureInitialized();
        return ConfigurationManager.removeManagedAppender(name);
    }
    
    /**
     * Reloads configuration from the original source.
     * This re-reads the configuration file and applies any changes.
     * 
     * @throws Exception if configuration cannot be reloaded
     */
    public static void reloadConfiguration() throws Exception {
        if (configPath != null) {
            Configuration config = ConfigLoader.loadConfiguration(configPath);
            ConfigurationManager.initialize(config);
        } else {
            ConfigurationManager.reloadConfiguration();
        }
    }
    
    /**
     * Gets the current configuration.
     * 
     * @return the current configuration object
     */
    public static Configuration getCurrentConfiguration() {
        ensureInitialized();
        return ConfigurationManager.getCurrentConfiguration();
    }
    
    /**
     * Gets configuration statistics.
     * Provides information about the current state of the logging system.
     * 
     * @return configuration statistics including logger and appender counts
     */
    public static ConfigurationManager.ConfigurationStats getStats() {
        ensureInitialized();
        return ConfigurationManager.getStats();
    }
    
    /**
     * Shuts down the logging framework.
     * This closes all loggers and their appenders, ensuring proper resource cleanup.
     * After shutdown, the framework can be reinitialized on the next logging call.
     */
    public static void shutdown() {
        LogManager.shutdown();
        ConfigurationManager.shutdown();
        initialized = false;
    }
    
    /**
     * Gets the current version of the log4Rich framework.
     * 
     * @return the version string (e.g., "1.0.0")
     */
    public static String getVersion() {
        return Version.getVersion();
    }
    
    /**
     * Gets the full version information including build details.
     * 
     * @return comprehensive version information
     */
    public static String getVersionInfo() {
        return Version.getVersionInfo();
    }
    
    /**
     * Gets a compact banner suitable for application startup.
     * 
     * @return compact version banner
     */
    public static String getBanner() {
        return Version.getBanner();
    }
    
    /**
     * Prints the framework banner to standard output.
     * Useful for application startup logging.
     */
    public static void printBanner() {
        Version.printBanner();
    }
    
    /**
     * Checks if the current Java version meets the minimum requirements.
     * 
     * @return true if Java version is compatible, false otherwise
     */
    public static boolean isJavaVersionCompatible() {
        return Version.isJavaVersionCompatible();
    }
    
    /**
     * Main method for testing, demonstration, and version checking.
     * 
     * @param args command line arguments:
     *             --version or -v: show version only
     *             --banner or -b: show banner only
     *             --info: show full version information
     *             (no args): run demonstration
     */
    public static void main(String[] args) {
        // Handle version checking arguments
        if (args.length > 0) {
            if ("--version".equals(args[0]) || "-v".equals(args[0])) {
                System.out.println(getVersion());
                return;
            } else if ("--banner".equals(args[0]) || "-b".equals(args[0])) {
                printBanner();
                return;
            } else if ("--info".equals(args[0])) {
                System.out.println(getVersionInfo());
                return;
            }
        }
        
        // Print banner at startup
        printBanner();
        System.out.println();
        // Create a sample logger
        Logger logger = Log4Rich.getLogger(Log4Rich.class);
        
        // Add a console appender with custom layout
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(new StandardLayout("[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message%n"));
        logger.addAppender(consoleAppender);
        
        // Set log level to DEBUG for demonstration
        logger.setLevel(LogLevel.DEBUG);
        
        // Test various log levels
        logger.trace("This is a TRACE message");
        logger.debug("This is a DEBUG message");
        logger.info("This is an INFO message");
        logger.warn("This is a WARN message");
        logger.error("This is an ERROR message");
        logger.fatal("This is a FATAL message");
        
        // Test with exception
        try {
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            logger.error("Caught an exception", e);
        }
        
        // Test level checking
        if (logger.isDebugEnabled()) {
            logger.debug("Debug logging is enabled");
        }
        
        System.out.println("\nlog4Rich demonstration complete!");
        
        // Shutdown
        Log4Rich.shutdown();
    }
}