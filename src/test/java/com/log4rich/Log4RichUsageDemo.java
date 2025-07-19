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
import com.log4rich.util.AsyncCompressionManager;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demonstration of log4Rich usage patterns.
 * 
 * This class demonstrates:
 * 1. Basic logging with configuration
 * 2. SLF4J-style placeholder logging (NEW in v1.0.0)
 * 3. High-performance memory-mapped file logging
 * 4. Ultra-high-throughput batch processing
 * 5. Asynchronous compression with adaptive management
 * 6. Multi-threaded logging scenarios
 * 7. Runtime configuration management
 * 8. Environment variable configuration
 * 9. Best practices for production use
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
        demonstrateSLF4JStyleLogging();
        demonstrateEnvironmentVariables();
        demonstrateHighPerformanceUsage();
        demonstrateAsyncCompressionUsage();
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
     * Demonstrates SLF4J-style placeholder logging (NEW in v1.0.0).
     */
    private static void demonstrateSLF4JStyleLogging() {
        System.out.println("2. SLF4J-Style Placeholder Logging (NEW!)");
        System.out.println("==========================================");
        
        Logger slf4jLogger = Log4Rich.getLogger("SLF4JStyleDemo");
        
        // Basic placeholder logging - identical to SLF4J syntax
        String username = "john_doe";
        int loginAttempts = 3;
        slf4jLogger.info("User {} failed login after {} attempts", username, loginAttempts);
        
        // Performance metrics logging
        long duration = 245;
        int recordCount = 1000;
        slf4jLogger.warn("Processing {} records took {} ms (threshold: {} ms)", 
                        recordCount, duration, 200);
        
        // Complex object logging
        Double price = 129.99;
        String productId = "WIDGET-ABC123";
        slf4jLogger.info("Product {} priced at ${}", productId, price);
        
        // Array logging (SLF4J compatible)
        String[] categories = {"electronics", "widgets", "gadgets"};
        slf4jLogger.debug("Product belongs to categories: {}", (Object) categories);
        
        // Exception with placeholders (automatic exception detection)
        String orderId = "ORDER-98765";
        slf4jLogger.error("Failed to process order {} for user {}", 
                         orderId, username, new RuntimeException("Payment gateway timeout"));
        
        // Mixed traditional and placeholder styles
        slf4jLogger.info("Order {} summary: {} items, total ${} (user: {})", 
                        orderId, 5, 299.95, username);
        
        // Multiple parameter types
        java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
        slf4jLogger.info("User {} logged in at {} from IP {}", 
                        username, timestamp, "192.168.1.100");
        
        System.out.println("✓ SLF4J-style logging demonstration complete\n");
    }
    
    /**
     * Demonstrates environment variable configuration (NEW in v1.0.0).
     */
    private static void demonstrateEnvironmentVariables() {
        System.out.println("3. Environment Variable Configuration (NEW!)");
        System.out.println("============================================");
        
        System.out.println("Environment variable examples:");
        System.out.println("Set LOG4RICH_ROOT_LEVEL=DEBUG to override log level");
        System.out.println("Set LOG4RICH_FILE_PATH=/custom/path.log to override file path");
        System.out.println("Set LOG4RICH_CONSOLE_ENABLED=false to disable console");
        System.out.println("Set LOG4RICH_LOCATION_CAPTURE=false for production");
        
        // Show which environment variables are supported
        String[] supportedVars = com.log4rich.config.ConfigLoader.getSupportedEnvironmentVariables();
        System.out.println("\nSupported environment variables (" + supportedVars.length + " total):");
        for (int i = 0; i < Math.min(10, supportedVars.length); i++) {
            System.out.println("  " + supportedVars[i]);
        }
        if (supportedVars.length > 10) {
            System.out.println("  ... and " + (supportedVars.length - 10) + " more");
        }
        
        Logger envLogger = Log4Rich.getLogger("EnvironmentDemo");
        envLogger.info("Environment variable configuration allows easy Docker/K8s deployment");
        envLogger.info("Example: docker run -e LOG4RICH_ROOT_LEVEL=WARN myapp");
        
        System.out.println("✓ Environment variable demonstration complete\n");
    }
    
    /**
     * Demonstrates high-performance features: memory-mapped files and batch processing.
     */
    private static void demonstrateHighPerformanceUsage() throws Exception {
        System.out.println("4. High-Performance Features");
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
     * Demonstrates asynchronous compression with adaptive management.
     */
    private static void demonstrateAsyncCompressionUsage() throws Exception {
        System.out.println("5. Asynchronous Compression with Adaptive Management");
        System.out.println("===================================================");
        
        // Create rolling file appender with async compression
        RollingFileAppender asyncAppender = new RollingFileAppender("demo-logs/async-compression.log");
        asyncAppender.setName("AsyncCompressionDemo");
        asyncAppender.setMaxFileSize(1024); // Very small for demo purposes
        asyncAppender.setMaxBackups(5);
        asyncAppender.setCompression(true);
        asyncAppender.setUseAsyncCompression(true); // Enable async compression
        
        Logger asyncLogger = Log4Rich.getLogger("AsyncCompressionDemo");
        asyncLogger.addAppender(asyncAppender);
        
        System.out.println("Writing messages to trigger file rollover and async compression...");
        
        // Generate enough messages to trigger multiple rollovers
        for (int i = 0; i < 200; i++) {
            asyncLogger.info("Async compression test message " + i + 
                           " - this message will trigger file rollover and background compression " +
                           "without blocking the logging thread, demonstrating the non-blocking nature " +
                           "of the new asynchronous compression system");
            
            // Brief pause every 20 messages to allow compression processing
            if (i % 20 == 0) {
                Thread.sleep(50);
                
                // Show compression statistics
                AsyncCompressionManager.CompressionStatistics stats = asyncAppender.getCompressionStatistics();
                if (stats != null) {
                    System.out.printf("  Compression stats: queue=%d/%d (%.1f%%), compressed=%d%n",
                                    stats.getCurrentQueueSize(), stats.getMaxQueueSize(),
                                    stats.getQueueUtilization() * 100, stats.getTotalCompressed());
                }
            }
        }
        
        // Allow time for final compressions
        Thread.sleep(1000);
        
        // Show final statistics
        AsyncCompressionManager.CompressionStatistics finalStats = asyncAppender.getCompressionStatistics();
        if (finalStats != null) {
            System.out.println("\nFinal compression statistics:");
            System.out.println("  Total compressed: " + finalStats.getTotalCompressed());
            System.out.println("  Total failed: " + finalStats.getTotalFailed());
            System.out.println("  Total blocked: " + finalStats.getTotalBlocked());
            System.out.println("  Adaptive resizes: " + finalStats.getAdaptiveResizes());
            System.out.println("  Queue utilization: " + String.format("%.1f%%", finalStats.getQueueUtilization() * 100));
            
            if (finalStats.getAdaptiveResizes() > 0) {
                System.out.println("  ⚠️  ADAPTIVE BEHAVIOR TRIGGERED: File size limits were automatically doubled!");
                System.out.println("     This happens when compression cannot keep pace with log rotation.");
            }
        }
        
        asyncAppender.close();
        System.out.println("✓ Async compression demonstration complete\n");
        
        // Show created files
        File demoDir = new File("demo-logs");
        if (demoDir.exists()) {
            File[] files = demoDir.listFiles((dir, name) -> name.startsWith("async-compression"));
            if (files != null && files.length > 0) {
                System.out.println("Created files:");
                for (File file : files) {
                    System.out.printf("  %s (%.1f KB)%n", file.getName(), file.length() / 1024.0);
                }
                System.out.println();
            }
        }
    }
    
    /**
     * Demonstrates multi-threaded logging scenarios.
     */
    private static void demonstrateMultiThreadedUsage() throws Exception {
        System.out.println("6. Multi-Threaded Logging");
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
        System.out.println("7. Runtime Configuration");
        System.out.println("========================");
        
        Logger configLogger = Log4Rich.getLogger("ConfigDemo");
        
        // Show current configuration
        System.out.println("Demonstrating runtime configuration changes...");
        
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
        System.out.println("8. Production Best Practices");
        System.out.println("============================");
        
        // 1. Use class-based loggers
        Logger prodLogger = Log4Rich.getLogger(Log4RichUsageDemo.class);
        
        // 2. Configure for performance in production
        System.out.println("Production configuration recommendations:");
        System.out.println("- Disable location capture: Log4Rich.setLocationCapture(false)");
        System.out.println("- Use WARN or ERROR level: Log4Rich.setRootLevel(LogLevel.WARN)");
        System.out.println("- Enable batch processing for high throughput");
        System.out.println("- Use memory-mapped files for low latency");
        System.out.println("- Enable async compression: appender.setUseAsyncCompression(true)");
        System.out.println("- Monitor compression queue utilization in high-volume scenarios");
        
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
        
        // 4. Structured logging approach (using SLF4J-style placeholders)
        String userId = "67890";
        String action = "login";
        String ipAddress = "192.168.1.100";
        int durationMs = 150;
        prodLogger.info("User action: user_id={}, action={}, ip={}, duration_ms={}", 
                       userId, action, ipAddress, durationMs);
        
        // Performance monitoring with placeholders
        String operation = "database_query";
        int queryDuration = 5000;
        int threshold = 3000;
        prodLogger.warn("Performance warning: operation={}, duration_ms={}, threshold_ms={}", 
                       operation, queryDuration, threshold);
        
        // 5. Use appropriate levels with SLF4J-style placeholders
        String orderId = "98765";
        prodLogger.trace("Entering method processOrder()"); // Very detailed, usually disabled
        prodLogger.debug("Order validation passed: order_id={}", orderId); // Development debugging
        prodLogger.info("Order processed successfully: order_id={}, amount=${}", orderId, 125.99); // Important events
        
        String productId = "ABC123";
        int quantity = 5;
        int inventoryThreshold = 10;
        prodLogger.warn("Inventory low: product_id={}, quantity={}, threshold={}", 
                       productId, quantity, inventoryThreshold); // Potential issues
        
        String gateway = "stripe";
        prodLogger.error("Payment processing failed: order_id={}, gateway={}", orderId, gateway); // Errors requiring attention
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