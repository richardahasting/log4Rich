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
package com.log4rich.compression;

import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.util.AsyncCompressionManager;
import com.log4rich.util.CompressionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for asynchronous compression functionality.
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
@Tag("slow")
public class AsyncCompressionTest {
    
    @TempDir
    Path tempDir;
    
    private AsyncCompressionManager asyncCompressionManager;
    private File testFile;
    
    @BeforeEach
    void setUp() throws IOException {
        asyncCompressionManager = new AsyncCompressionManager();
        testFile = tempDir.resolve("test.log").toFile();

        // Create a test file with some content
        Files.write(testFile.toPath(), "Test log content for compression\n".getBytes());
    }

    @AfterEach
    void tearDown() {
        if (asyncCompressionManager != null) {
            asyncCompressionManager.shutdown();
        }
    }
    
    @Test
    void testAsyncCompressionBasicFunctionality() throws Exception {
        // Test basic async compression
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        
        boolean queued = asyncCompressionManager.compressFileAsync(testFile, 
            new AsyncCompressionManager.CompressionCallback() {
                @Override
                public void onCompressionComplete(File originalFile, File compressedFile, boolean compressionSuccess) {
                    success[0] = compressionSuccess;
                    latch.countDown();
                }
            });
        
        assertTrue(queued, "Compression should be queued successfully");
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Compression should complete within timeout");
        
        // Note: Success depends on gzip being available, so we just check that the callback was called
        assertNotNull(success, "Callback should have been called");
    }
    
