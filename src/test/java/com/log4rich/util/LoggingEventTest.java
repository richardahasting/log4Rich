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

package com.log4rich.util;

import com.log4rich.Log4Rich;
import com.log4rich.appenders.Appender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LoggingEvent}, focusing on null message safety
 * and correct rendering through the full Logger pipeline.
 *
 * Covers GitHub issue #19: Object-typed message handling.
 */
public class LoggingEventTest {

    private Logger logger;
    private TestAppender testAppender;

    @BeforeEach
    public void setUp() {
        logger = new Logger("test.loggingEvent");
        logger.setLevel(LogLevel.TRACE);
        logger.setLocationCapture(false);
        testAppender = new TestAppender();
        logger.addAppender(testAppender);
    }

    @AfterEach
    public void tearDown() {
        logger.clearAppenders();
        Log4Rich.shutdown();
    }

    // ========== Direct LoggingEvent tests ==========

    @Test
    public void nullMessageNoThrowableReturnsEmpty() {
        LoggingEvent event = new LoggingEvent(LogLevel.INFO, null, "test", null);
        assertEquals("", event.getRenderedMessage());
    }

    @Test
    public void nullMessageWithThrowableDoesNotThrow() {
        RuntimeException ex = new RuntimeException("test error");
        LoggingEvent event = new LoggingEvent(LogLevel.ERROR, null, "test", null, ex);

        String rendered = event.getRenderedMessage();
        assertNotNull(rendered);
        assertTrue(rendered.contains("RuntimeException"));
        assertTrue(rendered.contains("test error"));
    }

    @Test
    public void normalMessagePreserved() {
        LoggingEvent event = new LoggingEvent(LogLevel.INFO, "hello world", "test", null);
        assertEquals("hello world", event.getRenderedMessage());
    }

    @Test
    public void normalMessageWithThrowableContainsBoth() {
        RuntimeException ex = new RuntimeException("boom");
        LoggingEvent event = new LoggingEvent(LogLevel.ERROR, "operation failed", "test", null, ex);

        String rendered = event.getRenderedMessage();
        assertTrue(rendered.startsWith("operation failed"));
        assertTrue(rendered.contains("RuntimeException"));
        assertTrue(rendered.contains("boom"));
    }

    @Test
    public void getMessageReturnsRawNull() {
        LoggingEvent event = new LoggingEvent(LogLevel.INFO, null, "test", null);
        assertNull(event.getMessage());
    }

    // ========== Full pipeline tests (Logger → StandardLayout) ==========

    @Test
    public void loggerWithNullMessageDoesNotThrow() {
        assertDoesNotThrow(() -> logger.log(LogLevel.INFO, null, null));
        assertEquals(1, testAppender.events.size());
        assertEquals("", testAppender.events.get(0).getRenderedMessage());
    }

    @Test
    public void loggerWithNullMessageAndThrowableDoesNotThrow() {
        RuntimeException ex = new RuntimeException("test");
        assertDoesNotThrow(() -> logger.log(LogLevel.INFO, null, ex));
        assertEquals(1, testAppender.events.size());
        String rendered = testAppender.events.get(0).getRenderedMessage();
        assertTrue(rendered.contains("RuntimeException"));
    }

    @Test
    public void standardLayoutWithNullMessageDoesNotThrow() {
        // Use a layout-equipped appender to exercise the full format path
        LayoutAppender layoutAppender = new LayoutAppender();
        logger.addAppender(layoutAppender);

        assertDoesNotThrow(() -> logger.log(LogLevel.INFO, null, null));
        assertNotNull(layoutAppender.lastFormatted);
    }

    // ========== Test helpers ==========

    private static class TestAppender implements Appender {
        public final List<LoggingEvent> events = new ArrayList<>();
        private String name = "test";
        private boolean closed = false;
        private LogLevel level = LogLevel.TRACE;
        private Layout layout = null;

        @Override public void append(LoggingEvent event) { events.add(event); }
        @Override public String getName() { return name; }
        @Override public void setName(String name) { this.name = name; }
        @Override public boolean isClosed() { return closed; }
        @Override public void close() { closed = true; }
        @Override public void setLayout(Layout layout) { this.layout = layout; }
        @Override public Layout getLayout() { return layout; }
        @Override public void setLevel(LogLevel level) { this.level = level; }
        @Override public LogLevel getLevel() { return level; }
        @Override public boolean isLevelEnabled(LogLevel level) { return level.isGreaterOrEqual(this.level); }
    }

    /**
     * Appender that runs StandardLayout.format() to exercise the full rendering path.
     */
    private static class LayoutAppender implements Appender {
        volatile String lastFormatted;
        private final Layout layout = new StandardLayout("%message%n");
        private String name = "layout-test";
        private boolean closed = false;
        private LogLevel level = LogLevel.TRACE;

        @Override public void append(LoggingEvent event) { lastFormatted = layout.format(event); }
        @Override public String getName() { return name; }
        @Override public void setName(String name) { this.name = name; }
        @Override public boolean isClosed() { return closed; }
        @Override public void close() { closed = true; }
        @Override public void setLayout(Layout layout) { /* fixed */ }
        @Override public Layout getLayout() { return layout; }
        @Override public void setLevel(LogLevel level) { this.level = level; }
        @Override public LogLevel getLevel() { return level; }
        @Override public boolean isLevelEnabled(LogLevel level) { return level.isGreaterOrEqual(this.level); }
    }
}
