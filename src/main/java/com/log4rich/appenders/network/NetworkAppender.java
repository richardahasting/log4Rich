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

package com.log4rich.appenders.network;

import com.log4rich.appenders.Appender;
import com.log4rich.core.LogLevel;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.LoggingEvent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for network-based appenders.
 *
 * <p>Provides common functionality for TCP, UDP, and Syslog appenders including
 * connection management, retry logic, and error handling.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public abstract class NetworkAppender implements Appender {

    protected final String host;
    protected final int port;
    protected int connectionTimeout = 5000;  // 5 seconds
    protected int reconnectDelay = 3000;     // 3 seconds
    protected int maxRetries = 3;

    protected String name;
    protected Layout layout;
    protected LogLevel level = LogLevel.ALL;
    protected volatile boolean closed = false;

    protected final AtomicBoolean connected = new AtomicBoolean(false);
    protected final AtomicLong messagesSent = new AtomicLong(0);
    protected final AtomicLong messagesFailed = new AtomicLong(0);

    /**
     * Creates a new network appender.
     *
     * @param host the target host
     * @param port the target port
     */
    public NetworkAppender(String host, int port) {
        this.host = host;
        this.port = port;
        this.layout = new StandardLayout("%message%n");
    }

    /**
     * Connects to the remote host.
     *
     * @throws IOException if connection fails
     */
    protected abstract void connect() throws IOException;

    /**
     * Disconnects from the remote host.
     */
    protected abstract void disconnect();

    /**
     * Sends a formatted message to the remote host.
     *
     * @param message the message to send
     * @throws IOException if sending fails
     */
    protected abstract void sendMessage(String message) throws IOException;

    @Override
    public void append(LoggingEvent event) {
        if (closed) {
            return;
        }

        String message = layout.format(event);

        int retries = 0;
        while (retries <= maxRetries) {
            try {
                if (!connected.get()) {
                    connect();
                    connected.set(true);
                }

                sendMessage(message);
                messagesSent.incrementAndGet();
                return;

            } catch (IOException e) {
                connected.set(false);
                retries++;

                if (retries <= maxRetries) {
                    System.err.println("[log4Rich] Network appender " + name +
                            " failed to send, retry " + retries + "/" + maxRetries + ": " + e.getMessage());
                    try {
                        Thread.sleep(reconnectDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    messagesFailed.incrementAndGet();
                    System.err.println("[log4Rich] Network appender " + name +
                            " failed after " + maxRetries + " retries: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        disconnect();
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

    /**
     * Gets the target host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the target port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param timeout timeout in milliseconds
     */
    public void setConnectionTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }

    /**
     * Sets the delay between reconnection attempts.
     *
     * @param delay delay in milliseconds
     */
    public void setReconnectDelay(int delay) {
        this.reconnectDelay = delay;
    }

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxRetries maximum retries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Checks if currently connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Gets the count of successfully sent messages.
     *
     * @return messages sent count
     */
    public long getMessagesSent() {
        return messagesSent.get();
    }

    /**
     * Gets the count of failed messages.
     *
     * @return messages failed count
     */
    public long getMessagesFailed() {
        return messagesFailed.get();
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
}
