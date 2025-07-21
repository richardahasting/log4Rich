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

package com.log4rich.layouts;

import com.log4rich.core.LogLevel;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for JsonLayout functionality.
 */
public class JsonLayoutTest {
    
    private JsonLayout layout;
    private LoggingEvent basicEvent;
    private LoggingEvent eventWithLocation;
    private LoggingEvent eventWithException;
    
    @BeforeEach
    public void setUp() {
        layout = new JsonLayout();
        
        // Basic logging event
        basicEvent = new LoggingEvent(
            LogLevel.INFO,
            "Test message",
            "com.example.TestClass",
            null
        );
        
        // Event with location info
        LocationInfo location = new LocationInfo(
            "com.example.TestClass",
            "testMethod",
            "TestClass.java",
            42
        );
        eventWithLocation = new LoggingEvent(
            LogLevel.WARN,
            "Warning message",
            "com.example.TestClass",
            location
        );
        
        // Event with exception
        Exception testException = new RuntimeException("Test exception");
        eventWithException = new LoggingEvent(
            LogLevel.ERROR,
            "Error occurred",
            "com.example.TestClass",
            null,
            testException
        );
    }
    
    @Test
    public void testBasicJsonFormatting() {
        String json = layout.format(basicEvent);
        
        // Should be valid JSON
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Should contain basic fields
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"logger\":\"com.example.TestClass\""));
        assertTrue(json.contains("\"message\":\"Test message\""));
        assertTrue(json.contains("\"thread\""));
        
        // Should be compact format by default
        assertFalse(json.contains("\n"));
    }
    
    @Test
    public void testPrettyPrintFormatting() {
        JsonLayout prettyLayout = new JsonLayout(true, true, true, "yyyy-MM-dd");
        String json = prettyLayout.format(basicEvent);
        
        // Should contain newlines and indentation
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }
    
    @Test
    public void testLocationInformation() {
        String json = layout.format(eventWithLocation);
        
        // Should contain location information
        assertTrue(json.contains("\"location\""));
        assertTrue(json.contains("\"class\":\"com.example.TestClass\""));
        assertTrue(json.contains("\"method\":\"testMethod\""));
        assertTrue(json.contains("\"file\":\"TestClass.java\""));
        assertTrue(json.contains("\"line\":42"));
    }
    
    @Test
    public void testLocationInformationDisabled() {
        JsonLayout noLocationLayout = new JsonLayout(false, false, true, "yyyy-MM-dd");
        String json = noLocationLayout.format(eventWithLocation);
        
        // Should not contain location information
        assertFalse(json.contains("\"location\""));
        assertFalse(json.contains("\"class\""));
        assertFalse(json.contains("\"method\""));
        assertFalse(json.contains("\"file\""));
        assertFalse(json.contains("\"line\""));
    }
    
    @Test
    public void testThreadInformation() {
        String json = layout.format(basicEvent);
        
        // Should contain thread information by default
        assertTrue(json.contains("\"thread\""));
    }
    
    @Test
    public void testThreadInformationDisabled() {
        JsonLayout noThreadLayout = new JsonLayout(false, true, false, "yyyy-MM-dd");
        String json = noThreadLayout.format(basicEvent);
        
        // Should not contain thread information
        assertFalse(json.contains("\"thread\""));
    }
    
    @Test
    public void testExceptionHandling() {
        String json = layout.format(eventWithException);
        
        // Should contain exception information
        assertTrue(json.contains("\"exception\""));
        assertTrue(json.contains("\"class\":\"java.lang.RuntimeException\""));
        assertTrue(json.contains("\"message\":\"Test exception\""));
        assertTrue(json.contains("\"stackTrace\""));
    }
    
    @Test
    public void testJsonEscaping() {
        LoggingEvent eventWithSpecialChars = new LoggingEvent(
            LogLevel.INFO,
            "Message with \"quotes\" and \n newlines and \t tabs",
            "com.example.TestClass",
            null
        );
        
        String json = layout.format(eventWithSpecialChars);
        
        // Should properly escape special characters
        assertTrue(json.contains("\\\"quotes\\\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        
        // Should still be valid JSON - check that we don't have literal unescaped newlines/tabs
        assertFalse(json.contains("\"\n"));  // No literal newline after quote
        assertFalse(json.contains("\"\t"));  // No literal tab after quote
    }
    
    @Test
    public void testAdditionalFields() {
        layout.addAdditionalField("application", "MyApp");
        layout.addAdditionalField("version", "1.0.0");
        layout.addAdditionalField("environment", "test");
        
        String json = layout.format(basicEvent);
        
        // Should contain additional fields
        assertTrue(json.contains("\"application\":\"MyApp\""));
        assertTrue(json.contains("\"version\":\"1.0.0\""));
        assertTrue(json.contains("\"environment\":\"test\""));
    }
    
    @Test
    public void testAdditionalFieldRemoval() {
        layout.addAdditionalField("temp", "value");
        layout.removeAdditionalField("temp");
        
        String json = layout.format(basicEvent);
        
        // Should not contain removed field
        assertFalse(json.contains("\"temp\""));
    }
    
    @Test
    public void testTimestampFormat() {
        JsonLayout customLayout = new JsonLayout(false, true, true, "yyyy-MM-dd HH:mm:ss");
        String json = customLayout.format(basicEvent);
        
        // Should contain timestamp in custom format
        assertTrue(json.contains("\"timestamp\""));
        // Timestamp should follow the pattern (4 digits - 2 digits - 2 digits space 2 digits : 2 digits : 2 digits)
        assertTrue(json.matches(".*\"timestamp\":\"\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\".*"));
    }
    
    @Test
    public void testIgnoresThrowable() {
        // JsonLayout should handle throwables, so it should return false
        assertFalse(layout.ignoresThrowable());
    }
    
    @Test
    public void testHeaderAndFooter() {
        // JSON layout typically doesn't use headers or footers
        assertNull(layout.getHeader());
        assertNull(layout.getFooter());
    }
    
    @Test
    public void testFactoryMethods() {
        // Test compact layout
        JsonLayout compact = JsonLayout.createCompactLayout();
        assertFalse(compact.isPrettyPrint());
        assertTrue(compact.isIncludeLocationInfo());
        assertTrue(compact.isIncludeThreadInfo());
        
        // Test pretty layout
        JsonLayout pretty = JsonLayout.createPrettyLayout();
        assertTrue(pretty.isPrettyPrint());
        assertTrue(pretty.isIncludeLocationInfo());
        assertTrue(pretty.isIncludeThreadInfo());
        
        // Test minimal layout
        JsonLayout minimal = JsonLayout.createMinimalLayout();
        assertFalse(minimal.isPrettyPrint());
        assertFalse(minimal.isIncludeLocationInfo());
        assertFalse(minimal.isIncludeThreadInfo());
        
        // Test production layout
        JsonLayout production = JsonLayout.createProductionLayout();
        assertFalse(production.isPrettyPrint());
        assertFalse(production.isIncludeLocationInfo());
        assertTrue(production.isIncludeThreadInfo());
    }
    
    @Test
    public void testGetters() {
        JsonLayout customLayout = new JsonLayout(true, false, true, "yyyy-MM-dd");
        
        assertTrue(customLayout.isPrettyPrint());
        assertFalse(customLayout.isIncludeLocationInfo());
        assertTrue(customLayout.isIncludeThreadInfo());
        assertEquals("yyyy-MM-dd", customLayout.getTimestampFormat());
    }
    
    @Test
    public void testNullValues() {
        // Test with null message
        LoggingEvent nullMessageEvent = new LoggingEvent(
            LogLevel.INFO,
            null,
            "com.example.TestClass",
            null
        );
        
        String json = layout.format(nullMessageEvent);
        assertTrue(json.contains("\"message\":null"));
    }
    
    @Test
    public void testExceptionWithCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        Exception exception = new RuntimeException("Wrapper exception", cause);
        
        LoggingEvent eventWithCause = new LoggingEvent(
            LogLevel.ERROR,
            "Error with cause",
            "com.example.TestClass",
            null,
            exception
        );
        
        String json = layout.format(eventWithCause);
        
        // Should contain both exception and cause
        assertTrue(json.contains("\"exception\""));
        assertTrue(json.contains("\"cause\""));
        assertTrue(json.contains("IllegalArgumentException"));
        assertTrue(json.contains("Root cause"));
    }
    
    @Test
    public void testUnicodeHandling() {
        LoggingEvent unicodeEvent = new LoggingEvent(
            LogLevel.INFO,
            "Unicode message: 你好世界 🌍",
            "com.example.TestClass",
            null
        );
        
        String json = layout.format(unicodeEvent);
        
        // Should properly handle Unicode characters
        assertTrue(json.contains("你好世界"));
        assertTrue(json.contains("🌍"));
    }
    
    @Test
    public void testPerformance() {
        // Simple performance test - formatting should be reasonably fast
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            layout.format(basicEvent);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should format 1000 events in less than 1 second
        assertTrue(duration < 1000, "JSON formatting too slow: " + duration + "ms for 1000 events");
    }
}