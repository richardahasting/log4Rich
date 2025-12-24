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

package com.log4rich.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory and manager for Logger instances.
 * Maintains a registry of loggers and provides thread-safe access.
 */
public class LogManager {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private LogManager() {
        // Utility class - no instantiation
    }
    
    private static final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static volatile boolean shutdownHookAdded = false;
    
    /**
     * Get a logger for the specified name.
     * @param name The logger name
     * @return Logger instance
     */
    public static Logger getLogger(String name) {
        if (name == null) {
            name = "root";
        }
        
        // Use computeIfAbsent for thread-safe logger creation
        Logger logger = loggers.computeIfAbsent(name, Logger::new);
        
        // Add shutdown hook if not already added
        addShutdownHookIfNeeded();
        
        return logger;
    }
    
    /**
     * Get a logger for the specified class.
     * @param clazz The class to get logger for
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            return getLogger("root");
        }
        return getLogger(clazz.getName());
    }
    
    /**
     * Get the root logger.
     * @return Root logger instance
     */
    public static Logger getRootLogger() {
        return getLogger("root");
    }
    
    /**
     * Check if a logger exists with the given name.
     * @param name The logger name
     * @return true if logger exists
     */
    public static boolean exists(String name) {
        return loggers.containsKey(name);
    }
    
    /**
     * Get all existing logger names.
     * @return Array of logger names
     */
    public static String[] getLoggerNames() {
        return loggers.keySet().toArray(new String[0]);
    }
    
    /**
     * Shutdown all loggers and their appenders.
     */
    public static void shutdown() {
        for (Logger logger : loggers.values()) {
            try {
                logger.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down logger " + logger.getName() + ": " + e.getMessage());
            }
        }
        loggers.clear();
    }
    
    /**
     * Reset the logging system by removing all loggers.
     */
    public static void reset() {
        shutdown();
    }
    
    /**
     * Add shutdown hook to ensure proper cleanup.
     */
    private static void addShutdownHookIfNeeded() {
        if (!shutdownHookAdded) {
            synchronized (LogManager.class) {
                if (!shutdownHookAdded) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            shutdown();
                        } catch (Exception e) {
                            System.err.println("Error during log4Rich shutdown: " + e.getMessage());
                        }
                    }, "log4Rich-shutdown"));
                    shutdownHookAdded = true;
                }
            }
        }
    }
}