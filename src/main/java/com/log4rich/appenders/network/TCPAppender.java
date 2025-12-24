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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Appender that sends log messages over TCP.
 *
 * <p>TCP provides reliable, ordered delivery of log messages. Messages are
 * sent over a persistent connection that is automatically reconnected on failure.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * TCPAppender appender = new TCPAppender("logserver.example.com", 9999);
 * appender.setName("TCPAppender");
 * logger.addAppender(appender);
 * }</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class TCPAppender extends NetworkAppender {

    private Socket socket;
    private OutputStream outputStream;
    private Charset charset = StandardCharsets.UTF_8;
    private boolean keepAlive = true;
    private int soTimeout = 30000;  // 30 seconds read timeout

    /**
     * Creates a new TCP appender.
     *
     * @param host the target host
     * @param port the target port
     */
    public TCPAppender(String host, int port) {
        super(host, port);
        this.name = "TCP-" + host + ":" + port;
    }

    @Override
    protected synchronized void connect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        socket = new Socket();
        socket.setKeepAlive(keepAlive);
        socket.setSoTimeout(soTimeout);
        socket.connect(new InetSocketAddress(host, port), connectionTimeout);
        outputStream = socket.getOutputStream();

        System.out.println("[log4Rich] TCP appender connected to " + host + ":" + port);
    }

    @Override
    protected synchronized void disconnect() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                // Ignore
            }
            outputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
            socket = null;
        }

        connected.set(false);
        System.out.println("[log4Rich] TCP appender disconnected from " + host + ":" + port);
    }

    @Override
    protected synchronized void sendMessage(String message) throws IOException {
        if (outputStream == null) {
            throw new IOException("Not connected");
        }

        byte[] bytes = message.getBytes(charset);
        outputStream.write(bytes);
        outputStream.flush();
    }

    /**
     * Sets the character encoding for messages.
     *
     * @param charset the charset to use
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     * Gets the character encoding.
     *
     * @return the charset
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Enables or disables TCP keep-alive.
     *
     * @param keepAlive true to enable keep-alive
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Sets the socket read timeout.
     *
     * @param timeout timeout in milliseconds
     */
    public void setSoTimeout(int timeout) {
        this.soTimeout = timeout;
    }
}
