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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe batch buffer for accumulating log entries for efficient I/O operations.
 * 
 * This buffer accumulates log entries and flushes them in batches based on size
 * and time thresholds. Batching reduces I/O system calls and improves overall
 * throughput by amortizing the cost of each I/O operation across multiple log entries.
 * 
 * Key features:
 * - Size-based flushing (when buffer reaches capacity)
 * - Time-based flushing (periodic flush regardless of size)
 * - Thread-safe operation with minimal lock contention
 * - Graceful handling of shutdown scenarios
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class BatchBuffer {
    
    private final List<LoggingEvent> buffer;
    private final int maxBatchSize;
    private final long maxBatchTimeMs;
    private final AtomicInteger currentSize;
    private final ReentrantLock bufferLock;
    
    private volatile long lastFlushTime;
    private volatile boolean shutdown;
    
    // Performance monitoring
    private long totalEvents;
    private long batchCount;
    private long timeFlushCount;
    private long sizeFlushCount;
    
    /**
     * Creates a new batch buffer with specified parameters.
     * 
     * @param maxBatchSize maximum number of events before forced flush
     * @param maxBatchTimeMs maximum time to wait before forced flush (milliseconds)
     */
    public BatchBuffer(int maxBatchSize, long maxBatchTimeMs) {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be positive");
        }
        if (maxBatchTimeMs <= 0) {
            throw new IllegalArgumentException("maxBatchTimeMs must be positive");
        }
        
        this.buffer = new ArrayList<>(maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTimeMs = maxBatchTimeMs;
        this.currentSize = new AtomicInteger(0);
        this.bufferLock = new ReentrantLock();
        this.lastFlushTime = System.currentTimeMillis();
        this.shutdown = false;
    }
    
    /**
     * Adds an event to the buffer.
     * 
     * @param event the logging event to add
     * @return true if buffer should be flushed immediately, false otherwise
     */
    public boolean add(LoggingEvent event) {
        if (shutdown) {
            return false;
        }
        
        bufferLock.lock();
        try {
            if (shutdown) {
                return false; // Double-check after acquiring lock
            }
            
            buffer.add(event);
            int newSize = currentSize.incrementAndGet();
            totalEvents++;
            
            // Check if we should flush due to size
            if (newSize >= maxBatchSize) {
                sizeFlushCount++;
                return true;
            }
            
            // Check if we should flush due to time
            if (shouldFlushByTime()) {
                timeFlushCount++;
                return true;
            }
            
            return false;
            
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Gets all buffered events and clears the buffer.
     * This method is typically called when a flush is needed.
     * 
     * @return list of events to be processed (may be empty)
     */
    public List<LoggingEvent> getAndClear() {
        bufferLock.lock();
        try {
            if (buffer.isEmpty()) {
                return new ArrayList<>(); // Return empty list instead of null
            }
            
            // Create copy of current events
            List<LoggingEvent> events = new ArrayList<>(buffer);
            
            // Clear buffer and reset counters
            buffer.clear();
            currentSize.set(0);
            lastFlushTime = System.currentTimeMillis();
            batchCount++;
            
            return events;
            
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Checks if buffer should be flushed based on time threshold.
     * 
     * @return true if time threshold exceeded
     */
    private boolean shouldFlushByTime() {
        return (System.currentTimeMillis() - lastFlushTime) >= maxBatchTimeMs;
    }
    
    /**
     * Checks if buffer should be flushed immediately based on current state.
     * This method can be called periodically to check for time-based flushes.
     * 
     * @return true if buffer should be flushed
     */
    public boolean shouldFlush() {
        if (shutdown) {
            return currentSize.get() > 0; // Flush remaining events on shutdown
        }
        
        return currentSize.get() >= maxBatchSize || shouldFlushByTime();
    }
    
    /**
     * Gets the current number of events in the buffer.
     * 
     * @return current buffer size
     */
    public int size() {
        return currentSize.get();
    }
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if buffer is empty
     */
    public boolean isEmpty() {
        return currentSize.get() == 0;
    }
    
    /**
     * Initiates shutdown of the buffer.
     * After calling this method, no new events will be accepted,
     * but existing events can still be retrieved via getAndClear().
     */
    public void shutdown() {
        this.shutdown = true;
    }
    
    /**
     * Checks if the buffer is in shutdown state.
     * 
     * @return true if shutdown has been initiated
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * Gets performance statistics for this buffer.
     * 
     * @return buffer statistics
     */
    public BatchStatistics getStatistics() {
        bufferLock.lock();
        try {
            return new BatchStatistics(
                totalEvents,
                batchCount,
                sizeFlushCount,
                timeFlushCount,
                currentSize.get(),
                maxBatchSize,
                maxBatchTimeMs
            );
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Resets performance counters.
     */
    public void resetStatistics() {
        bufferLock.lock();
        try {
            totalEvents = 0;
            batchCount = 0;
            sizeFlushCount = 0;
            timeFlushCount = 0;
        } finally {
            bufferLock.unlock();
        }
    }
    
    /**
     * Statistics for batch buffer performance monitoring.
     */
    public static class BatchStatistics {
        private final long totalEvents;
        private final long batchCount;
        private final long sizeFlushCount;
        private final long timeFlushCount;
        private final int currentSize;
        private final int maxBatchSize;
        private final long maxBatchTimeMs;
        
        BatchStatistics(long totalEvents, long batchCount, long sizeFlushCount, 
                       long timeFlushCount, int currentSize, int maxBatchSize, 
                       long maxBatchTimeMs) {
            this.totalEvents = totalEvents;
            this.batchCount = batchCount;
            this.sizeFlushCount = sizeFlushCount;
            this.timeFlushCount = timeFlushCount;
            this.currentSize = currentSize;
            this.maxBatchSize = maxBatchSize;
            this.maxBatchTimeMs = maxBatchTimeMs;
        }
        
        public long getTotalEvents() {
            return totalEvents;
        }
        
        public long getBatchCount() {
            return batchCount;
        }
        
        public long getSizeFlushCount() {
            return sizeFlushCount;
        }
        
        public long getTimeFlushCount() {
            return timeFlushCount;
        }
        
        public int getCurrentSize() {
            return currentSize;
        }
        
        public int getMaxBatchSize() {
            return maxBatchSize;
        }
        
        public long getMaxBatchTimeMs() {
            return maxBatchTimeMs;
        }
        
        public double getAverageEventsPerBatch() {
            return batchCount > 0 ? (double) totalEvents / batchCount : 0.0;
        }
        
        public double getBatchEfficiency() {
            return batchCount > 0 ? (double) sizeFlushCount / batchCount : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BatchStatistics{totalEvents=%d, batchCount=%d, avgPerBatch=%.1f, " +
                "sizeFlush=%d, timeFlush=%d, efficiency=%.1f%%, currentSize=%d/%d}",
                totalEvents, batchCount, getAverageEventsPerBatch(),
                sizeFlushCount, timeFlushCount, getBatchEfficiency() * 100,
                currentSize, maxBatchSize
            );
        }
    }
}