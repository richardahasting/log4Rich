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

import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.layouts.StandardLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerTest {
    
    private Logger logger;
    
    @BeforeEach
    void setUp() {
        logger = new Logger("test.logger");
    }
    
    @AfterEach
    void tearDown() {
        if (logger != null) {
            logger.shutdown();
        }
    }
    
    @Test
    void testLoggerCreation() {
        assertNotNull(logger);
        assertEquals("test.logger", logger.getName());
        assertEquals(LogLevel.INFO, logger.getLevel());
        assertTrue(logger.isLocationCaptureEnabled());
    }
    
    @Test
    void testLogLevelMethods() {
        logger.setLevel(LogLevel.WARN);
        
        assertFalse(logger.isTraceEnabled());
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isFatalEnabled());
    }
    
    @Test
    void testAppenderManagement() {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setName("test-console");
        
        logger.addAppender(appender);
        assertEquals(1, logger.getAppenders().size());
        
        logger.removeAppender("test-console");
        assertEquals(0, logger.getAppenders().size());
    }
    
    @Test
    void testLocationCaptureToggle() {
        assertTrue(logger.isLocationCaptureEnabled());
        
        logger.setLocationCapture(false);
        assertFalse(logger.isLocationCaptureEnabled());
        
        logger.setLocationCapture(true);
        assertTrue(logger.isLocationCaptureEnabled());
    }
    
    @Test
    void testBasicLogging() {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setLayout(new StandardLayout("[%level] %message%n"));
        logger.addAppender(appender);
        
        // This should work without throwing exceptions
        logger.info("Test info message");
        logger.error("Test error message");
        logger.debug("Test debug message"); // Should be filtered out by default INFO level
    }
}