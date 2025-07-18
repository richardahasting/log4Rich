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

package com.log4rich.util;

import com.log4rich.core.LogLevel;

/**
 * Represents a single logging event with all associated metadata.
 * This class is immutable and thread-safe.
 */
public class LoggingEvent {
    private final LogLevel level;
    private final String message;
    private final String loggerName;
    private final long timestamp;
    private final String threadName;
    private final LocationInfo locationInfo;
    private final Throwable throwable;
    
    /**
     * Creates a new LoggingEvent with all parameters.
     * 
     * @param level the log level
     * @param message the log message
     * @param loggerName the name of the logger
     * @param locationInfo location information where the log occurred
     * @param throwable optional exception associated with the log event
     */
    public LoggingEvent(LogLevel level, String message, String loggerName, 
                       LocationInfo locationInfo, Throwable throwable) {
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.timestamp = System.currentTimeMillis();
        this.threadName = Thread.currentThread().getName();
        this.locationInfo = locationInfo;
        this.throwable = throwable;
    }
    
    /**
     * Creates a new LoggingEvent without an exception.
     * 
     * @param level the log level
     * @param message the log message
     * @param loggerName the name of the logger
     * @param locationInfo location information where the log occurred
     */
    public LoggingEvent(LogLevel level, String message, String loggerName, 
                       LocationInfo locationInfo) {
        this(level, message, loggerName, locationInfo, null);
    }
    
    // Getters
    /**
     * Gets the log level of this event.
     * 
     * @return the log level
     */
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Gets the log message.
     * 
     * @return the log message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the name of the logger that created this event.
     * 
     * @return the logger name
     */
    public String getLoggerName() {
        return loggerName;
    }
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the name of the thread that created this event.
     * 
     * @return the thread name
     */
    public String getThreadName() {
        return threadName;
    }
    
    /**
     * Gets the location information where this event occurred.
     * 
     * @return the location information, or null if not available
     */
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }
    
    /**
     * Gets the throwable associated with this event.
     * 
     * @return the throwable, or null if none
     */
    public Throwable getThrowable() {
        return throwable;
    }
    
    /**
     * Checks if this event has an associated throwable.
     * 
     * @return true if this event has a throwable, false otherwise
     */
    public boolean hasThrowable() {
        return throwable != null;
    }
    
    /**
     * Gets the rendered message, including throwable stack trace if present.
     * This method combines the log message with the exception information
     * to provide a complete view of the log event.
     * 
     * @return the complete message with stack trace if applicable
     */
    public String getRenderedMessage() {
        if (throwable == null) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder(message);
        sb.append(System.lineSeparator());
        
        // Add throwable information
        sb.append(throwable.getClass().getSimpleName()).append(": ")
          .append(throwable.getMessage()).append(System.lineSeparator());
        
        // Add stack trace
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append(System.lineSeparator());
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a string representation of this logging event.
     * 
     * @return a formatted string containing level, logger name, and message
     */
    @Override
    public String toString() {
        return String.format("[%s] %s - %s", level, loggerName, message);
    }
}