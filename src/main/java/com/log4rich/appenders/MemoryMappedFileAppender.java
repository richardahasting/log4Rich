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
import com.log4rich.util.LoggingEvent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance file appender using memory-mapped files for minimal system call overhead.
 * 
 * This appender leverages Java NIO's memory mapping capabilities to achieve kernel-level
 * performance by eliminating user-space to kernel-space data copying and reducing system calls.
 * 
 * Key performance features:
 * - Memory-mapped I/O eliminates buffer copying
 * - Strategic mapping region sizing reduces remapping overhead  
 * - Configurable force() intervals for durability vs performance trade-offs
 * - Automatic region expansion when capacity is exceeded
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class MemoryMappedFileAppender implements Appender {
    
    // Default mapping size - 64MB provides good balance of memory usage and remapping frequency
    private static final long DEFAULT_MAPPED_SIZE = 64L * 1024 * 1024;
    
    // Minimum mapping size to prevent excessive remapping on small writes
    private static final long MIN_MAPPED_SIZE = 1024 * 1024; // 1MB
    
    // Maximum mapping size to avoid excessive virtual memory usage
    private static final long MAX_MAPPED_SIZE = 512L * 1024 * 1024; // 512MB
    
    // Appender fields
    private final ReentrantLock lock = new ReentrantLock();
    private String name;
    private File file;
    private Layout layout;
    private LogLevel level;
    private boolean closed;
    
    private RandomAccessFile randomAccessFile;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedBuffer;
    
    // Current mapping parameters
    private long mappedRegionStart;
    private long mappedRegionSize;
    private long currentFilePosition;
    
    // Configuration
    private long initialMappedSize;
    private boolean forceOnWrite;
    private long forceInterval; // milliseconds
    private long lastForceTime;
    
    // Thread safety for mapping operations
    private final ReentrantReadWriteLock mappingLock = new ReentrantReadWriteLock();
    
    // Performance monitoring
    private long totalBytesWritten;
    private long mappingCount;
    private long forceCount;
    
    /**
     * Creates a memory-mapped file appender with default settings.
     * 
     * @param name the appender name
     * @param filePath the file path to write to
     */
    public MemoryMappedFileAppender(String name, String filePath) {
        this(name, filePath, DEFAULT_MAPPED_SIZE, false, 1000);
    }
    
    /**
     * Creates a memory-mapped file appender with custom settings.
     * 
     * @param name the appender name
     * @param filePath the file path to write to
     * @param mappedSize initial size of memory mapped region
     * @param forceOnWrite whether to force writes to disk immediately
     * @param forceInterval interval between automatic force operations (ms)
     */
    public MemoryMappedFileAppender(String name, String filePath, long mappedSize, 
                                   boolean forceOnWrite, long forceInterval) {
        this.name = name;
        this.file = new File(filePath);
        this.layout = new StandardLayout();
        this.level = LogLevel.TRACE;
        this.closed = false;
        this.initialMappedSize = Math.max(MIN_MAPPED_SIZE, Math.min(MAX_MAPPED_SIZE, mappedSize));
        this.forceOnWrite = forceOnWrite;
        this.forceInterval = forceInterval;
        this.lastForceTime = System.currentTimeMillis();
    }
    
    @Override
    public void append(LoggingEvent event) {
        if (closed || !isLevelEnabled(event.getLevel())) {
            return;
        }
        
        lock.lock();
        try {
            if (closed) {
                return;
            }
            
            // Initialize file if needed
            if (randomAccessFile == null) {
                initializeFile();
            }
            
            // Format and write the event
            String formattedMessage = layout.format(event);
            writeToFile(formattedMessage);
            
        } catch (IOException e) {
            System.err.println("Error writing to memory-mapped file appender " + name + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
    
    private void initializeFile() throws IOException {
        // Create parent directories if needed
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        randomAccessFile = new RandomAccessFile(file, "rw");
        fileChannel = randomAccessFile.getChannel();
        
        // Get current file size and position
        currentFilePosition = fileChannel.size();
        
        // Create initial memory mapping
        createMapping(currentFilePosition, initialMappedSize);
        
        System.out.println("MemoryMappedFileAppender initialized: " + 
                          "file=" + file.getPath() + 
                          ", mappedSize=" + mappedRegionSize + 
                          ", position=" + currentFilePosition);
    }
    
    /**
     * Creates a new memory mapping for the specified file region.
     * 
     * @param startPosition starting position in file
     * @param size size of region to map
     * @throws IOException if mapping fails
     */
    private void createMapping(long startPosition, long size) throws IOException {
        mappingLock.writeLock().lock();
        try {
            // Unmap previous buffer if it exists
            if (mappedBuffer != null) {
                // Force any pending writes before unmapping
                mappedBuffer.force();
                // Note: Direct buffer cleanup is JVM implementation dependent
                // In practice, the GC will eventually clean this up
            }
            
            // Ensure file is large enough for mapping
            long requiredFileSize = startPosition + size;
            if (fileChannel.size() < requiredFileSize) {
                fileChannel.truncate(requiredFileSize);
            }
            
            // Create new mapping
            mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startPosition, size);
            
            // Position buffer at current write position within the mapping
            long bufferPosition = currentFilePosition - startPosition;
            mappedBuffer.position((int) bufferPosition);
            
            // Update mapping metadata
            mappedRegionStart = startPosition;
            mappedRegionSize = size;
            mappingCount++;
            
        } finally {
            mappingLock.writeLock().unlock();
        }
    }
    
    private void writeToFile(String content) throws IOException {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        
        mappingLock.readLock().lock();
        try {
            // Check if we need to expand the mapping
            if (mappedBuffer.remaining() < bytes.length) {
                // Release read lock and acquire write lock for remapping
                mappingLock.readLock().unlock();
                expandMapping(bytes.length);
                mappingLock.readLock().lock();
            }
            
            // Write to memory-mapped buffer
            mappedBuffer.put(bytes);
            currentFilePosition += bytes.length;
            totalBytesWritten += bytes.length;
            
            // Handle forcing to disk
            if (forceOnWrite) {
                mappedBuffer.force();
                forceCount++;
            } else if (shouldPeriodicForce()) {
                mappedBuffer.force();
                forceCount++;
                lastForceTime = System.currentTimeMillis();
            }
            
        } finally {
            mappingLock.readLock().unlock();
        }
    }
    
    /**
     * Expands the memory mapping to accommodate additional data.
     * 
     * @param additionalBytes minimum additional bytes needed
     * @throws IOException if remapping fails
     */
    private void expandMapping(int additionalBytes) throws IOException {
        mappingLock.writeLock().lock();
        try {
            // Calculate new mapping size - grow by at least 50% to reduce remapping frequency
            long currentUsed = currentFilePosition - mappedRegionStart;
            long newSize = Math.max(
                mappedRegionSize * 3 / 2,  // 50% growth
                currentUsed + additionalBytes + 1024  // Minimum needed plus buffer
            );
            
            // Cap at maximum size
            newSize = Math.min(newSize, MAX_MAPPED_SIZE);
            
            System.out.println("Expanding memory mapping: oldSize=" + mappedRegionSize + 
                              ", newSize=" + newSize + 
                              ", position=" + currentFilePosition);
            
            createMapping(mappedRegionStart, newSize);
            
        } finally {
            mappingLock.writeLock().unlock();
        }
    }
    
    /**
     * Determines if a periodic force operation should be performed.
     */
    private boolean shouldPeriodicForce() {
        return forceInterval > 0 && 
               (System.currentTimeMillis() - lastForceTime) >= forceInterval;
    }
    
    public long getCurrentFileSize() {
        return currentFilePosition;
    }
    
    @Override
    public void close() {
        mappingLock.writeLock().lock();
        try {
            if (closed) {
                return;
            }
            
            closed = true;
            
            if (mappedBuffer != null) {
                // Force any remaining data to disk
                mappedBuffer.force();
                
                // Clear the buffer reference
                mappedBuffer = null;
            }
            
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    System.err.println("Error closing file channel: " + e.getMessage());
                }
            }
            
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    System.err.println("Error closing random access file: " + e.getMessage());
                }
            }
            
            System.out.println("MemoryMappedFileAppender closed: " + 
                              "totalBytes=" + totalBytesWritten + 
                              ", mappings=" + mappingCount + 
                              ", forces=" + forceCount);
            
        } finally {
            mappingLock.writeLock().unlock();
        }
    }
    
    /**
     * Forces any cached data to be written to storage.
     * This is useful for ensuring durability at specific points.
     */
    public void force() {
        mappingLock.readLock().lock();
        try {
            if (mappedBuffer != null) {
                mappedBuffer.force();
                forceCount++;
            }
        } finally {
            mappingLock.readLock().unlock();
        }
    }
    
    // Getters for monitoring and configuration
    
    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }
    
    public long getMappingCount() {
        return mappingCount;
    }
    
    public long getForceCount() {
        return forceCount;
    }
    
    public long getMappedRegionSize() {
        return mappedRegionSize;
    }
    
    public boolean isForceOnWrite() {
        return forceOnWrite;
    }
    
    public void setForceOnWrite(boolean forceOnWrite) {
        this.forceOnWrite = forceOnWrite;
    }
    
    public long getForceInterval() {
        return forceInterval;
    }
    
    public void setForceInterval(long forceInterval) {
        this.forceInterval = forceInterval;
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
        this.name = name != null ? name : "MemoryMappedFile";
    }
    
    @Override
    public String getName() {
        return name;
    }
}