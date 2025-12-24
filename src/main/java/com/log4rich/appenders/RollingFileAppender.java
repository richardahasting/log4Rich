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
import com.log4rich.util.AsyncCompressionManager;
import com.log4rich.util.CompressionManager;
import com.log4rich.util.LoggingEvent;
import com.log4rich.util.ThreadSafeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File appender that rolls over based on file size.
 * Supports compression of rolled files using external programs.
 * This appender is thread-safe and handles automatic file rolling when the current
 * log file reaches the configured maximum size.
 */
public class RollingFileAppender implements Appender {
    
    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_MAX_BACKUPS = 10;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    private final ReentrantLock lock = new ReentrantLock();
    
    private String name;
    private File file;
    private Layout layout;
    private LogLevel level;
    private long maxFileSize;
    private int maxBackups;
    private boolean compression;
    private CompressionManager compressionManager;
    private AsyncCompressionManager asyncCompressionManager;
    private boolean useAsyncCompression;
    private Charset encoding;
    private boolean immediateFlush;
    private int bufferSize;
    private String datePattern;
    private boolean closed;
    
    private ThreadSafeWriter writer;
    private final SimpleDateFormat dateFormat;
    
    /**
     * Creates a new RollingFileAppender with default settings.
     * Uses "logs/application.log" as the default file path.
     */
    public RollingFileAppender() {
        this(new File("logs/application.log"));
    }
    
    /**
     * Creates a new RollingFileAppender with the specified file path.
     * 
     * @param filePath the path to the log file
     */
    public RollingFileAppender(String filePath) {
        this(new File(filePath));
    }
    
    /**
     * Creates a new RollingFileAppender with the specified file.
     * Initializes with default settings: 10MB max size, 10 backup files,
     * compression enabled, UTF-8 encoding, and immediate flush.
     * 
     * @param file the log file to write to
     */
    public RollingFileAppender(File file) {
        this.file = file;
        this.name = "RollingFile";
        this.layout = new StandardLayout();
        this.level = LogLevel.TRACE;
        this.maxFileSize = DEFAULT_MAX_SIZE;
        this.maxBackups = DEFAULT_MAX_BACKUPS;
        this.compression = true;
        this.compressionManager = new CompressionManager();
        this.asyncCompressionManager = new AsyncCompressionManager();
        this.useAsyncCompression = true; // Default to async compression
        this.encoding = StandardCharsets.UTF_8;
        this.immediateFlush = true;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.datePattern = "yyyy-MM-dd-HH-mm-ss";
        this.closed = false;
        this.dateFormat = new SimpleDateFormat(datePattern);
    }
    
    /**
     * Appends a log event to the file, handling rollover if necessary.
     * This method is thread-safe and handles file initialization, rollover checks,
     * and error recovery.
     * 
     * @param event the logging event to append
     */
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
            
            // Initialize writer if needed
            if (writer == null) {
                writer = new ThreadSafeWriter(file, encoding, immediateFlush, bufferSize);
            }
            
            // Check if rollover is needed
            if (needsRollover()) {
                performRollover();
            }
            
