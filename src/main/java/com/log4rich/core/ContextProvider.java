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

import java.util.List;
import java.util.Map;

/**
 * Interface for providing context information to loggers.
 * This allows external systems (like log4j2 bridges) to inject
 * context data such as MDC and NDC information.
 */
public interface ContextProvider {
    
    /**
     * Gets the current thread's Mapped Diagnostic Context (MDC) data.
     * 
     * @return a map of MDC key-value pairs, never null
     */
    Map<String, String> getMDC();
    
    /**
     * Gets the current thread's Nested Diagnostic Context (NDC) stack.
     * 
     * @return a list of NDC entries, never null
     */
    List<String> getNDC();
    
    /**
     * Checks if this provider has any context data.
     * This allows for optimization - if no context is available,
     * loggers can skip context processing.
     * 
     * @return true if context data is available, false otherwise
     */
    boolean hasContext();
}

/**
 * Default empty context provider that returns no context data.
 * Used when no context provider is configured.
 */
class EmptyContextProvider implements ContextProvider {
    
    public static final ContextProvider INSTANCE = new EmptyContextProvider();
    
    private static final Map<String, String> EMPTY_MAP = java.util.Collections.emptyMap();
    private static final List<String> EMPTY_LIST = java.util.Collections.emptyList();
    
    private EmptyContextProvider() {}
    
    @Override
    public Map<String, String> getMDC() {
        return EMPTY_MAP;
    }
    
    @Override
    public List<String> getNDC() {
        return EMPTY_LIST;
    }
    
    @Override
    public boolean hasContext() {
        return false;
    }
}