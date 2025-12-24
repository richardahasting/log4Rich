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
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.Java8Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for performance enhancement appenders.
 */
class PerformanceAppendersTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testBatchingFileAppenderBasicFunctionality() throws Exception {
        Path logFile = tempDir.resolve("batch.log");
        
        BatchingFileAppender appender = new BatchingFileAppender(
            "TestBatch", 
            logFile.toString(),
            10,  // batch size
            100  // batch time ms
        );
        
        // Write some events
        for (int i = 0; i < 5; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                "Test message " + i,
                "TestLogger",
                null
            );
            appender.append(event);
        }
        
        // Force flush to write the batch
        appender.forceFlush();
        
        // Verify the file was created and has content
        assertTrue(Files.exists(logFile));
        String content = Java8Utils.readString(logFile);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("Test message 0"));
        assertTrue(content.contains("Test message 4"));
        
        // Check statistics
        assertEquals(1, appender.getBatchesWritten());
        assertEquals(5, appender.getTotalEventsWritten());
        
        appender.close();
    }
    
    @Test
    void testMemoryMappedFileAppenderBasicFunctionality() throws Exception {
        Path logFile = tempDir.resolve("mmap.log");
        
        MemoryMappedFileAppender appender = new MemoryMappedFileAppender(
            "TestMMap", 
            logFile.toString(),
            1024 * 1024,  // 1MB mapping
            true,         // force on write
            0             // no periodic force
        );
        
        // Write some events
        for (int i = 0; i < 5; i++) {
            LoggingEvent event = new LoggingEvent(
                LogLevel.INFO,
                "Memory mapped message " + i,
                "TestLogger",
                null
            );
            appender.append(event);
        }
        
        // Close to ensure all data is written
        appender.close();
        
        // Verify the file was created and has content
        assertTrue(Files.exists(logFile));
        String content = Java8Utils.readString(logFile);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("Memory mapped message 0"));
        assertTrue(content.contains("Memory mapped message 4"));
        
        // Check statistics
        assertTrue(appender.getTotalBytesWritten() > 0);
        assertEquals(1, appender.getMappingCount());
        assertEquals(5, appender.getForceCount()); // One force per write since forceOnWrite=true
    }
    
    @Test
    void testAppenderInterfaceImplementation() {
        // Test that both appenders properly implement the Appender interface
        BatchingFileAppender batchAppender = new BatchingFileAppender(
            "Batch", tempDir.resolve("batch2.log").toString()
        );
        
        MemoryMappedFileAppender mmapAppender = new MemoryMappedFileAppender(
            "MMap", tempDir.resolve("mmap2.log").toString()
        );
        
        // Verify they implement Appender
        assertTrue(batchAppender instanceof Appender);
        assertTrue(mmapAppender instanceof Appender);
        
        // Test interface methods
        assertEquals("Batch", batchAppender.getName());
        assertEquals("MMap", mmapAppender.getName());
        
        assertEquals(LogLevel.TRACE, batchAppender.getLevel());
        assertEquals(LogLevel.TRACE, mmapAppender.getLevel());
        
        batchAppender.setLevel(LogLevel.WARN);
        mmapAppender.setLevel(LogLevel.ERROR);
        
        assertEquals(LogLevel.WARN, batchAppender.getLevel());
        assertEquals(LogLevel.ERROR, mmapAppender.getLevel());
        
        assertFalse(batchAppender.isClosed());
        assertFalse(mmapAppender.isClosed());
        
        batchAppender.close();
        mmapAppender.close();
        
        assertTrue(batchAppender.isClosed());
        assertTrue(mmapAppender.isClosed());
    }
}