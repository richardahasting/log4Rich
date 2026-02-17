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
import com.log4rich.slf4j.Log4RichLoggerAdapter;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that LocationInfo.getCaller() correctly identifies the application
 * caller regardless of how many logging adapter layers are in the call chain.
 *
 * <p>This test lives outside the {@code com.log4rich} package intentionally —
 * the package-based skip logic treats {@code com.log4rich.*} as framework code,
 * so these tests must be "application code" to verify correct behavior.</p>
 *
 * Covers issues #17 (SLF4J adapter) and #15 (commons-logging bridge).
 */
public class LocationInfoCallerTest {

    private Logger log4richLogger;
    private CaptureAppender captureAppender;

    @BeforeEach
    void setUp() {
        log4richLogger = LogManager.getLogger("caller.test");
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

    // ──── Direct Logger ────

    @Test
    void directLoggerCallReportsThisTestClass() {
        log4richLogger.info("direct call");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "Direct logger call should report test class as caller");
        assertEquals("directLoggerCallReportsThisTestClass", loc.getMethodName());
    }

    @Test
    void directLoggerParameterizedCallReportsThisTestClass() {
        log4richLogger.info("value is {}", 42);

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "Parameterized logger call should report test class as caller");
    }

    // ──── SLF4J Adapter ────

    @Test
    void slf4jAdapterCallReportsThisTestClass() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        adapter.info("via SLF4J adapter");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "SLF4J adapter call should report test class, not adapter");
        assertEquals("slf4jAdapterCallReportsThisTestClass", loc.getMethodName());
    }

    @Test
    void slf4jAdapterWithFormattingReportsThisTestClass() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        adapter.info("value is {}", 42);

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "SLF4J adapter formatted call should report test class");
    }

    @Test
    void slf4jAdapterWithMarkerReportsThisTestClass() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        adapter.warn(org.slf4j.MarkerFactory.getMarker("TEST"), "marker message");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "SLF4J adapter marker call should report test class");
    }

    // ──── SLF4J Fluent API ────

    @Test
    void fluentApiReportsThisTestClass() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        adapter.atInfo().log("via fluent API");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "Fluent API call should report test class, not event builder");
        assertEquals("fluentApiReportsThisTestClass", loc.getMethodName());
    }

    @Test
    void fluentApiWithSetMessageReportsThisTestClass() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        adapter.atInfo()
                .setMessage("fluent with setMessage")
                .addArgument("arg")
                .log();

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        assertEquals("LocationInfoCallerTest", loc.getClassName(),
                "Fluent API with setMessage should report test class");
    }

    // ──── Simulated commons-logging / spring-jcl bridge ────

    @Test
    void simulatedCommonsLoggingWrapperSkipsWrapper() {
        Log4RichLoggerAdapter adapter = new Log4RichLoggerAdapter(log4richLogger);

        // Simulate the call chain:
        //   this test method → CommonsLoggingSimulator.logViaWrapper → adapter.info → Logger.log → getCaller
        // CommonsLoggingSimulator is in this package (caller.test), so getCaller() should find it
        // as the first non-logging frame — NOT the adapter or builder.
        CommonsLoggingSimulator.logViaWrapper(adapter, "via commons-logging bridge");

        LocationInfo loc = captureAppender.lastLocation;
        assertNotNull(loc, "LocationInfo should be captured");
        // The first non-logging frame is CommonsLoggingSimulator.logViaWrapper
        assertEquals("LocationInfoCallerTest$CommonsLoggingSimulator", loc.getClassName(),
                "Should report the immediate non-framework caller");
    }

    @Test
    void getCallerSkipsAllLoggingFrames() {
        // When called directly from this test, getCaller() should return this test class
        LocationInfo loc = LocationInfo.getCaller();
        assertNotNull(loc);
        assertEquals("LocationInfoCallerTest", loc.getClassName());
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

    /**
     * Simulates an intermediate wrapper class (like Spring's Slf4jLogFactory$Slf4jLog)
     * that sits between application code and the SLF4J adapter.
     */
    static class CommonsLoggingSimulator {
        static void logViaWrapper(Log4RichLoggerAdapter adapter, String msg) {
            adapter.info(msg);
        }
    }
}
