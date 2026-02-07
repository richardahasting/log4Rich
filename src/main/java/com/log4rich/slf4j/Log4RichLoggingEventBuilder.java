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
import org.slf4j.spi.LoggingEventBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SLF4J 2.x fluent API LoggingEventBuilder implementation for log4Rich.
 *
 * <p>This class supports the fluent logging API introduced in SLF4J 2.0,
 * allowing method chaining for building log events.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class Log4RichLoggingEventBuilder implements LoggingEventBuilder {

    private final Logger logger;
    private final LogLevel level;
    private String message;
    private final List<Object> arguments = new ArrayList<>();
    private Throwable throwable;

    /**
     * Creates a new logging event builder.
     *
     * @param logger the target logger
     * @param level the log level for this event
     */
    public Log4RichLoggingEventBuilder(Logger logger, LogLevel level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public LoggingEventBuilder setCause(Throwable cause) {
        this.throwable = cause;
        return this;
    }

    @Override
    public LoggingEventBuilder addMarker(Marker marker) {
        // log4Rich doesn't have marker support, ignore
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Object p) {
        arguments.add(p);
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Supplier<?> objectSupplier) {
        arguments.add(objectSupplier.get());
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Object value) {
        // Format as key=value and add to message context
        arguments.add(key + "=" + value);
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Supplier<Object> valueSupplier) {
        return addKeyValue(key, valueSupplier.get());
    }

    @Override
    public LoggingEventBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public LoggingEventBuilder setMessage(Supplier<String> messageSupplier) {
        this.message = messageSupplier.get();
        return this;
    }

    @Override
    public void log() {
        if (message == null) {
            message = "";
        }

        // Format the message with arguments
        String formattedMessage;
        if (!arguments.isEmpty()) {
            formattedMessage = MessageFormatter.format(message, arguments.toArray());
        } else {
            formattedMessage = message;
        }

        // Call logger.log() directly to keep correct stack frame depth for LocationInfo
        logger.log(level, formattedMessage, throwable);
    }

    @Override
    public void log(String message) {
        setMessage(message);
        log();
    }

    @Override
    public void log(String message, Object arg) {
        setMessage(message);
        addArgument(arg);
        log();
    }

    @Override
    public void log(String message, Object arg0, Object arg1) {
        setMessage(message);
        addArgument(arg0);
        addArgument(arg1);
        log();
    }

    @Override
    public void log(String message, Object... args) {
        setMessage(message);
        for (Object arg : args) {
            addArgument(arg);
        }
        log();
    }

    @Override
    public void log(Supplier<String> messageSupplier) {
        setMessage(messageSupplier);
        log();
    }
}
