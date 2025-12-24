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

import com.log4rich.Log4Rich;
import com.log4rich.appenders.Appender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of the Log4RichMXBean interface.
 *
 * <p>Provides JMX management and monitoring capabilities for log4Rich.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class Log4RichMXBeanImpl implements Log4RichMXBean {

    private static final Log4RichMXBeanImpl INSTANCE = new Log4RichMXBeanImpl();

    // Statistics tracking
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final ConcurrentHashMap<LogLevel, AtomicLong> messagesPerLevel = new ConcurrentHashMap<>();

    private Log4RichMXBeanImpl() {
        // Initialize counters for all levels
        for (LogLevel level : LogLevel.values()) {
            messagesPerLevel.put(level, new AtomicLong(0));
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static Log4RichMXBeanImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Records a log message for statistics.
     *
     * @param level the log level
     */
    public void recordMessage(LogLevel level) {
        totalMessages.incrementAndGet();
        AtomicLong counter = messagesPerLevel.get(level);
        if (counter != null) {
            counter.incrementAndGet();
        }
    }

    // ======== Version Information ========

    @Override
    public String getVersion() {
        return Log4Rich.getVersion();
    }

    @Override
    public String getBuildTimestamp() {
        return com.log4rich.Version.getBuildTimestamp();
    }

    // ======== Configuration ========

    @Override
    public String getRootLevel() {
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger != null) {
            LogLevel level = rootLogger.getLevel();
            return level != null ? level.name() : "INFO";
        }
        return "INFO";
    }

    @Override
    public void setRootLevel(String level) {
        try {
            LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
            Logger rootLogger = LogManager.getRootLogger();
            if (rootLogger != null) {
                rootLogger.setLevel(logLevel);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid log level: " + level);
        }
    }

    @Override
    public String getLoggerLevel(String loggerName) {
        Logger logger = LogManager.getLogger(loggerName);
        if (logger != null) {
            LogLevel level = logger.getLevel();
            return level != null ? level.name() : null;
        }
        return null;
    }

    @Override
    public void setLoggerLevel(String loggerName, String level) {
        try {
            LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
            Logger logger = LogManager.getLogger(loggerName);
            if (logger != null) {
                logger.setLevel(logLevel);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid log level: " + level);
        }
    }

    @Override
    public Map<String, String> getLoggerLevels() {
        Map<String, String> levels = new HashMap<>();
        // Add root logger
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger != null) {
            LogLevel level = rootLogger.getLevel();
            levels.put("ROOT", level != null ? level.name() : "INFO");
        }
        return levels;
    }

    @Override
    public List<String> getRootAppenders() {
        List<String> appenderNames = new ArrayList<>();
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger != null) {
            List<Appender> appenders = rootLogger.getAppenders();
            if (appenders != null) {
                for (Appender appender : appenders) {
                    String name = appender.getName();
                    appenderNames.add(name != null ? name : appender.getClass().getSimpleName());
                }
            }
        }
        return appenderNames;
    }

    @Override
    public boolean isHotReloadEnabled() {
        return Log4Rich.isConfigurationHotReloadEnabled();
    }

    @Override
    public void setHotReloadEnabled(boolean enabled) {
        if (enabled) {
            // Hot reload requires a config file path, which we don't have here.
            // This operation will just report current state - to enable, use
            // Log4Rich.enableConfigurationHotReload(path) programmatically.
            System.out.println("[log4Rich] JMX: To enable hot reload, call Log4Rich.enableConfigurationHotReload(path)");
        } else {
            Log4Rich.disableConfigurationHotReload();
        }
    }

    // ======== Statistics ========

    @Override
    public long getTotalMessagesLogged() {
        return totalMessages.get();
    }

    @Override
    public Map<String, Long> getMessagesPerLevel() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<LogLevel, AtomicLong> entry : messagesPerLevel.entrySet()) {
            result.put(entry.getKey().name(), entry.getValue().get());
        }
        return result;
    }

    @Override
    public int getActiveLoggerCount() {
        // LogManager doesn't expose a count, so return estimate
        return 1; // At least root logger
    }

    @Override
    public int getActiveAppenderCount() {
        int count = 0;
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger != null) {
            List<Appender> appenders = rootLogger.getAppenders();
            if (appenders != null) {
                count = appenders.size();
            }
        }
        return count;
    }

    // ======== Operations ========

    @Override
    public boolean reloadConfiguration() {
        try {
            Log4Rich.reloadConfiguration();
            return true;
        } catch (Exception e) {
            System.err.println("[log4Rich] JMX configuration reload failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void flushAll() {
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger != null) {
            List<Appender> appenders = rootLogger.getAppenders();
            if (appenders != null) {
                for (Appender appender : appenders) {
                    // Try to call flush if available
                    try {
                        java.lang.reflect.Method flushMethod = appender.getClass().getMethod("flush");
                        flushMethod.invoke(appender);
                    } catch (Exception e) {
                        // Appender doesn't have flush method, ignore
                    }
                }
            }
        }
    }

    @Override
    public void resetStatistics() {
        totalMessages.set(0);
        for (AtomicLong counter : messagesPerLevel.values()) {
            counter.set(0);
        }
    }

    @Override
    public String getConfigurationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("log4Rich Configuration Summary\n");
        sb.append("==============================\n");
        sb.append("Version: ").append(getVersion()).append("\n");
        sb.append("Build: ").append(getBuildTimestamp()).append("\n");
        sb.append("Root Level: ").append(getRootLevel()).append("\n");
        sb.append("Hot Reload: ").append(isHotReloadEnabled() ? "enabled" : "disabled").append("\n");
        sb.append("Appenders: ").append(getRootAppenders()).append("\n");
        sb.append("Total Messages: ").append(getTotalMessagesLogged()).append("\n");
        return sb.toString();
    }
}
