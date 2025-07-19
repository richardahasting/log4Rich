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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MessageFormatterTest {
    
    @Test
    public void testBasicPlaceholderReplacement() {
        String result = MessageFormatter.format("Hello {}", "World");
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testMultiplePlaceholders() {
        String result = MessageFormatter.format("User {} logged in from {}", "john", "192.168.1.1");
        assertEquals("User john logged in from 192.168.1.1", result);
    }
    
    @Test
    public void testNoPlaceholders() {
        String result = MessageFormatter.format("Simple message", "unused");
        assertEquals("Simple message", result);
    }
    
    @Test
    public void testNoArguments() {
        String result = MessageFormatter.format("Simple message");
        assertEquals("Simple message", result);
    }
    
    @Test
    public void testNullMessage() {
        String result = MessageFormatter.format(null, "arg");
        assertNull(result);
    }
    
    @Test
    public void testNullArguments() {
        String result = MessageFormatter.format("Hello {}");
        assertEquals("Hello {}", result);
    }
    
    @Test
    public void testNullArgument() {
        String result = MessageFormatter.format("Value is {}", (Object) null);
        assertEquals("Value is null", result);
    }
    
    @Test
    public void testEscapedPlaceholder() {
        String result = MessageFormatter.format("Use \\{} for literal brackets, {} for replacement", "value");
        assertEquals("Use {} for literal brackets, value for replacement", result);
    }
    
    @Test
    public void testMoreArgumentsThanPlaceholders() {
        String result = MessageFormatter.format("Hello {}", "World", "Extra");
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testFewerArgumentsThanPlaceholders() {
        String result = MessageFormatter.format("Hello {} and {}", "World");
        assertEquals("Hello World and {}", result);
    }
    
    @Test
    public void testArrayArgument() {
        String result = MessageFormatter.format("Array: {}", (Object) new String[]{"a", "b", "c"});
        assertEquals("Array: [a, b, c]", result);
    }
    
    @Test
    public void testPrimitiveArrayArgument() {
        String result = MessageFormatter.format("Numbers: {}", (Object) new int[]{1, 2, 3});
        assertEquals("Numbers: [1, 2, 3]", result);
    }
    
    @Test
    public void testExtractThrowableFromArguments() {
        Exception e = new RuntimeException("test");
        Object[] args = new Object[]{"arg1", "arg2", e};
        Throwable result = MessageFormatter.extractThrowable(args);
        assertSame(e, result);
    }
    
    @Test
    public void testExtractThrowableNoThrowable() {
        Throwable result = MessageFormatter.extractThrowable(new Object[]{"arg1", "arg2"});
        assertNull(result);
    }
    
    @Test
    public void testExtractThrowableEmptyArray() {
        Throwable result = MessageFormatter.extractThrowable(new Object[0]);
        assertNull(result);
    }
    
    @Test
    public void testExtractThrowableNullArray() {
        Throwable result = MessageFormatter.extractThrowable(null);
        assertNull(result);
    }
    
    @Test
    public void testRemoveThrowableFromArguments() {
        Exception e = new RuntimeException("test");
        Object[] original = new Object[]{"arg1", "arg2", e};
        Object[] result = MessageFormatter.removeThrowable(original, e);
        
        assertEquals(2, result.length);
        assertEquals("arg1", result[0]);
        assertEquals("arg2", result[1]);
    }
    
    @Test
    public void testRemoveThrowableNotPresent() {
        Exception e = new RuntimeException("test");
        Object[] original = new Object[]{"arg1", "arg2"};
        Object[] result = MessageFormatter.removeThrowable(original, e);
        
        assertSame(original, result);
    }
    
    @Test
    public void testComplexScenario() {
        // Test scenario similar to the GitHub issue example
        String fileName = "config.xml";
        Exception e = new RuntimeException("File not found");
        
        String result = MessageFormatter.format("Failed to process {}: {}", fileName, e.getMessage(), e);
        assertEquals("Failed to process config.xml: File not found", result);
        
        Throwable extractedThrowable = MessageFormatter.extractThrowable(new Object[]{fileName, e.getMessage(), e});
        assertSame(e, extractedThrowable);
    }
}