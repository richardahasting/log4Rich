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

import java.io.*;
import java.util.Properties;
import com.log4rich.util.Java8Utils;

/**
 * Loads configuration from various sources in the search order specified.
 * Implements the configuration file search logic with multiple fallback locations.
 * This class handles caching and provides utility methods for configuration discovery.
 * 
 * Supported JSON configuration properties:
 * - log4rich.json.enabled - Enable JSON layout globally
 * - log4rich.json.prettyPrint - Pretty print JSON output
 * - log4rich.json.includeLocation - Include class/method/line information
 * - log4rich.json.includeThread - Include thread name
 * - log4rich.json.timestampFormat - Timestamp format pattern
 * - log4rich.json.additionalFields.* - Additional static fields
 * 
 * @since 1.0.0
 */
public class ConfigLoader {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConfigLoader() {
        // Utility class - no instantiation
    }
    
    private static final String CONFIG_FILENAME = "log4Rich.config";
    private static final String SYSTEM_PROPERTY_KEY = "log4rich.config";
    
    private static Configuration cachedConfig;
    
    /**
     * Loads configuration from the first available source.
     * The configuration is cached after the first load to improve performance.
     * Search order:
     * 1. System property: -Dlog4rich.config=/path/to/config
     * 2. Classpath: log4Rich.config (root of classpath)
     * 3. Current directory: ./log4Rich.config
     * 4. Parent directory: ../log4Rich.config
     * 5. Config directories:
     *    - ./config/log4Rich.config
     *    - ./conf/log4Rich.config
     *    - ../config/log4Rich.config
     *    - ../conf/log4Rich.config
     * 6. Default configuration if no file is found
     * 
     * @return configuration object with settings loaded from the first available source
     */
    public static Configuration loadConfiguration() {
        if (cachedConfig != null) {
            return cachedConfig;
        }
        
        Properties properties = new Properties();
        boolean loaded = false;
        
        // 1. System property
        String systemPropertyPath = System.getProperty(SYSTEM_PROPERTY_KEY);
        if (systemPropertyPath != null && !systemPropertyPath.trim().isEmpty()) {
            File systemFile = new File(systemPropertyPath);
            if (systemFile.exists() && systemFile.isFile()) {
                try {
                    loadPropertiesFromFile(properties, systemFile);
                    loaded = true;
                    System.out.println("log4Rich: Loaded configuration from system property: " + systemFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("log4Rich: Failed to load configuration from system property " + 
                                     systemFile.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
        
        // 2. Classpath
        if (!loaded) {
            try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
                if (is != null) {
                    properties.load(is);
                    loaded = true;
                    System.out.println("log4Rich: Loaded configuration from classpath: " + CONFIG_FILENAME);
                }
            } catch (IOException e) {
                System.err.println("log4Rich: Failed to load configuration from classpath: " + e.getMessage());
            }
        }
        
        // 3-5. File system locations
        if (!loaded) {
            String[] searchPaths = {
                "./" + CONFIG_FILENAME,                    // Current directory
                "../" + CONFIG_FILENAME,                   // Parent directory
                "./config/" + CONFIG_FILENAME,             // ./config/
                "./conf/" + CONFIG_FILENAME,               // ./conf/
                "../config/" + CONFIG_FILENAME,            // ../config/
                "../conf/" + CONFIG_FILENAME               // ../conf/
            };
            
            for (String path : searchPaths) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    try {
                        loadPropertiesFromFile(properties, file);
                        loaded = true;
                        System.out.println("log4Rich: Loaded configuration from: " + file.getAbsolutePath());
                        break;
                    } catch (IOException e) {
                        System.err.println("log4Rich: Failed to load configuration from " + 
                                         file.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // 6. Default configuration
        if (!loaded) {
            System.out.println("log4Rich: No configuration file found, using defaults");
        }
        
        // 7. Apply environment variable overrides
        applyEnvironmentVariableOverrides(properties);
        
        try {
            cachedConfig = new Configuration(properties);
        } catch (ConfigurationException e) {
            String separator = Java8Utils.repeat("=", 80);
            System.err.println("\n" + separator);
            System.err.println("log4Rich Configuration Error");
            System.err.println(separator);
            System.err.println(e.getMessage());
            System.err.println(separator + "\n");
            
            // For invalid configurations, fall back to defaults
            System.err.println("Falling back to default configuration to allow application startup.");
            System.err.println("Please fix the configuration errors and restart for changes to take effect.\n");
            cachedConfig = new Configuration(); // Use defaults
        }
        
        return cachedConfig;
    }
    
    /**
     * Loads configuration from a specific file.
     * This method bypasses the search order and loads directly from the specified file.
     * 
     * @param filePath path to the configuration file
     * @return configuration object with settings loaded from the file
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws FileNotFoundException if the file does not exist
     */
    public static Configuration loadConfiguration(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        
        Properties properties = new Properties();
        loadPropertiesFromFile(properties, file);
        
        // Apply environment variable overrides for explicit file loading too
        applyEnvironmentVariableOverrides(properties);
        
        try {
            return new Configuration(properties);
        } catch (ConfigurationException e) {
            // For explicit file loading, we want to propagate the error with file context
            throw new ConfigurationException(
                "Configuration validation failed for file: " + file.getAbsolutePath() + "\n\n" + e.getMessage(),
                e.getErrors()
            );
        }
    }
    
    /**
     * Loads properties from a file.
     * Uses buffered input stream for better performance.
     * 
     * @param properties Properties object to load into
     * @param file file to load from
     * @throws IOException if file cannot be read
     */
    private static void loadPropertiesFromFile(Properties properties, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            properties.load(bis);
        }
    }
    
    /**
     * Clears the cached configuration to force reload.
     * The next call to loadConfiguration() will re-read the configuration from disk.
     */
    public static void clearCache() {
        cachedConfig = null;
    }
    
    /**
     * Checks if a configuration file exists in any of the search locations.
     * This method follows the same search order as loadConfiguration().
     * 
     * @return true if a configuration file is found, false otherwise
     */
    public static boolean configExists() {
        // Check system property
        String systemPropertyPath = System.getProperty(SYSTEM_PROPERTY_KEY);
        if (systemPropertyPath != null && !systemPropertyPath.trim().isEmpty()) {
            File systemFile = new File(systemPropertyPath);
            if (systemFile.exists() && systemFile.isFile()) {
                return true;
            }
        }
        
        // Check classpath
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
            if (is != null) {
                return true;
            }
        } catch (IOException e) {
            // Ignore
        }
        
        // Check file system locations
        String[] searchPaths = {
            "./" + CONFIG_FILENAME,
            "../" + CONFIG_FILENAME,
            "./config/" + CONFIG_FILENAME,
            "./conf/" + CONFIG_FILENAME,
            "../config/" + CONFIG_FILENAME,
            "../conf/" + CONFIG_FILENAME
        };
        
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the search paths for configuration files.
     * Returns human-readable descriptions of all search locations.
     * 
     * @return array of search path descriptions
     */
    public static String[] getSearchPaths() {
        return new String[] {
            "System property: " + SYSTEM_PROPERTY_KEY,
            "Classpath: " + CONFIG_FILENAME,
            "Current directory: ./" + CONFIG_FILENAME,
            "Parent directory: ../" + CONFIG_FILENAME,
            "Config directory: ./config/" + CONFIG_FILENAME,
            "Conf directory: ./conf/" + CONFIG_FILENAME,
            "Parent config directory: ../config/" + CONFIG_FILENAME,
            "Parent conf directory: ../conf/" + CONFIG_FILENAME
        };
    }
    
    /**
     * Applies environment variable overrides to the configuration properties.
     * Environment variables follow the pattern: LOG4RICH_* where dots are replaced with underscores
     * and the prefix 'log4rich.' is replaced with 'LOG4RICH_'.
     * 
     * Examples:
     * - log4rich.rootLevel -> LOG4RICH_ROOT_LEVEL
     * - log4rich.console.enabled -> LOG4RICH_CONSOLE_ENABLED
     * - log4rich.file.maxSize -> LOG4RICH_FILE_MAX_SIZE
     * - log4rich.json.enabled -> LOG4RICH_JSON_ENABLED
     * - log4rich.json.prettyPrint -> LOG4RICH_JSON_PRETTY_PRINT
     * 
     * @param properties the properties to apply overrides to
     */
    private static void applyEnvironmentVariableOverrides(Properties properties) {
        int overrideCount = 0;
        
        for (String key : System.getenv().keySet()) {
            if (key.startsWith("LOG4RICH_")) {
                // Convert environment variable name to property name
                String propertyName = environmentVariableToPropertyName(key);
                String value = System.getenv(key);
                
                if (propertyName != null && value != null && !value.trim().isEmpty()) {
                    properties.setProperty(propertyName, value.trim());
                    overrideCount++;
                    System.out.println("log4Rich: Environment override: " + propertyName + "=" + value.trim());
                }
            }
        }
        
        if (overrideCount > 0) {
            System.out.println("log4Rich: Applied " + overrideCount + " environment variable override(s)");
        }
    }
    
    /**
     * Converts an environment variable name to a log4Rich property name.
     * Uses a mapping table to ensure exact property name matches.
     * 
     * @param envVarName the environment variable name (e.g., "LOG4RICH_ROOT_LEVEL")
     * @return the property name (e.g., "log4rich.rootLevel")
     */
    private static String environmentVariableToPropertyName(String envVarName) {
        // Use explicit mapping to ensure correct property names
        switch (envVarName) {
            case "LOG4RICH_ROOT_LEVEL": return "log4rich.rootLevel";
            case "LOG4RICH_CONSOLE_ENABLED": return "log4rich.console.enabled";
            case "LOG4RICH_CONSOLE_TARGET": return "log4rich.console.target";
            case "LOG4RICH_CONSOLE_LEVEL": return "log4rich.console.level";
            case "LOG4RICH_CONSOLE_PATTERN": return "log4rich.console.pattern";
            case "LOG4RICH_FILE_ENABLED": return "log4rich.file.enabled";
            case "LOG4RICH_FILE_PATH": return "log4rich.file.path";
            case "LOG4RICH_FILE_LEVEL": return "log4rich.file.level";
            case "LOG4RICH_FILE_PATTERN": return "log4rich.file.pattern";
            case "LOG4RICH_FILE_MAX_SIZE": return "log4rich.file.maxSize";
            case "LOG4RICH_FILE_MAX_BACKUPS": return "log4rich.file.maxBackups";
            case "LOG4RICH_FILE_COMPRESS": return "log4rich.file.compress";
            case "LOG4RICH_FILE_COMPRESS_PROGRAM": return "log4rich.file.compress.program";
            case "LOG4RICH_FILE_COMPRESS_ARGS": return "log4rich.file.compress.args";
            case "LOG4RICH_FILE_ENCODING": return "log4rich.file.encoding";
            case "LOG4RICH_FILE_BUFFER_SIZE": return "log4rich.file.bufferSize";
            case "LOG4RICH_FILE_IMMEDIATE_FLUSH": return "log4rich.file.immediateFlush";
            case "LOG4RICH_LOCATION_CAPTURE": return "log4rich.location.capture";
            case "LOG4RICH_PERFORMANCE_MEMORY_MAPPED": return "log4rich.performance.memoryMapped";
            case "LOG4RICH_PERFORMANCE_MAPPED_SIZE": return "log4rich.performance.mappedSize";
            case "LOG4RICH_PERFORMANCE_BATCH_ENABLED": return "log4rich.performance.batchEnabled";
            case "LOG4RICH_PERFORMANCE_BATCH_SIZE": return "log4rich.performance.batchSize";
            case "LOG4RICH_PERFORMANCE_BATCH_TIME_MS": return "log4rich.performance.batchTimeMs";
            case "LOG4RICH_PERFORMANCE_ZERO_ALLOCATION": return "log4rich.performance.zeroAllocation";
            case "LOG4RICH_ASYNC_ENABLED": return "log4rich.async.enabled";
            case "LOG4RICH_ASYNC_BUFFER_SIZE": return "log4rich.async.bufferSize";
            case "LOG4RICH_ASYNC_OVERFLOW_STRATEGY": return "log4rich.async.overflowStrategy";
            case "LOG4RICH_ASYNC_THREAD_PRIORITY": return "log4rich.async.threadPriority";
            case "LOG4RICH_ASYNC_SHUTDOWN_TIMEOUT": return "log4rich.async.shutdownTimeout";
            case "LOG4RICH_JSON_ENABLED": return "log4rich.json.enabled";
            case "LOG4RICH_JSON_PRETTY_PRINT": return "log4rich.json.prettyPrint";
            case "LOG4RICH_JSON_INCLUDE_LOCATION": return "log4rich.json.includeLocation";
            case "LOG4RICH_JSON_INCLUDE_THREAD": return "log4rich.json.includeThread";
            case "LOG4RICH_JSON_TIMESTAMP_FORMAT": return "log4rich.json.timestampFormat";
            default:
                // For unknown environment variables, log a warning and ignore
                System.err.println("log4Rich: Unknown environment variable: " + envVarName);
                return null;
        }
    }
    
    /**
     * Gets all environment variables that can be used to override log4Rich configuration.
     * 
     * @return array of environment variable names that would override configuration
     */
    public static String[] getSupportedEnvironmentVariables() {
        return new String[] {
            "LOG4RICH_ROOT_LEVEL",
            "LOG4RICH_CONSOLE_ENABLED", 
            "LOG4RICH_CONSOLE_TARGET",
            "LOG4RICH_CONSOLE_LEVEL",
            "LOG4RICH_CONSOLE_PATTERN",
            "LOG4RICH_FILE_ENABLED",
            "LOG4RICH_FILE_PATH",
            "LOG4RICH_FILE_LEVEL", 
            "LOG4RICH_FILE_PATTERN",
            "LOG4RICH_FILE_MAX_SIZE",
            "LOG4RICH_FILE_MAX_BACKUPS",
            "LOG4RICH_FILE_COMPRESS",
            "LOG4RICH_FILE_COMPRESS_PROGRAM",
            "LOG4RICH_FILE_COMPRESS_ARGS",
            "LOG4RICH_FILE_ENCODING",
            "LOG4RICH_FILE_BUFFER_SIZE",
            "LOG4RICH_FILE_IMMEDIATE_FLUSH",
            "LOG4RICH_LOCATION_CAPTURE",
            "LOG4RICH_PERFORMANCE_MEMORY_MAPPED",
            "LOG4RICH_PERFORMANCE_MAPPED_SIZE",
            "LOG4RICH_PERFORMANCE_BATCH_ENABLED",
            "LOG4RICH_PERFORMANCE_BATCH_SIZE",
            "LOG4RICH_PERFORMANCE_BATCH_TIME_MS",
            "LOG4RICH_PERFORMANCE_ZERO_ALLOCATION",
            "LOG4RICH_ASYNC_ENABLED",
            "LOG4RICH_ASYNC_BUFFER_SIZE",
            "LOG4RICH_ASYNC_OVERFLOW_STRATEGY",
            "LOG4RICH_ASYNC_THREAD_PRIORITY",
            "LOG4RICH_ASYNC_SHUTDOWN_TIMEOUT",
            "LOG4RICH_JSON_ENABLED",
            "LOG4RICH_JSON_PRETTY_PRINT",
            "LOG4RICH_JSON_INCLUDE_LOCATION",
            "LOG4RICH_JSON_INCLUDE_THREAD",
            "LOG4RICH_JSON_TIMESTAMP_FORMAT"
        };
    }
    
}