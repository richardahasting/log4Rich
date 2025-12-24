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

package com.log4rich.slf4j;

import com.log4rich.Log4Rich;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SLF4J compatibility adapter.
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class SLF4JAdapterTest {

    @BeforeEach
    void setUp() {
        // Clean state - loggers are cached so tests may share them
    }

    @AfterEach
    void tearDown() {
        Log4Rich.shutdown();
    }

    @Test
    void testGetLogger() {
        Logger logger = LoggerFactory.getLogger(SLF4JAdapterTest.class);
        assertNotNull(logger);
        assertEquals(SLF4JAdapterTest.class.getName(), logger.getName());
    }

    @Test
    void testGetLoggerByName() {
        Logger logger = LoggerFactory.getLogger("test.logger");
        assertNotNull(logger);
        assertEquals("test.logger", logger.getName());
    }

    @Test
    void testLoggerCaching() {
        Logger logger1 = LoggerFactory.getLogger("cached.logger");
        Logger logger2 = LoggerFactory.getLogger("cached.logger");
        assertSame(logger1, logger2);
    }

    @Test
    void testIsEnabled() {
        // Get the logger through SLF4J
        Logger logger = LoggerFactory.getLogger("test.enabled");

        // Get the underlying log4Rich logger and set level
        com.log4rich.core.Logger log4richLogger = LogManager.getLogger("test.enabled");
        log4richLogger.setLevel(LogLevel.DEBUG);

        // With DEBUG level set
        assertTrue(logger.isDebugEnabled(), "Debug should be enabled with DEBUG level");
        assertTrue(logger.isInfoEnabled(), "Info should be enabled with DEBUG level");
        assertTrue(logger.isWarnEnabled(), "Warn should be enabled with DEBUG level");
        assertTrue(logger.isErrorEnabled(), "Error should be enabled with DEBUG level");
        // TRACE is below DEBUG, so it should be disabled
        assertFalse(logger.isTraceEnabled(), "Trace should be disabled with DEBUG level");
    }

    @Test
    void testTraceLogging() {
        Logger logger = LoggerFactory.getLogger("test.trace");

        // Set the specific logger to TRACE level
        com.log4rich.core.Logger log4richLogger = LogManager.getLogger("test.trace");
        log4richLogger.setLevel(LogLevel.TRACE);

        assertTrue(logger.isTraceEnabled(), "Trace should be enabled with TRACE level");

        // These should not throw
        assertDoesNotThrow(() -> logger.trace("Trace message"));
        assertDoesNotThrow(() -> logger.trace("Trace with arg: {}", "value"));
        assertDoesNotThrow(() -> logger.trace("Trace with args: {} {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.trace("Trace with varargs: {} {} {}", "a", "b", "c"));
        assertDoesNotThrow(() -> logger.trace("Trace with exception", new RuntimeException("test")));
    }

    @Test
    void testDebugLogging() {
        Logger logger = LoggerFactory.getLogger("test.debug");

        assertDoesNotThrow(() -> logger.debug("Debug message"));
        assertDoesNotThrow(() -> logger.debug("Debug with arg: {}", "value"));
        assertDoesNotThrow(() -> logger.debug("Debug with args: {} {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.debug("Debug with exception", new RuntimeException("test")));
    }

    @Test
    void testInfoLogging() {
        Logger logger = LoggerFactory.getLogger("test.info");

        assertDoesNotThrow(() -> logger.info("Info message"));
        assertDoesNotThrow(() -> logger.info("Info with arg: {}", "value"));
        assertDoesNotThrow(() -> logger.info("Info with args: {} {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.info("Info with exception", new RuntimeException("test")));
    }

    @Test
    void testWarnLogging() {
        Logger logger = LoggerFactory.getLogger("test.warn");

        assertDoesNotThrow(() -> logger.warn("Warn message"));
        assertDoesNotThrow(() -> logger.warn("Warn with arg: {}", "value"));
        assertDoesNotThrow(() -> logger.warn("Warn with args: {} {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.warn("Warn with exception", new RuntimeException("test")));
    }

    @Test
    void testErrorLogging() {
        Logger logger = LoggerFactory.getLogger("test.error");

        assertDoesNotThrow(() -> logger.error("Error message"));
        assertDoesNotThrow(() -> logger.error("Error with arg: {}", "value"));
        assertDoesNotThrow(() -> logger.error("Error with args: {} {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.error("Error with exception", new RuntimeException("test")));
    }

    @Test
    void testFluentAPI() {
        Logger logger = LoggerFactory.getLogger("test.fluent");

        // Test fluent API
        assertDoesNotThrow(() -> {
            logger.atInfo()
                    .setMessage("Fluent log message")
                    .log();
        });

        assertDoesNotThrow(() -> {
            logger.atDebug()
                    .setMessage("Message with arg: {}")
                    .addArgument("value")
                    .log();
        });

        assertDoesNotThrow(() -> {
            logger.atError()
                    .setCause(new RuntimeException("test error"))
                    .log("Error with exception");
        });

        assertDoesNotThrow(() -> {
            logger.atWarn()
                    .addKeyValue("key", "value")
                    .log("Message with key-value");
        });
    }

    @Test
    void testFluentAPIShortcuts() {
        Logger logger = LoggerFactory.getLogger("test.fluent.shortcuts");

        assertDoesNotThrow(() -> logger.atInfo().log("Simple message"));
        assertDoesNotThrow(() -> logger.atDebug().log("Message with {}", "arg"));
        assertDoesNotThrow(() -> logger.atWarn().log("Message with {} and {}", "arg1", "arg2"));
        assertDoesNotThrow(() -> logger.atError().log("Message with {}, {}, {}", "a", "b", "c"));
    }

    @Test
    void testLog4RichLoggerFactory() {
        Log4RichLoggerFactory factory = new Log4RichLoggerFactory();

        org.slf4j.Logger logger1 = factory.getLogger("factory.test");
        org.slf4j.Logger logger2 = factory.getLogger("factory.test");

        assertSame(logger1, logger2);
        assertEquals(1, factory.size());

        factory.clear();
        assertEquals(0, factory.size());
    }

    @Test
    void testMarkerMethods() {
        Logger logger = LoggerFactory.getLogger("test.marker");

        // Marker methods should work (markers are ignored)
        org.slf4j.Marker marker = org.slf4j.MarkerFactory.getMarker("TEST");

        assertDoesNotThrow(() -> logger.debug(marker, "Debug with marker"));
        assertDoesNotThrow(() -> logger.info(marker, "Info with marker"));
        assertDoesNotThrow(() -> logger.warn(marker, "Warn with marker"));
        assertDoesNotThrow(() -> logger.error(marker, "Error with marker"));
    }

    @Test
    void testServiceProvider() {
        Log4RichServiceProvider provider = new Log4RichServiceProvider();

        assertEquals("2.0.99", provider.getRequestedApiVersion());

        provider.initialize();

        assertNotNull(provider.getLoggerFactory());
        assertNotNull(provider.getMarkerFactory());
        assertNotNull(provider.getMDCAdapter());

        assertTrue(provider.getLoggerFactory() instanceof Log4RichLoggerFactory);
    }
}
