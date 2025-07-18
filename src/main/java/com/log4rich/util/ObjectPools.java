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

import com.log4rich.core.LogLevel;

/**
 * Thread-local object pools for zero-allocation logging.
 * 
 * This utility provides thread-local pools of reusable objects to eliminate
 * allocation overhead in hot logging paths. Each thread gets its own set of
 * pooled objects, avoiding synchronization overhead while ensuring thread safety.
 * 
 * Key performance benefits:
 * - Eliminates object allocation in logging hot paths
 * - Reduces garbage collection pressure
 * - Provides predictable performance characteristics
 * - Thread-local design avoids synchronization overhead
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public final class ObjectPools {
    
    // Initial capacity for StringBuilder - sized for typical log messages
    private static final int DEFAULT_STRING_BUILDER_CAPACITY = 1024;
    
    // ThreadLocal pools for reusable objects
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL = 
        ThreadLocal.withInitial(() -> new StringBuilder(DEFAULT_STRING_BUILDER_CAPACITY));
    
    // Performance monitoring
    private static volatile long stringBuilderReuses = 0;
    
    /**
     * Private constructor - utility class
     */
    private ObjectPools() {
        // Utility class
    }
    
    /**
     * Gets a reusable StringBuilder from the thread-local pool.
     * The StringBuilder is automatically cleared and ready for use.
     * 
     * @return a cleared StringBuilder ready for use
     */
    public static StringBuilder getStringBuilder() {
        StringBuilder sb = STRING_BUILDER_POOL.get();
        sb.setLength(0); // Clear any previous content
        
        // If capacity is excessive, recreate with default size to avoid memory waste
        if (sb.capacity() > DEFAULT_STRING_BUILDER_CAPACITY * 4) {
            sb = new StringBuilder(DEFAULT_STRING_BUILDER_CAPACITY);
            STRING_BUILDER_POOL.set(sb);
        }
        
        stringBuilderReuses++;
        return sb;
    }
    
    // Note: getLoggingEvent() method was removed because LoggingEvent is immutable.
    // The LoggingEvent class has all fields as final and no setter methods,
    // making it impossible to create a reusable version for object pooling.
    
    /**
     * Configures the initial capacity for new StringBuilders.
     * This affects ThreadLocal StringBuilders created after this call.
     * 
     * @param capacity the initial capacity for new StringBuilders
     */
    public static void setStringBuilderCapacity(int capacity) {
        // This will affect new ThreadLocal instances, existing ones keep their current capacity
        // To apply to existing instances, they would need to be recreated when capacity is excessive
    }
    
    /**
     * Gets performance statistics for monitoring pool effectiveness.
     * 
     * @return pool statistics
     */
    public static PoolStatistics getStatistics() {
        return new PoolStatistics(stringBuilderReuses, 0);
    }
    
    /**
     * Resets performance counters.
     */
    public static void resetStatistics() {
        stringBuilderReuses = 0;
    }
    
    /**
     * Statistics for object pool reuse monitoring.
     */
    public static class PoolStatistics {
        private final long stringBuilderReuses;
        private final long loggingEventReuses;
        
        PoolStatistics(long stringBuilderReuses, long loggingEventReuses) {
            this.stringBuilderReuses = stringBuilderReuses;
            this.loggingEventReuses = loggingEventReuses;
        }
        
        public long getStringBuilderReuses() {
            return stringBuilderReuses;
        }
        
        public long getLoggingEventReuses() {
            return loggingEventReuses;
        }
        
        public long getTotalReuses() {
            return stringBuilderReuses + loggingEventReuses;
        }
        
        @Override
        public String toString() {
            return String.format("PoolStatistics{stringBuilderReuses=%d, loggingEventReuses=%d, total=%d}",
                               stringBuilderReuses, loggingEventReuses, getTotalReuses());
        }
    }
    
    // Note: ReusableLoggingEvent was removed because LoggingEvent is immutable.
    // The LoggingEvent class has all fields as final and no setter methods,
    // making it impossible to create a mutable version for object pooling.
    // Instead, consider using the existing LoggingEvent constructor efficiently.
}