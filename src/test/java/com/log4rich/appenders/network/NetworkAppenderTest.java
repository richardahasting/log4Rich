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
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for network appenders.
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class NetworkAppenderTest {

    @Test
    void testTCPAppenderCreation() {
        TCPAppender appender = new TCPAppender("localhost", 9999);

        assertEquals("localhost", appender.getHost());
        assertEquals(9999, appender.getPort());
        assertEquals(StandardCharsets.UTF_8, appender.getCharset());
        assertFalse(appender.isConnected());
        assertEquals(0, appender.getMessagesSent());

        appender.close();
    }

    @Test
    void testTCPAppenderConfiguration() {
        TCPAppender appender = new TCPAppender("localhost", 9999);

        appender.setConnectionTimeout(10000);
        appender.setReconnectDelay(5000);
        appender.setMaxRetries(5);
        appender.setKeepAlive(false);
        appender.setSoTimeout(60000);
        appender.setCharset(StandardCharsets.ISO_8859_1);

        assertEquals(StandardCharsets.ISO_8859_1, appender.getCharset());

        appender.close();
    }

    @Test
    void testUDPAppenderCreation() {
        UDPAppender appender = new UDPAppender("localhost", 9998);

        assertEquals("localhost", appender.getHost());
        assertEquals(9998, appender.getPort());
        assertEquals(8192, appender.getMaxPacketSize());
        assertFalse(appender.isConnected());

        appender.close();
    }

    @Test
    void testUDPAppenderConfiguration() {
        UDPAppender appender = new UDPAppender("localhost", 9998);

        appender.setMaxPacketSize(4096);
        appender.setCharset(StandardCharsets.UTF_16);

        assertEquals(4096, appender.getMaxPacketSize());
        assertEquals(StandardCharsets.UTF_16, appender.getCharset());

        appender.close();
    }

    @Test
    void testSyslogAppenderCreation() {
        SyslogAppender appender = new SyslogAppender("localhost");

        assertEquals("localhost", appender.getHost());
        assertEquals(514, appender.getPort());  // Default syslog port
        assertEquals(SyslogAppender.Facility.LOCAL0, appender.getFacility());
        assertEquals("log4Rich", appender.getAppName());
        assertNotNull(appender.getHostname());

        appender.close();
    }

    @Test
    void testSyslogAppenderWithPort() {
        SyslogAppender appender = new SyslogAppender("syslog.example.com", 1514);

        assertEquals("syslog.example.com", appender.getHost());
        assertEquals(1514, appender.getPort());

        appender.close();
    }

    @Test
    void testSyslogAppenderConfiguration() {
        SyslogAppender appender = new SyslogAppender("localhost");

        appender.setFacility(SyslogAppender.Facility.LOCAL7);
        appender.setAppName("myapp");
        appender.setHostname("myhost");

        assertEquals(SyslogAppender.Facility.LOCAL7, appender.getFacility());
        assertEquals("myapp", appender.getAppName());
        assertEquals("myhost", appender.getHostname());

        appender.close();
    }

    @Test
    void testSyslogFacilities() {
        assertEquals(0, SyslogAppender.Facility.KERN.getCode());
        assertEquals(1, SyslogAppender.Facility.USER.getCode());
        assertEquals(16, SyslogAppender.Facility.LOCAL0.getCode());
        assertEquals(23, SyslogAppender.Facility.LOCAL7.getCode());
    }

    @Test
    void testSyslogSeverities() {
        assertEquals(0, SyslogAppender.Severity.EMERGENCY.getCode());
        assertEquals(2, SyslogAppender.Severity.CRITICAL.getCode());
        assertEquals(3, SyslogAppender.Severity.ERROR.getCode());
        assertEquals(4, SyslogAppender.Severity.WARNING.getCode());
        assertEquals(6, SyslogAppender.Severity.INFO.getCode());
        assertEquals(7, SyslogAppender.Severity.DEBUG.getCode());
    }

    @Test
    void testAppenderNameGeneration() {
        TCPAppender tcp = new TCPAppender("host1", 1111);
        UDPAppender udp = new UDPAppender("host2", 2222);
        SyslogAppender syslog = new SyslogAppender("host3", 3333);

        assertTrue(tcp.getName().contains("TCP"));
        assertTrue(tcp.getName().contains("host1"));

        assertTrue(udp.getName().contains("UDP"));
        assertTrue(udp.getName().contains("host2"));

        assertTrue(syslog.getName().contains("Syslog"));
        assertTrue(syslog.getName().contains("host3"));

        tcp.close();
        udp.close();
        syslog.close();
    }

    @Test
    void testAppenderClose() {
        TCPAppender appender = new TCPAppender("localhost", 9999);

        assertDoesNotThrow(appender::close);
        // Double close should not throw
        assertDoesNotThrow(appender::close);
    }

    @Test
    void testLoggingEventFormat() {
        // Create a simple logging event
        LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                "Test message",
                "TestLogger",
                null  // LocationInfo
        );

        assertNotNull(event.getMessage());
        assertEquals(LogLevel.INFO, event.getLevel());
    }
}
