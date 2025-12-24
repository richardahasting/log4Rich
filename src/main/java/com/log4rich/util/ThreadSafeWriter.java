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

package com.log4rich.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe file writer with buffering support.
 * Provides synchronized access to file writing operations with automatic
 * initialization, parent directory creation, and resource management.
 */
public class ThreadSafeWriter {
    
    private final ReentrantLock lock;
    private final File file;
    private final Charset charset;
    private final boolean immediateFlush;
    private final int bufferSize;
    
    private BufferedWriter writer;
    private boolean closed;
    
    /**
     * Creates a new ThreadSafeWriter with default settings.
     * Uses UTF-8 encoding, immediate flush enabled, and 8KB buffer size.
     * 
     * @param file the file to write to
     */
    public ThreadSafeWriter(File file) {
        this(file, StandardCharsets.UTF_8, true, 8192);
    }
    
    /**
     * Creates a new ThreadSafeWriter with the specified settings.
     * 
     * @param file the file to write to
     * @param charset the character encoding to use
     * @param immediateFlush whether to flush after each write
     * @param bufferSize the buffer size in bytes
     */
    public ThreadSafeWriter(File file, Charset charset, boolean immediateFlush, int bufferSize) {
        this.file = file;
        this.charset = charset;
        this.immediateFlush = immediateFlush;
        this.bufferSize = bufferSize;
        this.lock = new ReentrantLock();
        this.closed = false;
    }
    
    /**
     * Initializes the writer. Creates parent directories if needed.
     * This method is called automatically when the first write occurs.
     * 
     * @throws IOException if initialization fails
     */
    public void initialize() throws IOException {
        lock.lock();
        try {
            if (closed) {
                throw new IOException("Writer is closed");
            }
            
            // Create parent directories if they don't exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directories: " + parentDir);
                }
            }
            
            // Create buffered writer
            FileOutputStream fos = new FileOutputStream(file, true); // Append mode
            OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
            writer = new BufferedWriter(osw, bufferSize);
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Writes a string to the file.
     * The writer is automatically initialized if not already done.
     * 
     * @param text the text to write
     * @throws IOException if writing fails
     */
    public void write(String text) throws IOException {
        lock.lock();
        try {
            if (closed) {
                throw new IOException("Writer is closed");
            }
            
            if (writer == null) {
                initialize();
            }
            
            writer.write(text);
            
            if (immediateFlush) {
                writer.flush();
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Flushes any buffered data to the file.
     * This method is safe to call even if the writer is closed.
     * 
     * @throws IOException if flushing fails
     */
    public void flush() throws IOException {
        lock.lock();
        try {
            if (writer != null && !closed) {
                writer.flush();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Closes the writer and releases resources.
     * This method is safe to call multiple times.
     * 
     * @throws IOException if closing fails
     */
    public void close() throws IOException {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            
            closed = true;
            
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Checks if the writer is closed.
     * 
     * @return true if the writer is closed, false otherwise
     */
    public boolean isClosed() {
        lock.lock();
        try {
            return closed;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets the file this writer is writing to.
     * 
     * @return the file being written to
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Gets the current file size.
     * 
     * @return the file size in bytes, or 0 if the file doesn't exist
     */
    public long getFileSize() {
        return file.exists() ? file.length() : 0;
    }
}