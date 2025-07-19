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

import com.log4rich.util.CompressionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test compression manager that introduces configurable delays to simulate slow compression.
 * This allows testing of adaptive compression management under stress conditions.
 * 
 * @author log4Rich Contributors
 * @since 1.1.0
 */
public class SlowCompressionManager extends CompressionManager {
    
    private final long delayMillis;
    private final AtomicInteger compressionCount = new AtomicInteger(0);
    
    /**
     * Creates a slow compression manager with specified delay.
     * 
     * @param delayMillis delay in milliseconds to introduce before compression
     */
    public SlowCompressionManager(long delayMillis) {
        super("echo", "dummy", 60000); // Use echo command to avoid gzip dependency
        this.delayMillis = delayMillis;
    }
    
    @Override
    public File compressFile(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            return sourceFile;
        }
        
        int count = compressionCount.incrementAndGet();
        System.out.println("[SlowCompressionManager] Starting compression #" + count + 
                          " of " + sourceFile.getName() + " with " + delayMillis + "ms delay");
        
        try {
            // Introduce the test delay
            Thread.sleep(delayMillis);
            
            // Simulate compression by creating a "compressed" file
            File compressedFile = new File(sourceFile.getParentFile(), 
                                         sourceFile.getName() + ".test-compressed");
            
            // Copy content to simulate compression (in real scenario this would be much smaller)
            byte[] originalContent = Files.readAllBytes(sourceFile.toPath());
            String compressedContent = "COMPRESSED: " + new String(originalContent);
            
            try (FileOutputStream fos = new FileOutputStream(compressedFile)) {
                fos.write(compressedContent.getBytes());
            }
            
            System.out.println("[SlowCompressionManager] Completed compression #" + count + 
                              " of " + sourceFile.getName() + " -> " + compressedFile.getName());
            
            return compressedFile;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[SlowCompressionManager] Compression interrupted for " + sourceFile.getName());
            return sourceFile;
        } catch (IOException e) {
            System.err.println("[SlowCompressionManager] Compression failed for " + sourceFile.getName() + 
                              ": " + e.getMessage());
            return sourceFile;
        }
    }
    
    @Override
    public boolean isProgramAvailable() {
        return true; // Always available for testing
    }
    
    /**
     * Gets the number of compressions performed.
     * 
     * @return compression count
     */
    public int getCompressionCount() {
        return compressionCount.get();
    }
    
    /**
     * Resets the compression count.
     */
    public void resetCount() {
        compressionCount.set(0);
    }
}