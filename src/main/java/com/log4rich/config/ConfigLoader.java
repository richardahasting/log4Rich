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

/**
 * Loads configuration from various sources in the search order specified.
 * Implements the configuration file search logic with multiple fallback locations.
 * This class handles caching and provides utility methods for configuration discovery.
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
        
        cachedConfig = new Configuration(properties);
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
        
        return new Configuration(properties);
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
}