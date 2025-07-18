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

package com.log4rich;

import com.log4rich.appenders.BatchingFileAppender;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.MemoryMappedFileAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demonstration of log4Rich usage patterns.
 * 
 * This class demonstrates:
 * 1. Basic logging with configuration
 * 2. High-performance memory-mapped file logging
 * 3. Ultra-high-throughput batch processing
 * 4. Multi-threaded logging scenarios
 * 5. Runtime configuration management
 * 6. Best practices for production use
 * 
 * Use this as a reference for implementing log4Rich in your applications.
 * 
 * @author log4Rich Contributors
 * @since 1.0.0
 */
public class Log4RichUsageDemo {
    
    // Example of class-level logger (recommended pattern)
    private static final Logger logger = Log4Rich.getLogger(Log4RichUsageDemo.class);
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== log4Rich Usage Demonstration ===\n");
        
        // Demonstrate different usage patterns
        demonstrateBasicUsage();
        demonstrateHighPerformanceUsage();
        demonstrateMultiThreadedUsage();
        demonstrateRuntimeConfiguration();
        demonstrateProductionBestPractices();
        
        System.out.println("\n=== Demonstration Complete ===");
        System.out.println("Check the 'demo-logs' directory for output files.");
        
        // Clean shutdown
        Log4Rich.shutdown();
    }
    
    /**
     * Demonstrates basic log4Rich usage with default configuration.
     */
    private static void demonstrateBasicUsage() {
        System.out.println("1. Basic Usage Example");
        System.out.println("====================");
        
        // Simple logging - log4Rich auto-configures with sensible defaults
        Logger simpleLogger = Log4Rich.getLogger("BasicExample");
        
        simpleLogger.info("Application starting up...");
        simpleLogger.debug("Debug information (may not appear depending on level)");
        simpleLogger.warn("This is a warning message");
        simpleLogger.error("Error occurred", new RuntimeException("Example exception"));
        
        // Level checking for expensive operations
        if (simpleLogger.isDebugEnabled()) {
            simpleLogger.debug("Expensive debug info: " + computeExpensiveValue());
        }
        
        System.out.println("✓ Basic logging complete\n");
    }
    
    /**
     * Demonstrates high-performance features: memory-mapped files and batch processing.
     */
    private static void demonstrateHighPerformanceUsage() throws Exception {
        System.out.println("2. High-Performance Features");
        System.out.println("============================");
        
        // Create loggers for performance comparison
        Logger standardLogger = Log4Rich.getLogger("StandardPerf");
        Logger mmapLogger = Log4Rich.getLogger("MemoryMappedPerf");
        Logger batchLogger = Log4Rich.getLogger("BatchPerf");
        
        // Standard file appender
        RollingFileAppender standardAppender = new RollingFileAppender("demo-logs/standard.log");
        standardAppender.setName("StandardAppender");
        standardLogger.addAppender(standardAppender);
        
        // Memory-mapped file appender (5.4x faster)
        MemoryMappedFileAppender mmapAppender = new MemoryMappedFileAppender(
            "MemoryMappedAppender",
            "demo-logs/mmap.log",
            16 * 1024 * 1024, // 16MB mapping
            false,             // Don't force on every write
            1000              // Force every 1 second
        );
        mmapLogger.addAppender(mmapAppender);
        
        // Batch processing appender (23x faster multi-threaded)
        BatchingFileAppender batchAppender = new BatchingFileAppender(
            "BatchAppender",
            "demo-logs/batch.log",
            500,  // Batch size
            50    // Batch time ms
        );
        batchLogger.addAppender(batchAppender);
        
        // Performance test with 10,000 messages
        int messageCount = 10000;
        System.out.println("Writing " + messageCount + " messages with each appender...");
        
        // Standard appender timing
        long start = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            standardLogger.info("Standard message " + i + " - some additional content for realistic message size");
        }
        long standardTime = System.currentTimeMillis() - start;
        
        // Memory-mapped appender timing
        start = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            mmapLogger.info("Memory-mapped message " + i + " - some additional content for realistic message size");
        }
        long mmapTime = System.currentTimeMillis() - start;
        
        // Batch appender timing
        start = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            batchLogger.info("Batch message " + i + " - some additional content for realistic message size");
        }
        // Force flush for accurate timing
        batchAppender.forceFlush();
        long batchTime = System.currentTimeMillis() - start;
        
        // Display results
        System.out.printf("Standard appender: %d ms (%.0f msg/s)%n", 
                         standardTime, messageCount * 1000.0 / standardTime);
        System.out.printf("Memory-mapped:     %d ms (%.0f msg/s) - %.1fx faster%n", 
                         mmapTime, messageCount * 1000.0 / mmapTime, (double) standardTime / mmapTime);
        System.out.printf("Batch processing:  %d ms (%.0f msg/s) - %.1fx faster%n", 
                         batchTime, messageCount * 1000.0 / batchTime, (double) standardTime / batchTime);
        
        // Cleanup
        standardAppender.close();
        mmapAppender.close();
        batchAppender.close();
        
        System.out.println("✓ High-performance demonstration complete\n");
    }
    
    /**
     * Demonstrates multi-threaded logging scenarios.
     */
    private static void demonstrateMultiThreadedUsage() throws Exception {
        System.out.println("3. Multi-Threaded Logging");
        System.out.println("=========================");
        
        // Create batch appender for best multi-threaded performance
        Logger mtLogger = Log4Rich.getLogger("MultiThreaded");
        BatchingFileAppender mtAppender = new BatchingFileAppender(
            "MultiThreadAppender",
            "demo-logs/multithreaded.log",
            1000, // Larger batch for better MT performance
            100   // Reasonable flush interval
        );
        mtLogger.addAppender(mtAppender);
        
        // Simulate multi-threaded application
        ExecutorService executor = Executors.newFixedThreadPool(4);
        int threadsCount = 4;
        int messagesPerThread = 2500; // 10,000 total messages
        
        System.out.println("Spawning " + threadsCount + " threads, " + 
                          messagesPerThread + " messages each...");
        
        long start = System.currentTimeMillis();
        
        for (int t = 0; t < threadsCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                Logger threadLogger = Log4Rich.getLogger("WorkerThread-" + threadId);
                threadLogger.addAppender(mtAppender);
                
                for (int i = 0; i < messagesPerThread; i++) {
                    threadLogger.info("Thread " + threadId + " message " + i + 
                                     " - processing important business logic");
                    
                    // Simulate some work
                    if (i % 100 == 0) {
                        threadLogger.debug("Thread " + threadId + " checkpoint: " + i);
                    }
                    
                    if (i % 500 == 0) {
                        threadLogger.warn("Thread " + threadId + " midpoint warning");
                    }
                }
                
                threadLogger.info("Thread " + threadId + " completed");
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Force final flush
        mtAppender.forceFlush();
        long totalTime = System.currentTimeMillis() - start;
        
        System.out.printf("Multi-threaded performance: %d ms (%.0f msg/s)%n", 
                         totalTime, (threadsCount * messagesPerThread * 1000.0) / totalTime);
        System.out.println("Batch statistics: " + mtAppender.getBatchStatistics());
        
        mtAppender.close();
        System.out.println("✓ Multi-threaded demonstration complete\n");
    }
    
    /**
     * Demonstrates runtime configuration management.
     */
    private static void demonstrateRuntimeConfiguration() {
        System.out.println("4. Runtime Configuration");
        System.out.println("========================");
        
        Logger configLogger = Log4Rich.getLogger("ConfigDemo");
        
        // Show current configuration
        System.out.println("Current root level: " + Log4Rich.getRootLevel());
        System.out.println("Console enabled: " + Log4Rich.isConsoleEnabled());
        System.out.println("File enabled: " + Log4Rich.isFileEnabled());
        
        // Test current level
        configLogger.debug("This debug message may not appear");
        configLogger.info("This info message should appear");
        
        // Change configuration at runtime
        System.out.println("\nChanging root level to DEBUG...");
        Log4Rich.setRootLevel(LogLevel.DEBUG);
        
        configLogger.debug("This debug message should now appear");
        
        // Configure specific logger
        Log4Rich.setLoggerLevel("ConfigDemo", LogLevel.WARN);
        System.out.println("Set ConfigDemo logger to WARN level");
        
        configLogger.debug("This debug message should not appear");
        configLogger.info("This info message should not appear");
        configLogger.warn("This warning message should appear");
        
        // Reset to INFO
        Log4Rich.setLoggerLevel("ConfigDemo", LogLevel.INFO);
        System.out.println("Reset ConfigDemo logger to INFO level");
        
        configLogger.info("Runtime configuration complete");
        System.out.println("✓ Runtime configuration demonstration complete\n");
    }
    
    /**
     * Demonstrates production best practices.
     */
    private static void demonstrateProductionBestPractices() {
        System.out.println("5. Production Best Practices");
        System.out.println("============================");
        
        // 1. Use class-based loggers
        Logger prodLogger = Log4Rich.getLogger(Log4RichUsageDemo.class);
        
        // 2. Configure for performance in production
        System.out.println("Production configuration recommendations:");
        System.out.println("- Disable location capture: Log4Rich.setLocationCapture(false)");
        System.out.println("- Use WARN or ERROR level: Log4Rich.setRootLevel(LogLevel.WARN)");
        System.out.println("- Enable batch processing for high throughput");
        System.out.println("- Use memory-mapped files for low latency");
        
        // 3. Demonstrate proper exception logging
        try {
            // Simulate business logic that might fail
            if (System.currentTimeMillis() % 2 == 0) {
                throw new IllegalStateException("Simulated business exception");
            }
        } catch (Exception e) {
            // Proper exception logging with context
            prodLogger.error("Business operation failed: customer_id=12345, operation=update_profile", e);
        }
        
        // 4. Structured logging approach
        prodLogger.info("User action: user_id=67890, action=login, ip=192.168.1.100, duration_ms=150");
        prodLogger.warn("Performance warning: operation=database_query, duration_ms=5000, threshold_ms=3000");
        
        // 5. Use appropriate levels
        prodLogger.trace("Entering method processOrder()"); // Very detailed, usually disabled
        prodLogger.debug("Order validation passed: order_id=98765"); // Development debugging
        prodLogger.info("Order processed successfully: order_id=98765, amount=$125.99"); // Important events
        prodLogger.warn("Inventory low: product_id=ABC123, quantity=5, threshold=10"); // Potential issues
        prodLogger.error("Payment processing failed: order_id=98765, gateway=stripe"); // Errors requiring attention
        prodLogger.fatal("Database connection pool exhausted"); // Critical system failures
        
        System.out.println("✓ Production best practices demonstration complete\n");
    }
    
    /**
     * Simulates an expensive computation for level checking example.
     */
    private static String computeExpensiveValue() {
        // Simulate expensive operation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("expensive_").append(i).append("_");
        }
        return sb.toString();
    }
}