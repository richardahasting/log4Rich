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

package com.log4rich.slf4j;

import com.log4rich.core.LogManager;
import com.log4rich.core.Logger;
import org.slf4j.ILoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SLF4J ILoggerFactory implementation for log4Rich.
 *
 * <p>This factory creates and caches SLF4J Logger adapters that wrap
 * log4Rich loggers. It maintains a single adapter per logger name
 * to ensure consistency across the application.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class Log4RichLoggerFactory implements ILoggerFactory {

    /**
     * Cache of logger adapters by name.
     */
    private final ConcurrentMap<String, Log4RichLoggerAdapter> loggerMap = new ConcurrentHashMap<>();

    /**
     * Gets or creates an SLF4J Logger for the specified name.
     *
     * @param name the logger name
     * @return an SLF4J Logger that delegates to log4Rich
     */
    @Override
    public org.slf4j.Logger getLogger(String name) {
        Log4RichLoggerAdapter adapter = loggerMap.get(name);

        if (adapter != null) {
            return adapter;
        }

        // Get or create the underlying log4Rich logger
        Logger log4RichLogger = LogManager.getLogger(name);

        // Create adapter
        adapter = new Log4RichLoggerAdapter(log4RichLogger);

        // Use putIfAbsent for thread-safety
        Log4RichLoggerAdapter existingAdapter = loggerMap.putIfAbsent(name, adapter);
        if (existingAdapter != null) {
            return existingAdapter;
        }

        return adapter;
    }

    /**
     * Clears the logger cache.
     * Useful for testing or reinitialization.
     */
    public void clear() {
        loggerMap.clear();
    }

    /**
     * Gets the number of cached loggers.
     *
     * @return the cache size
     */
    public int size() {
        return loggerMap.size();
    }
}
