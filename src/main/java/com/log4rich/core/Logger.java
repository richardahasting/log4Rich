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

import com.log4rich.appenders.Appender;
import com.log4rich.util.LocationInfo;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main logger class that handles log events.
 * Thread-safe implementation using concurrent collections.
 */
public class Logger {
    
    private final String name;
    private volatile LogLevel level;
    private final List<Appender> appenders;
    private volatile boolean locationCapture;
    private volatile ContextProvider contextProvider;
    
    /**
     * Creates a new logger with the specified name.
     * The logger is initialized with INFO level and location capture enabled.
     * 
     * @param name the name of the logger
     */
    public Logger(String name) {
        this.name = name;
        this.level = LogLevel.INFO;
        this.appenders = new CopyOnWriteArrayList<>();
        this.locationCapture = true;
        this.contextProvider = EmptyContextProvider.INSTANCE;
    }
    
    // Logging methods
    /**
     * Logs a message at TRACE level.
     * 
     * @param message the message to log
     */
    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }
    
    /**
     * Logs a message at TRACE level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void trace(String message, Throwable throwable) {
        log(LogLevel.TRACE, message, throwable);
    }
    
    /**
     * Logs a message at DEBUG level.
     * 
     * @param message the message to log
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    /**
     * Logs a message at DEBUG level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, message, throwable);
    }
    
    /**
     * Logs a message at INFO level.
     * 
     * @param message the message to log
     */
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    /**
     * Logs a message at INFO level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void info(String message, Throwable throwable) {
        log(LogLevel.INFO, message, throwable);
    }
    
    /**
     * Logs a message at WARN level.
     * 
     * @param message the message to log
     */
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    /**
     * Logs a message at WARN level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }
    
    /**
     * Logs a message at ERROR level.
     * 
     * @param message the message to log
     */
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    /**
     * Logs a message at ERROR level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    /**
     * Logs a message at FATAL level.
     * 
     * @param message the message to log
     */
    public void fatal(String message) {
        log(LogLevel.FATAL, message, null);
    }
    
    /**
     * Logs a message at FATAL level with an associated throwable.
     * 
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void fatal(String message, Throwable throwable) {
        log(LogLevel.FATAL, message, throwable);
    }

    /**
     * Logs a message at CRITICAL level.
     * CRITICAL is a synonym for FATAL with identical priority.
     *
     * @param message the message to log
     */
    public void critical(String message) {
        log(LogLevel.CRITICAL, message, null);
    }

    /**
     * Logs a message at CRITICAL level with an associated throwable.
     * CRITICAL is a synonym for FATAL with identical priority.
     *
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public void critical(String message, Throwable throwable) {
        log(LogLevel.CRITICAL, message, throwable);
    }

    // SLF4J-style parameterized logging methods
    /**
     * Logs a message at TRACE level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void trace(String messagePattern, Object... arguments) {
        if (isTraceEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.TRACE, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at DEBUG level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void debug(String messagePattern, Object... arguments) {
        if (isDebugEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.DEBUG, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at INFO level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void info(String messagePattern, Object... arguments) {
        if (isInfoEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.INFO, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at WARN level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void warn(String messagePattern, Object... arguments) {
        if (isWarnEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.WARN, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at ERROR level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void error(String messagePattern, Object... arguments) {
        if (isErrorEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.ERROR, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at FATAL level using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void fatal(String messagePattern, Object... arguments) {
        if (isFatalEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.FATAL, formattedMessage, throwable);
        }
    }

    /**
     * Logs a message at CRITICAL level using SLF4J-style {} placeholders.
     * CRITICAL is a synonym for FATAL with identical priority.
     *
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute into the pattern
     */
    public void critical(String messagePattern, Object... arguments) {
        if (isCriticalEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(messagePattern, args);
            log(LogLevel.CRITICAL, formattedMessage, throwable);
        }
    }
    
    // Convenience methods for common error patterns with one argument and throwable
    /**
     * Logs a message at ERROR level with one argument and an exception.
     * Convenience method for common error patterns.
     * 
     * @param messagePattern the message pattern with one {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void error(String messagePattern, Object argument, Throwable throwable) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.ERROR, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at WARN level with one argument and an exception.
     * Convenience method for common warning patterns.
     * 
     * @param messagePattern the message pattern with one {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void warn(String messagePattern, Object argument, Throwable throwable) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.WARN, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at DEBUG level with one argument and an exception.
     * Convenience method for common debug patterns.
     * 
     * @param messagePattern the message pattern with one {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void debug(String messagePattern, Object argument, Throwable throwable) {
        if (isDebugEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.DEBUG, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at TRACE level with one argument and an exception.
     * Convenience method for common trace patterns.
     * 
     * @param messagePattern the message pattern with one {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void trace(String messagePattern, Object argument, Throwable throwable) {
        if (isTraceEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.TRACE, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at FATAL level with one argument and an exception.
     * Convenience method for common fatal error patterns.
     * 
     * @param messagePattern the message pattern with one {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void fatal(String messagePattern, Object argument, Throwable throwable) {
        if (isFatalEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.FATAL, formattedMessage, throwable);
        }
    }

    /**
     * Logs a message at CRITICAL level with one argument and an exception.
     * CRITICAL is a synonym for FATAL with identical priority.
     *
     * @param messagePattern the message pattern with {} placeholder
     * @param argument the argument to substitute
     * @param throwable the exception to log
     */
    public void critical(String messagePattern, Object argument, Throwable throwable) {
        if (isCriticalEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, argument);
            log(LogLevel.CRITICAL, formattedMessage, throwable);
        }
    }
    
    // Convenience methods for two arguments and throwable
    /**
     * Logs a message at ERROR level with two arguments and an exception.
     * Convenience method for common error patterns.
     * 
     * @param messagePattern the message pattern with two {} placeholders
     * @param arg1 the first argument to substitute
     * @param arg2 the second argument to substitute
     * @param throwable the exception to log
     */
    public void error(String messagePattern, Object arg1, Object arg2, Throwable throwable) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, arg1, arg2);
            log(LogLevel.ERROR, formattedMessage, throwable);
        }
    }
    
    /**
     * Logs a message at WARN level with two arguments and an exception.
     * Convenience method for common warning patterns.
     * 
     * @param messagePattern the message pattern with two {} placeholders
     * @param arg1 the first argument to substitute
     * @param arg2 the second argument to substitute
     * @param throwable the exception to log
     */
    public void warn(String messagePattern, Object arg1, Object arg2, Throwable throwable) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(messagePattern, arg1, arg2);
            log(LogLevel.WARN, formattedMessage, throwable);
        }
    }
    
    // Level checking methods
    /**
     * Checks if TRACE level is enabled for this logger.
     * 
     * @return true if TRACE level is enabled, false otherwise
     */
    public boolean isTraceEnabled() {
        return isLevelEnabled(LogLevel.TRACE);
    }
    
    /**
     * Checks if DEBUG level is enabled for this logger.
     * 
     * @return true if DEBUG level is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return isLevelEnabled(LogLevel.DEBUG);
    }
    
    /**
     * Checks if INFO level is enabled for this logger.
     * 
     * @return true if INFO level is enabled, false otherwise
     */
    public boolean isInfoEnabled() {
        return isLevelEnabled(LogLevel.INFO);
    }
    
    /**
     * Checks if WARN level is enabled for this logger.
     * 
     * @return true if WARN level is enabled, false otherwise
     */
    public boolean isWarnEnabled() {
        return isLevelEnabled(LogLevel.WARN);
    }
    
    /**
     * Checks if ERROR level is enabled for this logger.
     * 
     * @return true if ERROR level is enabled, false otherwise
     */
    public boolean isErrorEnabled() {
        return isLevelEnabled(LogLevel.ERROR);
    }
    
    /**
     * Checks if FATAL level is enabled for this logger.
     * 
     * @return true if FATAL level is enabled, false otherwise
     */
    public boolean isFatalEnabled() {
        return isLevelEnabled(LogLevel.FATAL);
    }

    /**
     * Checks if CRITICAL level is enabled for this logger.
     * CRITICAL is a synonym for FATAL with identical priority.
     *
     * @return true if CRITICAL level is enabled, false otherwise
     */
    public boolean isCriticalEnabled() {
        return isLevelEnabled(LogLevel.CRITICAL);
    }
    
    /**
     * Checks if the specified level is enabled for this logger.
     * 
     * @param level the level to check
     * @return true if the level is enabled, false otherwise
     */
    public boolean isLevelEnabled(LogLevel level) {
        if (level == null) {
            return false;
        }
        return level.isGreaterOrEqual(this.level);
    }
    
    /**
     * Generic logging method for any level without throwable.
     * 
     * @param level the log level
     * @param message the log message
     */
    public void log(LogLevel level, String message) {
        log(level, message, null);
    }
    
    /**
     * Core logging method that processes the log event.
     * This method handles level checking, location capture, and appender dispatch.
     * 
     * @param level the log level
     * @param message the log message
     * @param throwable the throwable to log, may be null
     */
    public void log(LogLevel level, String message, Throwable throwable) {
        if (!isLevelEnabled(level)) {
            return;
        }
        
        // Capture location info if enabled
        LocationInfo locationInfo = null;
        if (locationCapture) {
            // Skip 3 frames: getStackTrace, getCaller, this log method
            locationInfo = LocationInfo.getCaller(3);
        }
        
        // Get context data if available
        Map<String, String> mdc = null;
        List<String> ndc = null;
        if (contextProvider.hasContext()) {
            mdc = contextProvider.getMDC();
            ndc = contextProvider.getNDC();
        }
        
        LoggingEvent event = new LoggingEvent(level, message, name, locationInfo, throwable, mdc, ndc);
        
        // Send to all appenders
        for (Appender appender : appenders) {
            try {
                appender.append(event);
            } catch (Exception e) {
                // Log appender errors to stderr to avoid infinite loops
                System.err.println("Error in appender " + appender.getName() + ": " + e.getMessage());
            }
        }
    }
    
    // Appender management
    /**
     * Adds an appender to this logger.
     * 
     * @param appender the appender to add, null values are ignored
     */
    public void addAppender(Appender appender) {
        if (appender != null) {
            appenders.add(appender);
        }
    }
    
    /**
     * Removes an appender from this logger.
     * 
     * @param appender the appender to remove, null values are ignored
     */
    public void removeAppender(Appender appender) {
        if (appender != null) {
            appenders.remove(appender);
        }
    }
    
    /**
     * Removes an appender from this logger by name.
     * 
     * @param name the name of the appender to remove, null values are ignored
     */
    public void removeAppender(String name) {
        if (name != null) {
            appenders.removeIf(appender -> name.equals(appender.getName()));
        }
    }
    
    /**
     * Gets a copy of all appenders associated with this logger.
     * 
     * @return a list of appenders (copy, not live view)
     */
    public List<Appender> getAppenders() {
        return new ArrayList<>(appenders);
    }
    
    /**
     * Removes all appenders from this logger.
     */
    public void clearAppenders() {
        appenders.clear();
    }
    
    // Configuration methods
    /**
     * Sets the log level for this logger.
     * 
     * @param level the log level to set, null values default to INFO
     */
    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.INFO;
    }
    
    /**
     * Gets the current log level for this logger.
     * 
     * @return the current log level
     */
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Gets the name of this logger.
     * 
     * @return the logger name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets whether this logger should capture location information.
     * 
     * @param enabled true to enable location capture, false to disable
     */
    public void setLocationCapture(boolean enabled) {
        this.locationCapture = enabled;
    }
    
    /**
     * Checks if location capture is enabled for this logger.
     * 
     * @return true if location capture is enabled, false otherwise
     */
    public boolean isLocationCaptureEnabled() {
        return locationCapture;
    }
    
    // Context provider management
    
    /**
     * Sets the context provider for this logger.
     * The context provider supplies MDC and NDC data for log events.
     * 
     * @param contextProvider the context provider to use, null to use empty provider
     */
    public void setContextProvider(ContextProvider contextProvider) {
        this.contextProvider = contextProvider != null ? contextProvider : EmptyContextProvider.INSTANCE;
    }
    
    /**
     * Gets the current context provider.
     * 
     * @return the current context provider, never null
     */
    public ContextProvider getContextProvider() {
        return contextProvider;
    }
    
    /**
     * Closes all appenders associated with this logger.
     * This method is called during shutdown to ensure proper resource cleanup.
     */
    public void shutdown() {
        for (Appender appender : appenders) {
            try {
                appender.close();
            } catch (Exception e) {
                System.err.println("Error closing appender " + appender.getName() + ": " + e.getMessage());
            }
        }
        appenders.clear();
    }
}