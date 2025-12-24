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

package com.log4rich.jmx;

import java.util.List;
import java.util.Map;

/**
 * JMX MXBean interface for log4Rich management and monitoring.
 *
 * <p>This MBean exposes log4Rich runtime configuration, statistics, and
 * management operations for monitoring tools like JConsole, VisualVM, or
 * custom monitoring solutions.</p>
 *
 * <h2>ObjectName:</h2>
 * <pre>com.log4rich:type=Log4Rich</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public interface Log4RichMXBean {

    // ======== Version Information ========

    /**
     * Gets the log4Rich version.
     *
     * @return the version string
     */
    String getVersion();

    /**
     * Gets the build timestamp.
     *
     * @return the build timestamp
     */
    String getBuildTimestamp();

    // ======== Configuration ========

    /**
     * Gets the root logger level.
     *
     * @return the root level name
     */
    String getRootLevel();

    /**
     * Sets the root logger level.
     *
     * @param level the level name (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
     */
    void setRootLevel(String level);

    /**
     * Gets the level for a specific logger.
     *
     * @param loggerName the logger name
     * @return the level name, or null if not configured
     */
    String getLoggerLevel(String loggerName);

    /**
     * Sets the level for a specific logger.
     *
     * @param loggerName the logger name
     * @param level the level name
     */
    void setLoggerLevel(String loggerName, String level);

    /**
     * Gets all configured loggers and their levels.
     *
     * @return map of logger name to level name
     */
    Map<String, String> getLoggerLevels();

    /**
     * Gets the list of appender names for the root logger.
     *
     * @return list of appender names
     */
    List<String> getRootAppenders();

    /**
     * Checks if configuration hot reload is enabled.
     *
     * @return true if hot reload is enabled
     */
    boolean isHotReloadEnabled();

    /**
     * Enables or disables configuration hot reload.
     *
     * @param enabled true to enable
     */
    void setHotReloadEnabled(boolean enabled);

    // ======== Statistics ========

    /**
     * Gets the total number of log messages processed.
     *
     * @return message count
     */
    long getTotalMessagesLogged();

    /**
     * Gets messages logged per level.
     *
     * @return map of level name to count
     */
    Map<String, Long> getMessagesPerLevel();

    /**
     * Gets the number of active loggers.
     *
     * @return logger count
     */
    int getActiveLoggerCount();

    /**
     * Gets the number of active appenders.
     *
     * @return appender count
     */
    int getActiveAppenderCount();

    // ======== Operations ========

    /**
     * Reloads the configuration from file.
     *
     * @return true if reload was successful
     */
    boolean reloadConfiguration();

    /**
     * Flushes all appenders.
     */
    void flushAll();

    /**
     * Resets all statistics counters.
     */
    void resetStatistics();

    /**
     * Gets a summary of the current configuration as a string.
     *
     * @return configuration summary
     */
    String getConfigurationSummary();
}
