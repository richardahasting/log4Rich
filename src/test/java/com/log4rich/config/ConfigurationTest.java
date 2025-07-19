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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationTest {
    
    @TempDir
    Path tempDir;
    
    private Properties testProperties;
    
    @BeforeEach
    void setUp() {
        testProperties = new Properties();
        testProperties.setProperty("log4rich.rootLevel", "DEBUG");
        testProperties.setProperty("log4rich.console.enabled", "true");
        testProperties.setProperty("log4rich.file.enabled", "false");
        testProperties.setProperty("log4rich.file.path", "/tmp/test.log"); // Use valid path for tests
        testProperties.setProperty("log4rich.file.maxSize", "50M");
        testProperties.setProperty("log4rich.file.maxBackups", "20");
        testProperties.setProperty("log4rich.file.compress", "true");
        testProperties.setProperty("log4rich.file.compress.program", "xz");
        testProperties.setProperty("log4rich.file.compress.args", "-9");
        testProperties.setProperty("log4rich.logger.com.example.test", "WARN");
        testProperties.setProperty("log4rich.logger.com.example.debug", "TRACE");
    }
    
    @Test
    void testDefaultConfiguration() {
        // Create logs directory for default configuration to pass validation
        File logsDir = new File("logs");
        boolean dirCreated = logsDir.mkdirs();
        
        try {
            Configuration config = new Configuration();
            
            assertEquals(LogLevel.INFO, config.getRootLevel());
            assertTrue(config.isConsoleEnabled());
            assertTrue(config.isFileEnabled());
            assertEquals("STDOUT", config.getConsoleTarget());
            assertEquals("logs/application.log", config.getFilePath());
            assertEquals("10M", config.getMaxSize());
            assertEquals(10, config.getMaxBackups());
            assertTrue(config.isCompressionEnabled());
            assertEquals("gzip", config.getCompressionProgram());
            assertEquals("", config.getCompressionArgs());
            assertTrue(config.isLocationCapture());
            assertEquals(5000, config.getLockTimeout());
        } finally {
            // Clean up created directory if we created it
            if (dirCreated && logsDir.exists()) {
                logsDir.delete();
            }
        }
    }
    
    @Test
    void testCustomConfiguration() {
        Configuration config = new Configuration(testProperties);
        
        assertEquals(LogLevel.DEBUG, config.getRootLevel());
        assertTrue(config.isConsoleEnabled());
        assertFalse(config.isFileEnabled());
        assertEquals("50M", config.getMaxSize());
        assertEquals(20, config.getMaxBackups());
        assertEquals("xz", config.getCompressionProgram());
        assertEquals("-9", config.getCompressionArgs());
    }
    
    @Test
    void testLoggerLevels() {
        Configuration config = new Configuration(testProperties);
        
        assertEquals(LogLevel.WARN, config.getLoggerLevel("com.example.test"));
        assertEquals(LogLevel.TRACE, config.getLoggerLevel("com.example.debug"));
        assertNull(config.getLoggerLevel("com.example.nonexistent"));
        
        assertEquals(2, config.getLoggerLevels().size());
    }
    
    @Test
    void testLogLevelFallback() {
        Properties props = new Properties();
        props.setProperty("log4rich.rootLevel", "ERROR");
        props.setProperty("log4rich.file.path", "/tmp/test.log"); // Use valid path
        // Don't set console.level or file.level
        
        Configuration config = new Configuration(props);
        
        assertEquals(LogLevel.ERROR, config.getRootLevel());
        assertEquals(LogLevel.ERROR, config.getConsoleLevel()); // Should fall back to root
        assertEquals(LogLevel.ERROR, config.getFileLevel());    // Should fall back to root
    }
    
    @Test
    void testSpecificLogLevels() {
        Properties props = new Properties();
        props.setProperty("log4rich.rootLevel", "INFO");
        props.setProperty("log4rich.console.level", "DEBUG");
        props.setProperty("log4rich.file.level", "WARN");
        props.setProperty("log4rich.file.path", "/tmp/test.log"); // Use valid path
        
        Configuration config = new Configuration(props);
        
        assertEquals(LogLevel.INFO, config.getRootLevel());
        assertEquals(LogLevel.DEBUG, config.getConsoleLevel());
        assertEquals(LogLevel.WARN, config.getFileLevel());
    }
    
    @Test
    void testInvalidLogLevel() {
        Properties props = new Properties();
        props.setProperty("log4rich.rootLevel", "INVALID");
        props.setProperty("log4rich.file.path", "/tmp/test.log"); // Use valid path
        
        // Should now throw ConfigurationException for invalid levels
        assertThrows(ConfigurationException.class, () -> {
            new Configuration(props);
        });
    }
    
    @Test
    void testConfigLoaderWithFile() throws IOException {
        // Create a test config file
        File configFile = tempDir.resolve("test.config").toFile();
        
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("log4rich.rootLevel=TRACE\n");
            writer.write("log4rich.console.enabled=false\n");
            writer.write("log4rich.file.path=custom.log\n");
            writer.write("log4rich.logger.test.package=ERROR\n");
        }
        
        Configuration config = ConfigLoader.loadConfiguration(configFile.getAbsolutePath());
        
        assertEquals(LogLevel.TRACE, config.getRootLevel());
        assertFalse(config.isConsoleEnabled());
        assertEquals("custom.log", config.getFilePath());
        assertEquals(LogLevel.ERROR, config.getLoggerLevel("test.package"));
    }
    
    @Test
    void testConfigLoaderFileNotFound() {
        String nonExistentFile = tempDir.resolve("nonexistent.config").toString();
        
        assertThrows(IOException.class, () -> {
            ConfigLoader.loadConfiguration(nonExistentFile);
        });
    }
    
    @Test
    void testConfigLoaderSearchPaths() {
        String[] searchPaths = ConfigLoader.getSearchPaths();
        
        assertTrue(searchPaths.length > 0);
        assertTrue(searchPaths[0].contains("System property"));
        assertTrue(searchPaths[1].contains("Classpath"));
    }
    
    @Test
    void testGenericPropertyAccess() {
        testProperties.setProperty("custom.property", "custom.value");
        Configuration config = new Configuration(testProperties);
        
        assertEquals("custom.value", config.getProperty("custom.property"));
        assertEquals("default", config.getProperty("nonexistent.property", "default"));
        assertNull(config.getProperty("nonexistent.property"));
    }
}