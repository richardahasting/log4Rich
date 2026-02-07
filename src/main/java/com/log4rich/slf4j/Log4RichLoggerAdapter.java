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

package com.log4rich.slf4j;

import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.util.MessageFormatter;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

/**
 * SLF4J Logger adapter that wraps a log4Rich Logger.
 *
 * <p>This adapter allows applications using SLF4J to use log4Rich as their
 * logging backend. It implements the SLF4J Logger interface and delegates
 * all logging calls to the underlying log4Rich Logger.</p>
 *
 * <p><strong>Important:</strong> All methods call {@code logger.log(LogLevel, msg, throwable)}
 * directly instead of convenience methods like {@code logger.info(msg)} to ensure
 * correct caller location capture. The stack frame count in LocationInfo.getCaller(3)
 * expects exactly: getCaller → Logger.log → adapter method → application code.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class Log4RichLoggerAdapter implements org.slf4j.Logger {

    private final Logger logger;
    private final String name;

    /**
     * Creates a new adapter wrapping the specified log4Rich logger.
     *
     * @param logger the log4Rich logger to wrap
     */
    public Log4RichLoggerAdapter(Logger logger) {
        this.logger = logger;
        this.name = logger.getName();
    }

    @Override
    public String getName() {
        return name;
    }

    // ========== TRACE ==========

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            logger.log(LogLevel.TRACE, msg, null);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.TRACE, formattedMessage, null);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.TRACE, formattedMessage, null);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.TRACE, formattedMessage, throwable);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            logger.log(LogLevel.TRACE, msg, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (isTraceEnabled()) {
            logger.log(LogLevel.TRACE, msg, null);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (isTraceEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.TRACE, formattedMessage, null);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.TRACE, formattedMessage, null);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (isTraceEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(argArray);
            Object[] args = MessageFormatter.removeThrowable(argArray, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.TRACE, formattedMessage, throwable);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled()) {
            logger.log(LogLevel.TRACE, msg, t);
        }
    }

    // ========== DEBUG ==========

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            logger.log(LogLevel.DEBUG, msg, null);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.DEBUG, formattedMessage, null);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.DEBUG, formattedMessage, null);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.DEBUG, formattedMessage, throwable);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            logger.log(LogLevel.DEBUG, msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (isDebugEnabled()) {
            logger.log(LogLevel.DEBUG, msg, null);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (isDebugEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.DEBUG, formattedMessage, null);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.DEBUG, formattedMessage, null);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (isDebugEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.DEBUG, formattedMessage, throwable);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled()) {
            logger.log(LogLevel.DEBUG, msg, t);
        }
    }

    // ========== INFO ==========

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            logger.log(LogLevel.INFO, msg, null);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.INFO, formattedMessage, null);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.INFO, formattedMessage, null);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.INFO, formattedMessage, throwable);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            logger.log(LogLevel.INFO, msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        if (isInfoEnabled()) {
            logger.log(LogLevel.INFO, msg, null);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (isInfoEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.INFO, formattedMessage, null);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.INFO, formattedMessage, null);
        }
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (isInfoEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.INFO, formattedMessage, throwable);
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled()) {
            logger.log(LogLevel.INFO, msg, t);
        }
    }

    // ========== WARN ==========

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            logger.log(LogLevel.WARN, msg, null);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.WARN, formattedMessage, null);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.WARN, formattedMessage, throwable);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.WARN, formattedMessage, null);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            logger.log(LogLevel.WARN, msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (isWarnEnabled()) {
            logger.log(LogLevel.WARN, msg, null);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.WARN, formattedMessage, null);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.WARN, formattedMessage, null);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        if (isWarnEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.WARN, formattedMessage, throwable);
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled()) {
            logger.log(LogLevel.WARN, msg, t);
        }
    }

    // ========== ERROR ==========

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            logger.log(LogLevel.ERROR, msg, null);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.ERROR, formattedMessage, null);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.ERROR, formattedMessage, null);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.ERROR, formattedMessage, throwable);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            logger.log(LogLevel.ERROR, msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        if (isErrorEnabled()) {
            logger.log(LogLevel.ERROR, msg, null);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg);
            logger.log(LogLevel.ERROR, formattedMessage, null);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            String formattedMessage = MessageFormatter.format(format, arg1, arg2);
            logger.log(LogLevel.ERROR, formattedMessage, null);
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        if (isErrorEnabled()) {
            Throwable throwable = MessageFormatter.extractThrowable(arguments);
            Object[] args = MessageFormatter.removeThrowable(arguments, throwable);
            String formattedMessage = MessageFormatter.format(format, args);
            logger.log(LogLevel.ERROR, formattedMessage, throwable);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled()) {
            logger.log(LogLevel.ERROR, msg, t);
        }
    }

    // ========== SLF4J 2.x Fluent API ==========

    @Override
    public LoggingEventBuilder atTrace() {
        if (isTraceEnabled()) {
            return new Log4RichLoggingEventBuilder(logger, LogLevel.TRACE);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atDebug() {
        if (isDebugEnabled()) {
            return new Log4RichLoggingEventBuilder(logger, LogLevel.DEBUG);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atInfo() {
        if (isInfoEnabled()) {
            return new Log4RichLoggingEventBuilder(logger, LogLevel.INFO);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atWarn() {
        if (isWarnEnabled()) {
            return new Log4RichLoggingEventBuilder(logger, LogLevel.WARN);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atError() {
        if (isErrorEnabled()) {
            return new Log4RichLoggingEventBuilder(logger, LogLevel.ERROR);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atLevel(Level level) {
        switch (level) {
            case TRACE:
                return atTrace();
            case DEBUG:
                return atDebug();
            case INFO:
                return atInfo();
            case WARN:
                return atWarn();
            case ERROR:
                return atError();
            default:
                return NOPLoggingEventBuilder.singleton();
        }
    }

    @Override
    public boolean isEnabledForLevel(Level level) {
        switch (level) {
            case TRACE:
                return isTraceEnabled();
            case DEBUG:
                return isDebugEnabled();
            case INFO:
                return isInfoEnabled();
            case WARN:
                return isWarnEnabled();
            case ERROR:
                return isErrorEnabled();
            default:
                return false;
        }
    }
}
