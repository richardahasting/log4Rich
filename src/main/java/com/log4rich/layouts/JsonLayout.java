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

import com.log4rich.util.JsonArray;
import com.log4rich.util.JsonObject;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout that formats log events as JSON.
 * 
 * This layout produces machine-readable JSON output suitable for log aggregation
 * and analysis tools. It supports configurable formatting options including
 * pretty-printing, location information, and additional static fields.
 * 
 * Example output (compact):
 * <pre>
 * {"timestamp":"2025-07-19T15:30:45.123Z","level":"INFO","logger":"com.example.MyClass","message":"User logged in","thread":"main"}
 * </pre>
 * 
 * Example output (pretty):
 * <pre>
 * {
 *   "timestamp": "2025-07-19T15:30:45.123Z",
 *   "level": "INFO",
 *   "logger": "com.example.MyClass",
 *   "message": "User logged in",
 *   "thread": "main"
 * }
 * </pre>
 * 
 * @author log4Rich Contributors
 * @since 1.0.2
 */
public class JsonLayout implements Layout {
    
    /** Default timestamp format (ISO 8601) */
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    private final boolean prettyPrint;
    private final boolean includeLocationInfo;
    private final boolean includeThreadInfo;
    private final String timestampFormat;
    private final Map<String, Object> additionalFields;
    private final SimpleDateFormat dateFormatter;
    
    /**
     * Creates a new JsonLayout with default settings.
     * Default: compact format, include thread info, include location info, ISO 8601 timestamps.
     */
    public JsonLayout() {
        this(false, true, true, DEFAULT_TIMESTAMP_FORMAT);
    }
    
    /**
     * Creates a new JsonLayout with specified settings.
     * 
     * @param prettyPrint whether to format JSON with indentation and newlines
     * @param includeLocationInfo whether to include class, method, and line information
     * @param includeThreadInfo whether to include thread name
     * @param timestampFormat format string for timestamps (SimpleDateFormat pattern)
     */
    public JsonLayout(boolean prettyPrint, boolean includeLocationInfo, 
                     boolean includeThreadInfo, String timestampFormat) {
        this.prettyPrint = prettyPrint;
        this.includeLocationInfo = includeLocationInfo;
        this.includeThreadInfo = includeThreadInfo;
        this.timestampFormat = timestampFormat != null ? timestampFormat : DEFAULT_TIMESTAMP_FORMAT;
        this.additionalFields = new LinkedHashMap<>();
        this.dateFormatter = new SimpleDateFormat(this.timestampFormat);
    }
    
    /**
     * Adds an additional static field that will be included in all log entries.
     * 
     * @param key the field name
     * @param value the field value
     */
    public void addAdditionalField(String key, Object value) {
        additionalFields.put(key, value);
    }
    
    /**
     * Removes an additional field.
     * 
     * @param key the field name to remove
     */
    public void removeAdditionalField(String key) {
        additionalFields.remove(key);
    }
    
