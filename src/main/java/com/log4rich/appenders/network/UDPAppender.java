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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Appender that sends log messages over UDP.
 *
 * <p>UDP provides fast, connectionless delivery of log messages. Messages may be
 * lost or arrive out of order, but UDP has lower latency than TCP.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * UDPAppender appender = new UDPAppender("logserver.example.com", 9998);
 * appender.setName("UDPAppender");
 * logger.addAppender(appender);
 * }</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class UDPAppender extends NetworkAppender {

    private DatagramSocket socket;
    private InetAddress address;
    private Charset charset = StandardCharsets.UTF_8;
    private int maxPacketSize = 8192;  // Max UDP packet size

    /**
     * Creates a new UDP appender.
     *
     * @param host the target host
     * @param port the target port
     */
    public UDPAppender(String host, int port) {
        super(host, port);
        this.name = "UDP-" + host + ":" + port;
        this.maxRetries = 0;  // UDP is fire-and-forget
    }

    @Override
    protected synchronized void connect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        address = InetAddress.getByName(host);
        socket = new DatagramSocket();

        System.out.println("[log4Rich] UDP appender initialized for " + host + ":" + port);
    }

    @Override
    protected synchronized void disconnect() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        address = null;
        connected.set(false);
        System.out.println("[log4Rich] UDP appender closed");
    }

    @Override
    protected synchronized void sendMessage(String message) throws IOException {
        if (socket == null || address == null) {
            throw new IOException("Not initialized");
        }

        byte[] bytes = message.getBytes(charset);

        // Truncate if too large for UDP
        if (bytes.length > maxPacketSize) {
            byte[] truncated = new byte[maxPacketSize];
            System.arraycopy(bytes, 0, truncated, 0, maxPacketSize);
            bytes = truncated;
        }

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        socket.send(packet);
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
     * Sets the maximum packet size.
     * Messages larger than this will be truncated.
     *
     * @param maxPacketSize maximum packet size in bytes
     */
    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    /**
     * Gets the maximum packet size.
     *
     * @return maximum packet size in bytes
     */
    public int getMaxPacketSize() {
        return maxPacketSize;
    }
}