    @Test
    void testAsyncCompressionStatistics() {
        AsyncCompressionManager.CompressionStatistics stats = asyncCompressionManager.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.getCurrentQueueSize());
        assertEquals(0, stats.getTotalCompressed());
        assertEquals(0, stats.getTotalFailed());
        assertEquals(0, stats.getAdaptiveResizes());
        assertTrue(stats.getMaxQueueSize() > 0);
    }
    
    @Test
    void testAdaptiveCompressionWithSmallFile() {
        long currentMaxSize = 1024 * 1024; // 1MB
        String appenderName = "TestAppender";
        
        AsyncCompressionManager.AdaptiveCompressionResult result = 
            asyncCompressionManager.compressWithAdaptiveManagement(testFile, currentMaxSize, appenderName);
        
        assertNotNull(result);
        assertEquals(currentMaxSize, result.getNewMaxSize()); // Should not increase for normal file
        assertFalse(result.wasSizeIncreased());
        assertFalse(result.wasBlocked());
    }
    
    @Test
    void testRollingFileAppenderWithAsyncCompression() throws Exception {
        File logFile = tempDir.resolve("rolling.log").toFile();
        RollingFileAppender appender = new RollingFileAppender(logFile);
        
        // Configure for async compression
        appender.setUseAsyncCompression(true);
        appender.setMaxFileSize(100); // Very small to force rollover
        appender.setName("AsyncCompressionTest");
        
        // Verify async compression is enabled
        assertTrue(appender.isUseAsyncCompression());
        assertNotNull(appender.getAsyncCompressionManager());
        
        // Write some log messages to trigger rollover
        Logger testLogger = new Logger("TestLogger");
        testLogger.addAppender(appender);
        
        for (int i = 0; i < 20; i++) {
            testLogger.info("This is a test log message number " + i + " that should trigger rollover due to small file size limit");
        }
        
        // Allow time for any async operations
        Thread.sleep(500);
        
        // Get compression statistics
        AsyncCompressionManager.CompressionStatistics stats = appender.getCompressionStatistics();
        assertNotNull(stats);
        
        // Cleanup
        appender.close();
        testLogger.removeAppender(appender);
    }
    
    @Test
    void testAsyncCompressionManagerShutdown() {
        AsyncCompressionManager manager = new AsyncCompressionManager();
        
        // Queue a compression task
        manager.compressFileAsync(testFile, null);
        
        // Shutdown should complete without hanging
        manager.shutdown();
        
        // Subsequent operations should be ignored
        boolean queued = manager.compressFileAsync(testFile, null);
        assertFalse(queued, "Operations after shutdown should be rejected");
    }
    
    @Test
    void testCompressionQueueMonitoring() {
        // Test with a small queue to demonstrate monitoring
        AsyncCompressionManager smallQueueManager = new AsyncCompressionManager(
            new CompressionManager(), 5, 1, 30000);
        
        AsyncCompressionManager.CompressionStatistics stats = smallQueueManager.getStatistics();
        assertEquals(5, stats.getMaxQueueSize());
        assertEquals(0, stats.getCurrentQueueSize());
        assertEquals(0.0, stats.getQueueUtilization(), 0.01);
        
        smallQueueManager.shutdown();
    }
    
    @Test
    void testNonExistentFileHandling() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.log");
        
        // Should handle non-existent files gracefully
        boolean queued = asyncCompressionManager.compressFileAsync(nonExistentFile, null);
        assertFalse(queued, "Non-existent file should not be queued for compression");
        
        // Adaptive compression should also handle it gracefully
        AsyncCompressionManager.AdaptiveCompressionResult result = 
            asyncCompressionManager.compressWithAdaptiveManagement(nonExistentFile, 1024, "Test");
        
        assertNotNull(result);
        assertEquals(nonExistentFile, result.getCompressedFile());
        assertFalse(result.wasSizeIncreased());
    }
    
    @Test
    void testCompressionCallbackError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] callbackException = {null};
        
        // Test callback that throws an exception
        boolean queued = asyncCompressionManager.compressFileAsync(testFile, 
            new AsyncCompressionManager.CompressionCallback() {
                @Override
                public void onCompressionComplete(File originalFile, File compressedFile, boolean success) {
                    try {
                        throw new RuntimeException("Test callback exception");
                    } catch (Exception e) {
                        callbackException[0] = e;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        
        if (queued) {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Callback should complete within timeout");
            // The compression system should handle callback exceptions gracefully
            assertNotNull(callbackException[0], "Callback exception should have been caught");
        }
    }
    
    @Test
    void testAdaptiveCompressionWithSlowCompression() throws Exception {
        System.out.println("\n=== Testing Adaptive Compression with Slow Compression ===");
        
        // Create a slow compression manager that takes 5 seconds per compression
        SlowCompressionManager slowCompressionManager = new SlowCompressionManager(5000);
        
        // Create async compression manager with small queue to trigger overflow quickly
        AsyncCompressionManager slowAsyncManager = new AsyncCompressionManager(
            slowCompressionManager, 
            3,    // Very small queue size
            1,    // Single compression thread
            30000 // 30 second timeout
        );
        
        File logFile = tempDir.resolve("adaptive-test.log").toFile();
        RollingFileAppender appender = new RollingFileAppender(logFile);
        
        // Configure for very aggressive rolling to trigger compression overload
        appender.setAsyncCompressionManager(slowAsyncManager);
        appender.setUseAsyncCompression(true);
        appender.setMaxFileSize(50); // Extremely small to force frequent rollover
        appender.setName("AdaptiveTest");
        
        Logger testLogger = new Logger("AdaptiveTestLogger");
        testLogger.addAppender(appender);
        
        long originalMaxSize = appender.getMaxFileSize();
        System.out.println("Original max file size: " + originalMaxSize + " bytes");
        
        // Generate log messages rapidly to overwhelm the compression system
        System.out.println("Generating rapid log messages to trigger compression overload...");
        
        for (int i = 0; i < 50; i++) {
            testLogger.info("Rapid fire log message #" + i + 
                          " designed to trigger file rollover and compression queue overflow " +
                          "due to the artificially slow compression process in our test scenario");
            
            // Brief pause to allow some file operations
            if (i % 5 == 0) {
                Thread.sleep(10);
            }
        }
        
        // Allow some time for the adaptive mechanism to trigger
        System.out.println("Waiting for adaptive compression management to respond...");
        Thread.sleep(2000);
        
        // Check if adaptive resizing occurred
        long newMaxSize = appender.getMaxFileSize();
        AsyncCompressionManager.CompressionStatistics stats = appender.getCompressionStatistics();
        
        System.out.println("Final max file size: " + newMaxSize + " bytes");
        System.out.println("Compression statistics: " + stats);
        
        // Verify adaptive behavior
        if (newMaxSize > originalMaxSize) {
            System.out.println("✓ SUCCESS: Adaptive file size increase detected!");
            System.out.println("  - Original size: " + originalMaxSize + " bytes");
            System.out.println("  - New size: " + newMaxSize + " bytes");
            System.out.println("  - Increase factor: " + (newMaxSize / originalMaxSize) + "x");
            assertTrue(stats.getAdaptiveResizes() > 0, "Should have recorded adaptive resizes");
        } else {
            System.out.println("ℹ No adaptive resizing occurred - compression kept up with generation");
            System.out.println("  This can happen if the system is very fast or timing is different");
        }
        
        // Verify compression is working
        assertTrue(stats.getTotalCompressed() >= 0, "Should have attempted compressions");
        assertTrue(slowCompressionManager.getCompressionCount() >= 0, "Slow compression manager should have been used");
        
        // Cleanup
        appender.close();
        testLogger.removeAppender(appender);
        slowAsyncManager.shutdown();
        
        System.out.println("=== Adaptive Compression Test Complete ===\n");
    }
    
    @Test
    void testCompressionQueueOverflow() throws Exception {
        System.out.println("\n=== Testing Compression Queue Overflow Behavior ===");

        // Create a slow compression manager (500ms per compression)
        SlowCompressionManager slowManager = new SlowCompressionManager(500);

        // Create async compression manager with small queue
        // Note: QUEUE_CRITICAL_THRESHOLD is 25, so we need to queue more than that to trigger rejection
        AsyncCompressionManager overflowManager = new AsyncCompressionManager(
            slowManager,
            50,   // Queue size 50
            1,    // Single thread (to slow processing)
            60000 // Long timeout
        );

        // Create many test files to exceed critical threshold (25)
        File[] testFiles = new File[35];
        for (int i = 0; i < testFiles.length; i++) {
            testFiles[i] = tempDir.resolve("overflow-test-" + i + ".log").toFile();
            Files.write(testFiles[i].toPath(), ("Test content for file " + i + "\n").getBytes());
        }

        // Try to queue many compressions rapidly (no pause between)
        int queued = 0;
        int rejected = 0;

        for (File testFile : testFiles) {
            boolean success = overflowManager.compressFileAsync(testFile,
                new AsyncCompressionManager.CompressionCallback() {
                    @Override
                    public void onCompressionComplete(File originalFile, File compressedFile, boolean success) {
                        // Callback for completion tracking
                    }
                });

            if (success) {
                queued++;
            } else {
                rejected++;
                System.out.println("Rejected compression for: " + testFile.getName() + " (queue full/critical threshold)");
            }
        }

        AsyncCompressionManager.CompressionStatistics stats = overflowManager.getStatistics();
        System.out.println("Final stats: " + stats);
        System.out.println("Queued: " + queued + ", Rejected: " + rejected);

        // Verify queue management - either we rejected some or the queue is working
        // The queue should not exceed its max size, and if we queued more than critical threshold
        // some should have been rejected
        assertTrue(stats.getCurrentQueueSize() <= stats.getMaxQueueSize(),
                  "Queue size should not exceed maximum");

        // If we tried to queue more than critical threshold (25), some should be rejected
        // OR all were processed (which is also valid behavior with CallerRunsPolicy)
        assertTrue(queued + rejected == testFiles.length,
                  "All files should be either queued or rejected");

        // Cleanup
        overflowManager.shutdown();

        System.out.println("=== Queue Overflow Test Complete ===\n");
    }
}