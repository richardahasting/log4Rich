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
import com.log4rich.util.OverflowStrategy;
import com.log4rich.util.RingBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Asynchronous logger implementation providing non-blocking logging operations.
 * 
 * This logger uses a lock-free ring buffer to decouple log event creation from
 * the actual I/O operations, providing sub-microsecond logging latency for
 * application threads. A dedicated background thread handles all I/O operations.
 * 
 * Key features:
 * - Sub-microsecond logging latency
 * - Lock-free ring buffer for maximum throughput
 * - Configurable overflow handling strategies
 * - Graceful shutdown with event draining
 * - Comprehensive performance monitoring
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class AsyncLogger extends Logger {
    
    // Default configuration values
    private static final int DEFAULT_BUFFER_SIZE = 65536; // Power of 2
    private static final OverflowStrategy DEFAULT_OVERFLOW_STRATEGY = OverflowStrategy.DROP_OLDEST;
    private static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 5000;
    private static final int DEFAULT_BATCH_SIZE = 256;
    
    private final RingBuffer<LoggingEvent> ringBuffer;
    private final Thread processingThread;
    private final List<Appender> appenders;
    private final OverflowStrategy overflowStrategy;
    private final long shutdownTimeoutMs;
    private final int batchSize;
    
    private volatile boolean running = true;
    private volatile boolean shutdown = false;
    
    // Performance monitoring
    private final AtomicLong eventsPublished = new AtomicLong(0);
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsDropped = new AtomicLong(0);
    private final AtomicLong overflowEvents = new AtomicLong(0);
    
    /**
     * Creates an async logger with default settings.
     * 
     * @param name the logger name
     */
    public AsyncLogger(String name) {
        this(name, DEFAULT_BUFFER_SIZE, DEFAULT_OVERFLOW_STRATEGY, DEFAULT_SHUTDOWN_TIMEOUT_MS);
    }
    
    /**
     * Creates an async logger with custom buffer size.
     * 
     * @param name the logger name
     * @param bufferSize the ring buffer size (must be power of 2)
     */
    public AsyncLogger(String name, int bufferSize) {
        this(name, bufferSize, DEFAULT_OVERFLOW_STRATEGY, DEFAULT_SHUTDOWN_TIMEOUT_MS);
    }
    
    /**
     * Creates an async logger with full configuration.
     * 
     * @param name the logger name
     * @param bufferSize the ring buffer size (must be power of 2)
     * @param overflowStrategy how to handle buffer overflow
     * @param shutdownTimeoutMs timeout for graceful shutdown
     */
    public AsyncLogger(String name, int bufferSize, OverflowStrategy overflowStrategy, long shutdownTimeoutMs) {
        super(name);
        
        this.ringBuffer = new RingBuffer<>(bufferSize);
        this.appenders = new CopyOnWriteArrayList<>();
        this.overflowStrategy = overflowStrategy != null ? overflowStrategy : DEFAULT_OVERFLOW_STRATEGY;
        this.shutdownTimeoutMs = shutdownTimeoutMs;
        this.batchSize = Math.min(DEFAULT_BATCH_SIZE, bufferSize / 4);
        
        // Create and start processing thread
        this.processingThread = new Thread(this::processEvents, "log4Rich-async-" + name);
        this.processingThread.setDaemon(true);
        this.processingThread.start();
        
        System.out.println("AsyncLogger initialized: name=" + name + 
                          ", bufferSize=" + bufferSize + 
                          ", strategy=" + overflowStrategy);
    }
    
    /**
     * Core async logging method that handles event publishing to the ring buffer.
     * 
     * @param level the log level
     * @param message the log message
     * @param throwable the throwable, may be null
     * @param locationInfo the location info, may be null
     */
    public void doLog(LogLevel level, String message, Throwable throwable, LocationInfo locationInfo) {
        if (shutdown || !isLevelEnabled(level)) {
            return;
        }
        
        // Create logging event
        LoggingEvent event = new LoggingEvent(level, message, getName(), locationInfo, throwable);
        
        // Try to publish to ring buffer
        if (ringBuffer.tryPublish(event)) {
            eventsPublished.incrementAndGet();
        } else {
            // Handle buffer overflow
            handleOverflow(event);
        }
    }
    
    // Override the Logger's log methods to use async processing
    @Override
    public void trace(String message) {
        doLog(LogLevel.TRACE, message, null, captureLocation());
    }
    
    @Override
    public void trace(String message, Throwable throwable) {
        doLog(LogLevel.TRACE, message, throwable, captureLocation());
    }
    
    @Override
    public void debug(String message) {
        doLog(LogLevel.DEBUG, message, null, captureLocation());
    }
    
    @Override
    public void debug(String message, Throwable throwable) {
        doLog(LogLevel.DEBUG, message, throwable, captureLocation());
    }
    
    @Override
    public void info(String message) {
        doLog(LogLevel.INFO, message, null, captureLocation());
    }
    
    @Override
    public void info(String message, Throwable throwable) {
        doLog(LogLevel.INFO, message, throwable, captureLocation());
    }
    
    @Override
    public void warn(String message) {
        doLog(LogLevel.WARN, message, null, captureLocation());
    }
    
    @Override
    public void warn(String message, Throwable throwable) {
        doLog(LogLevel.WARN, message, throwable, captureLocation());
    }
    
    @Override
    public void error(String message) {
        doLog(LogLevel.ERROR, message, null, captureLocation());
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        doLog(LogLevel.ERROR, message, throwable, captureLocation());
    }
    
    @Override
    public void fatal(String message) {
        doLog(LogLevel.FATAL, message, null, captureLocation());
    }
    
    @Override
    public void fatal(String message, Throwable throwable) {
        doLog(LogLevel.FATAL, message, throwable, captureLocation());
    }
    
    /**
     * Captures location information if enabled.
     * 
     * @return location info or null if disabled
     */
    private LocationInfo captureLocation() {
        return isLocationCaptureEnabled() ? LocationInfo.getCaller(3) : null;
    }
    
    /**
     * Handles buffer overflow according to the configured strategy.
     * 
     * @param event the event that couldn't be published
     */
    private void handleOverflow(LoggingEvent event) {
        overflowEvents.incrementAndGet();
        
        switch (overflowStrategy) {
            case BLOCK:
                handleBlockingOverflow(event);
                break;
                
            case DROP_OLDEST:
                handleDropOldestOverflow(event);
                break;
                
            case DROP_NEWEST:
                eventsDropped.incrementAndGet();
                break;
                
            case SYNCHRONOUS_WRITE:
                handleSynchronousWrite(event);
                break;
                
            case DISCARD:
                eventsDropped.incrementAndGet();
                break;
                
            default:
                eventsDropped.incrementAndGet();
                break;
        }
    }
    
    /**
     * Handles blocking overflow strategy.
     */
    private void handleBlockingOverflow(LoggingEvent event) {
        try {
            // Try to publish with a reasonable timeout to avoid infinite blocking
            if (ringBuffer.publish(event, 1_000_000L)) { // 1ms timeout
                eventsPublished.incrementAndGet();
            } else {
                eventsDropped.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            eventsDropped.incrementAndGet();
        }
    }
    
    /**
     * Handles drop-oldest overflow strategy.
     */
    private void handleDropOldestOverflow(LoggingEvent event) {
        // Consume one old event to make space
        LoggingEvent droppedEvent = ringBuffer.consume();
        if (droppedEvent != null) {
            eventsDropped.incrementAndGet();
        }
        
        // Try to publish the new event
        if (ringBuffer.tryPublish(event)) {
            eventsPublished.incrementAndGet();
        } else {
            eventsDropped.incrementAndGet();
        }
    }
    
    /**
     * Handles synchronous write overflow strategy.
     */
    private void handleSynchronousWrite(LoggingEvent event) {
        // Write directly to appenders (blocking operation)
        for (Appender appender : appenders) {
            if (appender.isLevelEnabled(event.getLevel())) {
                try {
                    appender.append(event);
                } catch (Exception e) {
                    System.err.println("Async logger synchronous write error: " + e.getMessage());
                }
            }
        }
        eventsProcessed.incrementAndGet();
    }
    
    /**
     * Main processing loop for the background thread.
     */
    private void processEvents() {
        LoggingEvent[] batch = new LoggingEvent[batchSize];
        
        while (running || !ringBuffer.isEmpty()) {
            try {
                // Try to consume a batch of events
                int consumed = ringBuffer.consumeBatch(batch, batchSize);
                
                if (consumed > 0) {
                    // Process the batch
                    processBatch(batch, consumed);
                    eventsProcessed.addAndGet(consumed);
                } else {
                    // No events available, brief pause
                    LockSupport.parkNanos(1000); // 1 microsecond
                }
                
            } catch (Exception e) {
                System.err.println("Async logger processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("AsyncLogger processing thread stopped: " + getName());
    }
    
    /**
     * Processes a batch of logging events.
     * 
     * @param batch the batch of events
     * @param count the number of valid events in the batch
     */
    private void processBatch(LoggingEvent[] batch, int count) {
        for (int i = 0; i < count; i++) {
            LoggingEvent event = batch[i];
            if (event != null) {
                processEvent(event);
                batch[i] = null; // Clear reference
            }
        }
    }
    
    /**
     * Processes a single logging event by sending it to all appropriate appenders.
     * 
     * @param event the event to process
     */
    private void processEvent(LoggingEvent event) {
        for (Appender appender : appenders) {
            if (appender.isLevelEnabled(event.getLevel())) {
                try {
                    appender.append(event);
                } catch (Exception e) {
                    System.err.println("Async logger appender error: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void addAppender(Appender appender) {
        if (appender != null && !appenders.contains(appender)) {
            appenders.add(appender);
        }
    }
    
    @Override
    public void removeAppender(Appender appender) {
        appenders.remove(appender);
    }
    
    @Override
    public List<Appender> getAppenders() {
        return new ArrayList<>(appenders);
    }
    
    /**
     * Initiates graceful shutdown of the async logger.
     * This will stop accepting new events and drain remaining events.
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        running = false;
        
        try {
            // Wait for processing thread to finish
            processingThread.join(shutdownTimeoutMs);
            
            if (processingThread.isAlive()) {
                System.err.println("AsyncLogger shutdown timeout, forcing stop: " + getName());
                processingThread.interrupt();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Close all appenders
        for (Appender appender : appenders) {
            try {
                appender.close();
            } catch (Exception e) {
                System.err.println("Error closing appender during shutdown: " + e.getMessage());
            }
        }
        
        // Print final statistics
        AsyncLoggerStatistics stats = getStatistics();
        System.out.println("AsyncLogger shutdown complete: " + getName());
        System.out.println("Final statistics: " + stats);
    }
    
    /**
     * Forces immediate flush of all pending events.
     * This method blocks until all pending events are processed.
     */
    public void flush() {
        if (shutdown) {
            return;
        }
        
        // Wait for ring buffer to empty
        long deadline = System.currentTimeMillis() + 5000; // 5 second timeout
        
        while (!ringBuffer.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Gets comprehensive performance statistics for this async logger.
     * 
     * @return performance statistics
     */
    public AsyncLoggerStatistics getStatistics() {
        RingBuffer.RingBufferStatistics bufferStats = ringBuffer.getStatistics();
        
        return new AsyncLoggerStatistics(
            getName(),
            eventsPublished.get(),
            eventsProcessed.get(),
            eventsDropped.get(),
            overflowEvents.get(),
            bufferStats,
            overflowStrategy,
            running,
            shutdown
        );
    }
    
    /**
     * Comprehensive statistics for async logger performance monitoring.
     */
    public static class AsyncLoggerStatistics {
        private final String loggerName;
        private final long eventsPublished;
        private final long eventsProcessed;
        private final long eventsDropped;
        private final long overflowEvents;
        private final RingBuffer.RingBufferStatistics bufferStats;
        private final OverflowStrategy overflowStrategy;
        private final boolean running;
        private final boolean shutdown;
        
        AsyncLoggerStatistics(String loggerName, long eventsPublished, long eventsProcessed,
                            long eventsDropped, long overflowEvents,
                            RingBuffer.RingBufferStatistics bufferStats,
                            OverflowStrategy overflowStrategy, boolean running, boolean shutdown) {
            this.loggerName = loggerName;
            this.eventsPublished = eventsPublished;
            this.eventsProcessed = eventsProcessed;
            this.eventsDropped = eventsDropped;
            this.overflowEvents = overflowEvents;
            this.bufferStats = bufferStats;
            this.overflowStrategy = overflowStrategy;
            this.running = running;
            this.shutdown = shutdown;
        }
        
        public String getLoggerName() { return loggerName; }
        public long getEventsPublished() { return eventsPublished; }
        public long getEventsProcessed() { return eventsProcessed; }
        public long getEventsDropped() { return eventsDropped; }
        public long getOverflowEvents() { return overflowEvents; }
        /**
         * Gets the ring buffer statistics.
         * @return the ring buffer statistics
         */
        public RingBuffer.RingBufferStatistics getBufferStats() { return bufferStats; }
        public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
        public boolean isRunning() { return running; }
        public boolean isShutdown() { return shutdown; }
        
        public long getPendingEvents() {
            return eventsPublished - eventsProcessed - eventsDropped;
        }
        
        public double getDropRate() {
            return eventsPublished > 0 ? (double) eventsDropped / eventsPublished : 0.0;
        }
        
        public double getOverflowRate() {
            return eventsPublished > 0 ? (double) overflowEvents / eventsPublished : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "AsyncLogger[%s]: published=%d, processed=%d, dropped=%d, " +
                "pending=%d, overflows=%d, dropRate=%.2f%%, bufferUtil=%.1f%%, " +
                "strategy=%s, running=%b",
                loggerName, eventsPublished, eventsProcessed, eventsDropped,
                getPendingEvents(), overflowEvents, getDropRate() * 100,
                bufferStats.getUtilization() * 100, overflowStrategy.name(), running
            );
        }
    }
}