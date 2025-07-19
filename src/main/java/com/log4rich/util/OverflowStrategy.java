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

/**
 * Strategies for handling ring buffer overflow in asynchronous logging.
 * 
 * When the async logging ring buffer becomes full, these strategies determine
 * how to handle new log events. Each strategy has different trade-offs between
 * performance, reliability, and resource usage.
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public enum OverflowStrategy {
    
    /**
     * Block the calling thread until space becomes available in the buffer.
     * 
     * Pros:
     * - No log events are lost
     * - Simple and predictable behavior
     * - Maintains event ordering
     * 
     * Cons:
     * - Can block application threads
     * - May impact application performance
     * - Can cause deadlocks if not configured properly
     * 
     * Best for: Applications where log completeness is critical and occasional
     * blocking is acceptable.
     */
    BLOCK("Block until space available", true, false),
    
    /**
     * Drop the oldest events in the buffer to make room for new ones.
     * 
     * Pros:
     * - Never blocks application threads
     * - Maintains recent log events (most relevant for debugging)
     * - Constant memory usage
     * 
     * Cons:
     * - Older log events are lost
     * - May lose important historical context
     * - Not suitable for audit logging
     * 
     * Best for: High-throughput applications where recent events are more
     * important than historical ones.
     */
    DROP_OLDEST("Drop oldest events", false, true),
    
    /**
     * Drop the newest events when buffer is full.
     * 
     * Pros:
     * - Never blocks application threads
     * - Preserves historical log context
     * - Constant memory usage
     * 
     * Cons:
     * - Newest log events are lost
     * - May miss critical recent events
     * - Can hide current problems
     * 
     * Best for: Applications where historical context is more important
     * than real-time events.
     */
    DROP_NEWEST("Drop newest events", false, true),
    
    /**
     * Synchronously write the event directly to appenders, bypassing the async buffer.
     * 
     * Pros:
     * - No log events are lost
     * - Graceful degradation under high load
     * - Simple fallback behavior
     * 
     * Cons:
     * - Calling thread may be blocked by I/O
     * - Performance degrades to synchronous logging
     * - May cause latency spikes
     * 
     * Best for: Applications that prefer synchronous fallback over event loss.
     */
    SYNCHRONOUS_WRITE("Write synchronously", true, false),
    
    /**
     * Discard the event and increment a counter (fire-and-forget).
     * 
     * Pros:
     * - Never blocks application threads
     * - Minimal performance impact
     * - Simple implementation
     * 
     * Cons:
     * - Log events are permanently lost
     * - No indication of what was lost
     * - Not suitable for important logging
     * 
     * Best for: High-frequency debug logging where some loss is acceptable.
     */
    DISCARD("Discard events", false, true);
    
    private final String description;
    private final boolean preservesEvents;
    private final boolean nonBlocking;
    
    OverflowStrategy(String description, boolean preservesEvents, boolean nonBlocking) {
        this.description = description;
        this.preservesEvents = preservesEvents;
        this.nonBlocking = nonBlocking;
    }
    
    /**
     * Gets a human-readable description of this strategy.
     * 
     * @return strategy description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Indicates whether this strategy preserves all log events.
     * 
     * @return true if no events are lost with this strategy
     */
    public boolean preservesEvents() {
        return preservesEvents;
    }
    
    /**
     * Indicates whether this strategy never blocks the calling thread.
     * 
     * @return true if this strategy is non-blocking
     */
    public boolean isNonBlocking() {
        return nonBlocking;
    }
    
    /**
     * Parses an overflow strategy from a string (case-insensitive).
     * 
     * @param strategy the strategy name
     * @return the corresponding OverflowStrategy
     * @throws IllegalArgumentException if the strategy is not recognized
     */
    public static OverflowStrategy fromString(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return DROP_OLDEST; // Default strategy
        }
        
        String normalized = strategy.trim().toUpperCase().replace('-', '_');
        
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown overflow strategy: " + strategy + 
                ". Valid options: BLOCK, DROP_OLDEST, DROP_NEWEST, SYNCHRONOUS_WRITE, DISCARD"
            );
        }
    }
    
    /**
     * Gets the default overflow strategy.
     * 
     * @return the default strategy (DROP_OLDEST)
     */
    public static OverflowStrategy getDefault() {
        return DROP_OLDEST;
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}