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

import com.log4rich.core.AsyncLogger;
import com.log4rich.core.LogLevel;
import com.log4rich.layouts.Layout;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.OverflowStrategy;

/**
 * Wrapper that makes any appender asynchronous by using an AsyncLogger internally.
 * 
 * This wrapper allows existing synchronous appenders to benefit from asynchronous
 * processing without modification. It creates an internal AsyncLogger that handles
 * the ring buffer management and background processing.
 * 
 * Key features:
 * - Zero-modification async conversion for existing appenders
 * - Configurable buffer size and overflow strategy
 * - Automatic resource management and cleanup
 * - Performance monitoring and statistics
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class AsyncAppenderWrapper implements Appender {
    
    private final Appender targetAppender;
    private final AsyncLogger asyncLogger;
    private final String name;
    
    /**
     * Creates an async wrapper with default settings.
     * 
     * @param targetAppender the appender to wrap
     */
    public AsyncAppenderWrapper(Appender targetAppender) {
        this(targetAppender, 65536, OverflowStrategy.DROP_OLDEST);
    }
    
    /**
     * Creates an async wrapper with custom buffer size.
     * 
     * @param targetAppender the appender to wrap
     * @param bufferSize the ring buffer size (must be power of 2)
     */
    public AsyncAppenderWrapper(Appender targetAppender, int bufferSize) {
        this(targetAppender, bufferSize, OverflowStrategy.DROP_OLDEST);
    }
    
    /**
     * Creates an async wrapper with full configuration.
     * 
     * @param targetAppender the appender to wrap
     * @param bufferSize the ring buffer size (must be power of 2)
     * @param overflowStrategy how to handle buffer overflow
     */
    public AsyncAppenderWrapper(Appender targetAppender, int bufferSize, OverflowStrategy overflowStrategy) {
        if (targetAppender == null) {
            throw new IllegalArgumentException("Target appender cannot be null");
        }
        
        this.targetAppender = targetAppender;
        this.name = "Async-" + targetAppender.getName();
        
        // Create async logger with the target appender
        this.asyncLogger = new AsyncLogger(this.name, bufferSize, overflowStrategy, 5000);
        this.asyncLogger.addAppender(targetAppender);
        
        System.out.println("AsyncAppenderWrapper created: " + this.name + 
                          " wrapping " + targetAppender.getName());
    }
    
    @Override
    public void append(LoggingEvent event) {
        if (event == null || asyncLogger == null) {
            return;
        }
        
        // Delegate to async logger - this is the non-blocking call
        asyncLogger.doLog(event.getLevel(), event.getMessage(), 
                         event.getThrowable(), event.getLocationInfo());
    }
    
    @Override
    public void close() {
        if (asyncLogger != null) {
            asyncLogger.shutdown();
        }
        
        // Don't close the target appender here - let the AsyncLogger handle it
        System.out.println("AsyncAppenderWrapper closed: " + name);
    }
    
    @Override
    public void setLayout(Layout layout) {
        if (targetAppender != null) {
            targetAppender.setLayout(layout);
        }
    }
    
    @Override
    public Layout getLayout() {
        return targetAppender != null ? targetAppender.getLayout() : null;
    }
    
    @Override
    public void setLevel(LogLevel level) {
        if (targetAppender != null) {
            targetAppender.setLevel(level);
        }
    }
    
    @Override
    public LogLevel getLevel() {
        return targetAppender != null ? targetAppender.getLevel() : LogLevel.TRACE;
    }
    
    @Override
    public boolean isLevelEnabled(LogLevel level) {
        return targetAppender != null ? targetAppender.isLevelEnabled(level) : false;
    }
    
    @Override
    public boolean isClosed() {
        return targetAppender == null || targetAppender.isClosed();
    }
    
    @Override
    public void setName(String name) {
        // Don't allow name changes - the async wrapper controls naming
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Gets the wrapped target appender.
     * 
     * @return the target appender being wrapped
     */
    public Appender getTargetAppender() {
        return targetAppender;
    }
    
    /**
     * Gets the internal async logger used for processing.
     * 
     * @return the async logger
     */
    public AsyncLogger getAsyncLogger() {
        return asyncLogger;
    }
    
    /**
     * Forces immediate flush of all pending events.
     * This method blocks until all pending events are processed.
     */
    public void flush() {
        if (asyncLogger != null) {
            asyncLogger.flush();
        }
    }
    
    /**
     * Gets comprehensive performance statistics for this async wrapper.
     * 
     * @return performance statistics
     */
    public AsyncLogger.AsyncLoggerStatistics getStatistics() {
        return asyncLogger != null ? asyncLogger.getStatistics() : null;
    }
    
    /**
     * Gets a summary of the wrapper's current state.
     * 
     * @return formatted summary string
     */
    public String getSummary() {
        AsyncLogger.AsyncLoggerStatistics stats = getStatistics();
        if (stats == null) {
            return "AsyncAppenderWrapper[" + name + "]: Not initialized";
        }
        
        return String.format(
            "AsyncAppenderWrapper[%s]: target=%s, pending=%d, processed=%d, dropped=%d, " +
            "dropRate=%.2f%%, bufferUtil=%.1f%%, strategy=%s",
            name, 
            targetAppender != null ? targetAppender.getName() : "null",
            stats.getPendingEvents(),
            stats.getEventsProcessed(),
            stats.getEventsDropped(),
            stats.getDropRate() * 100,
            stats.getBufferStats().getUtilization() * 100,
            stats.getOverflowStrategy().name()
        );
    }
    
    /**
     * Factory method to create async wrappers with common configurations.
     */
    public static class Factory {
        
        /**
         * Creates a high-throughput async wrapper optimized for maximum performance.
         * Uses large buffer and drop-oldest strategy.
         * 
         * @param appender the appender to wrap
         * @return configured async wrapper
         */
        public static AsyncAppenderWrapper createHighThroughput(Appender appender) {
            return new AsyncAppenderWrapper(appender, 262144, OverflowStrategy.DROP_OLDEST); // 256K buffer
        }
        
        /**
         * Creates a low-latency async wrapper optimized for minimal delay.
         * Uses smaller buffer and blocking strategy.
         * 
         * @param appender the appender to wrap
         * @return configured async wrapper
         */
        public static AsyncAppenderWrapper createLowLatency(Appender appender) {
            return new AsyncAppenderWrapper(appender, 16384, OverflowStrategy.BLOCK); // 16K buffer
        }
        
        /**
         * Creates a reliable async wrapper that never loses events.
         * Uses synchronous write fallback strategy.
         * 
         * @param appender the appender to wrap
         * @return configured async wrapper
         */
        public static AsyncAppenderWrapper createReliable(Appender appender) {
            return new AsyncAppenderWrapper(appender, 65536, OverflowStrategy.SYNCHRONOUS_WRITE);
        }
        
        /**
         * Creates a fire-and-forget async wrapper optimized for minimal impact.
         * Uses discard strategy for absolute non-blocking behavior.
         * 
         * @param appender the appender to wrap
         * @return configured async wrapper
         */
        public static AsyncAppenderWrapper createFireAndForget(Appender appender) {
            return new AsyncAppenderWrapper(appender, 32768, OverflowStrategy.DISCARD); // 32K buffer
        }
    }
}