package com.log4rich.layouts;

import com.log4rich.core.LogLevel;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;

public class JsonDebugTest {
    public static void main(String[] args) {
        JsonLayout layout = new JsonLayout();
        
        // Test with location info
        LocationInfo location = new LocationInfo(
            "com.example.TestClass",
            "testMethod",
            "TestClass.java",
            42
        );
        LoggingEvent eventWithLocation = new LoggingEvent(
            LogLevel.WARN,
            "Warning message",
            "com.example.TestClass",
            location
        );
        
        System.out.println("JSON with location:");
        System.out.println(layout.format(eventWithLocation));
        
        // Test with special characters
        LoggingEvent eventWithSpecialChars = new LoggingEvent(
            LogLevel.INFO,
            "Message with \"quotes\" and \n newlines and \t tabs",
            "com.example.TestClass",
            null
        );
        
        System.out.println("\nJSON with special chars:");
        System.out.println(layout.format(eventWithSpecialChars));
    }
}