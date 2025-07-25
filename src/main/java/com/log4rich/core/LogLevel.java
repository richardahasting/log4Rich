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

package com.log4rich.core;

/**
 * Enumeration of logging levels supported by log4Rich.
 * Levels are ordered from most verbose (TRACE) to least verbose (OFF).
 */
public enum LogLevel {
    /** Enables all logging, most verbose level */
    ALL(0),
    /** Most verbose logging level, shows all messages */
    TRACE(100),
    /** Debug level for detailed diagnostic information */
    DEBUG(200),
    /** General information about application progress */
    INFO(300),
    /** Warning level for potentially harmful situations */
    WARN(400),
    /** Error level for error events that still allow application to continue */
    ERROR(500),
    /** Fatal level for severe error events that will lead to application termination */
    FATAL(600),
    /** Turns off all logging */
    OFF(Integer.MAX_VALUE);
    
    private final int value;
    
    LogLevel(int value) {
        this.value = value;
    }
    
    /**
     * Gets the numeric value of this log level.
     * Lower values indicate more verbose logging.
     * 
     * @return the numeric value of this log level
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Check if this level is enabled for the given level.
     * @param level The level to check against
     * @return true if this level is greater than or equal to the given level
     */
    public boolean isGreaterOrEqual(LogLevel level) {
        return this.value >= level.value;
    }
    
    /**
     * Parse a string to get the corresponding LogLevel.
     * @param level The string representation of the level
     * @return The corresponding LogLevel, or INFO if not found
     */
    public static LogLevel fromString(String level) {
        if (level == null) {
            return INFO;
        }
        try {
            return valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO; // Default to INFO if invalid
        }
    }
}