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

import com.log4rich.core.LogLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Properties;

public class ConfigLoaderEnvironmentTest {
    
    private Properties originalProperties;
    
    @BeforeEach
    public void setUp() {
        // Clear configuration cache before each test
        ConfigLoader.clearCache();
        
        // Save original system properties
        originalProperties = new Properties();
        originalProperties.putAll(System.getProperties());
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system properties
        System.setProperties(originalProperties);
        
        // Clear configuration cache after each test
        ConfigLoader.clearCache();
    }
    
    @Test
    public void testEnvironmentVariableToPropertyNameConversion() throws Exception {
        // Use reflection to access private method for testing
        Method method = ConfigLoader.class.getDeclaredMethod("environmentVariableToPropertyName", String.class);
        method.setAccessible(true);
        
        // Test basic conversion
        String result1 = (String) method.invoke(null, "LOG4RICH_ROOT_LEVEL");
        assertEquals("log4rich.rootLevel", result1);
        
        // Test nested properties
        String result2 = (String) method.invoke(null, "LOG4RICH_CONSOLE_ENABLED");
        assertEquals("log4rich.console.enabled", result2);
        
        // Test complex nested properties with camelCase
        String result3 = (String) method.invoke(null, "LOG4RICH_FILE_MAX_SIZE");
        assertEquals("log4rich.file.maxSize", result3);
        
        // Test performance properties with camelCase
        String result4 = (String) method.invoke(null, "LOG4RICH_PERFORMANCE_BATCH_ENABLED");
        assertEquals("log4rich.performance.batchEnabled", result4);
    }
    
    @Test
    public void testApplyEnvironmentVariableOverrides() throws Exception {
        // Create a mock environment by temporarily setting system properties
        // (since we can't easily modify environment variables in tests)
        
        Properties testProperties = new Properties();
        testProperties.setProperty("log4rich.rootLevel", "INFO");
        testProperties.setProperty("log4rich.console.enabled", "true");
        
        // Use reflection to access private method
        Method method = ConfigLoader.class.getDeclaredMethod("applyEnvironmentVariableOverrides", Properties.class);
        method.setAccessible(true);
        
        // Since we can't easily set environment variables in tests, we'll test the conversion logic
        // The actual environment variable application would be tested in integration tests
        
        // Test that properties remain unchanged when no environment variables are set
        Properties originalProps = new Properties();
        originalProps.putAll(testProperties);
        
        method.invoke(null, testProperties);
        
        // Properties should be unchanged (no LOG4RICH_* environment variables set)
        assertEquals(originalProps.size(), testProperties.size());
    }
    
    @Test
    public void testGetSupportedEnvironmentVariables() {
        String[] supportedVars = ConfigLoader.getSupportedEnvironmentVariables();
        
        assertNotNull(supportedVars);
        assertTrue(supportedVars.length > 0);
        
        // Check that common environment variables are included
        boolean hasRootLevel = false;
        boolean hasConsoleEnabled = false;
        boolean hasFileEnabled = false;
        
        for (String var : supportedVars) {
            if ("LOG4RICH_ROOT_LEVEL".equals(var)) {
                hasRootLevel = true;
            }
            if ("LOG4RICH_CONSOLE_ENABLED".equals(var)) {
                hasConsoleEnabled = true;
            }
            if ("LOG4RICH_FILE_ENABLED".equals(var)) {
                hasFileEnabled = true;
            }
        }
        
        assertTrue(hasRootLevel, "Should include LOG4RICH_ROOT_LEVEL");
        assertTrue(hasConsoleEnabled, "Should include LOG4RICH_CONSOLE_ENABLED");
        assertTrue(hasFileEnabled, "Should include LOG4RICH_FILE_ENABLED");
    }
    
    @Test
    public void testEnvironmentVariableNaming() {
        String[] supportedVars = ConfigLoader.getSupportedEnvironmentVariables();
        
        // All environment variables should start with LOG4RICH_
        for (String var : supportedVars) {
            assertTrue(var.startsWith("LOG4RICH_"), 
                "Environment variable should start with LOG4RICH_: " + var);
            
            // Should not contain lowercase letters (should be uppercase)
            assertEquals(var.toUpperCase(), var, 
                "Environment variable should be uppercase: " + var);
            
            // Should not contain dots (should use underscores)
            assertFalse(var.contains("."), 
                "Environment variable should not contain dots: " + var);
        }
    }
    
    @Test
    public void testEnvironmentVariableConversionRoundTrip() throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod("environmentVariableToPropertyName", String.class);
        method.setAccessible(true);
        
        // Test that known property names convert correctly
        String[][] testCases = {
            {"LOG4RICH_ROOT_LEVEL", "log4rich.rootLevel"},
            {"LOG4RICH_CONSOLE_ENABLED", "log4rich.console.enabled"},
            {"LOG4RICH_CONSOLE_TARGET", "log4rich.console.target"},
            {"LOG4RICH_FILE_PATH", "log4rich.file.path"},
            {"LOG4RICH_FILE_MAX_SIZE", "log4rich.file.maxSize"},
            {"LOG4RICH_ASYNC_BUFFER_SIZE", "log4rich.async.bufferSize"}
        };
        
        for (String[] testCase : testCases) {
            String envVar = testCase[0];
            String expectedProperty = testCase[1];
            
            String actualProperty = (String) method.invoke(null, envVar);
            assertEquals(expectedProperty, actualProperty, 
                "Environment variable " + envVar + " should convert to " + expectedProperty);
        }
    }
    
    @Test
    public void testConfigurationPrecedence() {
        // Test that environment variables should override file-based configuration
        // This test documents the expected behavior that environment variables have higher precedence
        
        // Create properties as if loaded from file
        Properties fileProperties = new Properties();
        fileProperties.setProperty("log4rich.rootLevel", "INFO");
        fileProperties.setProperty("log4rich.console.enabled", "false");
        fileProperties.setProperty("log4rich.file.enabled", "false"); // Disable file logging to avoid path validation
        fileProperties.setProperty("log4rich.file.path", "/tmp/test.log"); // Use valid path for test
        
        // Simulate environment variable override (in real usage, this would come from actual env vars)
        // Here we're just testing the concept that properties can be overridden
        fileProperties.setProperty("log4rich.rootLevel", "DEBUG"); // Simulating env var override
        
        Configuration config = new Configuration(fileProperties);
        assertEquals(LogLevel.DEBUG, config.getRootLevel());
    }
}