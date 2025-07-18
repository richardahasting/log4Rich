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

import com.log4rich.core.LogLevel;
import com.log4rich.layouts.Layout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.BatchBuffer;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.ObjectPools;
import com.log4rich.util.ThreadSafeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File appender that batches log entries for efficient I/O operations.
 * 
 * This appender accumulates log entries in a buffer and writes them in batches
 * to reduce I/O system calls and improve overall throughput. It extends the
 * RollingFileAppender to provide file rolling capabilities while maintaining
 * batch processing efficiency.
 * 
 * Key features:
 * - Configurable batch size and time limits
 * - Automatic periodic flushing
 * - Thread-safe batch processing  
 * - Graceful shutdown with remaining event flushing
 * - Performance monitoring and statistics
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class BatchingFileAppender implements Appender {
    
    // Default configuration values
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final long DEFAULT_BATCH_TIME_MS = 100;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 50;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    private final BatchBuffer batchBuffer;
    private final ScheduledExecutorService scheduler;
    private final int batchSize;
    private final long batchTimeMs;
    private final ReentrantLock lock = new ReentrantLock();
    
    // Appender fields
    private String name;
    private File file;
    private Layout layout;
    private LogLevel level;
    private Charset encoding;
    private boolean immediateFlush;
    private int bufferSize;
    private boolean closed;
    private ThreadSafeWriter writer;
    
    // Performance monitoring
    private final AtomicLong batchesWritten;
    private final AtomicLong totalEventsWritten;
    private volatile long lastFlushTime;
    
    /**
     * Creates a batching file appender with default settings.
     * 
     * @param name the appender name
     * @param filePath the file path to write to
     */
    public BatchingFileAppender(String name, String filePath) {
        this(name, filePath, DEFAULT_BATCH_SIZE, DEFAULT_BATCH_TIME_MS);
    }
    
    /**
     * Creates a batching file appender with custom batch settings.
     * 
     * @param name the appender name
     * @param filePath the file path to write to
     * @param batchSize maximum number of events per batch
     * @param batchTimeMs maximum time to wait before flushing batch (milliseconds)
     */
    public BatchingFileAppender(String name, String filePath, int batchSize, long batchTimeMs) {
        this.name = name;
        this.file = new File(filePath);
        this.layout = new StandardLayout();
        this.level = LogLevel.TRACE;
        this.encoding = StandardCharsets.UTF_8;
        this.immediateFlush = true;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.closed = false;
        
        this.batchSize = batchSize;
        this.batchTimeMs = batchTimeMs;
        this.batchBuffer = new BatchBuffer(batchSize, batchTimeMs);
        this.batchesWritten = new AtomicLong(0);
        this.totalEventsWritten = new AtomicLong(0);
        this.lastFlushTime = System.currentTimeMillis();
        
        // Create scheduler for periodic flushing with daemon threads
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log4Rich-batch-" + name);
                t.setDaemon(true);
                return t;
            }
        });
        
        // Schedule periodic flush checks
        scheduler.scheduleAtFixedRate(
            this::checkAndFlushBatch, 
            DEFAULT_FLUSH_INTERVAL_MS, 
            DEFAULT_FLUSH_INTERVAL_MS, 
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("BatchingFileAppender initialized: " + 
                          "name=" + name + 
                          ", batchSize=" + batchSize + 
                          ", batchTimeMs=" + batchTimeMs);
    }
    
    @Override
    public void append(LoggingEvent event) {
        if (closed || !isLevelEnabled(event.getLevel()) || batchBuffer.isShutdown()) {
            return;
        }
        
        // Add event to batch buffer
        boolean shouldFlush = batchBuffer.add(event);
        
        if (shouldFlush) {
            flushBatch();
        }
    }
    
    /**
     * Checks if batch should be flushed and flushes if necessary.
     * This method is called periodically by the scheduler.
     */
    private void checkAndFlushBatch() {
        if (batchBuffer.shouldFlush()) {
            flushBatch();
        }
    }
    
    /**
     * Flushes the current batch to the file.
     * This method handles the actual I/O operation with all batched events.
     */
    private void flushBatch() {
        List<LoggingEvent> events = batchBuffer.getAndClear();
        
        if (events.isEmpty()) {
            return;
        }
        
        try {
            writeBatch(events);
            batchesWritten.incrementAndGet();
            totalEventsWritten.addAndGet(events.size());
            lastFlushTime = System.currentTimeMillis();
            
        } catch (IOException e) {
            System.err.println("Failed to write batch: " + e.getMessage());
            // Note: In a production system, you might want to implement
            // retry logic or write to an error log here
        }
    }
    
    /**
     * Writes a batch of events to the file in a single I/O operation.
     * 
     * @param events the events to write
     * @throws IOException if writing fails
     */
    private void writeBatch(List<LoggingEvent> events) throws IOException {
        StringBuilder batchContent = ObjectPools.getStringBuilder();
        
        try {
            // Format all events in the batch into a single string
            for (LoggingEvent event : events) {
                String formattedMessage = layout.format(event);
                batchContent.append(formattedMessage);
            }
            
            // Write entire batch in one I/O operation
            String content = batchContent.toString();
            if (!content.isEmpty()) {
                lock.lock();
                try {
                    // Initialize writer if needed
                    if (writer == null) {
                        writer = new ThreadSafeWriter(file, encoding, immediateFlush, bufferSize);
                    }
                    writer.write(content);
                } finally {
                    lock.unlock();
                }
            }
            
        } finally {
            // StringBuilder is automatically returned to pool when it goes out of scope
            // The ObjectPools implementation handles this via ThreadLocal
        }
    }
    
    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            
            closed = true;
            
            // Shutdown the batch buffer to prevent new events
            batchBuffer.shutdown();
            
            // Flush any remaining events
            flushRemainingEvents();
            
            // Shutdown the scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Close the writer
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing writer: " + e.getMessage());
                }
                writer = null;
            }
            
            System.out.println("BatchingFileAppender closed: " + 
                              "name=" + getName() + 
                              ", batchesWritten=" + batchesWritten.get() + 
                              ", totalEvents=" + totalEventsWritten.get());
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Flushes any remaining events during shutdown.
     */
    private void flushRemainingEvents() {
        int attempts = 0;
        while (!batchBuffer.isEmpty() && attempts < 10) {
            flushBatch();
            attempts++;
            
            // Brief pause to allow any in-flight operations to complete
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Forces immediate flush of all buffered events.
     * This method can be called externally when immediate flushing is required.
     */
    public void forceFlush() {
        flushBatch();
    }
    
    // Getters for monitoring and configuration
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public long getBatchTimeMs() {
        return batchTimeMs;
    }
    
    public long getBatchesWritten() {
        return batchesWritten.get();
    }
    
    public long getTotalEventsWritten() {
        return totalEventsWritten.get();
    }
    
    public int getCurrentBufferSize() {
        return batchBuffer.size();
    }
    
    public BatchBuffer.BatchStatistics getBatchStatistics() {
        return batchBuffer.getStatistics();
    }
    
    public long getTimeSinceLastFlush() {
        return System.currentTimeMillis() - lastFlushTime;
    }
    
    /**
     * Gets performance information for monitoring.
     * 
     * @return formatted performance information
     */
    public String getPerformanceInfo() {
        BatchBuffer.BatchStatistics stats = batchBuffer.getStatistics();
        return String.format(
            "BatchingFileAppender[%s]: batches=%d, events=%d, avgPerBatch=%.1f, " +
            "currentBuffer=%d/%d, timeSinceFlush=%dms",
            getName(), batchesWritten.get(), totalEventsWritten.get(),
            stats.getAverageEventsPerBatch(), getCurrentBufferSize(), 
            batchSize, getTimeSinceLastFlush()
        );
    }
    
    // Appender interface implementation
    @Override
    public void setLayout(Layout layout) {
        this.layout = layout != null ? layout : new StandardLayout();
    }
    
    @Override
    public Layout getLayout() {
        return layout;
    }
    
    @Override
    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.TRACE;
    }
    
    @Override
    public LogLevel getLevel() {
        return level;
    }
    
    @Override
    public boolean isLevelEnabled(LogLevel level) {
        return level != null && level.isGreaterOrEqual(this.level);
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void setName(String name) {
        this.name = name != null ? name : "BatchingFile";
    }
    
    @Override
    public String getName() {
        return name;
    }
}