    /**
     * Formats a logging event as JSON.
     * 
     * @param event the logging event to format
     * @return JSON representation of the log event
     */
    @Override
    public String format(LoggingEvent event) {
        JsonObject json = new JsonObject();
        
        // Core fields in logical order
        json.addProperty("timestamp", formatTimestamp(event.getTimestamp()));
        json.addProperty("level", event.getLevel().toString());
        json.addProperty("logger", event.getLoggerName());
        json.addProperty("message", event.getMessage());
        
        // Thread information
        if (includeThreadInfo) {
            json.addProperty("thread", event.getThreadName());
        }
        
        // Location information
        if (includeLocationInfo && event.getLocationInfo() != null) {
            LocationInfo location = event.getLocationInfo();
            JsonObject locationObj = new JsonObject();
            locationObj.addProperty("class", location.getFullClassName());
            locationObj.addProperty("method", location.getMethodName());
            locationObj.addProperty("file", location.getFileName());
            locationObj.addProperty("line", location.getLineNumber());
            json.add("location", locationObj);
        }
        
        // Exception information
        if (event.getThrowable() != null) {
            Throwable throwable = event.getThrowable();
            JsonObject exception = new JsonObject();
            exception.addProperty("class", throwable.getClass().getName());
            exception.addProperty("message", throwable.getMessage());
            exception.add("stackTrace", JsonArray.fromStackTrace(throwable));
            
            // Include cause if present
            if (throwable.getCause() != null) {
                JsonObject cause = new JsonObject();
                cause.addProperty("class", throwable.getCause().getClass().getName());
                cause.addProperty("message", throwable.getCause().getMessage());
                exception.add("cause", cause);
            }
            
            json.add("exception", exception);
        }
        
        // Additional static fields
        for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                json.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                json.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(key, (Boolean) value);
            } else {
                json.addProperty(key, value.toString());
            }
        }
        
        return prettyPrint ? json.toString() : json.toCompactString();
    }
    
    /**
     * Formats a timestamp using the configured format.
     * 
     * @param timestamp the timestamp in milliseconds
     * @return formatted timestamp string
     */
    private String formatTimestamp(long timestamp) {
        synchronized (dateFormatter) {
            return dateFormatter.format(new Date(timestamp));
        }
    }
    
    /**
     * Returns whether this layout handles throwables.
     * JsonLayout includes throwable information in the JSON output.
     * 
     * @return false, indicating this layout handles throwables
     */
    @Override
    public boolean ignoresThrowable() {
        return false;
    }
    
    /**
     * Returns a header for JSON log files.
     * For JSON logs, no header is typically needed since each line is a complete JSON object.
     * 
     * @return null (no header)
     */
    @Override
    public String getHeader() {
        return null;
    }
    
    /**
     * Returns a footer for JSON log files.
     * For JSON logs, no footer is typically needed since each line is a complete JSON object.
     * 
     * @return null (no footer)
     */
    @Override
    public String getFooter() {
        return null;
    }
    
    /**
     * Creates a compact JsonLayout (single-line JSON).
     * 
     * @return a JsonLayout configured for compact output
     */
    public static JsonLayout createCompactLayout() {
        return new JsonLayout(false, true, true, DEFAULT_TIMESTAMP_FORMAT);
    }
    
    /**
     * Creates a pretty-printed JsonLayout (indented JSON).
     * 
     * @return a JsonLayout configured for pretty-printed output
     */
    public static JsonLayout createPrettyLayout() {
        return new JsonLayout(true, true, true, DEFAULT_TIMESTAMP_FORMAT);
    }
    
    /**
     * Creates a minimal JsonLayout with only essential fields.
     * 
     * @return a JsonLayout with minimal field set
     */
    public static JsonLayout createMinimalLayout() {
        return new JsonLayout(false, false, false, DEFAULT_TIMESTAMP_FORMAT);
    }
    
    /**
     * Creates a JsonLayout suitable for production use.
     * Compact format with thread info but no location info for performance.
     * 
     * @return a JsonLayout configured for production
     */
    public static JsonLayout createProductionLayout() {
        return new JsonLayout(false, false, true, DEFAULT_TIMESTAMP_FORMAT);
    }
    
    // Getters for configuration introspection
    
    /**
     * Returns whether this layout uses pretty printing.
     * 
     * @return true if pretty printing is enabled
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }
    
    /**
     * Returns whether this layout includes location information.
     * 
     * @return true if location info is included
     */
    public boolean isIncludeLocationInfo() {
        return includeLocationInfo;
    }
    
    /**
     * Returns whether this layout includes thread information.
     * 
     * @return true if thread info is included
     */
    public boolean isIncludeThreadInfo() {
        return includeThreadInfo;
    }
    
    /**
     * Returns the timestamp format pattern.
     * 
     * @return the timestamp format string
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }
    
    /**
     * Returns a copy of the additional fields map.
     * 
     * @return map of additional static fields
     */
    public Map<String, Object> getAdditionalFields() {
        return new LinkedHashMap<>(additionalFields);
    }
}