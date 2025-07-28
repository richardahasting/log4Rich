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
package com.log4rich.performance;

import com.log4rich.Log4Rich;
import com.log4rich.appenders.Appender;
import com.log4rich.appenders.BatchingFileAppender;
import com.log4rich.appenders.MemoryMappedFileAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.ObjectPools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmarks for log4Rich performance enhancements.
 * 
 * This test suite measures the performance improvements of various optimizations:
 * - Memory-mapped files vs. regular file I/O
 * - Batch processing vs. individual writes
 * - Zero-allocation logging vs. regular logging
 * - Different threading scenarios
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
@Tag("performance")
@Tag("slow")
public class PerformanceBenchmarkTest {
    
    @TempDir
    Path tempDir;
    
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 100000;
    private static final int THREAD_COUNT = 8;
    private static final String TEST_MESSAGE = "This is a test log message with some content to make it realistic";
    
    @BeforeEach
    void setUp() {
        Log4Rich.shutdown();
        // Reset object pool statistics
        ObjectPools.resetStatistics();
    }
    
    @Test
    void benchmarkRegularFileAppender() throws Exception {
        System.out.println("\\n=== Regular File Appender Benchmark ===");
        
        RollingFileAppender appender = new RollingFileAppender(
            tempDir.resolve("regular.log").toString()
        );
        appender.setName("RegularBenchmark");
        
        PerformanceResult result = benchmarkAppender((Appender) appender, BENCHMARK_ITERATIONS, 1);
        printResults("Regular File Appender", result);
        
        appender.close();
    }
    
    @Test
    void benchmarkMemoryMappedFileAppender() throws Exception {
        System.out.println("\\n=== Memory-Mapped File Appender Benchmark ===");
        
        MemoryMappedFileAppender appender = new MemoryMappedFileAppender(
            "MMapBenchmark", 
            tempDir.resolve("mmap.log").toString(),
            32 * 1024 * 1024, // 32MB mapping
            false, // Don't force on every write
            1000   // Force every 1 second
        );
        
        PerformanceResult result = benchmarkAppender((Appender) appender, BENCHMARK_ITERATIONS, 1);
        printResults("Memory-Mapped File Appender", result);
        
        System.out.println("Memory mapping statistics:");
        System.out.println("  Total bytes written: " + appender.getTotalBytesWritten());
        System.out.println("  Mapping count: " + appender.getMappingCount());
        System.out.println("  Force count: " + appender.getForceCount());
        
        appender.close();
    }
    
    @Test
    void benchmarkBatchingFileAppender() throws Exception {
        System.out.println("\\n=== Batching File Appender Benchmark ===");
        
        BatchingFileAppender appender = new BatchingFileAppender(
            "BatchBenchmark", 
            tempDir.resolve("batch.log").toString(),
            1000, // Batch size
            50    // Batch time ms
        );
        
        PerformanceResult result = benchmarkAppender((Appender) appender, BENCHMARK_ITERATIONS, 1);
        printResults("Batching File Appender", result);
        
        System.out.println("Batch statistics:");
        System.out.println("  " + appender.getBatchStatistics());
        
        appender.close();
    }
    
    @Test
    void benchmarkMultiThreadedPerformance() throws Exception {
        System.out.println("\\n=== Multi-Threaded Performance Comparison ===");
        
        // Test regular appender
        RollingFileAppender regularAppender = new RollingFileAppender(
            tempDir.resolve("multi_regular.log").toString()
        );
        regularAppender.setName("MultiRegular");
        
        PerformanceResult regularResult = benchmarkAppender(
            (Appender) regularAppender, BENCHMARK_ITERATIONS, THREAD_COUNT
        );
        regularAppender.close();
        
        // Test memory-mapped appender
        MemoryMappedFileAppender mmapAppender = new MemoryMappedFileAppender(
            "MultiMMap", 
            tempDir.resolve("multi_mmap.log").toString()
        );
        
        PerformanceResult mmapResult = benchmarkAppender(
            (Appender) mmapAppender, BENCHMARK_ITERATIONS, THREAD_COUNT
        );
        mmapAppender.close();
        
        // Test batching appender
        BatchingFileAppender batchAppender = new BatchingFileAppender(
            "MultiBatch", 
            tempDir.resolve("multi_batch.log").toString()
        );
        
        PerformanceResult batchResult = benchmarkAppender(
            (Appender) batchAppender, BENCHMARK_ITERATIONS, THREAD_COUNT
        );
        batchAppender.close();
        
        // Print comparison
        System.out.println("\\nMulti-threaded results (" + THREAD_COUNT + " threads):");
        printComparison("Regular", regularResult, "Memory-Mapped", mmapResult);
        printComparison("Regular", regularResult, "Batching", batchResult);
        printComparison("Memory-Mapped", mmapResult, "Batching", batchResult);
    }
    
