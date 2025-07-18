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
import com.log4rich.util.LoggingEvent;

/**
 * Base interface for all log appenders.
 * Appenders are responsible for writing log events to their destination.
 */
public interface Appender {
    
    /**
     * Append a logging event.
     * @param event The event to append
     */
    void append(LoggingEvent event);
    
    /**
     * Set the layout for this appender.
     * @param layout The layout to use
     */
    void setLayout(Layout layout);
    
    /**
     * Get the layout for this appender.
     * @return The current layout
     */
    Layout getLayout();
    
    /**
     * Set the minimum log level for this appender.
     * @param level The minimum level
     */
    void setLevel(LogLevel level);
    
    /**
     * Get the minimum log level for this appender.
     * @return The current minimum level
     */
    LogLevel getLevel();
    
    /**
     * Check if this appender will handle the given level.
     * @param level The level to check
     * @return true if this appender will handle the level
     */
    boolean isLevelEnabled(LogLevel level);
    
    /**
     * Close this appender and release any resources.
     */
    void close();
    
    /**
     * Check if this appender is closed.
     * @return true if closed
     */
    boolean isClosed();
    
    /**
     * Set the name of this appender.
     * @param name The name
     */
    void setName(String name);
    
    /**
     * Get the name of this appender.
     * @return The name
     */
    String getName();
}