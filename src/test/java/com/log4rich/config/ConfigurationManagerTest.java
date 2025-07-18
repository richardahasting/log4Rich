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

import com.log4rich.Log4Rich;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationManagerTest {
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Reset configuration before each test
        Log4Rich.shutdown();
        
        // Create a minimal configuration without console and file appenders
        Properties props = new Properties();
        props.setProperty("log4rich.console.enabled", "false");
        props.setProperty("log4rich.file.enabled", "false");
        Configuration config = new Configuration(props);
        ConfigurationManager.initialize(config);
    }
    
    @AfterEach
    void tearDown() {
        Log4Rich.shutdown();
    }
    
    @Test
    void testRuntimeRootLevelChange() {
        Logger logger = Log4Rich.getLogger("test");
        
        // Initially should be INFO
        assertEquals(LogLevel.INFO, logger.getLevel());
        
        // Change to DEBUG
        Log4Rich.setRootLevel(LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, logger.getLevel());
        
        // Change to ERROR
        Log4Rich.setRootLevel(LogLevel.ERROR);
        assertEquals(LogLevel.ERROR, logger.getLevel());
    }
    
    @Test
    void testRuntimeLoggerSpecificLevel() {
        Logger logger1 = Log4Rich.getLogger("com.example.test1");
        Logger logger2 = Log4Rich.getLogger("com.example.test2");
        
        // Set different levels for different loggers
        Log4Rich.setLoggerLevel("com.example.test1", LogLevel.DEBUG);
        Log4Rich.setLoggerLevel("com.example.test2", LogLevel.ERROR);
        
        assertEquals(LogLevel.DEBUG, logger1.getLevel());
        assertEquals(LogLevel.ERROR, logger2.getLevel());
    }
    
    @Test
    void testRuntimeLocationCaptureToggle() {
        Logger logger = Log4Rich.getLogger("test");
        
        // Should be enabled by default
        assertTrue(logger.isLocationCaptureEnabled());
        
        // Disable globally
        Log4Rich.setLocationCapture(false);
        assertFalse(logger.isLocationCaptureEnabled());
        
        // Enable globally
        Log4Rich.setLocationCapture(true);
        assertTrue(logger.isLocationCaptureEnabled());
    }
    
    @Test
    void testRuntimeConsoleConfiguration() {
        // Test console enable/disable
        Log4Rich.setConsoleEnabled(false);
        assertFalse(Log4Rich.getCurrentConfiguration().isConsoleEnabled());
        
        Log4Rich.setConsoleEnabled(true);
        assertTrue(Log4Rich.getCurrentConfiguration().isConsoleEnabled());
        
        // Test console target
        Log4Rich.setConsoleTarget("STDERR");
        assertEquals("STDERR", Log4Rich.getCurrentConfiguration().getConsoleTarget());
        
        // Test console pattern
        String pattern = "[%level] %message%n";
        Log4Rich.setConsolePattern(pattern);
        assertEquals(pattern, Log4Rich.getCurrentConfiguration().getConsolePattern());
    }
    
    @Test
    void testRuntimeFileConfiguration() {
        // Test file enable/disable
        Log4Rich.setFileEnabled(false);
        assertFalse(Log4Rich.getCurrentConfiguration().isFileEnabled());
        
        Log4Rich.setFileEnabled(true);
        assertTrue(Log4Rich.getCurrentConfiguration().isFileEnabled());
        
        // Test file path
        String filePath = tempDir.resolve("test.log").toString();
        Log4Rich.setFilePath(filePath);
        assertEquals(filePath, Log4Rich.getCurrentConfiguration().getFilePath());
        
        // Test file pattern
        String pattern = "[%level] %date{HH:mm:ss} %message%n";
        Log4Rich.setFilePattern(pattern);
        assertEquals(pattern, Log4Rich.getCurrentConfiguration().getFilePattern());
        
        // Test max file size
        Log4Rich.setMaxFileSize("50M");
        assertEquals("50M", Log4Rich.getCurrentConfiguration().getMaxSize());
        
        // Test max backups
        Log4Rich.setMaxBackups(20);
        assertEquals(20, Log4Rich.getCurrentConfiguration().getMaxBackups());
    }
    
    @Test
    void testRuntimeCompressionConfiguration() {
        // Test compression settings
        Log4Rich.setCompression(true, "xz", "-6");
        
        Configuration config = Log4Rich.getCurrentConfiguration();
        assertTrue(config.isCompressionEnabled());
        assertEquals("xz", config.getCompressionProgram());
        assertEquals("-6", config.getCompressionArgs());
        
        // Test disabling compression
        Log4Rich.setCompression(false, null, null);
        assertFalse(Log4Rich.getCurrentConfiguration().isCompressionEnabled());
    }
    
    @Test
    void testDynamicAppenderCreation() {
        // Create console appender
        ConsoleAppender consoleAppender = Log4Rich.createConsoleAppender(
            "TestConsole", "STDOUT", "[%level] %message%n"
        );
        assertNotNull(consoleAppender);
        assertEquals("TestConsole", consoleAppender.getName());
        assertEquals(ConsoleAppender.Target.STDOUT, consoleAppender.getTarget());
        
        // Verify it's managed
        assertEquals(consoleAppender, Log4Rich.getManagedAppender("TestConsole"));
        
        // Create rolling file appender
        String filePath = tempDir.resolve("dynamic.log").toString();
        RollingFileAppender fileAppender = Log4Rich.createRollingFileAppender(
            "TestFile", filePath, "5M", 15, "[%level] %message%n"
        );
        assertNotNull(fileAppender);
        assertEquals("TestFile", fileAppender.getName());
        assertEquals(new File(filePath), fileAppender.getFile());
        assertEquals(5 * 1024 * 1024, fileAppender.getMaxFileSize());
        assertEquals(15, fileAppender.getMaxBackups());
        
        // Verify it's managed
        assertEquals(fileAppender, Log4Rich.getManagedAppender("TestFile"));
    }
    
    @Test
    void testSimpleAppenderCreation() {
        String filePath = tempDir.resolve("simple.log").toString();
        RollingFileAppender appender = Log4Rich.createRollingFileAppender(filePath);
        
        assertNotNull(appender);
        assertEquals(new File(filePath), appender.getFile());
        assertEquals(10 * 1024 * 1024, appender.getMaxFileSize()); // Default 10M
        assertEquals(10, appender.getMaxBackups()); // Default 10
    }
    
    @Test
    void testAppenderManagement() {
        // Create appenders
        ConsoleAppender consoleAppender = Log4Rich.createConsoleAppender(
            "TestConsole", "STDOUT", null
        );
        RollingFileAppender fileAppender = Log4Rich.createRollingFileAppender(
            "TestFile", tempDir.resolve("test.log").toString(), "1M", 5, null
        );
        
        // Verify they're managed
        assertEquals(consoleAppender, Log4Rich.getManagedAppender("TestConsole"));
        assertEquals(fileAppender, Log4Rich.getManagedAppender("TestFile"));
        
        // Remove appender
        assertEquals(consoleAppender, Log4Rich.removeManagedAppender("TestConsole"));
        assertNull(Log4Rich.getManagedAppender("TestConsole"));
        
        // Verify it's closed
        assertTrue(consoleAppender.isClosed());
        
        // Remove non-existent appender
        assertNull(Log4Rich.removeManagedAppender("NonExistent"));
    }
    
    @Test
    void testConfigurationStats() {
        // Create some loggers and appenders
        Log4Rich.getLogger("com.example.test1");
        Log4Rich.getLogger("com.example.test2");
        Log4Rich.setLoggerLevel("com.example.test1", LogLevel.DEBUG);
        
        Log4Rich.createConsoleAppender("Console1", "STDOUT", null);
        Log4Rich.createRollingFileAppender("File1", tempDir.resolve("test1.log").toString(), "1M", 5, null);
        
        ConfigurationManager.ConfigurationStats stats = Log4Rich.getStats();
        
        assertTrue(stats.getLoggerCount() >= 2); // At least our 2 loggers
        assertTrue(stats.getAppenderCount() >= 2); // At least our 2 appenders
        assertTrue(stats.getConfiguredLoggerCount() >= 1); // At least 1 configured logger
        
        assertNotNull(stats.toString());
    }
    
    @Test
    void testConfigurationReload() throws Exception {
        // Create a temporary config file
        File configFile = tempDir.resolve("test.config").toFile();
        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            writer.write("log4rich.rootLevel=TRACE\n");
            writer.write("log4rich.console.enabled=false\n");
            writer.write("log4rich.file.enabled=true\n");
            writer.write("log4rich.file.path=" + tempDir.resolve("reload.log").toString() + "\n");
        }
        
        // Load specific configuration
        Log4Rich.setConfigPath(configFile.getAbsolutePath());
        
        Configuration config = Log4Rich.getCurrentConfiguration();
        assertEquals(LogLevel.TRACE, config.getRootLevel());
        assertFalse(config.isConsoleEnabled());
        assertTrue(config.isFileEnabled());
        
        // Modify config file
        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            writer.write("log4rich.rootLevel=ERROR\n");
            writer.write("log4rich.console.enabled=true\n");
            writer.write("log4rich.file.enabled=false\n");
        }
        
        // Reload configuration
        Log4Rich.reloadConfiguration();
        
        config = Log4Rich.getCurrentConfiguration();
        assertEquals(LogLevel.ERROR, config.getRootLevel());
        assertTrue(config.isConsoleEnabled());
        assertFalse(config.isFileEnabled());
    }
    
    @Test
    void testMultipleConfigurationChanges() {
        // Rapid configuration changes to test thread safety
        for (int i = 0; i < 10; i++) {
            Log4Rich.setRootLevel(i % 2 == 0 ? LogLevel.DEBUG : LogLevel.INFO);
            Log4Rich.setConsoleEnabled(i % 2 == 0);
            Log4Rich.setFileEnabled(i % 2 == 1);
            Log4Rich.setLocationCapture(i % 3 == 0);
            
            // Verify configuration is consistent
            Configuration config = Log4Rich.getCurrentConfiguration();
            assertNotNull(config);
            assertEquals(i % 2 == 0 ? LogLevel.DEBUG : LogLevel.INFO, config.getRootLevel());
            assertEquals(i % 2 == 0, config.isConsoleEnabled());
            assertEquals(i % 2 == 1, config.isFileEnabled());
            assertEquals(i % 3 == 0, config.isLocationCapture());
        }
    }
    
    @Test
    void testConfigurationPersistence() {
        // Make configuration changes
        Log4Rich.setRootLevel(LogLevel.WARN);
        Log4Rich.setConsolePattern("[%level] %message%n");
        Log4Rich.setFilePath(tempDir.resolve("persist.log").toString());
        Log4Rich.setMaxFileSize("25M");
        Log4Rich.setMaxBackups(7);
        
        // Get configuration
        Configuration config = Log4Rich.getCurrentConfiguration();
        
        // Verify all changes are persisted
        assertEquals(LogLevel.WARN, config.getRootLevel());
        assertEquals("[%level] %message%n", config.getConsolePattern());
        assertEquals(tempDir.resolve("persist.log").toString(), config.getFilePath());
        assertEquals("25M", config.getMaxSize());
        assertEquals(7, config.getMaxBackups());
    }
}