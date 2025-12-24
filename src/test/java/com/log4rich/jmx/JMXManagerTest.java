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

package com.log4rich.jmx;

import com.log4rich.Log4Rich;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JMX management functionality.
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class JMXManagerTest {

    @BeforeEach
    void setUp() {
        // Ensure clean state
        JMXManager.unregister();
    }

    @AfterEach
    void tearDown() {
        JMXManager.unregister();
    }

    @Test
    void testMBeanRegistration() throws Exception {
        assertFalse(JMXManager.isRegistered());

        boolean result = JMXManager.register();

        assertTrue(result);
        assertTrue(JMXManager.isRegistered());

        // Verify in MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName(JMXManager.getObjectName());
        assertTrue(mbs.isRegistered(objectName));
    }

    @Test
    void testMBeanUnregistration() throws Exception {
        JMXManager.register();
        assertTrue(JMXManager.isRegistered());

        JMXManager.unregister();

        assertFalse(JMXManager.isRegistered());

        // Verify removed from MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName(JMXManager.getObjectName());
        assertFalse(mbs.isRegistered(objectName));
    }

    @Test
    void testDoubleRegistration() {
        assertTrue(JMXManager.register());
        assertTrue(JMXManager.register());  // Should succeed, already registered
        assertTrue(JMXManager.isRegistered());
    }

    @Test
    void testDoubleUnregistration() {
        JMXManager.register();
        JMXManager.unregister();
        assertDoesNotThrow(() -> JMXManager.unregister());  // Should not throw
    }

    @Test
    void testObjectName() {
        assertEquals("com.log4rich:type=Log4Rich", JMXManager.getObjectName());
    }

    @Test
    void testMBeanInstance() {
        Log4RichMXBeanImpl mbean = JMXManager.getMBean();
        assertNotNull(mbean);
        assertSame(mbean, Log4RichMXBeanImpl.getInstance());
    }

    @Test
    void testVersionInfo() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        assertNotNull(mbean.getVersion());
        assertFalse(mbean.getVersion().isEmpty());

        assertNotNull(mbean.getBuildTimestamp());
    }

    @Test
    void testRootLevel() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        // Set and get root level
        mbean.setRootLevel("DEBUG");
        assertEquals("DEBUG", mbean.getRootLevel());

        mbean.setRootLevel("WARN");
        assertEquals("WARN", mbean.getRootLevel());
    }

    @Test
    void testInvalidLogLevel() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        assertThrows(IllegalArgumentException.class, () -> {
            mbean.setRootLevel("INVALID");
        });
    }

    @Test
    void testLoggerLevel() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        mbean.setLoggerLevel("com.test", "ERROR");
        assertEquals("ERROR", mbean.getLoggerLevel("com.test"));
    }

    @Test
    void testLoggerLevelsMap() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        Map<String, String> levels = mbean.getLoggerLevels();
        assertNotNull(levels);
        assertTrue(levels.containsKey("ROOT"));
    }

    @Test
    void testRootAppenders() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        List<String> appenders = mbean.getRootAppenders();
        assertNotNull(appenders);
    }

    @Test
    void testStatisticsRecording() {
        Log4RichMXBeanImpl mbean = JMXManager.getMBean();

        // Reset to known state
        mbean.resetStatistics();
        assertEquals(0, mbean.getTotalMessagesLogged());

        // Record some messages
        mbean.recordMessage(LogLevel.INFO);
        mbean.recordMessage(LogLevel.INFO);
        mbean.recordMessage(LogLevel.ERROR);

        assertEquals(3, mbean.getTotalMessagesLogged());

        Map<String, Long> perLevel = mbean.getMessagesPerLevel();
        assertEquals(Long.valueOf(2), perLevel.get("INFO"));
        assertEquals(Long.valueOf(1), perLevel.get("ERROR"));
    }

    @Test
    void testResetStatistics() {
        Log4RichMXBeanImpl mbean = JMXManager.getMBean();

        mbean.recordMessage(LogLevel.DEBUG);
        mbean.recordMessage(LogLevel.INFO);
        assertTrue(mbean.getTotalMessagesLogged() > 0);

        mbean.resetStatistics();

        assertEquals(0, mbean.getTotalMessagesLogged());
        for (Long count : mbean.getMessagesPerLevel().values()) {
            assertEquals(Long.valueOf(0), count);
        }
    }

    @Test
    void testConfigurationSummary() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        String summary = mbean.getConfigurationSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("log4Rich"));
        assertTrue(summary.contains("Version"));
        assertTrue(summary.contains("Root Level"));
    }

    @Test
    void testActiveCounters() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        int loggerCount = mbean.getActiveLoggerCount();
        assertTrue(loggerCount >= 0);

        int appenderCount = mbean.getActiveAppenderCount();
        assertTrue(appenderCount >= 0);
    }

    @Test
    void testFlushAll() {
        Log4RichMXBean mbean = JMXManager.getMBean();

        // Should not throw even if no appenders have flush
        assertDoesNotThrow(mbean::flushAll);
    }

    @Test
    void testJMXAccessViaServer() throws Exception {
        JMXManager.register();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName(JMXManager.getObjectName());

        // Access attributes via JMX
        String version = (String) mbs.getAttribute(objectName, "Version");
        assertNotNull(version);
        assertEquals(Log4Rich.getVersion(), version);

        String rootLevel = (String) mbs.getAttribute(objectName, "RootLevel");
        assertNotNull(rootLevel);

        // getConfigurationSummary is exposed as an attribute (getter), not an operation
        String summary = (String) mbs.getAttribute(objectName, "ConfigurationSummary");
        assertNotNull(summary);
        assertTrue(summary.contains("log4Rich"));

        // Invoke operation (void operation) via JMX
        assertDoesNotThrow(() -> mbs.invoke(objectName, "flushAll", null, null));
    }
}
