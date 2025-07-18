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

import com.log4rich.core.LogLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration management for log4Rich.
 * Holds all configuration settings loaded from the config file.
 * This class provides type-safe access to configuration properties
 * and maintains default values for all settings.
 */
public class Configuration {
    
    // Default values
    private static final LogLevel DEFAULT_ROOT_LEVEL = LogLevel.INFO;
    private static final boolean DEFAULT_CONSOLE_ENABLED = true;
    private static final boolean DEFAULT_FILE_ENABLED = true;
    private static final String DEFAULT_CONSOLE_TARGET = "STDOUT";
    private static final String DEFAULT_CONSOLE_PATTERN = "[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message%n";
    private static final String DEFAULT_FILE_PATH = "logs/application.log";
    private static final String DEFAULT_FILE_PATTERN = "[%level] %date{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %class.%method:%line - %message%n";
    private static final String DEFAULT_MAX_SIZE = "10M";
    private static final int DEFAULT_MAX_BACKUPS = 10;
    private static final boolean DEFAULT_COMPRESSION = true;
    private static final String DEFAULT_COMPRESSION_PROGRAM = "gzip";
    private static final String DEFAULT_COMPRESSION_ARGS = "";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_IMMEDIATE_FLUSH = true;
    private static final boolean DEFAULT_LOCATION_CAPTURE = true;
    private static final long DEFAULT_LOCK_TIMEOUT = 5000;
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    private static final boolean DEFAULT_TRUNCATE_LOGGER_NAMES = false;
    private static final int DEFAULT_MAX_LOGGER_NAME_LENGTH = 30;
    private static final String DEFAULT_CLASS_FORMAT = "SIMPLE";
    
    // Performance enhancement defaults
    private static final boolean DEFAULT_MEMORY_MAPPED = false;
    private static final long DEFAULT_MAPPED_SIZE = 64L * 1024 * 1024; // 64MB
    private static final boolean DEFAULT_FORCE_ON_WRITE = false;
    private static final long DEFAULT_FORCE_INTERVAL = 1000; // 1 second
    private static final boolean DEFAULT_BATCH_ENABLED = false;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final long DEFAULT_BATCH_TIME_MS = 100;
    private static final boolean DEFAULT_ZERO_ALLOCATION = false;
    private static final int DEFAULT_STRING_BUILDER_CAPACITY = 1024;
    
    private final Properties properties;
    private final Map<String, LogLevel> loggerLevels;
    
    /**
     * Creates a new Configuration with default settings.
     * All properties are initialized to their default values.
     */
    public Configuration() {
        this.properties = new Properties();
        this.loggerLevels = new HashMap<>();
        setDefaults();
    }
    
    /**
     * Creates a new Configuration with the specified properties.
     * Default values are set first, then overridden with provided properties.
     * 
     * @param properties the properties to use for configuration
     */
    public Configuration(Properties properties) {
        this.properties = new Properties();
        this.loggerLevels = new HashMap<>();
        setDefaults();
        // Override defaults with provided properties
        this.properties.putAll(properties);
        loadLoggerLevels();
    }
    
