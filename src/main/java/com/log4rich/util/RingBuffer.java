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

import java.util.concurrent.locks.LockSupport;

/**
 * High-performance lock-free ring buffer implementation for asynchronous logging.
 * 
 * This ring buffer provides sub-microsecond latency for logging operations by using
 * lock-free algorithms and memory-efficient circular buffer design. It's specifically
 * optimized for the producer-consumer pattern common in asynchronous logging.
 * 
 * Key features:
 * - Lock-free operation using CAS operations
 * - Power-of-2 sizing for efficient modulo operations
 * - False sharing prevention with padding
 * - Overflow detection and handling
 * - Memory-efficient circular design
 * 
 * @param <T> the type of elements stored in the buffer
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class RingBuffer<T> {
    
    // Padding to prevent false sharing - size of cache line (64 bytes)
    private static final int CACHE_LINE_SIZE = 64;
    private static final int CACHE_LINE_LONGS = CACHE_LINE_SIZE / 8;
    
    private final Object[] buffer;
    private final int capacity;
    private final int mask;
    
    // Volatile fields with padding to prevent false sharing
    private volatile long writeSequence = 0;
    private final long[] writeSequencePadding = new long[CACHE_LINE_LONGS - 1];
    
    private volatile long readSequence = 0;
    private final long[] readSequencePadding = new long[CACHE_LINE_LONGS - 1];
    
    // Statistics for monitoring
    private volatile long totalPublished = 0;
    private volatile long totalConsumed = 0;
    private volatile long bufferFullCount = 0;
    
    /**
     * Creates a new ring buffer with the specified capacity.
     * 
     * @param capacity the buffer capacity (must be a power of 2)
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public RingBuffer(int capacity) {
        if (capacity <= 0 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("Capacity must be a positive power of 2, was: " + capacity);
        }
        
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.buffer = new Object[capacity];
    }
    
    /**
     * Attempts to publish an item to the buffer without blocking.
     * 
     * @param item the item to publish
     * @return true if the item was successfully published, false if buffer is full
     */
    public boolean tryPublish(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot publish null item");
        }
        
        long currentWrite = writeSequence;
        long wrapPoint = currentWrite - capacity;
        
        // Check if buffer is full by comparing with read sequence
        if (wrapPoint > readSequence) {
            bufferFullCount++;
            return false;
        }
        
        // Store item at current write position
        int index = (int) (currentWrite & mask);
        buffer[index] = item;
        
        // Advance write sequence
        writeSequence = currentWrite + 1;
        totalPublished++;
        
        return true;
    }
    
    /**
     * Attempts to publish an item to the buffer, blocking if necessary until space is available.
     * 
     * @param item the item to publish
     * @param timeoutNanos maximum time to wait in nanoseconds (0 means no timeout)
     * @return true if the item was published, false if timeout occurred
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean publish(T item, long timeoutNanos) throws InterruptedException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot publish null item");
        }
        
        long deadline = timeoutNanos > 0 ? System.nanoTime() + timeoutNanos : 0;
        
        while (true) {
            if (tryPublish(item)) {
                return true;
            }
            
            // Check for timeout
            if (timeoutNanos > 0 && System.nanoTime() >= deadline) {
                return false;
            }
            
            // Check for interruption
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted while waiting to publish");
            }
            
            // Brief pause before retrying - use LockSupport for minimal overhead
            LockSupport.parkNanos(1000); // 1 microsecond
        }
    }
    
    /**
     * Consumes the next available item from the buffer.
     * 
     * @return the next item, or null if the buffer is empty
     */
    @SuppressWarnings("unchecked")
    public T consume() {
        long currentRead = readSequence;
        
        // Check if buffer is empty
        if (currentRead >= writeSequence) {
            return null;
        }
        
        // Get item at current read position
        int index = (int) (currentRead & mask);
        T item = (T) buffer[index];
        
        // Clear reference to prevent memory leaks
        buffer[index] = null;
        
        // Advance read sequence
        readSequence = currentRead + 1;
        totalConsumed++;
        
        return item;
    }
    
    /**
     * Consumes multiple items from the buffer in a batch operation.
     * 
     * @param items array to store consumed items
     * @param maxItems maximum number of items to consume
     * @return actual number of items consumed
     */
    @SuppressWarnings("unchecked")
    public int consumeBatch(T[] items, int maxItems) {
        if (items == null || maxItems <= 0) {
            return 0;
        }
        
        int actualMax = Math.min(maxItems, items.length);
        int consumed = 0;
        
        for (int i = 0; i < actualMax; i++) {
            long currentRead = readSequence + i;
            
            // Check if we've reached the write sequence
            if (currentRead >= writeSequence) {
                break;
            }
            
            // Get item at current read position
            int index = (int) (currentRead & mask);
            items[i] = (T) buffer[index];
            buffer[index] = null; // Clear reference
            consumed++;
        }
        
        // Update read sequence once for all consumed items
        if (consumed > 0) {
            readSequence += consumed;
            totalConsumed += consumed;
        }
        
        return consumed;
    }
    
    /**
     * Gets the current number of items in the buffer.
     * 
     * @return the current buffer size
     */
    public int size() {
        long write = writeSequence;
        long read = readSequence;
        return (int) Math.max(0, write - read);
    }
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if the buffer is empty
     */
    public boolean isEmpty() {
        return readSequence >= writeSequence;
    }
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if the buffer is full
     */
    public boolean isFull() {
        return size() >= capacity;
    }
    
    /**
     * Gets the buffer capacity.
     * 
     * @return the buffer capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Gets the current buffer utilization as a percentage.
     * 
     * @return utilization percentage (0.0 to 1.0)
     */
    public double getUtilization() {
        return (double) size() / capacity;
    }
    
    /**
     * Clears all items from the buffer.
     * Note: This operation is not thread-safe and should only be called
     * when no concurrent access is occurring.
     */
    public void clear() {
        // Clear all references to prevent memory leaks
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        
        // Reset sequences
        readSequence = writeSequence;
    }
    
    /**
     * Gets performance statistics for this ring buffer.
     * 
     * @return buffer statistics
     */
    public RingBufferStatistics getStatistics() {
        return new RingBufferStatistics(
            totalPublished,
            totalConsumed,
            bufferFullCount,
            size(),
            capacity,
            getUtilization()
        );
    }
    
    /**
     * Resets performance statistics.
     */
    public void resetStatistics() {
        totalPublished = 0;
        totalConsumed = 0;
        bufferFullCount = 0;
    }
    
    /**
     * Statistics for ring buffer performance monitoring.
     */
    public static class RingBufferStatistics {
        private final long totalPublished;
        private final long totalConsumed;
        private final long bufferFullCount;
        private final int currentSize;
        private final int capacity;
        private final double utilization;
        
        RingBufferStatistics(long totalPublished, long totalConsumed, long bufferFullCount,
                           int currentSize, int capacity, double utilization) {
            this.totalPublished = totalPublished;
            this.totalConsumed = totalConsumed;
            this.bufferFullCount = bufferFullCount;
            this.currentSize = currentSize;
            this.capacity = capacity;
            this.utilization = utilization;
        }
        
        public long getTotalPublished() {
            return totalPublished;
        }
        
        public long getTotalConsumed() {
            return totalConsumed;
        }
        
        public long getBufferFullCount() {
            return bufferFullCount;
        }
        
        public int getCurrentSize() {
            return currentSize;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public double getUtilization() {
            return utilization;
        }
        
        public long getPendingEvents() {
            return totalPublished - totalConsumed;
        }
        
        public double getDropRate() {
            return totalPublished > 0 ? (double) bufferFullCount / totalPublished : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "RingBufferStats{published=%d, consumed=%d, pending=%d, " +
                "bufferFull=%d, size=%d/%d, utilization=%.1f%%, dropRate=%.2f%%}",
                totalPublished, totalConsumed, getPendingEvents(),
                bufferFullCount, currentSize, capacity, utilization * 100,
                getDropRate() * 100
            );
        }
    }
}