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

import com.log4rich.appenders.RollingFileAppender;
import com.log4rich.core.LogLevel;
import com.log4rich.core.Logger;
import com.log4rich.layouts.StandardLayout;
import com.log4rich.util.CompressionManager;

import java.io.File;

/**
 * Demonstration of the RollingFileAppender with compression.
 * This class shows how to configure and use the rolling file appender.
 */
public class RollingFileDemo {
    
    public static void main(String[] args) {
        // Create a logger
        Logger logger = new Logger("RollingFileDemo");
        
        // Configure rolling file appender
        RollingFileAppender fileAppender = new RollingFileAppender("logs/demo.log");
        fileAppender.setName("DemoFileAppender");
        fileAppender.setLayout(new StandardLayout("[%level] %date{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %class.%method:%line - %message%n"));
        fileAppender.setMaxFileSize("1K"); // Very small for demonstration
        fileAppender.setMaxBackups(5);
        fileAppender.setCompression(true);
        
        // Configure compression (if gzip is available)
        CompressionManager compressionManager = new CompressionManager("gzip", "-9", 30000);
        fileAppender.setCompressionManager(compressionManager);
        
        // Add appender to logger
        logger.addAppender(fileAppender);
        logger.setLevel(LogLevel.DEBUG);
        
        System.out.println("Starting rolling file appender demonstration...");
        System.out.println("Log files will be created in: " + new File("logs").getAbsolutePath());
        System.out.println("Max file size: 1KB, Max backups: 5, Compression: enabled");
        System.out.println();
        
        // Generate lots of log messages to trigger rollover
        for (int i = 1; i <= 50; i++) {
            logger.info("This is log message number " + i + " - " + 
                       "Adding some extra text to make the messages longer and trigger rollover sooner. " +
                       "Each message contains useful information for debugging purposes.");
            
            if (i % 10 == 0) {
                logger.warn("Checkpoint reached: " + i + " messages written");
                
                // Check what files exist
                File logsDir = new File("logs");
                if (logsDir.exists()) {
                    File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("demo.log"));
                    if (logFiles != null) {
                        System.out.println("Current log files: " + logFiles.length);
                        for (File file : logFiles) {
                            System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                        }
                    }
                }
                System.out.println();
            }
            
            // Add some variety
            if (i % 7 == 0) {
                logger.debug("Debug message " + i);
            }
            if (i % 13 == 0) {
                logger.error("Error message " + i, new RuntimeException("Demo exception " + i));
            }
        }
        
        logger.info("Demonstration complete. Check the logs directory for rolled files.");
        
        // Clean up
        fileAppender.close();
        
        // Show final file listing
        System.out.println("\nFinal file listing:");
        File logsDir = new File("logs");
        if (logsDir.exists()) {
            File[] allFiles = logsDir.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }
    }
}