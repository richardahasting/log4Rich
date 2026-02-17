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

import com.log4rich.Log4Rich;
import com.log4rich.appenders.Appender;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that pre-formatted flow tracing messages from the log4j2-log4Rich
 * bridge pass through log4Rich core faithfully without modification.
 *
 * <p>The bridge converts {@code EntryMessage} and exit messages to plain
 * Strings before calling {@code Logger.log(LogLevel, String, Throwable)}.
 * log4Rich never sees the {@code EntryMessage} object — it receives
 * pre-formatted strings like {@code "Enter myMethod(param1)"}.</p>
 *
 * <p>These tests verify the contract: whatever string the bridge passes in,
 * log4Rich outputs it unchanged.</p>
 *
 * Covers GitHub issue #18: Ensure Logger handles EntryMessage and flow tracing message types.
 */
public class FlowTracingMessageTest {

    private Logger logger;
    private TestAppender testAppender;

    @BeforeEach
    public void setUp() {
        logger = new Logger("test.flowTracing");
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

    @Test
    public void entryMessagePassesThroughUnmodified() {
        String entryMsg = "Enter myMethod(param1, param2)";
        logger.log(LogLevel.TRACE, entryMsg);

        assertEquals(1, testAppender.events.size());
        assertEquals(entryMsg, testAppender.events.get(0).getMessage());
        assertEquals(entryMsg, testAppender.events.get(0).getRenderedMessage());
    }

    @Test
    public void exitMessagePassesThroughUnmodified() {
        String exitMsg = "Exit myMethod: result=42";
        logger.log(LogLevel.TRACE, exitMsg);

        assertEquals(1, testAppender.events.size());
        assertEquals(exitMsg, testAppender.events.get(0).getMessage());
    }

    @Test
    public void entryMessageWithNoParamsPassesThrough() {
        String entryMsg = "Enter myMethod()";
        logger.log(LogLevel.TRACE, entryMsg);

        assertEquals(1, testAppender.events.size());
        assertEquals(entryMsg, testAppender.events.get(0).getMessage());
    }

    @Test
    public void entryWithThrowablePreservesBoth() {
        String entryMsg = "Enter riskyMethod(arg)";
        RuntimeException ex = new RuntimeException("entry failed");
        logger.log(LogLevel.TRACE, entryMsg, ex);

        assertEquals(1, testAppender.events.size());
        LoggingEvent event = testAppender.events.get(0);
        assertEquals(entryMsg, event.getMessage());
        assertSame(ex, event.getThrowable());

        String rendered = event.getRenderedMessage();
        assertTrue(rendered.startsWith(entryMsg));
        assertTrue(rendered.contains("RuntimeException"));
    }

    @Test
    public void standardLayoutRendersFlowMessageFaithfully() {
        LayoutAppender layoutAppender = new LayoutAppender();
        logger.addAppender(layoutAppender);

        String entryMsg = "Enter processRequest(userId=123)";
        logger.log(LogLevel.TRACE, entryMsg);

        assertNotNull(layoutAppender.lastFormatted);
        assertTrue(layoutAppender.lastFormatted.contains(entryMsg),
                "StandardLayout output should contain the flow tracing message verbatim");
    }

    @Test
    public void nullMessageDoesNotThrow() {
        assertDoesNotThrow(() -> logger.log(LogLevel.TRACE, null, null));
        assertEquals(1, testAppender.events.size());
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
