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

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous compression manager that handles log file compression in background threads.
 * 
 * This manager prevents blocking during log file compression by using a dedicated thread pool.
 * It includes adaptive logic to detect when compression cannot keep up with log rotation
 * and automatically adjusts file size limits to prevent compression queue overflow.
 * 
 * Key features:
 * - Non-blocking compression operations
 * - Compression queue monitoring
 * - Adaptive file size management
 * - Performance statistics and monitoring
 * - Graceful shutdown with queue draining
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class AsyncCompressionManager {
    
    private static final int DEFAULT_QUEUE_SIZE = 100;
    private static final int DEFAULT_THREAD_POOL_SIZE = 2;
    private static final long DEFAULT_COMPRESSION_TIMEOUT = 30000; // 30 seconds
    private static final int QUEUE_WARNING_THRESHOLD = 10;
    private static final int QUEUE_CRITICAL_THRESHOLD = 25;
    
    private final CompressionManager compressionManager;
    private final ThreadPoolExecutor compressionExecutor;
    private final BlockingQueue<CompressionTask> compressionQueue;
    private final int maxQueueSize;
    private final long compressionTimeout;
    
    // Statistics and monitoring
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicLong totalCompressed = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalBlocked = new AtomicLong(0);
    private final AtomicLong adaptiveResizes = new AtomicLong(0);
    
    private volatile boolean shutdown = false;
    
    /**
     * Creates an async compression manager with default settings.
     */
    public AsyncCompressionManager() {
        this(new CompressionManager(), DEFAULT_QUEUE_SIZE, DEFAULT_THREAD_POOL_SIZE, DEFAULT_COMPRESSION_TIMEOUT);
    }
    
    /**
     * Creates an async compression manager with custom settings.
     * 
     * @param compressionManager the underlying compression manager
     * @param maxQueueSize maximum number of files that can be queued for compression
     * @param threadPoolSize number of compression threads
     * @param compressionTimeout timeout for individual compression operations
     */
    public AsyncCompressionManager(CompressionManager compressionManager, int maxQueueSize, 
                                 int threadPoolSize, long compressionTimeout) {
        this.compressionManager = compressionManager;
        this.maxQueueSize = maxQueueSize;
        this.compressionTimeout = compressionTimeout;
        
        // Create bounded queue for compression tasks
        this.compressionQueue = new ArrayBlockingQueue<>(maxQueueSize);
        
        // Create separate queue for executor since ThreadPoolExecutor needs BlockingQueue<Runnable>
        BlockingQueue<Runnable> executorQueue = new ArrayBlockingQueue<>(maxQueueSize);
        
        // Create thread pool with daemon threads
        this.compressionExecutor = new ThreadPoolExecutor(
            threadPoolSize, 
            threadPoolSize,
            60L, TimeUnit.SECONDS,
            executorQueue,
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "log4Rich-compression-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority than logging
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // Fallback to caller thread if queue full
        );
        
        System.out.println("AsyncCompressionManager initialized: maxQueue=" + maxQueueSize + 
                          ", threads=" + threadPoolSize + ", timeout=" + compressionTimeout + "ms");
    }
    
    /**
     * Compresses a file asynchronously. Returns immediately without blocking.
     * 
     * @param sourceFile the file to compress
     * @param callback callback to handle compression completion (optional)
     * @return true if the compression was queued successfully, false if queue is full
     */
    public boolean compressFileAsync(File sourceFile, CompressionCallback callback) {
        if (shutdown || sourceFile == null || !sourceFile.exists()) {
            return false;
        }
        
        // Check queue size and handle potential overflow
        int currentQueueSize = queueSize.get();
        
        if (currentQueueSize >= QUEUE_CRITICAL_THRESHOLD) {
            logError("CRITICAL: Compression queue size (" + currentQueueSize + 
                    ") exceeds critical threshold (" + QUEUE_CRITICAL_THRESHOLD + 
                    "). Log rotation may be faster than compression capability.");
            return false;
        }
        
        if (currentQueueSize >= QUEUE_WARNING_THRESHOLD) {
            logWarning("WARNING: Compression queue size (" + currentQueueSize + 
                      ") exceeds warning threshold (" + QUEUE_WARNING_THRESHOLD + 
                      "). Consider increasing file size limits.");
        }
        
        try {
            CompressionTask task = new CompressionTask(sourceFile, callback);
            boolean queued = compressionExecutor.submit(task) != null;
            
            if (queued) {
                queueSize.incrementAndGet();
            }
            
            return queued;
            
        } catch (RejectedExecutionException e) {
            logError("Compression task rejected for file: " + sourceFile.getName() + 
                    ", queue full or executor shutdown");
            return false;
        }
    }
    
    /**
     * Compresses a file with adaptive management. If compression queue is overwhelmed,
     * this method will block, compress the file, and return adaptive recommendations.
     * 
     * @param sourceFile the file to compress
     * @param currentMaxSize current file size limit
     * @param appenderName name of the appender for logging
     * @return compression result with adaptive recommendations
     */
    public AdaptiveCompressionResult compressWithAdaptiveManagement(File sourceFile, 
                                                                   long currentMaxSize, 
                                                                   String appenderName) {
        if (sourceFile == null || !sourceFile.exists()) {
            return new AdaptiveCompressionResult(sourceFile, currentMaxSize, false, false);
        }
        
        int currentQueueSize = queueSize.get();
        boolean shouldBlock = currentQueueSize >= QUEUE_CRITICAL_THRESHOLD;
        boolean shouldResize = false;
        long newMaxSize = currentMaxSize;
        
        if (shouldBlock) {
            // Log the critical situation
            String errorMsg = "CRITICAL COMPRESSION OVERLOAD DETECTED!\n" +
                            "  Appender: " + appenderName + "\n" +
                            "  Queue size: " + currentQueueSize + " (critical threshold: " + QUEUE_CRITICAL_THRESHOLD + ")\n" +
                            "  Current file size limit: " + formatFileSize(currentMaxSize) + "\n" +
                            "  Action: Blocking until compression completes, then doubling file size limit";
            
            logError(errorMsg);
            totalBlocked.incrementAndGet();
            
            // Wait for queue to drain below critical level
            waitForQueueToDrain();
            
            // Double the file size limit
            newMaxSize = currentMaxSize * 2;
            shouldResize = true;
            adaptiveResizes.incrementAndGet();
            
            // Log the adaptation
            String adaptMsg = "ADAPTIVE FILE SIZE INCREASE APPLIED!\n" +
                            "  Appender: " + appenderName + "\n" +
                            "  OLD SIZE LIMIT: " + formatFileSize(currentMaxSize) + "\n" +
                            "  NEW SIZE LIMIT: " + formatFileSize(newMaxSize) + " (DOUBLED)\n" +
                            "  Reason: Compression queue overload prevention\n" +
                            "  Total adaptive resizes: " + adaptiveResizes.get();
            
            logImportant(adaptMsg);
        }
        
        // Now compress the file (synchronously if we were blocked, async otherwise)
        File resultFile;
        if (shouldBlock) {
            // Compress synchronously since we're already blocking
            resultFile = compressionManager.compressFile(sourceFile);
            totalCompressed.incrementAndGet();
        } else {
            // Try async compression
            boolean queued = compressFileAsync(sourceFile, new CompressionCallback() {
                @Override
                public void onCompressionComplete(File original, File compressed, boolean success) {
                    if (success) {
                        totalCompressed.incrementAndGet();
                    } else {
                        totalFailed.incrementAndGet();
                    }
                }
            });
            
            resultFile = queued ? sourceFile : compressionManager.compressFile(sourceFile);
            if (!queued) {
                totalCompressed.incrementAndGet();
            }
        }
        
        return new AdaptiveCompressionResult(resultFile, newMaxSize, shouldResize, shouldBlock);
    }
    
    /**
     * Waits for the compression queue to drain below critical threshold.
     */
    private void waitForQueueToDrain() {
        long startTime = System.currentTimeMillis();
        long timeout = compressionTimeout * 2; // Allow extra time for queue draining
        
        while (queueSize.get() >= QUEUE_CRITICAL_THRESHOLD && 
               System.currentTimeMillis() - startTime < timeout) {
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long waitTime = System.currentTimeMillis() - startTime;
        if (waitTime > 1000) { // Log if we waited more than 1 second
            logWarning("Waited " + waitTime + "ms for compression queue to drain. " +
                      "Final queue size: " + queueSize.get());
        }
    }
    
    /**
     * Shuts down the compression manager gracefully.
     * Waits for pending compressions to complete.
     */
    public void shutdown() {
        shutdown = true;
        
        logInfo("AsyncCompressionManager shutting down...");
        compressionExecutor.shutdown();
        
        try {
            // Wait for existing tasks to complete
            if (!compressionExecutor.awaitTermination(compressionTimeout, TimeUnit.MILLISECONDS)) {
                logWarning("Compression tasks did not complete within timeout, forcing shutdown");
                compressionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            compressionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Print final statistics
        logInfo("AsyncCompressionManager shutdown complete. " +
               "Statistics: compressed=" + totalCompressed.get() + 
               ", failed=" + totalFailed.get() + 
               ", blocked=" + totalBlocked.get() + 
               ", adaptiveResizes=" + adaptiveResizes.get());
    }
    
    /**
     * Gets current statistics for monitoring.
     * 
     * @return current compression statistics including queue utilization and performance metrics
     */
    public CompressionStatistics getStatistics() {
        return new CompressionStatistics(
            queueSize.get(),
            totalCompressed.get(),
            totalFailed.get(),
            totalBlocked.get(),
            adaptiveResizes.get(),
            maxQueueSize
        );
    }
    
    /**
     * Formats file size for human readable output.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    // Logging methods that avoid circular dependencies
    private void logError(String message) {
        System.err.println("[log4Rich] COMPRESSION ERROR: " + message);
    }
    
    private void logWarning(String message) {
        System.err.println("[log4Rich] COMPRESSION WARNING: " + message);
    }
    
    private void logImportant(String message) {
        System.out.println("[log4Rich] COMPRESSION ADAPTATION: " + message);
    }
    
    private void logInfo(String message) {
        System.out.println("[log4Rich] Compression: " + message);
    }
    
    /**
     * Compression task for background execution.
     */
    private class CompressionTask implements Runnable {
        private final File sourceFile;
        private final CompressionCallback callback;
        
        CompressionTask(File sourceFile, CompressionCallback callback) {
            this.sourceFile = sourceFile;
            this.callback = callback;
        }
        
        @Override
        public void run() {
            try {
                File compressed = compressionManager.compressFile(sourceFile);
                boolean success = compressed != null && !compressed.equals(sourceFile);
                
                if (callback != null) {
                    callback.onCompressionComplete(sourceFile, compressed, success);
                }
                
                if (success) {
                    totalCompressed.incrementAndGet();
                } else {
                    totalFailed.incrementAndGet();
                }
                
            } catch (Exception e) {
                totalFailed.incrementAndGet();
                logError("Compression task failed for file: " + sourceFile.getName() + 
                        ", error: " + e.getMessage());
                
                if (callback != null) {
                    callback.onCompressionComplete(sourceFile, sourceFile, false);
                }
            } finally {
                queueSize.decrementAndGet();
            }
        }
    }
    
    /**
     * Callback interface for compression completion notification.
     */
    public interface CompressionCallback {
        /**
         * Called when compression operation completes.
         * 
         * @param originalFile the original file that was compressed
         * @param compressedFile the resulting compressed file (may be same as original if compression failed)
         * @param success true if compression succeeded, false otherwise
         */
        void onCompressionComplete(File originalFile, File compressedFile, boolean success);
    }
    
    /**
     * Result of adaptive compression operation.
     */
    public static class AdaptiveCompressionResult {
        private final File compressedFile;
        private final long newMaxSize;
        private final boolean sizeWasIncreased;
        private final boolean wasBlocked;
        
        AdaptiveCompressionResult(File compressedFile, long newMaxSize, 
                                boolean sizeWasIncreased, boolean wasBlocked) {
            this.compressedFile = compressedFile;
            this.newMaxSize = newMaxSize;
            this.sizeWasIncreased = sizeWasIncreased;
            this.wasBlocked = wasBlocked;
        }
        
        /** @return the compressed file */
        public File getCompressedFile() { return compressedFile; }
        /** @return the new maximum file size after adaptation */
        public long getNewMaxSize() { return newMaxSize; }
        /** @return true if file size was increased due to compression overload */
        public boolean wasSizeIncreased() { return sizeWasIncreased; }
        /** @return true if operation was blocked waiting for compression */
        public boolean wasBlocked() { return wasBlocked; }
    }
    
    /**
     * Statistics for compression monitoring.
     */
    public static class CompressionStatistics {
        private final int currentQueueSize;
        private final long totalCompressed;
        private final long totalFailed;
        private final long totalBlocked;
        private final long adaptiveResizes;
        private final int maxQueueSize;
        
        CompressionStatistics(int currentQueueSize, long totalCompressed, long totalFailed,
                            long totalBlocked, long adaptiveResizes, int maxQueueSize) {
            this.currentQueueSize = currentQueueSize;
            this.totalCompressed = totalCompressed;
            this.totalFailed = totalFailed;
            this.totalBlocked = totalBlocked;
            this.adaptiveResizes = adaptiveResizes;
            this.maxQueueSize = maxQueueSize;
        }
        
        /** @return current number of files in compression queue */
        public int getCurrentQueueSize() { return currentQueueSize; }
        /** @return total number of files successfully compressed */
        public long getTotalCompressed() { return totalCompressed; }
        /** @return total number of compression failures */
        public long getTotalFailed() { return totalFailed; }
        /** @return total number of times compression was blocked */
        public long getTotalBlocked() { return totalBlocked; }
        /** @return total number of adaptive file size increases */
        public long getAdaptiveResizes() { return adaptiveResizes; }
        /** @return maximum compression queue size */
        public int getMaxQueueSize() { return maxQueueSize; }
        /** @return queue utilization as percentage (0.0 to 1.0) */
        public double getQueueUtilization() { return (double) currentQueueSize / maxQueueSize; }
        
        @Override
        public String toString() {
            return String.format(
                "CompressionStats{queue=%d/%d (%.1f%%), compressed=%d, failed=%d, " +
                "blocked=%d, adaptiveResizes=%d}",
                currentQueueSize, maxQueueSize, getQueueUtilization() * 100,
                totalCompressed, totalFailed, totalBlocked, adaptiveResizes
            );
        }
    }
}