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

import com.log4rich.core.LogLevel;
import com.log4rich.util.LoggingEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Appender that sends log messages using the Syslog protocol (RFC 3164).
 *
 * <p>Syslog is a standard protocol for system logging, widely supported by
 * log aggregation systems, network devices, and Unix/Linux systems.</p>
 *
 * <h2>Syslog Message Format (RFC 3164):</h2>
 * <pre>
 * &lt;PRI&gt;TIMESTAMP HOSTNAME TAG: MESSAGE
 * </pre>
 *
 * <h2>Facilities:</h2>
 * <ul>
 *   <li>0 - kernel messages</li>
 *   <li>1 - user-level messages</li>
 *   <li>16-23 - local0 through local7</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * SyslogAppender appender = new SyslogAppender("syslog.example.com", 514);
 * appender.setFacility(SyslogAppender.Facility.LOCAL0);
 * appender.setAppName("myapp");
 * logger.addAppender(appender);
 * }</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class SyslogAppender extends NetworkAppender {

    /**
     * Syslog facility codes.
     */
    public enum Facility {
        KERN(0),
        USER(1),
        MAIL(2),
        DAEMON(3),
        AUTH(4),
        SYSLOG(5),
        LPR(6),
        NEWS(7),
        UUCP(8),
        CRON(9),
        AUTHPRIV(10),
        FTP(11),
        LOCAL0(16),
        LOCAL1(17),
        LOCAL2(18),
        LOCAL3(19),
        LOCAL4(20),
        LOCAL5(21),
        LOCAL6(22),
        LOCAL7(23);

        private final int code;

        Facility(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Syslog severity codes.
     */
    public enum Severity {
        EMERGENCY(0),
        ALERT(1),
        CRITICAL(2),
        ERROR(3),
        WARNING(4),
        NOTICE(5),
        INFO(6),
        DEBUG(7);

        private final int code;

        Severity(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private static final int DEFAULT_PORT = 514;
    private static final int MAX_MESSAGE_LENGTH = 1024;  // RFC 3164 limit

    private DatagramSocket socket;
    private InetAddress address;
    private Charset charset = StandardCharsets.UTF_8;

    private Facility facility = Facility.LOCAL0;
    private String appName = "log4Rich";
    private String hostname;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH);

    /**
     * Creates a new Syslog appender with default port 514.
     *
     * @param host the syslog server host
     */
    public SyslogAppender(String host) {
        this(host, DEFAULT_PORT);
    }

    /**
     * Creates a new Syslog appender.
     *
     * @param host the syslog server host
     * @param port the syslog server port
     */
    public SyslogAppender(String host, int port) {
        super(host, port);
        this.name = "Syslog-" + host + ":" + port;
        this.maxRetries = 0;  // Syslog uses UDP, fire-and-forget

        // Get local hostname
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.hostname = "localhost";
        }
    }

    @Override
    protected synchronized void connect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        address = InetAddress.getByName(host);
        socket = new DatagramSocket();

        System.out.println("[log4Rich] Syslog appender initialized for " + host + ":" + port);
    }

    @Override
    protected synchronized void disconnect() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        address = null;
        connected.set(false);
        System.out.println("[log4Rich] Syslog appender closed");
    }

    @Override
    protected synchronized void sendMessage(String message) throws IOException {
        if (socket == null || address == null) {
            throw new IOException("Not initialized");
        }

        byte[] bytes = message.getBytes(charset);

        // Truncate if too large
        if (bytes.length > MAX_MESSAGE_LENGTH) {
            byte[] truncated = new byte[MAX_MESSAGE_LENGTH];
            System.arraycopy(bytes, 0, truncated, 0, MAX_MESSAGE_LENGTH);
            bytes = truncated;
        }

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        socket.send(packet);
    }

    @Override
    public void append(LoggingEvent event) {
        if (closed) {
            return;
        }

        String syslogMessage = formatSyslogMessage(event);

        try {
            if (!connected.get()) {
                connect();
                connected.set(true);
            }
            sendMessage(syslogMessage);
            messagesSent.incrementAndGet();
        } catch (IOException e) {
            messagesFailed.incrementAndGet();
            System.err.println("[log4Rich] Syslog appender failed: " + e.getMessage());
        }
    }

    /**
     * Formats a log event as an RFC 3164 syslog message.
     *
     * @param event the logging event
     * @return the formatted syslog message
     */
    private String formatSyslogMessage(LoggingEvent event) {
        // Calculate priority: facility * 8 + severity
        Severity severity = mapLogLevelToSeverity(event.getLevel());
        int priority = facility.getCode() * 8 + severity.getCode();

        // Format timestamp
        String timestamp;
        synchronized (dateFormat) {
            timestamp = dateFormat.format(new Date(event.getTimestamp()));
        }

        // Build message: <PRI>TIMESTAMP HOSTNAME TAG: MESSAGE
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(priority).append('>');
        sb.append(timestamp).append(' ');
        sb.append(hostname).append(' ');
        sb.append(appName).append(": ");
        sb.append(event.getMessage());

        return sb.toString();
    }

    /**
     * Maps a log4Rich LogLevel to a Syslog severity.
     *
     * @param level the log level
     * @return the corresponding syslog severity
     */
    private Severity mapLogLevelToSeverity(LogLevel level) {
        switch (level) {
            case FATAL:
            case CRITICAL:
                return Severity.CRITICAL;
            case ERROR:
                return Severity.ERROR;
            case WARN:
                return Severity.WARNING;
            case INFO:
                return Severity.INFO;
            case DEBUG:
            case TRACE:
                return Severity.DEBUG;
            default:
                return Severity.INFO;
        }
    }

    /**
     * Sets the syslog facility.
     *
     * @param facility the facility
     */
    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    /**
     * Gets the syslog facility.
     *
     * @return the facility
     */
    public Facility getFacility() {
        return facility;
    }

    /**
     * Sets the application name (TAG in syslog message).
     *
     * @param appName the application name
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Gets the application name.
     *
     * @return the application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets the hostname reported in syslog messages.
     *
     * @param hostname the hostname
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Gets the hostname.
     *
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the character encoding.
     *
     * @param charset the charset
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
