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

package com.log4rich.appenders;

import com.log4rich.core.LogLevel;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.LoggingEvent;

import java.io.PrintStream;

/**
 * Appender that writes log events to console (stdout or stderr).
 * Thread-safe implementation using synchronized methods.
 */
public class ConsoleAppender implements Appender {
    
    /**
     * Target destination for console output.
     */
    public enum Target {
        /** Standard output stream */
        STDOUT(System.out),
        /** Standard error stream */
        STDERR(System.err);
        
        private final PrintStream stream;
        
        /**
         * Creates a new target with the specified print stream.
         * 
         * @param stream the print stream to use
         */
        Target(PrintStream stream) {
            this.stream = stream;
        }
        
        /**
         * Gets the print stream associated with this target.
         * 
         * @return the print stream
         */
        public PrintStream getStream() {
            return stream;
        }
    }
    
    private Layout layout;
    private LogLevel level;
    private String name;
    private Target target;
    private boolean closed;
    private final Object lock = new Object();
    
    /**
     * Creates a new console appender that writes to STDOUT.
     */
    public ConsoleAppender() {
        this(Target.STDOUT);
    }
    
    /**
     * Creates a new console appender that writes to the specified target.
     * 
     * @param target the target stream to write to
     */
    public ConsoleAppender(Target target) {
        this.target = target != null ? target : Target.STDOUT;
        this.layout = new StandardLayout();
        this.level = LogLevel.TRACE; // Accept all levels by default
        this.name = "Console";
        this.closed = false;
    }
    
    /**
     * Appends a log event to the console output stream.
     * This method is thread-safe and handles formatting, output, and error recovery.
     * 
     * @param event the logging event to append
     */
    @Override
    public void append(LoggingEvent event) {
        if (closed || !isLevelEnabled(event.getLevel())) {
            return;
        }
        
        synchronized (lock) {
            if (closed) {
                return;
            }
            
            try {
                String formattedMessage = layout.format(event);
                target.getStream().print(formattedMessage);
                target.getStream().flush();
            } catch (Exception e) {
                // Fallback to stderr if there's an issue
                System.err.println("Error writing to console: " + e.getMessage());
                System.err.println("Original message: " + event.getMessage());
            }
        }
    }
    
    /**
     * Sets the layout for formatting log events.
     * 
     * @param layout the layout to use, or null to use the default StandardLayout
     */
    @Override
    public void setLayout(Layout layout) {
        synchronized (lock) {
            this.layout = layout != null ? layout : new StandardLayout();
        }
    }
    
    /**
     * Gets the current layout used for formatting log events.
     * 
     * @return the current layout
     */
    @Override
    public Layout getLayout() {
        synchronized (lock) {
            return layout;
        }
    }
    
    /**
     * Sets the minimum log level for this appender.
     * Only events at or above this level will be processed.
     * 
     * @param level the minimum log level, or null to use TRACE (accept all)
     */
    @Override
    public void setLevel(LogLevel level) {
        synchronized (lock) {
            this.level = level != null ? level : LogLevel.TRACE;
        }
    }
    
    /**
     * Gets the minimum log level for this appender.
     * 
     * @return the minimum log level
     */
    @Override
    public LogLevel getLevel() {
        synchronized (lock) {
            return level;
        }
    }
    
    /**
     * Checks if the specified log level is enabled for this appender.
     * 
     * @param level the log level to check
     * @return true if the level is enabled, false otherwise
     */
    @Override
    public boolean isLevelEnabled(LogLevel level) {
        if (level == null) {
            return false;
        }
        synchronized (lock) {
            return level.isGreaterOrEqual(this.level);
        }
    }
    
    /**
     * Closes this appender and releases any resources.
     * Note: System.out and System.err are not closed as they are shared resources.
     */
    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            // Note: We don't close System.out or System.err as they're shared resources
        }
    }
    
    /**
     * Checks if this appender is closed.
     * 
     * @return true if the appender is closed, false otherwise
     */
    @Override
    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }
    
    /**
     * Sets the name of this appender.
     * 
     * @param name the name to set, or null to use the default "Console"
     */
    @Override
    public void setName(String name) {
        synchronized (lock) {
            this.name = name != null ? name : "Console";
        }
    }
    
    /**
     * Gets the name of this appender.
     * 
     * @return the appender name
     */
    @Override
    public String getName() {
        synchronized (lock) {
            return name;
        }
    }
    
    /**
     * Sets the target output stream for this appender.
     * 
     * @param target the target stream to use, or null to use STDOUT
     */
    public void setTarget(Target target) {
        synchronized (lock) {
            this.target = target != null ? target : Target.STDOUT;
        }
    }
    
    /**
     * Gets the current target output stream for this appender.
     * 
     * @return the current target stream
     */
    public Target getTarget() {
        synchronized (lock) {
            return target;
        }
    }
}