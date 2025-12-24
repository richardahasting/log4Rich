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

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Validates log4Rich configuration settings and provides detailed error messages.
 * This class helps users identify and fix configuration issues with specific guidance.
 */
public final class ConfigurationValidator {
    
    private ConfigurationValidator() {
        // Utility class
    }
    
    /**
     * Validates configuration and returns a list of validation errors.
     * Each error includes the property name, invalid value, and specific guidance.
     * 
     * @param properties the configuration properties to validate
     * @return list of validation errors (empty if all valid)
     */
    public static List<ConfigurationError> validate(Properties properties) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Validate log levels
        validateLogLevel(properties, "log4rich.rootLevel", errors);
        validateLogLevel(properties, "log4rich.console.level", errors);
        validateLogLevel(properties, "log4rich.file.level", errors);
        
        // Validate boolean properties
        validateBoolean(properties, "log4rich.console.enabled", errors);
        validateBoolean(properties, "log4rich.file.enabled", errors);
        validateBoolean(properties, "log4rich.file.compress", errors);
        validateBoolean(properties, "log4rich.file.immediateFlush", errors);
        validateBoolean(properties, "log4rich.location.capture", errors);
        validateBoolean(properties, "log4rich.performance.memoryMapped", errors);
        validateBoolean(properties, "log4rich.performance.batchEnabled", errors);
        validateBoolean(properties, "log4rich.performance.zeroAllocation", errors);
        validateBoolean(properties, "log4rich.async.enabled", errors);
        
        // Validate console target
        validateConsoleTarget(properties, errors);
        
        // Validate file path
        validateFilePath(properties, errors);
        
        // Validate size properties
        validateSize(properties, "log4rich.file.maxSize", errors);
        validateSize(properties, "log4rich.performance.mappedSize", errors);
        
        // Validate numeric properties
        validateInteger(properties, "log4rich.file.maxBackups", 0, 1000, errors);
        validateInteger(properties, "log4rich.file.bufferSize", 1024, 1024 * 1024, errors);
        validateInteger(properties, "log4rich.performance.batchSize", 1, 100000, errors);
        validateInteger(properties, "log4rich.performance.stringBuilderCapacity", 64, 64 * 1024, errors);
        validateInteger(properties, "log4rich.async.bufferSize", 1024, 1024 * 1024, errors);
        validateInteger(properties, "log4rich.async.threadPriority", Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, errors);
        
        validateLong(properties, "log4rich.thread.lockTimeout", 100L, 60000L, errors);
        validateLong(properties, "log4rich.performance.batchTimeMs", 1L, 10000L, errors);
        validateLong(properties, "log4rich.performance.forceInterval", 100L, 300000L, errors);
        validateLong(properties, "log4rich.async.shutdownTimeout", 1000L, 60000L, errors);
        
        // Validate encoding
        validateEncoding(properties, errors);
        
        // Validate compression program
        validateCompressionProgram(properties, errors);
        
        // Validate overflow strategy
        validateOverflowStrategy(properties, errors);
        
        // Validate logger-specific levels
        validateLoggerLevels(properties, errors);
        
        // Validate async buffer size is power of 2
        validateAsyncBufferSize(properties, errors);
        
