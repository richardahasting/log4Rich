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

package com.log4rich.appenders.jdbc;

import com.log4rich.core.LogLevel;
import com.log4rich.util.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JDBCAppender using H2 in-memory database.
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class JDBCAppenderTest {

    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private JDBCAppender appender;

    @BeforeEach
    void setUp() {
        appender = new JDBCAppender(JDBC_URL);
        appender.setTableName("test_logs");
        appender.setBatchSize(1);  // Immediate flush for testing
        appender.setAutoCreateTable(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (appender != null) {
            appender.close();
        }
        // Clean up the database
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_logs");
        }
    }

    @Test
    void testAppenderCreation() {
        assertNotNull(appender);
        assertEquals(JDBC_URL, appender.getJdbcUrl());
        assertEquals("test_logs", appender.getTableName());
        assertEquals(1, appender.getBatchSize());
        assertTrue(appender.isAutoCreateTable());
        assertFalse(appender.isClosed());
    }

    @Test
    void testAppenderConfiguration() {
        appender.setTableName("custom_logs");
        appender.setBatchSize(50);
        appender.setFlushInterval(10000);
        appender.setAutoCreateTable(false);
        appender.setName("CustomJDBC");

        assertEquals("custom_logs", appender.getTableName());
        assertEquals(50, appender.getBatchSize());
        assertEquals(10000, appender.getFlushInterval());
        assertFalse(appender.isAutoCreateTable());
        assertEquals("CustomJDBC", appender.getName());
    }

    @Test
    void testSingleLogEvent() throws Exception {
        LoggingEvent event = new LoggingEvent(
            LogLevel.INFO,
            "Test message",
            "TestLogger",
            null
        );

        appender.append(event);
        appender.flush();

        // Verify the log was written
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_logs")) {

            assertTrue(rs.next());
            assertEquals("INFO", rs.getString("level"));
            assertEquals("TestLogger", rs.getString("logger"));
            assertTrue(rs.getString("message").contains("Test message"));
            assertFalse(rs.next());  // Only one record
        }

        assertEquals(1, appender.getMessagesWritten());
        assertEquals(0, appender.getMessagesFailed());
    }

    @Test
    void testMultipleLogEvents() throws Exception {
        for (int i = 0; i < 5; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.DEBUG,
                "Message " + i,
                "TestLogger",
                null
            );
            appender.append(event);
        }
        appender.flush();

        // Verify all logs were written
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_logs")) {

            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }

        assertEquals(5, appender.getMessagesWritten());
    }

    @Test
    void testLogLevels() throws Exception {
        LogLevel[] levels = {LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO,
                           LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL};

        for (LogLevel level : levels) {
            LoggingEvent event = new LoggingEvent(
                level,
                level.name() + " message",
                "TestLogger",
                null
            );
            appender.append(event);
        }
        appender.flush();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT level FROM test_logs ORDER BY id")) {

            for (LogLevel level : levels) {
                assertTrue(rs.next());
                assertEquals(level.name(), rs.getString("level"));
            }
        }
    }

    @Test
    void testExceptionLogging() throws Exception {
        Exception testException = new RuntimeException("Test exception");

        LoggingEvent event = new LoggingEvent(
            LogLevel.ERROR,
            "Error occurred",
            "TestLogger",
            null,
            testException
        );

        appender.append(event);
        appender.flush();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT exception FROM test_logs")) {

            assertTrue(rs.next());
            String exception = rs.getString("exception");
            assertNotNull(exception);
            assertTrue(exception.contains("RuntimeException"));
            assertTrue(exception.contains("Test exception"));
        }
    }

    @Test
    void testLevelFiltering() throws Exception {
        appender.setLevel(LogLevel.WARN);

        // This should be filtered out
        LoggingEvent debugEvent = new LoggingEvent(
            LogLevel.DEBUG,
            "Debug message",
            "TestLogger",
            null
        );
        appender.append(debugEvent);

        // This should be written
        LoggingEvent errorEvent = new LoggingEvent(
            LogLevel.ERROR,
            "Error message",
            "TestLogger",
            null
        );
        appender.append(errorEvent);
        appender.flush();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_logs")) {

            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));  // Only ERROR message
        }
    }

    @Test
    void testBatchFlush() throws Exception {
        appender.setBatchSize(3);

        // Add 2 events (under batch size)
        for (int i = 0; i < 2; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                "Message " + i,
                "TestLogger",
                null
            );
            appender.append(event);
        }

        // Should not be written yet (batch not full)
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM test_logs WHERE 1=1")) {
            // Table may not exist yet or be empty
        } catch (Exception e) {
            // Expected - table might not exist yet
        }

        // Add third event to trigger batch
        LoggingEvent event = new LoggingEvent(
            LogLevel.INFO,
            "Message 2",
            "TestLogger",
            null
        );
        appender.append(event);

        // Now all 3 should be written
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_logs")) {

            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    void testClose() {
        assertFalse(appender.isClosed());

        appender.close();

        assertTrue(appender.isClosed());
        assertFalse(appender.isConnected());
    }

    @Test
    void testDoubleClose() {
        appender.close();
        assertDoesNotThrow(() -> appender.close());
    }

    @Test
    void testAppendAfterClose() throws Exception {
        appender.close();

        LoggingEvent event = new LoggingEvent(
            LogLevel.INFO,
            "Should not be written",
            "TestLogger",
            null
        );
        appender.append(event);

        // Should not throw, just silently ignore
        assertEquals(0, appender.getMessagesWritten());
    }

    @Test
    void testStatistics() throws Exception {
        for (int i = 0; i < 10; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                "Message " + i,
                "TestLogger",
                null
            );
            appender.append(event);
        }
        appender.flush();

        assertEquals(10, appender.getMessagesWritten());
        assertEquals(0, appender.getMessagesFailed());
        assertTrue(appender.getBatchesWritten() >= 1);
    }
}
