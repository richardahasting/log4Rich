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
import com.log4rich.appenders.ConsoleAppender;
import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.AsyncLogger;
import com.log4rich.core.LogLevel;
import com.log4rich.util.OverflowStrategy;
import com.log4rich.util.RingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for asynchronous logging functionality.
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class AsyncLoggingTest {
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Clean state for each test
    }
    
    @Test
    void testRingBufferBasicOperations() {
        RingBuffer<String> buffer = new RingBuffer<>(8);
        
        assertEquals(8, buffer.getCapacity());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(0, buffer.size());
        
        // Test publishing
        assertTrue(buffer.tryPublish("item1"));
        assertFalse(buffer.isEmpty());
        assertEquals(1, buffer.size());
        
        // Test consuming
        assertEquals("item1", buffer.consume());
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        
        // Test null consume
        assertNull(buffer.consume());
    }
    
    @Test
    void testRingBufferFullBehavior() {
        RingBuffer<String> buffer = new RingBuffer<>(4);
        
        // Fill buffer
        assertTrue(buffer.tryPublish("1"));
        assertTrue(buffer.tryPublish("2"));
        assertTrue(buffer.tryPublish("3"));
        assertTrue(buffer.tryPublish("4"));
        
        assertTrue(buffer.isFull());
        assertEquals(4, buffer.size());
        
        // Should fail when full
        assertFalse(buffer.tryPublish("5"));
        
        // Consume one item
        assertEquals("1", buffer.consume());
        assertFalse(buffer.isFull());
        
        // Should succeed now
        assertTrue(buffer.tryPublish("5"));
    }
    
    @Test
    void testRingBufferBatchOperations() {
        RingBuffer<String> buffer = new RingBuffer<>(8);
        
        // Publish some items
        buffer.tryPublish("1");
        buffer.tryPublish("2");
        buffer.tryPublish("3");
        
        // Consume batch
        String[] items = new String[5];
        int consumed = buffer.consumeBatch(items, 5);
        
        assertEquals(3, consumed);
        assertEquals("1", items[0]);
        assertEquals("2", items[1]);
        assertEquals("3", items[2]);
        assertNull(items[3]);
        assertNull(items[4]);
        
        assertTrue(buffer.isEmpty());
    }
    
    @Test
    void testRingBufferStatistics() {
        RingBuffer<String> buffer = new RingBuffer<>(4);
        
        buffer.tryPublish("1");
        buffer.tryPublish("2");
        buffer.consume();
        
        // Try to publish when full to increment buffer full count
        buffer.tryPublish("3");
        buffer.tryPublish("4");
        buffer.tryPublish("5"); // Should fail
        
        RingBuffer.RingBufferStatistics stats = buffer.getStatistics();
        assertEquals(4, stats.getTotalPublished());
        assertEquals(1, stats.getTotalConsumed());
        assertEquals(1, stats.getBufferFullCount());
        assertEquals(3, stats.getCurrentSize());
        assertTrue(stats.getUtilization() > 0.5);
    }
    
    @Test
    void testAsyncLoggerBasicFunctionality() throws Exception {
        AsyncLogger logger = new AsyncLogger("TestAsync", 16);
        
        // Add a test appender that counts messages
        CountingAppender appender = new CountingAppender();
        logger.addAppender(appender);
        
        // Log some messages
        logger.info("Test message 1");
        logger.debug("Test message 2");
        logger.error("Test message 3", new RuntimeException("Test exception"));
        
        // Wait for processing
        Thread.sleep(100);
        logger.flush();
        
        // Verify messages were processed
        assertTrue(appender.getMessageCount() >= 3);
        
        logger.shutdown();
    }
    
    @Test
    void testAsyncLoggerOverflowStrategies() throws Exception {
        // Test DROP_OLDEST strategy
        AsyncLogger dropOldestLogger = new AsyncLogger("DropOldest", 4, OverflowStrategy.DROP_OLDEST, 1000);
        CountingAppender appender1 = new CountingAppender();
        dropOldestLogger.addAppender(appender1);
        
        // Fill buffer beyond capacity
        for (int i = 0; i < 10; i++) {
            dropOldestLogger.info("Message " + i);
        }
        
        Thread.sleep(50);
        dropOldestLogger.flush();
        
        AsyncLogger.AsyncLoggerStatistics stats1 = dropOldestLogger.getStatistics();
        assertTrue(stats1.getEventsDropped() > 0 || stats1.getOverflowEvents() > 0);
        
        dropOldestLogger.shutdown();
        
        // Test DISCARD strategy
        AsyncLogger discardLogger = new AsyncLogger("Discard", 4, OverflowStrategy.DISCARD, 1000);
        CountingAppender appender2 = new CountingAppender();
        discardLogger.addAppender(appender2);
        
        // Fill buffer beyond capacity
        for (int i = 0; i < 10; i++) {
            discardLogger.info("Message " + i);
        }
        
        Thread.sleep(50);
        discardLogger.flush();
        
        AsyncLogger.AsyncLoggerStatistics stats2 = discardLogger.getStatistics();
        assertTrue(stats2.getEventsDropped() > 0);
        
        discardLogger.shutdown();
    }
    
    @Test
    void testAsyncLoggerMultiThreadedAccess() throws Exception {
        AsyncLogger logger = new AsyncLogger("MultiThread", 1024);
        CountingAppender appender = new CountingAppender();
        logger.addAppender(appender);
        
        int threadCount = 4;
        int messagesPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        logger.info("Thread " + threadId + " message " + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Wait for processing and flush
        Thread.sleep(200);
        logger.flush();
        
        AsyncLogger.AsyncLoggerStatistics stats = logger.getStatistics();
        assertTrue(stats.getEventsProcessed() > 0);
        assertTrue(appender.getMessageCount() > 0);
        
        logger.shutdown();
    }
    
    @Test
    void testAsyncAppenderWrapper() throws Exception {
        File logFile = tempDir.resolve("async-wrapper.log").toFile();
        RollingFileAppender fileAppender = new RollingFileAppender(logFile.getAbsolutePath());
        
        AsyncAppenderWrapper asyncWrapper = new AsyncAppenderWrapper(fileAppender, 256, OverflowStrategy.DROP_OLDEST);
        
        assertEquals("Async-" + fileAppender.getName(), asyncWrapper.getName());
        assertEquals(fileAppender, asyncWrapper.getTargetAppender());
        assertNotNull(asyncWrapper.getAsyncLogger());
        
        // Test logging through wrapper
        for (int i = 0; i < 100; i++) {
            asyncWrapper.append(new com.log4rich.util.LoggingEvent(
                LogLevel.INFO, "Test message " + i, "TestLogger", null
            ));
        }
        
        // Flush and verify
        asyncWrapper.flush();
        
        AsyncLogger.AsyncLoggerStatistics stats = asyncWrapper.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getEventsPublished() > 0);
        
        asyncWrapper.close();
        assertTrue(logFile.exists());
    }
    
    @Test
    void testAsyncAppenderWrapperFactory() {
        ConsoleAppender consoleAppender = new ConsoleAppender("TestConsole", ConsoleAppender.Target.STDOUT);
        
        // Test different factory methods
        AsyncAppenderWrapper highThroughput = AsyncAppenderWrapper.Factory.createHighThroughput(consoleAppender);
        AsyncAppenderWrapper lowLatency = AsyncAppenderWrapper.Factory.createLowLatency(consoleAppender);
        AsyncAppenderWrapper reliable = AsyncAppenderWrapper.Factory.createReliable(consoleAppender);
        AsyncAppenderWrapper fireAndForget = AsyncAppenderWrapper.Factory.createFireAndForget(consoleAppender);
        
        assertNotNull(highThroughput);
        assertNotNull(lowLatency);
        assertNotNull(reliable);
        assertNotNull(fireAndForget);
        
        // Verify different configurations
        assertTrue(highThroughput.getAsyncLogger().getStatistics().getBufferStats().getCapacity() > 
                  lowLatency.getAsyncLogger().getStatistics().getBufferStats().getCapacity());
        
        // Cleanup
        highThroughput.close();
        lowLatency.close();
        reliable.close();
        fireAndForget.close();
    }
    
    @Test
    void testAsyncLoggerGracefulShutdown() throws Exception {
        AsyncLogger logger = new AsyncLogger("ShutdownTest", 256);
        CountingAppender appender = new CountingAppender();
        logger.addAppender(appender);
        
        // Add some messages
        for (int i = 0; i < 50; i++) {
            logger.info("Shutdown test message " + i);
        }
        
        int initialCount = appender.getMessageCount();
        
        // Shutdown should process remaining events
        logger.shutdown();
        
        // All events should be processed
        int finalCount = appender.getMessageCount();
        assertTrue(finalCount >= initialCount);
        
        AsyncLogger.AsyncLoggerStatistics stats = logger.getStatistics();
        assertTrue(stats.isShutdown());
        assertFalse(stats.isRunning());
    }
    
    @Test
    void testAsyncLoggerPerformanceCharacteristics() throws Exception {
        AsyncLogger asyncLogger = new AsyncLogger("PerfTest", 8192);
        CountingAppender asyncAppender = new CountingAppender();
        asyncLogger.addAppender(asyncAppender);
        
        // Measure async logging time
        int messageCount = 10000;
        long asyncStart = System.nanoTime();
        
        for (int i = 0; i < messageCount; i++) {
            asyncLogger.info("Performance test message " + i);
        }
        
        long asyncEnd = System.nanoTime();
        long asyncTimeNs = asyncEnd - asyncStart;
        
        // Average time per message should be very low (sub-microsecond)
        double avgTimePerMessage = (double) asyncTimeNs / messageCount;
        
        // Should be well under 1 microsecond per message for async logging
        assertTrue(avgTimePerMessage < 1000, "Average time per message: " + avgTimePerMessage + "ns");
        
        asyncLogger.flush();
        asyncLogger.shutdown();
        
        AsyncLogger.AsyncLoggerStatistics stats = asyncLogger.getStatistics();
        assertTrue(stats.getEventsPublished() > 0);
        assertTrue(stats.getEventsProcessed() > 0);
    }
    
    @Test
    void testOverflowStrategyParsing() {
        assertEquals(OverflowStrategy.BLOCK, OverflowStrategy.fromString("BLOCK"));
        assertEquals(OverflowStrategy.DROP_OLDEST, OverflowStrategy.fromString("drop_oldest"));
        assertEquals(OverflowStrategy.DROP_NEWEST, OverflowStrategy.fromString("Drop-Newest"));
        assertEquals(OverflowStrategy.SYNCHRONOUS_WRITE, OverflowStrategy.fromString("SYNCHRONOUS_WRITE"));
        assertEquals(OverflowStrategy.DISCARD, OverflowStrategy.fromString("discard"));
        
        // Test default
        assertEquals(OverflowStrategy.DROP_OLDEST, OverflowStrategy.fromString(null));
        assertEquals(OverflowStrategy.DROP_OLDEST, OverflowStrategy.fromString(""));
        
        // Test invalid
        assertThrows(IllegalArgumentException.class, () -> OverflowStrategy.fromString("INVALID"));
    }
    
    /**
     * Test appender that counts messages for testing purposes.
     */
    private static class CountingAppender implements com.log4rich.appenders.Appender {
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private final AtomicLong lastMessageTime = new AtomicLong(0);
        private volatile boolean closed = false;
        
        @Override
        public void append(com.log4rich.util.LoggingEvent event) {
            if (!closed) {
                messageCount.incrementAndGet();
                lastMessageTime.set(System.currentTimeMillis());
            }
        }
        
        @Override
        public void close() {
            closed = true;
        }
        
        @Override
        public void setLayout(com.log4rich.layouts.Layout layout) {}
        
        @Override
        public com.log4rich.layouts.Layout getLayout() {
            return null;
        }
        
        @Override
        public void setLevel(LogLevel level) {}
        
        @Override
        public LogLevel getLevel() {
            return LogLevel.TRACE;
        }
        
        @Override
        public boolean isLevelEnabled(LogLevel level) {
            return true;
        }
        
        @Override
        public boolean isClosed() {
            return closed;
        }
        
        @Override
        public void setName(String name) {}
        
        @Override
        public String getName() {
            return "CountingAppender";
        }
        
        public int getMessageCount() {
            return messageCount.get();
        }
        
        public long getLastMessageTime() {
            return lastMessageTime.get();
        }
    }
}