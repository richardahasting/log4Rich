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
package com.log4rich.async;

import com.log4rich.appenders.AsyncAppenderWrapper;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.AsyncLogger;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.OverflowStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmarks for asynchronous logging to validate sub-microsecond latency claims.
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class AsyncPerformanceBenchmark {
    
    @TempDir
    Path tempDir;
    
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 100000;
    private static final int THREAD_COUNT = 8;
    private static final String TEST_MESSAGE = "This is a performance test message with realistic content length";
    
    @BeforeEach
    void setUp() {
        // Clean state for each test
    }
    
    @Test
    void benchmarkSynchronousVsAsynchronousLogging() throws Exception {
        System.out.println("\n=== Synchronous vs Asynchronous Logging Benchmark ===");
        
        // Synchronous logging benchmark
        Logger syncLogger = new Logger("SyncBenchmark");
        RollingFileAppender syncAppender = new RollingFileAppender(
            tempDir.resolve("sync.log").toString()
        );
        syncLogger.addAppender(syncAppender);
        
        System.out.println("Warming up synchronous logger...");
        benchmarkSyncLogger(syncLogger, WARMUP_ITERATIONS);
        
        System.out.println("Benchmarking synchronous logger...");
        long syncTime = benchmarkSyncLogger(syncLogger, BENCHMARK_ITERATIONS);
        double syncThroughput = (BENCHMARK_ITERATIONS * 1000.0) / syncTime;
        double syncLatency = (syncTime * 1000000.0) / BENCHMARK_ITERATIONS; // nanoseconds
        
        syncAppender.close();
        
        // Asynchronous logging benchmark
        AsyncLogger asyncLogger = new AsyncLogger("AsyncBenchmark", 65536, OverflowStrategy.DROP_OLDEST, 5000);
        RollingFileAppender asyncAppender = new RollingFileAppender(
            tempDir.resolve("async.log").toString()
        );
        asyncLogger.addAppender(asyncAppender);
        
        System.out.println("Warming up asynchronous logger...");
        benchmarkAsyncLogger(asyncLogger, WARMUP_ITERATIONS);
        
        System.out.println("Benchmarking asynchronous logger...");
        long asyncTime = benchmarkAsyncLogger(asyncLogger, BENCHMARK_ITERATIONS);
        double asyncThroughput = (BENCHMARK_ITERATIONS * 1000.0) / asyncTime;
        double asyncLatency = (asyncTime * 1000000.0) / BENCHMARK_ITERATIONS; // nanoseconds
        
        asyncLogger.flush();
        asyncLogger.shutdown();
        
        // Print results
        System.out.printf("Synchronous logging: %d ms (%.0f msg/s, %.0f ns/msg)%n",
                         syncTime, syncThroughput, syncLatency);
        System.out.printf("Asynchronous logging: %d ms (%.0f msg/s, %.0f ns/msg)%n",
                         asyncTime, asyncThroughput, asyncLatency);
        System.out.printf("Async improvement: %.1fx faster, %.0f ns latency%n",
                         (double) syncTime / asyncTime, asyncLatency);
        
        // Verify sub-microsecond latency claim
        System.out.printf("Sub-microsecond latency achieved: %s (%.0f ns < 1000 ns)%n",
                         asyncLatency < 1000 ? "YES" : "NO", asyncLatency);
    }
    
    @Test
    void benchmarkMultiThreadedAsyncLogging() throws Exception {
        System.out.println("\n=== Multi-Threaded Asynchronous Logging Benchmark ===");
        
        AsyncLogger asyncLogger = new AsyncLogger("MultiAsyncBenchmark", 262144, OverflowStrategy.DROP_OLDEST, 5000);
        RollingFileAppender appender = new RollingFileAppender(
            tempDir.resolve("multi_async.log").toString()
        );
        asyncLogger.addAppender(appender);
        
        // Warmup
        System.out.println("Warming up multi-threaded async logger...");
        benchmarkMultiThreadedAsync(asyncLogger, THREAD_COUNT, WARMUP_ITERATIONS / THREAD_COUNT);
        
        // Benchmark
        System.out.println("Benchmarking multi-threaded async logger...");
        long totalTime = benchmarkMultiThreadedAsync(asyncLogger, THREAD_COUNT, BENCHMARK_ITERATIONS / THREAD_COUNT);
        
        asyncLogger.flush();
        
        double totalThroughput = (BENCHMARK_ITERATIONS * 1000.0) / totalTime;
        double avgLatencyPerThread = (totalTime * 1000000.0) / BENCHMARK_ITERATIONS; // nanoseconds
        
        AsyncLogger.AsyncLoggerStatistics stats = asyncLogger.getStatistics();
        
        System.out.printf("Multi-threaded async (%d threads): %d ms (%.0f msg/s, %.0f ns/msg)%n",
                         THREAD_COUNT, totalTime, totalThroughput, avgLatencyPerThread);
        System.out.printf("Events published: %d, processed: %d, dropped: %d%n",
                         stats.getEventsPublished(), stats.getEventsProcessed(), stats.getEventsDropped());
        System.out.printf("Drop rate: %.2f%%, Buffer utilization: %.1f%%n",
                         stats.getDropRate() * 100, stats.getBufferStats().getUtilization() * 100);
        
        asyncLogger.shutdown();
    }
    
    @Test
    void benchmarkAsyncAppenderWrapper() throws Exception {
        System.out.println("\n=== AsyncAppenderWrapper Performance Benchmark ===");
        
        RollingFileAppender baseAppender = new RollingFileAppender(
            tempDir.resolve("wrapper.log").toString()
        );
        
        AsyncAppenderWrapper asyncWrapper = AsyncAppenderWrapper.Factory.createHighThroughput(baseAppender);
        
        // Warmup
        System.out.println("Warming up async wrapper...");
        benchmarkAsyncWrapper(asyncWrapper, WARMUP_ITERATIONS);
        
        // Benchmark
        System.out.println("Benchmarking async wrapper...");
        long wrapperTime = benchmarkAsyncWrapper(asyncWrapper, BENCHMARK_ITERATIONS);
        
        asyncWrapper.flush();
        
        double wrapperThroughput = (BENCHMARK_ITERATIONS * 1000.0) / wrapperTime;
        double wrapperLatency = (wrapperTime * 1000000.0) / BENCHMARK_ITERATIONS; // nanoseconds
        
        AsyncLogger.AsyncLoggerStatistics stats = asyncWrapper.getStatistics();
        
        System.out.printf("AsyncAppenderWrapper: %d ms (%.0f msg/s, %.0f ns/msg)%n",
                         wrapperTime, wrapperThroughput, wrapperLatency);
        System.out.printf("Wrapper stats: %s%n", asyncWrapper.getSummary());
        
        asyncWrapper.close();
    }
    
    @Test
    void benchmarkOverflowStrategies() throws Exception {
        System.out.println("\n=== Overflow Strategy Performance Comparison ===");
        
        OverflowStrategy[] strategies = {
            OverflowStrategy.DROP_OLDEST,
            OverflowStrategy.DROP_NEWEST,
            OverflowStrategy.DISCARD
        };
        
        int smallBufferSize = 512; // Intentionally small to trigger overflow
        int overflowMessages = 10000;
        
        for (OverflowStrategy strategy : strategies) {
            AsyncLogger logger = new AsyncLogger("Overflow-" + strategy.name(), 
                                                smallBufferSize, strategy, 5000);
            RollingFileAppender appender = new RollingFileAppender(
                tempDir.resolve("overflow_" + strategy.name().toLowerCase() + ".log").toString()
            );
            logger.addAppender(appender);
            
            // Benchmark with overflow conditions
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < overflowMessages; i++) {
                logger.info("Overflow test message " + i + " - " + strategy.name());
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            logger.flush();
            AsyncLogger.AsyncLoggerStatistics stats = logger.getStatistics();
            logger.shutdown();
            
            double throughput = (overflowMessages * 1000.0) / duration;
            double latency = (duration * 1000000.0) / overflowMessages; // nanoseconds
            
            System.out.printf("%s: %d ms (%.0f msg/s, %.0f ns/msg) - " +
                             "Published: %d, Processed: %d, Dropped: %d (%.1f%%)%n",
                             strategy.name(), duration, throughput, latency,
                             stats.getEventsPublished(), stats.getEventsProcessed(),
                             stats.getEventsDropped(), stats.getDropRate() * 100);
        }
    }
    
    @Test
    void benchmarkBufferSizeImpact() throws Exception {
        System.out.println("\n=== Buffer Size Impact on Performance ===");
        
        int[] bufferSizes = {1024, 4096, 16384, 65536, 262144};
        
        for (int bufferSize : bufferSizes) {
            AsyncLogger logger = new AsyncLogger("BufferSize-" + bufferSize,
                                                bufferSize, OverflowStrategy.DROP_OLDEST, 5000);
            RollingFileAppender appender = new RollingFileAppender(
                tempDir.resolve("buffer_" + bufferSize + ".log").toString()
            );
            logger.addAppender(appender);
            
            // Warmup
            benchmarkAsyncLogger(logger, 1000);
            
            // Benchmark
            long benchmarkTime = benchmarkAsyncLogger(logger, 50000);
            
            logger.flush();
            AsyncLogger.AsyncLoggerStatistics stats = logger.getStatistics();
            logger.shutdown();
            
            double throughput = (50000 * 1000.0) / benchmarkTime;
            double latency = (benchmarkTime * 1000000.0) / 50000; // nanoseconds
            
            System.out.printf("Buffer %6d: %4d ms (%.0f msg/s, %.0f ns/msg) - " +
                             "Utilization: %.1f%%, Drops: %d%n",
                             bufferSize, benchmarkTime, throughput, latency,
                             stats.getBufferStats().getUtilization() * 100,
                             stats.getEventsDropped());
        }
    }
    
    private long benchmarkSyncLogger(Logger logger, int iterations) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            logger.info(TEST_MESSAGE + " " + i);
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    private long benchmarkAsyncLogger(AsyncLogger logger, int iterations) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            logger.info(TEST_MESSAGE + " " + i);
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    private long benchmarkMultiThreadedAsync(AsyncLogger logger, int threadCount, int messagesPerThread) 
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        logger.info("Thread " + threadId + " " + TEST_MESSAGE + " " + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        return endTime - startTime;
    }
    
    private long benchmarkAsyncWrapper(AsyncAppenderWrapper wrapper, int iterations) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO, TEST_MESSAGE + " " + i, "WrapperBenchmark", null
            );
            wrapper.append(event);
        }
        
        return System.currentTimeMillis() - startTime;
    }
}