    /**
     * Sets all default configuration values.
     * This method populates the properties with sensible defaults.
     */
    private void setDefaults() {
        properties.setProperty("log4rich.rootLevel", DEFAULT_ROOT_LEVEL.name());
        properties.setProperty("log4rich.console.enabled", String.valueOf(DEFAULT_CONSOLE_ENABLED));
        properties.setProperty("log4rich.file.enabled", String.valueOf(DEFAULT_FILE_ENABLED));
        properties.setProperty("log4rich.console.target", DEFAULT_CONSOLE_TARGET);
        properties.setProperty("log4rich.console.pattern", DEFAULT_CONSOLE_PATTERN);
        properties.setProperty("log4rich.file.path", DEFAULT_FILE_PATH);
        properties.setProperty("log4rich.file.pattern", DEFAULT_FILE_PATTERN);
        properties.setProperty("log4rich.file.maxSize", DEFAULT_MAX_SIZE);
        properties.setProperty("log4rich.file.maxBackups", String.valueOf(DEFAULT_MAX_BACKUPS));
        properties.setProperty("log4rich.file.compress", String.valueOf(DEFAULT_COMPRESSION));
        properties.setProperty("log4rich.file.compress.program", DEFAULT_COMPRESSION_PROGRAM);
        properties.setProperty("log4rich.file.compress.args", DEFAULT_COMPRESSION_ARGS);
        properties.setProperty("log4rich.file.encoding", DEFAULT_ENCODING);
        properties.setProperty("log4rich.file.bufferSize", String.valueOf(DEFAULT_BUFFER_SIZE));
        properties.setProperty("log4rich.file.immediateFlush", String.valueOf(DEFAULT_IMMEDIATE_FLUSH));
        properties.setProperty("log4rich.location.capture", String.valueOf(DEFAULT_LOCATION_CAPTURE));
        properties.setProperty("log4rich.thread.lockTimeout", String.valueOf(DEFAULT_LOCK_TIMEOUT));
        properties.setProperty("log4rich.file.datePattern", DEFAULT_DATE_PATTERN);
        properties.setProperty("log4rich.truncateLoggerNames", String.valueOf(DEFAULT_TRUNCATE_LOGGER_NAMES));
        properties.setProperty("log4rich.maxLoggerNameLength", String.valueOf(DEFAULT_MAX_LOGGER_NAME_LENGTH));
        properties.setProperty("log4rich.class.format", DEFAULT_CLASS_FORMAT);
        
        // Performance enhancement defaults
        properties.setProperty("log4rich.performance.memoryMapped", String.valueOf(DEFAULT_MEMORY_MAPPED));
        properties.setProperty("log4rich.performance.mappedSize", String.valueOf(DEFAULT_MAPPED_SIZE));
        properties.setProperty("log4rich.performance.forceOnWrite", String.valueOf(DEFAULT_FORCE_ON_WRITE));
        properties.setProperty("log4rich.performance.forceInterval", String.valueOf(DEFAULT_FORCE_INTERVAL));
        properties.setProperty("log4rich.performance.batchEnabled", String.valueOf(DEFAULT_BATCH_ENABLED));
        properties.setProperty("log4rich.performance.batchSize", String.valueOf(DEFAULT_BATCH_SIZE));
        properties.setProperty("log4rich.performance.batchTimeMs", String.valueOf(DEFAULT_BATCH_TIME_MS));
        properties.setProperty("log4rich.performance.zeroAllocation", String.valueOf(DEFAULT_ZERO_ALLOCATION));
        properties.setProperty("log4rich.performance.stringBuilderCapacity", String.valueOf(DEFAULT_STRING_BUILDER_CAPACITY));
    }
    
