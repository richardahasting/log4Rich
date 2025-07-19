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

import com.log4rich.appenders.Appender;
import com.log4rich.layouts.Layout;
import com.log4rich.util.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class LoggerParameterizedTest {
    
    private Logger logger;
    private TestAppender testAppender;
    
    @BeforeEach
    public void setUp() {
        logger = new Logger("test.logger");
        logger.setLevel(LogLevel.TRACE); // Enable all levels for testing
        testAppender = new TestAppender();
        logger.addAppender(testAppender);
    }
    
    @Test
    public void testParameterizedInfo() {
        logger.info("User {} logged in from {}", "john", "192.168.1.1");
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.INFO, event.getLevel());
        assertEquals("User john logged in from 192.168.1.1", event.getMessage());
        assertNull(event.getThrowable());
    }
    
    @Test
    public void testParameterizedError() {
        logger.error("Failed to process {}: {}", "config.xml", "File not found");
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("Failed to process config.xml: File not found", event.getMessage());
        assertNull(event.getThrowable());
    }
    
    @Test
    public void testParameterizedWithThrowable() {
        Exception e = new RuntimeException("Test exception");
        logger.error("Failed to process {}: {}", "config.xml", e.getMessage(), e);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("Failed to process config.xml: Test exception", event.getMessage());
        assertSame(e, event.getThrowable());
    }
    
    @Test
    public void testParameterizedDebug() {
        logger.debug("Processing item {} of {}", 5, 10);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.DEBUG, event.getLevel());
        assertEquals("Processing item 5 of 10", event.getMessage());
    }
    
    @Test
    public void testParameterizedWarn() {
        logger.warn("Cache miss for key {}", "user:123");
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.WARN, event.getLevel());
        assertEquals("Cache miss for key user:123", event.getMessage());
    }
    
    @Test
    public void testParameterizedTrace() {
        logger.trace("Entering method {} with args {}", "processData", (Object) new Object[]{"arg1", "arg2"});
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.TRACE, event.getLevel());
        assertEquals("Entering method processData with args [arg1, arg2]", event.getMessage());
    }
    
    @Test
    public void testParameterizedFatal() {
        logger.fatal("System failure: {}", "Database connection lost");
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.FATAL, event.getLevel());
        assertEquals("System failure: Database connection lost", event.getMessage());
    }
    
    @Test
    public void testParameterizedWithLevelDisabled() {
        logger.setLevel(LogLevel.WARN); // Disable DEBUG and below
        logger.debug("This should not be logged: {}", "value");
        
        assertEquals(0, testAppender.events.size());
    }
    
    @Test
    public void testParameterizedWithNullArguments() {
        logger.info("Value is {}", (Object) null);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals("Value is null", event.getMessage());
    }
    
    @Test
    public void testParameterizedWithArrayArguments() {
        logger.info("Array: {}", (Object) new int[]{1, 2, 3});
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals("Array: [1, 2, 3]", event.getMessage());
    }
    
    @Test
    public void testParameterizedPerformanceWithDisabledLevel() {
        logger.setLevel(LogLevel.WARN); // Disable DEBUG
        
        // This should not cause any string formatting since DEBUG is disabled
        logger.debug("Expensive operation: {} + {}", 
            new ExpensiveToString("value1"),
            new ExpensiveToString("value2"));
        
        assertEquals(0, testAppender.events.size());
        // The toString() methods should not have been called
    }
    
    @Test
    public void testConvenienceMethodErrorWithOneArg() {
        Exception e = new RuntimeException("Test exception");
        logger.error("Failed to process {}", "config.xml", e);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("Failed to process config.xml", event.getMessage());
        assertSame(e, event.getThrowable());
    }
    
    @Test
    public void testConvenienceMethodErrorWithTwoArgs() {
        Exception e = new RuntimeException("Permission denied");
        logger.error("Failed to access {} at {}", "database", "localhost:5432", e);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("Failed to access database at localhost:5432", event.getMessage());
        assertSame(e, event.getThrowable());
    }
    
    @Test
    public void testConvenienceMethodWarnWithOneArg() {
        Exception e = new RuntimeException("Timeout");
        logger.warn("Slow response from {}", "external-api", e);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.WARN, event.getLevel());
        assertEquals("Slow response from external-api", event.getMessage());
        assertSame(e, event.getThrowable());
    }
    
    @Test
    public void testConvenienceMethodDebugWithOneArg() {
        Exception e = new RuntimeException("Debug info");
        logger.debug("Processing {}", "user-request", e);
        
        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(LogLevel.DEBUG, event.getLevel());
        assertEquals("Processing user-request", event.getMessage());
        assertSame(e, event.getThrowable());
    }
    
    /**
     * Helper class to test that toString() is not called when logging is disabled
     */
    private static class ExpensiveToString {
        private final String value;
        
        public ExpensiveToString(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            throw new RuntimeException("toString() should not be called when logging is disabled");
        }
    }
    
    /**
     * Test appender that captures events for verification
     */
    private static class TestAppender implements Appender {
        public final List<LoggingEvent> events = new ArrayList<>();
        private String name = "test";
        private boolean closed = false;
        private LogLevel level = LogLevel.TRACE;
        private Layout layout = null;
        
        @Override
        public void append(LoggingEvent event) {
            events.add(event);
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public boolean isClosed() {
            return closed;
        }
        
        @Override
        public void close() {
            events.clear();
            closed = true;
        }
        
        @Override
        public void setLayout(Layout layout) {
            this.layout = layout;
        }
        
        @Override
        public Layout getLayout() {
            return layout;
        }
        
        @Override
        public void setLevel(LogLevel level) {
            this.level = level;
        }
        
        @Override
        public LogLevel getLevel() {
            return level;
        }
        
        @Override
        public boolean isLevelEnabled(LogLevel level) {
            return level.isGreaterOrEqual(this.level);
        }
    }
}