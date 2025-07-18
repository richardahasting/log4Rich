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

import com.log4rich.util.LoggingEvent;
import com.log4rich.util.LocationInfo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard layout implementation with pattern support.
 * Supports various placeholders for formatting log messages.
 */
public class StandardLayout implements Layout {
    
    private static final String DEFAULT_PATTERN = 
        "[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message%n";
    
    private final String pattern;
    private final DateTimeFormatter defaultDateFormatter;
    
    // Pattern for extracting date format from %date{format}
    private static final Pattern DATE_PATTERN = Pattern.compile("%date\\{([^}]+)\\}");
    
    /**
     * Creates a new StandardLayout with the default pattern.
     * Default pattern: "[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message%n"
     */
    public StandardLayout() {
        this(DEFAULT_PATTERN);
    }
    
    /**
     * Creates a new StandardLayout with the specified pattern.
     * 
     * @param pattern the pattern to use for formatting, or null for default
     */
    public StandardLayout(String pattern) {
        this.pattern = pattern != null ? pattern : DEFAULT_PATTERN;
        this.defaultDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    }
    
    @Override
    public String format(LoggingEvent event) {
        String result = pattern;
        
        // Replace level
        result = result.replace("%level", event.getLevel().name());
        
        // Replace date patterns
        result = replaceDatePatterns(result, event.getTimestamp());
        
        // Replace thread
        result = result.replace("%thread", event.getThreadName());
        
        // Replace logger name
        result = result.replace("%logger", event.getLoggerName());
        
        // Replace location information
        result = replaceLocationInfo(result, event.getLocationInfo());
        
        // Replace message
        result = result.replace("%message", event.getRenderedMessage());
        
        // Replace newline
        result = result.replace("%n", System.lineSeparator());
        
        return result;
    }
    
    private String replaceDatePatterns(String input, long timestamp) {
        Matcher matcher = DATE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String dateFormat = matcher.group(1);
            DateTimeFormatter formatter;
            
            try {
                // Handle milliseconds specially
                if (dateFormat.contains("SSS")) {
                    formatter = new DateTimeFormatterBuilder()
                        .appendPattern(dateFormat.replace("SSS", ""))
                        .appendValue(ChronoField.MILLI_OF_SECOND, 3)
                        .toFormatter()
                        .withZone(ZoneId.systemDefault());
                } else {
                    formatter = DateTimeFormatter.ofPattern(dateFormat)
                        .withZone(ZoneId.systemDefault());
                }
            } catch (IllegalArgumentException e) {
                // Fallback to default if pattern is invalid
                formatter = defaultDateFormatter;
            }
            
            String formattedDate = formatter.format(Instant.ofEpochMilli(timestamp));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formattedDate));
        }
        matcher.appendTail(sb);
        
        // Handle simple %date without format
        return sb.toString().replace("%date", 
            defaultDateFormatter.format(Instant.ofEpochMilli(timestamp)));
    }
    
    private String replaceLocationInfo(String input, LocationInfo locationInfo) {
        if (locationInfo == null) {
            return input
                .replace("%class", "Unknown")
                .replace("%method", "unknown")
                .replace("%line", "0")
                .replace("%file", "Unknown");
        }
        
        String result = input;
        result = result.replace("%class", locationInfo.getClassName());
        result = result.replace("%method", locationInfo.getMethodName());
        result = result.replace("%line", String.valueOf(locationInfo.getLineNumber()));
        result = result.replace("%file", locationInfo.getFileName() != null ? 
            locationInfo.getFileName() : "Unknown");
        
        return result;
    }
    
    @Override
    public boolean ignoresThrowable() {
        return false; // We handle throwables in the message
    }
    
    /**
     * Gets the pattern used by this layout.
     * 
     * @return the pattern string
     */
    public String getPattern() {
        return pattern;
    }
}