            // Write the event
            String formattedMessage = layout.format(event);
            writer.write(formattedMessage);
            
        } catch (IOException e) {
            System.err.println("Error writing to file appender " + name + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Checks if a rollover is needed based on file size.
     * 
     * @return true if rollover is needed, false otherwise
     */
    private boolean needsRollover() {
        return writer != null && writer.getFileSize() >= maxFileSize;
    }
    
    /**
     * Performs the rollover operation by closing the current file,
     * renaming it with a timestamp, optionally compressing it,
     * and creating a new file for writing.
     * 
     * @throws IOException if rollover fails
     */
    private void performRollover() throws IOException {
        // Close current writer
        if (writer != null) {
            writer.close();
            writer = null;
        }
        
        // Generate backup filename with timestamp
        String timestamp = dateFormat.format(new Date());
        String baseName = file.getName();
        String backupName = baseName + "." + timestamp;
        File backupFile = new File(file.getParentFile(), backupName);
        
        // Rename current file to backup
        if (file.exists()) {
            if (!file.renameTo(backupFile)) {
                throw new IOException("Failed to rename " + file.getName() + " to " + backupFile.getName());
            }
            
            // Compress the backup file if enabled
            if (compression) {
                if (useAsyncCompression && asyncCompressionManager != null) {
                    // Use adaptive async compression
                    AsyncCompressionManager.AdaptiveCompressionResult result = 
                        asyncCompressionManager.compressWithAdaptiveManagement(
                            backupFile, maxFileSize, getName());
                    
                    // Handle adaptive file size increase if needed
                    if (result.wasSizeIncreased()) {
                        long oldSize = maxFileSize;
                        maxFileSize = result.getNewMaxSize();
                        
                        // Log the adaptive change to the log file itself
                        String adaptiveMsg = String.format(
                            "\n*** ADAPTIVE FILE SIZE INCREASE ***\n" +
                            "APPENDER: %s\n" +
                            "OLD MAX SIZE: %s\n" +
                            "NEW MAX SIZE: %s (DOUBLED DUE TO COMPRESSION OVERLOAD)\n" +
                            "TIMESTAMP: %s\n" +
                            "*** END ADAPTIVE CHANGE ***\n",
                            getName(),
                            formatFileSize(oldSize),
                            formatFileSize(maxFileSize),
                            new Date()
                        );
                        
                        try {
                            // Write adaptive message to new log file
                            if (writer == null) {
                                writer = new ThreadSafeWriter(file, encoding, immediateFlush, bufferSize);
                            }
                            writer.write(adaptiveMsg);
                        } catch (IOException e) {
                            System.err.println("Failed to write adaptive message to log: " + e.getMessage());
                        }
                    }
                    
                    // Clean up uncompressed file if compression succeeded
                    File compressedFile = result.getCompressedFile();
                    if (compressedFile != backupFile && compressedFile.exists()) {
                        if (!backupFile.delete()) {
                            System.err.println("Warning: Failed to delete uncompressed backup file: " + 
                                             backupFile.getName());
                        }
                    }
                } else if (compressionManager != null) {
                    // Use traditional blocking compression
                    try {
                        File compressedFile = compressionManager.compressFile(backupFile);
                        // If compression succeeded and created a new file, delete the original
                        if (compressedFile != backupFile && compressedFile.exists()) {
                            if (!backupFile.delete()) {
                                System.err.println("Warning: Failed to delete uncompressed backup file: " + 
                                                 backupFile.getName());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Compression failed for " + backupFile.getName() + 
                                         ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Clean up old backup files
        cleanupOldBackups();
        
        // Create new writer for the main file
        writer = new ThreadSafeWriter(file, encoding, immediateFlush, bufferSize);
    }
    
    /**
     * Cleans up old backup files, keeping only the most recent maxBackups files.
     * Files are sorted by last modified time and the oldest ones are deleted.
     */
    private void cleanupOldBackups() {
        File parentDir = file.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            return;
        }
        
        String baseName = file.getName();
        File[] backupFiles = parentDir.listFiles((dir, name) -> {
            return name.startsWith(baseName + ".") && 
                   (name.endsWith(".gz") || name.endsWith(".bz2") || name.endsWith(".xz") || 
                    name.endsWith(".zip") || name.endsWith(".7z") || name.endsWith(".compressed") ||
                    name.matches(baseName + "\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}"));
        });
        
        if (backupFiles != null && backupFiles.length > maxBackups) {
            // Sort by last modified time (oldest first)
            java.util.Arrays.sort(backupFiles, (a, b) -> 
                Long.compare(a.lastModified(), b.lastModified()));
            
            // Delete oldest files
            int filesToDelete = backupFiles.length - maxBackups;
            for (int i = 0; i < filesToDelete; i++) {
                if (!backupFiles[i].delete()) {
                    System.err.println("Warning: Failed to delete old backup file: " + 
                                     backupFiles[i].getName());
                }
            }
        }
    }
    
    /**
     * Closes this appender and releases all resources.
     * Flushes any remaining data and closes the file writer.
     */
    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            
            closed = true;
            
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing file appender " + name + ": " + e.getMessage());
                }
                writer = null;
            }
            
            // Shutdown async compression manager if used
            if (asyncCompressionManager != null) {
                asyncCompressionManager.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }
    
    // Configuration methods
    /**
     * Sets the file path for this appender.
     * 
     * @param filePath the path to the log file
     */
    public void setFile(String filePath) {
        setFile(new File(filePath));
    }
    
    /**
     * Sets the file for this appender.
     * If a writer is currently open, it will be closed and a new one created.
     * 
     * @param file the log file to write to
     */
    public void setFile(File file) {
        lock.lock();
        try {
            this.file = file;
            // Close existing writer to force re-initialization
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing existing writer: " + e.getMessage());
                }
                writer = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Sets the maximum file size in bytes before rollover occurs.
     * 
     * @param maxFileSize the maximum file size in bytes
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_SIZE;
    }
    
    /**
     * Sets the maximum file size using a string format (e.g., "10M", "100K", "1G").
     * 
     * @param maxFileSize the maximum file size as a string
     */
    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = parseSize(maxFileSize);
    }
    
    /**
     * Sets the maximum number of backup files to keep.
     * 
     * @param maxBackups the maximum number of backup files
     */
    public void setMaxBackups(int maxBackups) {
        this.maxBackups = maxBackups > 0 ? maxBackups : DEFAULT_MAX_BACKUPS;
    }
    
    /**
     * Sets whether backup files should be compressed.
     * 
     * @param compression true to enable compression, false to disable
     */
    public void setCompression(boolean compression) {
        this.compression = compression;
    }
    
    /**
     * Sets the compression manager to use for compressing backup files.
     * 
     * @param compressionManager the compression manager to use
     */
    public void setCompressionManager(CompressionManager compressionManager) {
        this.compressionManager = compressionManager;
    }
    
    /**
     * Sets the character encoding for the log file.
     * 
     * @param encoding the character encoding to use
     */
    public void setEncoding(Charset encoding) {
        this.encoding = encoding != null ? encoding : StandardCharsets.UTF_8;
    }
    
    /**
     * Sets whether to flush the output stream immediately after each write.
     * 
     * @param immediateFlush true to enable immediate flushing, false otherwise
     */
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
    
    /**
     * Sets the buffer size for the file writer.
     * 
     * @param bufferSize the buffer size in bytes
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE;
    }
    
    /**
     * Sets the date pattern used for backup file naming.
     * 
     * @param datePattern the date pattern string (e.g., "yyyy-MM-dd-HH-mm-ss")
     */
    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern != null ? datePattern : "yyyy-MM-dd-HH-mm-ss";
    }
    
    /**
     * Parses a size string like "10M", "100K", "1G" into bytes.
     * 
     * @param sizeStr the size string to parse
     * @return the size in bytes
     */
    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return DEFAULT_MAX_SIZE;
        }
        
        String str = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        
        if (str.endsWith("K")) {
            multiplier = 1024;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("M")) {
            multiplier = 1024 * 1024;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
            str = str.substring(0, str.length() - 1);
        }
        
        try {
            return Long.parseLong(str) * multiplier;
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid size format '" + sizeStr + "', using default");
            return DEFAULT_MAX_SIZE;
        }
    }
    
    // Appender interface implementation
    /**
     * Sets the layout for formatting log events.
     * 
     * @param layout the layout to use, or null for default StandardLayout
     */
    @Override
    public void setLayout(Layout layout) {
        this.layout = layout != null ? layout : new StandardLayout();
    }
    
    /**
     * Gets the current layout used for formatting log events.
     * 
     * @return the current layout
     */
    @Override
    public Layout getLayout() {
        return layout;
    }
    
    /**
     * Sets the minimum log level for this appender.
     * 
     * @param level the minimum log level, or null to use TRACE
     */
    @Override
    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.TRACE;
    }
    
    /**
     * Gets the minimum log level for this appender.
     * 
     * @return the minimum log level
     */
    @Override
    public LogLevel getLevel() {
        return level;
    }
    
    /**
     * Checks if the specified log level is enabled for this appender.
     * 
     * @param level the log level to check
     * @return true if the level is enabled, false otherwise
     */
    @Override
    public boolean isLevelEnabled(LogLevel level) {
        return level != null && level.isGreaterOrEqual(this.level);
    }
    
    /**
     * Checks if this appender is closed.
     * 
     * @return true if the appender is closed, false otherwise
     */
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * Sets the name of this appender.
     * 
     * @param name the name to set, or null for default "RollingFile"
     */
    @Override
    public void setName(String name) {
        this.name = name != null ? name : "RollingFile";
    }
    
    /**
     * Gets the name of this appender.
     * 
     * @return the appender name
     */
    @Override
    public String getName() {
        return name;
    }
    
    // Getters
    /**
     * Gets the current log file.
     * 
     * @return the log file
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Gets the maximum file size before rollover.
     * 
     * @return the maximum file size in bytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * Gets the maximum number of backup files to keep.
     * 
     * @return the maximum number of backup files
     */
    public int getMaxBackups() {
        return maxBackups;
    }
    
    /**
     * Checks if compression is enabled for backup files.
     * 
     * @return true if compression is enabled, false otherwise
     */
    public boolean isCompression() {
        return compression;
    }
    
    /**
     * Gets the compression manager used for backup files.
     * 
     * @return the compression manager
     */
    public CompressionManager getCompressionManager() {
        return compressionManager;
    }
    
    /**
     * Gets the async compression manager used for backup files.
     * 
     * @return the async compression manager
     */
    public AsyncCompressionManager getAsyncCompressionManager() {
        return asyncCompressionManager;
    }
    
    /**
     * Sets the async compression manager for backup files.
     * 
     * @param asyncCompressionManager the async compression manager to use
     */
    public void setAsyncCompressionManager(AsyncCompressionManager asyncCompressionManager) {
        this.asyncCompressionManager = asyncCompressionManager;
    }
    
    /**
     * Checks if async compression is enabled.
     * 
     * @return true if async compression is enabled, false for blocking compression
     */
    public boolean isUseAsyncCompression() {
        return useAsyncCompression;
    }
    
    /**
     * Sets whether to use async compression or blocking compression.
     * 
     * @param useAsyncCompression true to use async compression, false for blocking
     */
    public void setUseAsyncCompression(boolean useAsyncCompression) {
        this.useAsyncCompression = useAsyncCompression;
    }
    
    /**
     * Gets compression statistics if async compression is enabled.
     * 
     * @return compression statistics or null if async compression is not used
     */
    public AsyncCompressionManager.CompressionStatistics getCompressionStatistics() {
        return asyncCompressionManager != null ? asyncCompressionManager.getStatistics() : null;
    }
    
    /**
     * Formats file size for human readable output.
     * 
     * @param bytes the size in bytes
     * @return formatted size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}