        return errors;
    }
    
    private static void validateLogLevel(Properties properties, String key, List<ConfigurationError> errors) {
        String value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim().toUpperCase();
            boolean valid = false;
            for (LogLevel level : LogLevel.values()) {
                if (level.name().equals(trimmed)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                errors.add(new ConfigurationError(
                    key,
                    value,
                    "Invalid log level. Valid levels are: " + Arrays.toString(LogLevel.values()) + 
                    ". Level names are case-insensitive.",
                    "Use one of: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF"
                ));
            }
        }
    }
    
    private static void validateBoolean(Properties properties, String key, List<ConfigurationError> errors) {
        String value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim().toLowerCase();
            if (!trimmed.equals("true") && !trimmed.equals("false")) {
                errors.add(new ConfigurationError(
                    key,
                    value,
                    "Invalid boolean value. Must be 'true' or 'false' (case-insensitive).",
                    "Change to: " + key + "=true or " + key + "=false"
                ));
            }
        }
    }
    
    private static void validateConsoleTarget(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.console.target");
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim().toUpperCase();
            if (!trimmed.equals("STDOUT") && !trimmed.equals("STDERR")) {
                errors.add(new ConfigurationError(
                    "log4rich.console.target",
                    value,
                    "Invalid console target. Must be 'STDOUT' or 'STDERR' (case-insensitive).",
                    "Change to: log4rich.console.target=STDOUT or log4rich.console.target=STDERR"
                ));
            }
        }
    }
    
    private static void validateFilePath(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.file.path");
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim();
            File file = new File(trimmed);
            File parentDir = file.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                errors.add(new ConfigurationError(
                    "log4rich.file.path",
                    value,
                    "Parent directory does not exist: " + parentDir.getAbsolutePath(),
                    "Create the directory first: mkdir -p \"" + parentDir.getAbsolutePath() + "\""
                ));
            }
            
            // Check for invalid characters in filename
            String filename = file.getName();
            if (filename.contains("*") || filename.contains("?") || filename.contains("<") || 
                filename.contains(">") || filename.contains("|")) {
                errors.add(new ConfigurationError(
                    "log4rich.file.path",
                    value,
                    "Invalid characters in filename. Avoid: * ? < > |",
                    "Use a simple filename like: logs/application.log"
                ));
            }
        }
    }
    
    private static void validateSize(Properties properties, String key, List<ConfigurationError> errors) {
        String value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                parseSize(value);
            } catch (Exception e) {
                errors.add(new ConfigurationError(
                    key,
                    value,
                    "Invalid size format. Use format like: 10M, 500K, 1G, or plain numbers for bytes.",
                    "Examples: " + key + "=10M (10 megabytes), " + key + "=500K (500 kilobytes), " + key + "=1G (1 gigabyte)"
                ));
            }
        }
    }
    
    private static void validateInteger(Properties properties, String key, int min, int max, List<ConfigurationError> errors) {
        String value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                int intValue = Integer.parseInt(value.trim());
                if (intValue < min || intValue > max) {
                    errors.add(new ConfigurationError(
                        key,
                        value,
                        "Value out of range. Must be between " + min + " and " + max + " (inclusive).",
                        "Change to a value between " + min + " and " + max + ", for example: " + key + "=" + 
                        (min + (max - min) / 2)
                    ));
                }
            } catch (NumberFormatException e) {
                errors.add(new ConfigurationError(
                    key,
                    value,
                    "Invalid integer value. Must be a whole number between " + min + " and " + max + ".",
                    "Change to a number like: " + key + "=" + (min + (max - min) / 2)
                ));
            }
        }
    }
    
    private static void validateLong(Properties properties, String key, long min, long max, List<ConfigurationError> errors) {
        String value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                long longValue = Long.parseLong(value.trim());
                if (longValue < min || longValue > max) {
                    errors.add(new ConfigurationError(
                        key,
                        value,
                        "Value out of range. Must be between " + min + " and " + max + " (inclusive).",
                        "Change to a value between " + min + " and " + max + ", for example: " + key + "=" + 
                        (min + (max - min) / 2)
                    ));
                }
            } catch (NumberFormatException e) {
                errors.add(new ConfigurationError(
                    key,
                    value,
                    "Invalid number value. Must be a whole number between " + min + " and " + max + ".",
                    "Change to a number like: " + key + "=" + (min + (max - min) / 2)
                ));
            }
        }
    }
    
    private static void validateEncoding(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.file.encoding");
        if (value != null && !value.trim().isEmpty()) {
            try {
                Charset.forName(value.trim());
            } catch (Exception e) {
                errors.add(new ConfigurationError(
                    "log4rich.file.encoding",
                    value,
                    "Invalid character encoding. Common encodings: UTF-8, ISO-8859-1, US-ASCII, UTF-16.",
                    "Use a standard encoding like: log4rich.file.encoding=UTF-8"
                ));
            }
        }
    }
    
    private static void validateCompressionProgram(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.file.compress.program");
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim().toLowerCase();
            if (!trimmed.equals("gzip") && !trimmed.equals("bzip2") && !trimmed.equals("xz")) {
                errors.add(new ConfigurationError(
                    "log4rich.file.compress.program",
                    value,
                    "Unsupported compression program. Supported programs: gzip, bzip2, xz.",
                    "Use a supported program: log4rich.file.compress.program=gzip"
                ));
            }
        }
    }
    
    private static void validateOverflowStrategy(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.async.overflowStrategy");
        if (value != null && !value.trim().isEmpty()) {
            String trimmed = value.trim().toUpperCase();
            if (!trimmed.equals("DROP_OLDEST") && !trimmed.equals("BLOCK") && !trimmed.equals("DISCARD")) {
                errors.add(new ConfigurationError(
                    "log4rich.async.overflowStrategy",
                    value,
                    "Invalid overflow strategy. Valid strategies: DROP_OLDEST, BLOCK, DISCARD.",
                    "Use: log4rich.async.overflowStrategy=DROP_OLDEST (recommended for high throughput)"
                ));
            }
        }
    }
    
    private static void validateLoggerLevels(Properties properties, List<ConfigurationError> errors) {
        String prefix = "log4rich.logger.";
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String loggerName = key.substring(prefix.length());
                if (loggerName.trim().isEmpty()) {
                    errors.add(new ConfigurationError(
                        key,
                        properties.getProperty(key),
                        "Empty logger name. Logger names must not be empty after 'log4rich.logger.' prefix.",
                        "Use format: log4rich.logger.com.example.MyClass=DEBUG"
                    ));
                } else {
                    validateLogLevel(properties, key, errors);
                }
            }
        }
    }
    
    private static void validateAsyncBufferSize(Properties properties, List<ConfigurationError> errors) {
        String value = properties.getProperty("log4rich.async.bufferSize");
        if (value != null && !value.trim().isEmpty()) {
            try {
                int size = Integer.parseInt(value.trim());
                if (size > 0 && (size & (size - 1)) != 0) {
                    // Find next power of 2
                    int nextPowerOf2 = 1;
                    while (nextPowerOf2 < size) {
                        nextPowerOf2 <<= 1;
                    }
                    errors.add(new ConfigurationError(
                        "log4rich.async.bufferSize",
                        value,
                        "Async buffer size must be a power of 2 for optimal performance.",
                        "Use a power of 2 like: log4rich.async.bufferSize=" + nextPowerOf2 + 
                        " (valid sizes: 1024, 2048, 4096, 8192, 16384, 32768, 65536, etc.)"
                    ));
                }
            } catch (NumberFormatException e) {
                // This will be caught by the integer validation
            }
        }
    }
    
    /**
     * Parses a size string (e.g., "10M", "64MB", "1G") to bytes.
     * 
     * @param sizeStr the size string to parse
     * @return the size in bytes
     * @throws IllegalArgumentException if the format is invalid
     */
    private static long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Size string cannot be null or empty");
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
            long value = Long.parseLong(str.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("Size must be positive");
            }
            return value * multiplier;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size format: " + sizeStr);
        }
    }
    
    /**
     * Represents a configuration validation error with detailed information.
     */
    public static class ConfigurationError {
        private final String propertyName;
        private final String invalidValue;
        private final String errorMessage;
        private final String suggestion;
        
        public ConfigurationError(String propertyName, String invalidValue, String errorMessage, String suggestion) {
            this.propertyName = propertyName;
            this.invalidValue = invalidValue;
            this.errorMessage = errorMessage;
            this.suggestion = suggestion;
        }
        
        public String getPropertyName() { return propertyName; }
        public String getInvalidValue() { return invalidValue; }
        public String getErrorMessage() { return errorMessage; }
        public String getSuggestion() { return suggestion; }
        
        @Override
        public String toString() {
            return String.format("Configuration error in '%s' with value '%s': %s\nSuggestion: %s",
                propertyName, invalidValue, errorMessage, suggestion);
        }
        
        /**
         * Returns a formatted error message suitable for console output.
         */
        public String getFormattedMessage() {
            return String.format("ERROR: %s=%s\n  Problem: %s\n  Solution: %s",
                propertyName, invalidValue, errorMessage, suggestion);
        }
    }
}