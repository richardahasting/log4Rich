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

package com.log4rich.config;

import java.util.List;

/**
 * Exception thrown when configuration validation fails.
 * Contains detailed information about configuration errors with specific guidance.
 */
public class ConfigurationException extends RuntimeException {
    
    private final List<ConfigurationValidator.ConfigurationError> errors;
    
    /**
     * Creates a new ConfigurationException with a message and list of errors.
     * 
     * @param message the error message
     * @param errors the list of configuration errors
     */
    public ConfigurationException(String message, List<ConfigurationValidator.ConfigurationError> errors) {
        super(message);
        this.errors = errors;
    }
    
    /**
     * Creates a new ConfigurationException with a message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = null;
    }
    
    /**
     * Creates a new ConfigurationException with a message.
     * 
     * @param message the error message
     */
    public ConfigurationException(String message) {
        super(message);
        this.errors = null;
    }
    
    /**
     * Gets the list of configuration errors.
     * 
     * @return the list of errors, or null if not available
     */
    public List<ConfigurationValidator.ConfigurationError> getErrors() {
        return errors;
    }
    
    /**
     * Checks if this exception has detailed validation errors.
     * 
     * @return true if validation errors are available
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Gets the number of validation errors.
     * 
     * @return the number of errors, or 0 if none
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
}