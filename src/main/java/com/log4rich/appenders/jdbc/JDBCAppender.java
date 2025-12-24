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

import com.log4rich.appenders.Appender;
import com.log4rich.core.LogLevel;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.LoggingEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Appender that writes log messages to a database using JDBC.
 *
 * <p>This appender supports batch inserts for improved performance and
 * automatic table creation if the target table doesn't exist.</p>
 *
 * <h2>Default Table Schema:</h2>
 * <pre>
 * CREATE TABLE log_entries (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     timestamp TIMESTAMP NOT NULL,
 *     level VARCHAR(10) NOT NULL,
 *     logger VARCHAR(255) NOT NULL,
 *     thread VARCHAR(255),
 *     message TEXT,
 *     exception TEXT
 * );
 * </pre>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * JDBCAppender appender = new JDBCAppender(
 *     "jdbc:mysql://localhost:3306/logs",
 *     "loguser",
 *     "password"
 * );
 * appender.setTableName("application_logs");
 * appender.setBatchSize(50);
 * logger.addAppender(appender);
 * }</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class JDBCAppender implements Appender {

    private static final String DEFAULT_TABLE_NAME = "log_entries";
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final long DEFAULT_FLUSH_INTERVAL = 5000; // 5 seconds

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private String tableName = DEFAULT_TABLE_NAME;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private long flushInterval = DEFAULT_FLUSH_INTERVAL;
    private boolean autoCreateTable = true;

    private String name = "JDBCAppender";
    private Layout layout = new StandardLayout("%message");
    private LogLevel level = LogLevel.ALL;
    private volatile boolean closed = false;
    private volatile boolean initialized = false;

    private Connection connection;
    private PreparedStatement insertStatement;
    private final List<LoggingEvent> batchBuffer = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private long lastFlushTime = System.currentTimeMillis();

    private final AtomicLong messagesWritten = new AtomicLong(0);
    private final AtomicLong messagesFailed = new AtomicLong(0);
    private final AtomicLong batchesWritten = new AtomicLong(0);

    /**
     * Creates a new JDBC appender.
     *
     * @param jdbcUrl  the JDBC connection URL
     * @param username the database username
     * @param password the database password
     */
    public JDBCAppender(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a new JDBC appender with connection URL only (for databases without auth).
     *
     * @param jdbcUrl the JDBC connection URL
     */
    public JDBCAppender(String jdbcUrl) {
        this(jdbcUrl, null, null);
    }

    /**
     * Initializes the database connection and creates the table if needed.
     *
     * @throws SQLException if database initialization fails
     */
    private synchronized void initialize() throws SQLException {
        if (initialized) {
            return;
        }

        // Create connection
        if (username != null && password != null) {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        } else {
            connection = DriverManager.getConnection(jdbcUrl);
        }

        // Auto-create table if configured
        if (autoCreateTable) {
            createTableIfNotExists();
        }

        // Prepare the insert statement
        String sql = String.format(
            "INSERT INTO %s (timestamp, level, logger, thread, message, exception) VALUES (?, ?, ?, ?, ?, ?)",
            tableName
        );
        insertStatement = connection.prepareStatement(sql);

        initialized = true;
        System.out.println("[log4Rich] JDBC appender initialized for " + jdbcUrl);
    }

    /**
     * Creates the log table if it doesn't exist.
     *
     * @throws SQLException if table creation fails
     */
    private void createTableIfNotExists() throws SQLException {
        String createTableSql = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "timestamp TIMESTAMP NOT NULL, " +
            "level VARCHAR(10) NOT NULL, " +
            "logger VARCHAR(255) NOT NULL, " +
            "thread VARCHAR(255), " +
            "message TEXT, " +
            "exception TEXT" +
            ")",
            tableName
        );

        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    @Override
    public void append(LoggingEvent event) {
        if (closed) {
            return;
        }

        if (!isLevelEnabled(event.getLevel())) {
            return;
        }

        lock.lock();
        try {
            // Initialize on first use
            if (!initialized) {
                try {
                    initialize();
                } catch (SQLException e) {
                    messagesFailed.incrementAndGet();
                    System.err.println("[log4Rich] JDBC appender initialization failed: " + e.getMessage());
                    return;
                }
            }

            batchBuffer.add(event);

            // Flush if batch is full or interval has elapsed
            if (batchBuffer.size() >= batchSize ||
                (System.currentTimeMillis() - lastFlushTime) >= flushInterval) {
                flush();
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * Flushes the batch buffer to the database.
     */
    public void flush() {
        lock.lock();
        try {
            if (batchBuffer.isEmpty() || insertStatement == null) {
                return;
            }

            for (LoggingEvent event : batchBuffer) {
                try {
                    insertStatement.setTimestamp(1, new Timestamp(event.getTimestamp()));
                    insertStatement.setString(2, event.getLevel().name());
                    insertStatement.setString(3, event.getLoggerName());
                    insertStatement.setString(4, event.getThreadName());
                    insertStatement.setString(5, layout.format(event));
                    insertStatement.setString(6, formatException(event.getThrowable()));
                    insertStatement.addBatch();
                } catch (SQLException e) {
                    messagesFailed.incrementAndGet();
                    System.err.println("[log4Rich] JDBC appender failed to add batch: " + e.getMessage());
                }
            }

            try {
                int[] results = insertStatement.executeBatch();
                int successCount = 0;
                for (int result : results) {
                    if (result >= 0 || result == PreparedStatement.SUCCESS_NO_INFO) {
                        successCount++;
                    }
                }
                messagesWritten.addAndGet(successCount);
                messagesFailed.addAndGet(batchBuffer.size() - successCount);
                batchesWritten.incrementAndGet();

            } catch (SQLException e) {
                messagesFailed.addAndGet(batchBuffer.size());
                System.err.println("[log4Rich] JDBC appender batch execution failed: " + e.getMessage());

                // Try to reconnect on next append
                closeConnection();
                initialized = false;
            }

            batchBuffer.clear();
            lastFlushTime = System.currentTimeMillis();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Formats an exception as a string.
     *
     * @param throwable the exception
     * @return the formatted exception or null
     */
    private String formatException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Closes the database connection.
     */
    private void closeConnection() {
        if (insertStatement != null) {
            try {
                insertStatement.close();
            } catch (SQLException e) {
                // Ignore
            }
            insertStatement = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
            connection = null;
        }
    }

    @Override
    public void close() {
        closed = true;
        lock.lock();
        try {
            // Flush any remaining events
            if (!batchBuffer.isEmpty() && insertStatement != null) {
                flush();
            }
            closeConnection();
            System.out.println("[log4Rich] JDBC appender closed. Statistics: written=" +
                messagesWritten.get() + ", failed=" + messagesFailed.get() +
                ", batches=" + batchesWritten.get());
        } finally {
            lock.unlock();
        }
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
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(Layout layout) {
        this.layout = layout;
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

    @Override
    public boolean isClosed() {
        return closed;
    }

    // Configuration methods

    /**
     * Sets the database table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets the database table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the batch size for inserts.
     *
     * @param batchSize the batch size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Gets the batch size.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the flush interval in milliseconds.
     *
     * @param flushInterval the flush interval
     */
    public void setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
    }

    /**
     * Gets the flush interval.
     *
     * @return the flush interval in milliseconds
     */
    public long getFlushInterval() {
        return flushInterval;
    }

    /**
     * Enables or disables automatic table creation.
     *
     * @param autoCreateTable true to auto-create the table
     */
    public void setAutoCreateTable(boolean autoCreateTable) {
        this.autoCreateTable = autoCreateTable;
    }

    /**
     * Gets whether auto table creation is enabled.
     *
     * @return true if auto-create is enabled
     */
    public boolean isAutoCreateTable() {
        return autoCreateTable;
    }

    /**
     * Gets the JDBC URL.
     *
     * @return the JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Gets the count of successfully written messages.
     *
     * @return messages written count
     */
    public long getMessagesWritten() {
        return messagesWritten.get();
    }

    /**
     * Gets the count of failed messages.
     *
     * @return messages failed count
     */
    public long getMessagesFailed() {
        return messagesFailed.get();
    }

    /**
     * Gets the count of batches written.
     *
     * @return batches written count
     */
    public long getBatchesWritten() {
        return batchesWritten.get();
    }

    /**
     * Checks if the appender is connected to the database.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