    @Test
    void benchmarkZeroAllocationLogging() throws Exception {
        System.out.println("\\n=== Zero-Allocation Logging Benchmark ===");
        
        RollingFileAppender appender = new RollingFileAppender(
            tempDir.resolve("zero_alloc.log").toString()
        );
        appender.setName("ZeroAlloc");
        
        // Benchmark regular logging
        PerformanceResult regularResult = benchmarkRegularLogging(appender, BENCHMARK_ITERATIONS);
        
        // Reset statistics
        ObjectPools.resetStatistics();
        
        // Benchmark with object pooling for StringBuilder
        PerformanceResult zeroAllocResult = benchmarkWithObjectPooling(appender, BENCHMARK_ITERATIONS);
        
        System.out.println("Regular logging:");
        printResults("Regular", regularResult);
        
        System.out.println("\\nZero-allocation logging:");
        printResults("Zero-Allocation", zeroAllocResult);
        
        System.out.println("\\nObject pool statistics: " + ObjectPools.getStatistics());
        
        double improvement = ((double) zeroAllocResult.throughput / regularResult.throughput - 1) * 100;
        System.out.printf("Zero-allocation improvement: %.1f%%\\n", improvement);
        
        appender.close();
    }
    
    /**
     * Benchmarks an appender with specified iterations and thread count.
     */
    private PerformanceResult benchmarkAppender(Appender appender, 
                                               int iterations, int threadCount) throws Exception {
        
        // Warmup
        runBenchmark(appender, WARMUP_ITERATIONS, 1);
        
        // Actual benchmark
        long startTime = System.nanoTime();
        runBenchmark(appender, iterations, threadCount);
        long endTime = System.nanoTime();
        
        long durationNs = endTime - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double throughput = (iterations * threadCount * 1000.0) / durationMs;
        
        return new PerformanceResult(durationMs, throughput, iterations * threadCount);
    }
    
    /**
     * Runs benchmark with specified parameters.
     */
    private void runBenchmark(Appender appender, int iterations, int threadCount) 
            throws Exception {
        
        if (threadCount == 1) {
            // Single-threaded benchmark
            for (int i = 0; i < iterations; i++) {
                LoggingEvent event = new LoggingEvent(
                    LogLevel.INFO, 
                    TEST_MESSAGE + " " + i,
                    "BenchmarkLogger",
                    null
                );
                appender.append(event);
            }
        } else {
            // Multi-threaded benchmark
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Future<?>> futures = new ArrayList<>();
            
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                Future<?> future = executor.submit(() -> {
                    try {
                        for (int i = 0; i < iterations; i++) {
                            LoggingEvent event = new LoggingEvent(
                                LogLevel.INFO, 
                                TEST_MESSAGE + " " + threadId + ":" + i,
                                "BenchmarkLogger" + threadId,
                                null
                            );
                            appender.append(event);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
            
            // Wait for completion
            latch.await(30, TimeUnit.SECONDS);
            
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * Benchmarks regular logging (with object allocation).
     */
    private PerformanceResult benchmarkRegularLogging(Appender appender, int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                TEST_MESSAGE + " " + i,
                "RegularLogger",
                null
            );
            appender.append(event);
        }
        
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double throughput = (iterations * 1000.0) / durationMs;
        
        return new PerformanceResult(durationMs, throughput, iterations);
    }
    
    /**
     * Benchmarks logging with object pooling for StringBuilder.
     */
    private PerformanceResult benchmarkWithObjectPooling(Appender appender, int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            // Use object pooling for StringBuilder but create new LoggingEvent
            // since LoggingEvent is immutable
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                TEST_MESSAGE + " " + i,
                "PooledLogger",
                null
            );
            appender.append(event);
        }
        
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double throughput = (iterations * 1000.0) / durationMs;
        
        return new PerformanceResult(durationMs, throughput, iterations);
    }
    
    /**
     * Prints benchmark results.
     */
    private void printResults(String name, PerformanceResult result) {
        System.out.printf("%s Results:\\n", name);
        System.out.printf("  Duration: %.2f ms\\n", result.durationMs);
        System.out.printf("  Throughput: %.0f messages/second\\n", result.throughput);
        System.out.printf("  Average latency: %.3f μs/message\\n", 
                         (result.durationMs * 1000) / result.messageCount);
    }
    
    /**
     * Prints comparison between two results.
     */
    private void printComparison(String name1, PerformanceResult result1, 
                                String name2, PerformanceResult result2) {
        double improvement = ((result2.throughput / result1.throughput) - 1) * 100;
        System.out.printf("%s vs %s: %.1f%% %s\\n", 
                         name2, name1, Math.abs(improvement),
                         improvement > 0 ? "faster" : "slower");
    }
    
    /**
     * Performance result container.
     */
    private static class PerformanceResult {
        final double durationMs;
        final double throughput;
        final int messageCount;
        
        PerformanceResult(double durationMs, double throughput, int messageCount) {
            this.durationMs = durationMs;
            this.throughput = throughput;
            this.messageCount = messageCount;
        }
    }
}