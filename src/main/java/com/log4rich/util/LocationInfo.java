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

/**
 * Captures location information about where a log event occurred.
 * This includes class name, method name, line number, and filename.
 */
public class LocationInfo {
    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    private final String fullClassName;
    
    /**
     * Creates a new LocationInfo with the specified location details.
     * 
     * @param className the fully qualified class name
     * @param methodName the method name
     * @param fileName the source file name
     * @param lineNumber the line number in the source file
     */
    public LocationInfo(String className, String methodName, String fileName, int lineNumber) {
        this.fullClassName = className;
        this.className = extractSimpleClassName(className);
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
    
    /**
     * Create LocationInfo by analyzing the current stack trace.
     * @param skipFrames Number of stack frames to skip (typically 2-3 for logger calls)
     * @return LocationInfo object or null if unable to determine
     */
    public static LocationInfo getCaller(int skipFrames) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        
        // Skip getStackTrace() call + skipFrames
        int targetFrame = 1 + skipFrames;
        
        if (stack.length <= targetFrame) {
            return null;
        }
        
        StackTraceElement element = stack[targetFrame];
        return new LocationInfo(
            element.getClassName(),
            element.getMethodName(),
            element.getFileName(),
            element.getLineNumber()
        );
    }
    
    /**
     * Extract simple class name from full class name.
     * @param fullClassName Full class name (e.g., com.example.MyClass)
     * @return Simple class name (e.g., MyClass)
     */
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "Unknown";
        }
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    /**
     * Get truncated class name (e.g., c.e.MyClass).
     * @return Truncated class name
     */
    public String getTruncatedClassName() {
        if (fullClassName == null) {
            return "Unknown";
        }
        
        String[] parts = fullClassName.split("\\.");
        if (parts.length <= 1) {
            return fullClassName;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            result.append(parts[i].charAt(0)).append('.');
        }
        result.append(parts[parts.length - 1]);
        return result.toString();
    }
    
    // Getters
    /**
     * Gets the simple class name (without package).
     * @return the simple class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Gets the fully qualified class name.
     * @return the full class name including package
     */
    public String getFullClassName() {
        return fullClassName;
    }
    
    /**
     * Gets the method name where the log event occurred.
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * Gets the source file name.
     * @return the source file name
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Gets the line number where the log event occurred.
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Returns a string representation of the location information.
     * @return formatted string containing class.method(file:line)
     */
    @Override
    public String toString() {
        return String.format("%s.%s(%s:%d)", 
            className, methodName, fileName, lineNumber);
    }
}