    /**
     * Loads logger-specific levels from the properties.
     * Processes all properties with the "log4rich.logger." prefix.
     */
    private void loadLoggerLevels() {
        String prefix = "log4rich.logger.";
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String loggerName = key.substring(prefix.length());
                String levelStr = properties.getProperty(key);
                LogLevel level = LogLevel.fromString(levelStr);
                loggerLevels.put(loggerName, level);
            }
        }
    }
    
    // Getters with type conversion
    /**
     * Gets the root logger level.
     * 
     * @return the root logger level
     */
    public LogLevel getRootLevel() {
        return LogLevel.fromString(properties.getProperty("log4rich.rootLevel"));
    }
    
    /**
     * Checks if console logging is enabled.
     * 
     * @return true if console logging is enabled, false otherwise
     */
    public boolean isConsoleEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.console.enabled"));
    }
    
    /**
     * Checks if file logging is enabled.
     * 
     * @return true if file logging is enabled, false otherwise
     */
    public boolean isFileEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.file.enabled"));
    }
    
    /**
     * Gets the console target stream (STDOUT or STDERR).
     * 
     * @return the console target stream
     */
    public String getConsoleTarget() {
        return properties.getProperty("log4rich.console.target");
    }
    
    /**
     * Gets the console log level, falling back to root level if not set.
     * 
     * @return the console log level
     */
    public LogLevel getConsoleLevel() {
        String level = properties.getProperty("log4rich.console.level");
        return (level == null || level.trim().isEmpty()) ? getRootLevel() : LogLevel.fromString(level);
    }
    
    /**
     * Gets the console log pattern for formatting.
     * 
     * @return the console log pattern
     */
    public String getConsolePattern() {
        return properties.getProperty("log4rich.console.pattern");
    }
    
    /**
     * Gets the file path for logging.
     * 
     * @return the file path
     */
    public String getFilePath() {
        return properties.getProperty("log4rich.file.path");
    }
    
    /**
     * Gets the file log level, falling back to root level if not set.
     * 
     * @return the file log level
     */
    public LogLevel getFileLevel() {
        String level = properties.getProperty("log4rich.file.level");
        return (level == null || level.trim().isEmpty()) ? getRootLevel() : LogLevel.fromString(level);
    }
    
    /**
     * Gets the file log pattern for formatting.
     * 
     * @return the file log pattern
     */
    public String getFilePattern() {
        return properties.getProperty("log4rich.file.pattern");
    }
    
    /**
     * Gets the maximum file size before rolling over.
     * 
     * @return the maximum file size (e.g., "10M", "100K")
     */
    public String getMaxSize() {
        return properties.getProperty("log4rich.file.maxSize");
    }
    
    /**
     * Gets the maximum number of backup files to keep.
     * 
     * @return the maximum number of backup files
     */
    public int getMaxBackups() {
        return Integer.parseInt(properties.getProperty("log4rich.file.maxBackups"));
    }
    
    /**
     * Checks if compression is enabled for rolled files.
     * 
     * @return true if compression is enabled, false otherwise
     */
    public boolean isCompressionEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.file.compress"));
    }
    
    /**
     * Gets the compression program to use for rolled files.
     * 
     * @return the compression program name (e.g., "gzip", "bzip2")
     */
    public String getCompressionProgram() {
        return properties.getProperty("log4rich.file.compress.program");
    }
    
    /**
     * Gets the arguments to pass to the compression program.
     * 
     * @return the compression program arguments
     */
    public String getCompressionArgs() {
        return properties.getProperty("log4rich.file.compress.args");
    }
    
    /**
     * Gets the file encoding to use for log files.
     * 
     * @return the file encoding (e.g., "UTF-8", "ISO-8859-1")
     */
    public String getFileEncoding() {
        return properties.getProperty("log4rich.file.encoding");
    }
    
    /**
     * Gets the buffer size for file I/O operations.
     * 
     * @return the buffer size in bytes
     */
    public int getBufferSize() {
        return Integer.parseInt(properties.getProperty("log4rich.file.bufferSize"));
    }
    
    /**
     * Checks if immediate flush is enabled for file operations.
     * 
     * @return true if immediate flush is enabled, false otherwise
     */
    public boolean isImmediateFlush() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.file.immediateFlush"));
    }
    
    /**
     * Checks if location capture is enabled (class, method, line number).
     * 
     * @return true if location capture is enabled, false otherwise
     */
    public boolean isLocationCapture() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.location.capture"));
    }
    
    /**
     * Gets the lock timeout for thread synchronization.
     * 
     * @return the lock timeout in milliseconds
     */
    public long getLockTimeout() {
        return Long.parseLong(properties.getProperty("log4rich.thread.lockTimeout"));
    }
    
    /**
     * Gets the date pattern used for backup file naming.
     * 
     * @return the date pattern (e.g., "yyyy-MM-dd-HH-mm-ss")
     */
    public String getDatePattern() {
        return properties.getProperty("log4rich.file.datePattern");
    }
    
    /**
     * Checks if logger names should be truncated for display.
     * 
     * @return true if logger names should be truncated, false otherwise
     */
    public boolean isTruncateLoggerNames() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.truncateLoggerNames"));
    }
    
    /**
     * Gets the maximum length for logger names when truncation is enabled.
     * 
     * @return the maximum logger name length
     */
    public int getMaxLoggerNameLength() {
        return Integer.parseInt(properties.getProperty("log4rich.maxLoggerNameLength"));
    }
    
    /**
     * Gets the class name format for display (SIMPLE, FULL, etc.).
     * 
     * @return the class name format
     */
    public String getClassFormat() {
        return properties.getProperty("log4rich.class.format");
    }
    
    /**
     * Gets a copy of all logger-specific levels.
     * 
     * @return a map of logger names to their specific levels
     */
    public Map<String, LogLevel> getLoggerLevels() {
        return new HashMap<>(loggerLevels);
    }
    
    /**
     * Gets the specific level for a logger, or null if not set.
     * 
     * @param loggerName the name of the logger
     * @return the specific level for the logger, or null if not set
     */
    public LogLevel getLoggerLevel(String loggerName) {
        return loggerLevels.get(loggerName);
    }
    
    /**
     * Sets the specific level for a logger.
     * 
     * @param loggerName the name of the logger
     * @param level the level to set for the logger
     */
    public void setLoggerLevel(String loggerName, LogLevel level) {
        loggerLevels.put(loggerName, level);
    }
    
    // Generic property access
    /**
     * Gets a property value by key.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets a property value by key with a default value.
     * 
     * @param key the property key
     * @param defaultValue the default value if the key is not found
     * @return the property value, or the default value if not found
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    // Performance enhancement configuration methods
    
    /**
     * Checks if memory-mapped file I/O is enabled.
     * 
     * @return true if memory-mapped files should be used
     */
    public boolean isMemoryMappedEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.performance.memoryMapped"));
    }
    
    /**
     * Gets the initial size for memory-mapped file regions.
     * 
     * @return the mapped size in bytes
     */
    public long getMappedSize() {
        String sizeStr = properties.getProperty("log4rich.performance.mappedSize");
        if (sizeStr == null) {
            return DEFAULT_MAPPED_SIZE;
        }
        return parseSize(sizeStr);
    }
    
    /**
     * Checks if force-on-write is enabled for memory-mapped files.
     * 
     * @return true if every write should be forced to disk
     */
    public boolean isForceOnWriteEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.performance.forceOnWrite"));
    }
    
    /**
     * Gets the force interval for memory-mapped files.
     * 
     * @return the force interval in milliseconds
     */
    public long getForceInterval() {
        return Long.parseLong(properties.getProperty("log4rich.performance.forceInterval"));
    }
    
    /**
     * Checks if batch processing is enabled for file writes.
     * 
     * @return true if batch processing should be used
     */
    public boolean isBatchEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.performance.batchEnabled"));
    }
    
    /**
     * Gets the batch size for batch processing.
     * 
     * @return the maximum number of events per batch
     */
    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty("log4rich.performance.batchSize"));
    }
    
    /**
     * Gets the batch time limit for batch processing.
     * 
     * @return the maximum time to wait before flushing batch (milliseconds)
     */
    public long getBatchTimeMs() {
        return Long.parseLong(properties.getProperty("log4rich.performance.batchTimeMs"));
    }
    
    /**
     * Checks if zero-allocation logging is enabled.
     * 
     * @return true if object pooling should be used
     */
    public boolean isZeroAllocationEnabled() {
        return Boolean.parseBoolean(properties.getProperty("log4rich.performance.zeroAllocation"));
    }
    
    /**
     * Gets the initial StringBuilder capacity for object pooling.
     * 
     * @return the StringBuilder capacity in characters
     */
    public int getStringBuilderCapacity() {
        return Integer.parseInt(properties.getProperty("log4rich.performance.stringBuilderCapacity"));
    }
    
    /**
     * Parses a size string (e.g., "10M", "64MB", "1G") to bytes.
     * 
     * @param sizeStr the size string to parse
     * @return the size in bytes
     */
    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return DEFAULT_MAPPED_SIZE;
        }
        
        String str = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        
        if (str.endsWith("K") || str.endsWith("KB")) {
            multiplier = 1024L;
            str = str.replaceAll("KB?$", "");
        } else if (str.endsWith("M") || str.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            str = str.replaceAll("MB?$", "");
        } else if (str.endsWith("G") || str.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            str = str.replaceAll("GB?$", "");
        }
        
        try {
            return Long.parseLong(str.trim()) * multiplier;
        } catch (NumberFormatException e) {
            System.err.println("Invalid size format: " + sizeStr + ", using default");
            return DEFAULT_MAPPED_SIZE;
        }
    }
    
    /**
     * Gets the underlying Properties object.
     * This returns the actual properties object, not a copy.
     * 
     * @return the Properties object containing all configuration
     */
    public Properties getProperties() {
        return properties;
    }
}