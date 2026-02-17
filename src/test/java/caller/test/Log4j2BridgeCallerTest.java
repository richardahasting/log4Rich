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

package caller.test;

import com.log4rich.Log4Rich;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;
import org.apache.logging.log4j.spi.Log4j2BridgeSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link LocationInfo#getCaller()} correctly skips classes
 * in the {@code org.apache.logging.log4j.*} package (the log4j2-log4Rich bridge).
 *
 * <p>This test lives in {@code caller.test} (outside {@code com.log4rich.*})
 * so that the test class itself is treated as application code by the
 * package-based skip logic.</p>
 *
 * Covers GitHub issue #20: Support ExtendedLoggerWrapper for correct SLF4J caller location.
 */
public class Log4j2BridgeCallerTest {

    private Logger log4richLogger;
    private CaptureAppender captureAppender;

    @BeforeEach
    void setUp() {
        log4richLogger = LogManager.getLogger("caller.test.bridge");
        log4richLogger.setLevel(LogLevel.TRACE);
        log4richLogger.setLocationCapture(true);
        captureAppender = new CaptureAppender();
        log4richLogger.addAppender(captureAppender);
    }

    @AfterEach
    void tearDown() {
        log4richLogger.clearAppenders();
        Log4Rich.shutdown();
    }

    @Test
    void directCallReportsTestClass() {
        log4richLogger.info("direct call");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("Log4j2BridgeCallerTest", loc.getClassName(),
                "Direct call should report test class as caller");
        assertEquals("directCallReportsTestClass", loc.getMethodName());
    }

    @Test
    void log4j2BridgeSimulatorIsSkipped() {
        Log4j2BridgeSimulator.logViaSimulatedBridge(log4richLogger, "via bridge");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("Log4j2BridgeCallerTest", loc.getClassName(),
                "Bridge simulator in org.apache.logging.log4j.spi should be skipped");
        assertEquals("log4j2BridgeSimulatorIsSkipped", loc.getMethodName());
    }

    @Test
    void twoLevelBridgeSimulatorIsSkipped() {
        Log4j2BridgeSimulator.logViaTwoLevelBridge(log4richLogger, "via two-level bridge");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("Log4j2BridgeCallerTest", loc.getClassName(),
                "Two-level bridge in org.apache.logging.log4j.spi should be fully skipped");
        assertEquals("twoLevelBridgeSimulatorIsSkipped", loc.getMethodName());
    }

    // ──── Helpers ────

    /**
     * Appender that captures the last LocationInfo for assertion.
     */
    static class CaptureAppender extends ConsoleAppender {
        volatile LocationInfo lastLocation;

        CaptureAppender() {
            super();
            setName("capture");
        }

        @Override
        public void append(LoggingEvent event) {
            lastLocation = event.getLocationInfo();
        }
    }
}
