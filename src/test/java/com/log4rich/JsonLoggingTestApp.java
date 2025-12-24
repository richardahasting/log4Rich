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

import com.log4rich.Log4Rich;
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.layouts.JsonLayout;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.Java8Utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive test application demonstrating JSON logging capabilities.
 * 
 * This application showcases various JSON logging scenarios including:
 * - Basic JSON logging with different levels
 * - Structured data logging 
 * - Exception handling in JSON format
 * - Performance comparison between Standard and JSON layouts
 * - Multi-threaded JSON logging
 * - Different JSON configuration options
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class JsonLoggingTestApp {
    
    private static final Logger logger = Log4Rich.getLogger(JsonLoggingTestApp.class);
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println(Java8Utils.repeat("=", 80));
        System.out.println("         log4Rich JSON Logging Test Application");
        System.out.println(Java8Utils.repeat("=", 80));
        
        try {
            // Ensure logs directory exists
            createLogsDirectory();
            
            // Run different test scenarios
            runBasicJsonLoggingDemo();
            runLayoutComparisonDemo();
            runStructuredDataDemo();
            runExceptionHandlingDemo();
            runPerformanceComparison();
            runMultiThreadedDemo();
            runConfigurationOptionsDemo();
            
            System.out.println("\n" + Java8Utils.repeat("=", 80));
            System.out.println("✅ All JSON logging tests completed successfully!");
            System.out.println("📁 Check the 'logs/' directory for output files");
            System.out.println(Java8Utils.repeat("=", 80));
            
        } catch (Exception e) {
            System.err.println("❌ Test application failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the logs directory if it doesn't exist.
     */
    private static void createLogsDirectory() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
            System.out.println("📁 Created logs directory");
        }
    }
    
    /**
     * Demonstrates basic JSON logging with different log levels.
     */
    private static void runBasicJsonLoggingDemo() {
        System.out.println("\n🔧 1. Basic JSON Logging Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        // Create JSON layout appenders
        JsonLayout compactLayout = JsonLayout.createCompactLayout();
        JsonLayout prettyLayout = JsonLayout.createPrettyLayout();
        
        ConsoleAppender compactConsole = new ConsoleAppender(ConsoleAppender.Target.STDOUT);
        compactConsole.setLayout(compactLayout);
        RollingFileAppender compactFile = new RollingFileAppender("logs/json-compact.log");
        compactFile.setLayout(compactLayout);
        
        RollingFileAppender prettyFile = new RollingFileAppender("logs/json-pretty.log");
        prettyFile.setLayout(prettyLayout);
        
        Logger testLogger = Log4Rich.getLogger("JsonBasicTest");
        testLogger.addAppender(compactConsole);
        testLogger.addAppender(compactFile);
        testLogger.addAppender(prettyFile);
        
        // Generate different log levels
        testLogger.trace("This is a trace message with details");
        testLogger.debug("Debug: Processing user authentication for session {}", "abc123");
        testLogger.info("User 'john.doe' logged in successfully from IP {}", "192.168.1.100");
        testLogger.warn("High memory usage detected: {}% of heap used", 87);
        testLogger.error("Database connection failed after {} retries", 3);
        testLogger.fatal("System is shutting down due to critical error");
        
        System.out.println("✅ Basic JSON logging completed");
        System.out.println("   • Compact JSON: logs/json-compact.log");
        System.out.println("   • Pretty JSON:  logs/json-pretty.log");
    }
    
    /**
     * Compares Standard layout vs JSON layout output.
     */
    private static void runLayoutComparisonDemo() {
        System.out.println("\n📊 2. Layout Comparison Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        // Standard layout appender
        StandardLayout standardLayout = new StandardLayout("[%level] %date{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %class.%method:%line - %message%n");
        RollingFileAppender standardFile = new RollingFileAppender("logs/standard-format.log");
        standardFile.setLayout(standardLayout);
        
        // JSON layout appender
        JsonLayout jsonLayout = JsonLayout.createCompactLayout();
        RollingFileAppender jsonFile = new RollingFileAppender("logs/json-format.log");
        jsonFile.setLayout(jsonLayout);
        
        Logger comparisonLogger = Log4Rich.getLogger("LayoutComparison");
        comparisonLogger.addAppender(standardFile);
        comparisonLogger.addAppender(jsonFile);
        
        // Log the same messages to both formats
        comparisonLogger.info("Application started successfully");
        comparisonLogger.debug("Loading configuration from {}", "/etc/myapp/config.properties");
        comparisonLogger.warn("Configuration property '{}' not found, using default: {}", "cache.size", "1000");
        comparisonLogger.error("Failed to connect to service at {}", "https://api.example.com/v1");
        
        System.out.println("✅ Layout comparison completed");
        System.out.println("   • Standard format: logs/standard-format.log");
        System.out.println("   • JSON format:     logs/json-format.log");
    }
    
    /**
     * Demonstrates structured data logging with additional fields.
     */
    private static void runStructuredDataDemo() {
        System.out.println("\n🏗️  3. Structured Data Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        // Create JSON layout with additional fields
        JsonLayout structuredLayout = JsonLayout.createCompactLayout();
        structuredLayout.addAdditionalField("application", "JsonTestApp");
        structuredLayout.addAdditionalField("version", "1.1.0");
        structuredLayout.addAdditionalField("environment", "development");
        structuredLayout.addAdditionalField("datacenter", "us-east-1");
        
        RollingFileAppender structuredFile = new RollingFileAppender("logs/structured-data.log");
        structuredFile.setLayout(structuredLayout);
        
        Logger structuredLogger = Log4Rich.getLogger("StructuredDataTest");
        structuredLogger.addAppender(structuredFile);
        
        // Simulate structured logging scenarios
        structuredLogger.info("User registration completed for email: {}", "user@example.com");
        structuredLogger.info("Payment processed: amount=${}, currency={}, method={}", "99.99", "USD", "credit_card");
        structuredLogger.info("API request: method={}, endpoint={}, response_time={}ms", "GET", "/api/v1/users", 45);
        structuredLogger.warn("Rate limit approaching: current={}, limit={}, window={}s", 450, 500, 60);
        
        System.out.println("✅ Structured data logging completed");
        System.out.println("   • Output: logs/structured-data.log");
    }
    
    /**
     * Demonstrates exception handling in JSON format.
     */
    private static void runExceptionHandlingDemo() {
        System.out.println("\n🚨 4. Exception Handling Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        JsonLayout exceptionLayout = JsonLayout.createCompactLayout();
        RollingFileAppender exceptionFile = new RollingFileAppender("logs/exceptions.log");
        exceptionFile.setLayout(exceptionLayout);
        
        Logger exceptionLogger = Log4Rich.getLogger("ExceptionTest");
        exceptionLogger.addAppender(exceptionFile);
        
        try {
            // Simulate various exceptions
            simulateNullPointerException();
        } catch (Exception e) {
            exceptionLogger.error("Null pointer exception in user service", e);
        }
        
        try {
            simulateIOException();
        } catch (Exception e) {
            exceptionLogger.error("File operation failed for path: {}", "/tmp/nonexistent.txt", e);
        }
        
        try {
            simulateChainedException();
        } catch (Exception e) {
            exceptionLogger.fatal("Critical system error during startup", e);
        }
        
        System.out.println("✅ Exception handling demo completed");
        System.out.println("   • Output: logs/exceptions.log");
    }
    
    /**
     * Compares performance between Standard and JSON layouts.
     */
    private static void runPerformanceComparison() {
        System.out.println("\n⚡ 5. Performance Comparison");
        System.out.println(Java8Utils.repeat("-", 50));
        
        int messageCount = 10000;
        
        // Test Standard layout performance
        StandardLayout standardLayout = new StandardLayout("%date{yyyy-MM-dd HH:mm:ss.SSS} [%level] %message%n");
        RollingFileAppender standardPerfFile = new RollingFileAppender("logs/perf-standard.log");
        standardPerfFile.setLayout(standardLayout);
        
        Logger standardPerfLogger = Log4Rich.getLogger("StandardPerf");
        standardPerfLogger.addAppender(standardPerfFile);
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            standardPerfLogger.info("Performance test message {} with data: {}", i, "sample_data_" + i);
        }
        long standardTime = System.currentTimeMillis() - startTime;
        
        // Test JSON layout performance
        JsonLayout jsonPerfLayout = JsonLayout.createProductionLayout();
        RollingFileAppender jsonPerfFile = new RollingFileAppender("logs/perf-json.log");
        jsonPerfFile.setLayout(jsonPerfLayout);
        
        Logger jsonPerfLogger = Log4Rich.getLogger("JsonPerf");
        jsonPerfLogger.addAppender(jsonPerfFile);
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            jsonPerfLogger.info("Performance test message {} with data: {}", i, "sample_data_" + i);
        }
        long jsonTime = System.currentTimeMillis() - startTime;
        
        // Calculate performance metrics
        double standardThroughput = (double) messageCount / standardTime * 1000;
        double jsonThroughput = (double) messageCount / jsonTime * 1000;
        double overhead = ((double) jsonTime / standardTime - 1) * 100;
        
        System.out.printf("   📊 Standard Layout: %,d messages in %,d ms (%.0f msg/sec)%n", 
                         messageCount, standardTime, standardThroughput);
        System.out.printf("   📊 JSON Layout:     %,d messages in %,d ms (%.0f msg/sec)%n", 
                         messageCount, jsonTime, jsonThroughput);
        System.out.printf("   📈 JSON Overhead:   %.1f%%%n", overhead);
        
        if (overhead < 20) {
            System.out.println("   ✅ JSON performance is excellent (< 20% overhead)");
        } else if (overhead < 50) {
            System.out.println("   👍 JSON performance is good (< 50% overhead)");
        } else {
            System.out.println("   ⚠️  JSON performance needs optimization (> 50% overhead)");
        }
    }
    
    /**
     * Demonstrates multi-threaded JSON logging.
     */
    private static void runMultiThreadedDemo() {
        System.out.println("\n🔀 6. Multi-threaded JSON Logging Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        JsonLayout threadLayout = JsonLayout.createCompactLayout();
        threadLayout.addAdditionalField("test_type", "multi_threaded");
        
        RollingFileAppender threadFile = new RollingFileAppender("logs/multi-threaded.log");
        threadFile.setLayout(threadLayout);
        
        Logger threadLogger = Log4Rich.getLogger("MultiThreadTest");
        threadLogger.addAppender(threadFile);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Submit tasks to multiple threads
        for (int threadId = 1; threadId <= 5; threadId++) {
            final int id = threadId;
            executor.submit(() -> {
                for (int i = 1; i <= 20; i++) {
                    try {
                        Thread.sleep(random.nextInt(10)); // Simulate work
                        threadLogger.info("Thread-{} processing item {} of 20", id, i);
                        
                        if (i % 7 == 0) {
                            threadLogger.warn("Thread-{} encountered warning at item {}", id, i);
                        }
                        
                        if (i == 15) {
                            threadLogger.debug("Thread-{} reached checkpoint at item {}", id, i);
                        }
                        
                    } catch (InterruptedException e) {
                        threadLogger.error("Thread-{} was interrupted", id, e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                threadLogger.info("Thread-{} completed all tasks", id);
            });
        }
        
        executor.shutdown();
        try {
            if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("✅ Multi-threaded logging completed");
                System.out.println("   • Output: logs/multi-threaded.log");
            } else {
                System.out.println("⚠️  Multi-threaded test timed out");
            }
        } catch (InterruptedException e) {
            System.out.println("❌ Multi-threaded test interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Demonstrates different JSON configuration options.
     */
    private static void runConfigurationOptionsDemo() {
        System.out.println("\n⚙️  7. Configuration Options Demo");
        System.out.println(Java8Utils.repeat("-", 50));
        
        // Test different layout configurations
        testMinimalLayout();
        testProductionLayout();
        testDevelopmentLayout();
        testCustomTimestampLayout();
        
        System.out.println("✅ Configuration options demo completed");
    }
    
    private static void testMinimalLayout() {
        JsonLayout minimalLayout = JsonLayout.createMinimalLayout();
        RollingFileAppender minimalFile = new RollingFileAppender("logs/minimal-json.log");
        minimalFile.setLayout(minimalLayout);
        
        Logger minimalLogger = Log4Rich.getLogger("MinimalTest");
        minimalLogger.addAppender(minimalFile);
        minimalLogger.info("Minimal layout: only essential fields");
        
        System.out.println("   📝 Minimal layout: logs/minimal-json.log");
    }
    
    private static void testProductionLayout() {
        JsonLayout productionLayout = JsonLayout.createProductionLayout();
        productionLayout.addAdditionalField("service", "user-service");
        productionLayout.addAdditionalField("version", "2.1.0");
        
        RollingFileAppender productionFile = new RollingFileAppender("logs/production-json.log");
        productionFile.setLayout(productionLayout);
        
        Logger productionLogger = Log4Rich.getLogger("ProductionTest");
        productionLogger.addAppender(productionFile);
        productionLogger.info("Production layout: optimized for performance");
        
        System.out.println("   🏭 Production layout: logs/production-json.log");
    }
    
    private static void testDevelopmentLayout() {
        JsonLayout devLayout = JsonLayout.createPrettyLayout();
        devLayout.addAdditionalField("developer", "test-user");
        devLayout.addAdditionalField("branch", "feature/json-logging");
        
        RollingFileAppender devFile = new RollingFileAppender("logs/development-json.log");
        devFile.setLayout(devLayout);
        
        Logger devLogger = Log4Rich.getLogger("DevelopmentTest");
        devLogger.addAppender(devFile);
        devLogger.info("Development layout: pretty-printed for readability");
        
        System.out.println("   🛠️  Development layout: logs/development-json.log");
    }
    
    private static void testCustomTimestampLayout() {
        JsonLayout customLayout = new JsonLayout(false, true, true, "yyyy/MM/dd HH:mm:ss.SSS");
        RollingFileAppender customFile = new RollingFileAppender("logs/custom-timestamp.log");
        customFile.setLayout(customLayout);
        
        Logger customLogger = Log4Rich.getLogger("CustomTimestampTest");
        customLogger.addAppender(customFile);
        customLogger.info("Custom timestamp format: yyyy/MM/dd HH:mm:ss.SSS");
        
        System.out.println("   🕐 Custom timestamp: logs/custom-timestamp.log");
    }
    
    // Helper methods to simulate exceptions
    
    private static void simulateNullPointerException() {
        String nullString = null;
        nullString.length(); // This will throw NPE
    }
    
    private static void simulateIOException() throws java.io.IOException {
        throw new java.io.IOException("File not found: /tmp/nonexistent.txt");
    }
    
    private static void simulateChainedException() throws Exception {
        try {
            throw new IllegalStateException("Invalid application state");
        } catch (IllegalStateException e) {
            throw new RuntimeException("System initialization failed", e);
        }
    }
}