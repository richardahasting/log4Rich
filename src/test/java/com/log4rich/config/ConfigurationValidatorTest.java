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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Properties;

public class ConfigurationValidatorTest {
    
    @Test
    public void testValidConfiguration() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.rootLevel", "INFO");
        properties.setProperty("log4rich.console.enabled", "true");
        properties.setProperty("log4rich.file.maxSize", "10M");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertTrue(errors.isEmpty());
    }
    
    @Test
    public void testInvalidLogLevel() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.rootLevel", "INVALID_LEVEL");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.rootLevel", error.getPropertyName());
        assertEquals("INVALID_LEVEL", error.getInvalidValue());
        assertTrue(error.getErrorMessage().contains("Invalid log level"));
        assertTrue(error.getSuggestion().contains("TRACE, DEBUG, INFO, WARN, ERROR, FATAL"));
    }
    
    @Test
    public void testInvalidBooleanValue() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.console.enabled", "maybe");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.console.enabled", error.getPropertyName());
        assertEquals("maybe", error.getInvalidValue());
        assertTrue(error.getErrorMessage().contains("Invalid boolean value"));
        assertTrue(error.getSuggestion().contains("true") || error.getSuggestion().contains("false"));
    }
    
    @Test
    public void testInvalidConsoleTarget() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.console.target", "CONSOLE");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.console.target", error.getPropertyName());
        assertEquals("CONSOLE", error.getInvalidValue());
        assertTrue(error.getErrorMessage().contains("Invalid console target"));
        assertTrue(error.getSuggestion().contains("STDOUT") || error.getSuggestion().contains("STDERR"));
    }
    
    @Test
    public void testInvalidSizeFormat() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.file.maxSize", "10X");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.file.maxSize", error.getPropertyName());
        assertTrue(error.getErrorMessage().contains("Invalid size format"));
        assertTrue(error.getSuggestion().contains("10M"));
    }
    
    @Test
    public void testIntegerOutOfRange() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.file.maxBackups", "2000");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.file.maxBackups", error.getPropertyName());
        assertTrue(error.getErrorMessage().contains("Value out of range"));
        assertTrue(error.getSuggestion().contains("between 0 and 1000"));
    }
    
    @Test
    public void testInvalidEncoding() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.file.encoding", "INVALID-ENCODING");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.file.encoding", error.getPropertyName());
        assertTrue(error.getErrorMessage().contains("Invalid character encoding"));
        assertTrue(error.getSuggestion().contains("UTF-8"));
    }
    
    @Test
    public void testInvalidAsyncBufferSize() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.async.bufferSize", "2000"); // Not a power of 2, but within range
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.async.bufferSize", error.getPropertyName());
        assertTrue(error.getErrorMessage().contains("power of 2"));
        assertTrue(error.getSuggestion().contains("2048"));
    }
    
    @Test
    public void testInvalidOverflowStrategy() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.async.overflowStrategy", "INVALID_STRATEGY");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(1, errors.size());
        
        ConfigurationValidator.ConfigurationError error = errors.get(0);
        assertEquals("log4rich.async.overflowStrategy", error.getPropertyName());
        assertTrue(error.getErrorMessage().contains("Invalid overflow strategy"));
        assertTrue(error.getSuggestion().contains("DROP_OLDEST"));
    }
    
    @Test
    public void testMultipleErrors() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.rootLevel", "INVALID");
        properties.setProperty("log4rich.console.enabled", "maybe");
        properties.setProperty("log4rich.file.maxSize", "invalid");
        
        List<ConfigurationValidator.ConfigurationError> errors = ConfigurationValidator.validate(properties);
        assertEquals(3, errors.size());
        
        // Check that all errors are reported
        assertTrue(errors.stream().anyMatch(e -> e.getPropertyName().equals("log4rich.rootLevel")));
        assertTrue(errors.stream().anyMatch(e -> e.getPropertyName().equals("log4rich.console.enabled")));
        assertTrue(errors.stream().anyMatch(e -> e.getPropertyName().equals("log4rich.file.maxSize")));
    }
    
    @Test
    public void testConfigurationExceptionWithErrors() {
        Properties properties = new Properties();
        properties.setProperty("log4rich.rootLevel", "INVALID");
        
        assertThrows(ConfigurationException.class, () -> {
            new Configuration(properties);
        });
    }
    
    @Test
    public void testFormattedErrorMessage() {
        ConfigurationValidator.ConfigurationError error = new ConfigurationValidator.ConfigurationError(
            "test.property",
            "invalid_value",
            "This is a test error",
            "Fix it like this"
        );
        
        String formatted = error.getFormattedMessage();
        assertTrue(formatted.contains("ERROR: test.property=invalid_value"));
        assertTrue(formatted.contains("Problem: This is a test error"));
        assertTrue(formatted.contains("Solution: Fix it like this"));
    }
}