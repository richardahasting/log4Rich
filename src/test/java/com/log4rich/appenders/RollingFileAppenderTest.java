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

package com.log4rich.appenders;

import com.log4rich.core.LogLevel;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.CompressionManager;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.Java8Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RollingFileAppenderTest {
    
    
    @TempDir
    Path tempDir;
    
    private RollingFileAppender appender;
    private File logFile;
    
    @BeforeEach
    void setUp() {
        logFile = tempDir.resolve("test.log").toFile();
        appender = new RollingFileAppender(logFile);
        appender.setLayout(new StandardLayout("[%level] %message%n"));
        appender.setMaxFileSize(100); // Very small for testing rollover
        appender.setMaxBackups(3);
        appender.setCompression(false); // Disable compression for easier testing
    }
    
    @AfterEach
    void tearDown() {
        if (appender != null) {
            appender.close();
        }
    }
    
    @Test
    void testBasicLogging() throws IOException {
        LoggingEvent event = new LoggingEvent(
            LogLevel.INFO, 
            "Test message", 
            "TestLogger", 
            null
        );
        
        appender.append(event);
        
        assertTrue(logFile.exists());
        String content = Java8Utils.readString(logFile.toPath());
        assertTrue(content.contains("Test message"));
    }
    
    @Test
    void testRollover() throws IOException {
        // Write enough messages to trigger rollover
        for (int i = 0; i < 10; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO, 
                "This is a longer test message to trigger rollover: " + i, 
                "TestLogger", 
                null
            );
            appender.append(event);
        }
        
        // Check that backup files were created
        File[] backupFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("test.log.") && name.matches("test\\.log\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}"));
        
        assertNotNull(backupFiles);
        assertTrue(backupFiles.length > 0, "Should have created backup files");
        assertTrue(logFile.exists(), "Main log file should still exist");
    }
    
    @Test
    void testMaxBackups() throws IOException {
        // Set very small max backups
        appender.setMaxBackups(2);
        
        // Force multiple rollovers
        for (int i = 0; i < 20; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO, 
                "Message " + i + " - " + Java8Utils.repeat("x", 50), // Make message long enough to trigger rollover
                "TestLogger", 
                null
            );
            appender.append(event);
        }
        
        // Check that we don't have more than maxBackups backup files
        File[] backupFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("test.log.") && name.matches("test\\.log\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}"));
        
        assertNotNull(backupFiles);
        assertTrue(backupFiles.length <= 2, "Should not have more than maxBackups files");
    }
    
    @Test
    void testLevelFiltering() throws IOException {
        appender.setLevel(LogLevel.WARN);
        
        LoggingEvent debugEvent = new LoggingEvent(LogLevel.DEBUG, "Debug message", "TestLogger", null);
        LoggingEvent warnEvent = new LoggingEvent(LogLevel.WARN, "Warning message", "TestLogger", null);
        
        appender.append(debugEvent);
        appender.append(warnEvent);
        
        if (logFile.exists()) {
            String content = Java8Utils.readString(logFile.toPath());
            assertFalse(content.contains("Debug message"));
            assertTrue(content.contains("Warning message"));
        }
    }
    
    @Test
    void testSizeParsing() {
        appender.setMaxFileSize("10K");
        assertEquals(10 * 1024, appender.getMaxFileSize());
        
        appender.setMaxFileSize("5M");
        assertEquals(5 * 1024 * 1024, appender.getMaxFileSize());
        
        appender.setMaxFileSize("1G");
        assertEquals(1024 * 1024 * 1024, appender.getMaxFileSize());
        
        appender.setMaxFileSize("100");
        assertEquals(100, appender.getMaxFileSize());
    }
    
    @Test
    void testCompressionManagerConfiguration() {
        CompressionManager compressionManager = new CompressionManager("bzip2", "-9", 60000);
        appender.setCompressionManager(compressionManager);
        
        assertEquals(compressionManager, appender.getCompressionManager());
        assertEquals("bzip2", appender.getCompressionManager().getProgram());
        assertEquals("-9", appender.getCompressionManager().getArguments());
    }
    
    @Test
    void testAppenderConfiguration() {
        appender.setName("TestAppender");
        appender.setLevel(LogLevel.ERROR);
        appender.setMaxFileSize(50 * 1024);
        appender.setMaxBackups(5);
        appender.setCompression(true);
        
        assertEquals("TestAppender", appender.getName());
        assertEquals(LogLevel.ERROR, appender.getLevel());
        assertEquals(50 * 1024, appender.getMaxFileSize());
        assertEquals(5, appender.getMaxBackups());
        assertTrue(appender.isCompression());
    }
}