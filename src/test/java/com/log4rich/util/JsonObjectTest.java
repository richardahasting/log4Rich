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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for JsonObject utility class.
 */
public class JsonObjectTest {
    
    private JsonObject jsonObject;
    
    @BeforeEach
    public void setUp() {
        jsonObject = new JsonObject();
    }
    
    @Test
    public void testEmptyObject() {
        assertEquals("{}", jsonObject.toCompactString());
        assertEquals("{}", jsonObject.toString());
        assertTrue(jsonObject.isEmpty());
        assertEquals(0, jsonObject.size());
    }
    
    @Test
    public void testStringProperties() {
        jsonObject.addProperty("name", "John Doe");
        jsonObject.addProperty("city", "New York");
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"name\":\"John Doe\""));
        assertTrue(compact.contains("\"city\":\"New York\""));
        assertFalse(jsonObject.isEmpty());
        assertEquals(2, jsonObject.size());
    }
    
    @Test
    public void testNumericProperties() {
        jsonObject.addProperty("age", 30);
        jsonObject.addProperty("height", 5.9);
        jsonObject.addProperty("score", 95L);
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"age\":30"));
        assertTrue(compact.contains("\"height\":5.9"));
        assertTrue(compact.contains("\"score\":95"));
    }
    
    @Test
    public void testBooleanProperties() {
        jsonObject.addProperty("active", true);
        jsonObject.addProperty("verified", false);
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"active\":true"));
        assertTrue(compact.contains("\"verified\":false"));
    }
    
    @Test
    public void testNestedObjects() {
        JsonObject address = new JsonObject();
        address.addProperty("street", "123 Main St");
        address.addProperty("zip", "12345");
        
        jsonObject.addProperty("name", "John");
        jsonObject.add("address", address);
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"name\":\"John\""));
        assertTrue(compact.contains("\"address\":{"));
        assertTrue(compact.contains("\"street\":\"123 Main St\""));
        assertTrue(compact.contains("\"zip\":\"12345\""));
    }
    
    @Test
    public void testNestedArrays() {
        JsonArray skills = new JsonArray();
        skills.add("Java").add("Python").add("JavaScript");
        
        jsonObject.addProperty("name", "Developer");
        jsonObject.add("skills", skills);
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"skills\":["));
        assertTrue(compact.contains("\"Java\""));
        assertTrue(compact.contains("\"Python\""));
        assertTrue(compact.contains("\"JavaScript\""));
    }
    
    @Test
    public void testPrettyPrinting() {
        jsonObject.addProperty("name", "John");
        jsonObject.addProperty("age", 30);
        
        String pretty = jsonObject.toString();
        
        // Should contain newlines and indentation
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("  "));
        
        // Should start and end with braces on separate lines
        assertTrue(pretty.startsWith("{"));
        assertTrue(pretty.endsWith("}"));
        
        // Should have proper formatting
        assertTrue(pretty.contains("  \"name\": \"John\""));
        assertTrue(pretty.contains("  \"age\": 30"));
    }
    
    @Test
    public void testJsonEscaping() {
        jsonObject.addProperty("quote", "He said \"Hello\"");
        jsonObject.addProperty("newline", "Line 1\nLine 2");
        jsonObject.addProperty("tab", "Column1\tColumn2");
        jsonObject.addProperty("backslash", "Path\\to\\file");
        jsonObject.addProperty("carriage", "Line 1\rLine 2");
        jsonObject.addProperty("backspace", "Text\bBackspace");
        jsonObject.addProperty("formfeed", "Text\fFormfeed");
        
        String compact = jsonObject.toCompactString();
        
        // Check proper escaping
        assertTrue(compact.contains("\\\"Hello\\\""));
        assertTrue(compact.contains("\\n"));
        assertTrue(compact.contains("\\t"));
        assertTrue(compact.contains("\\\\"));
        assertTrue(compact.contains("\\r"));
        assertTrue(compact.contains("\\b"));
        assertTrue(compact.contains("\\f"));
    }
    
    @Test
    public void testControlCharacterEscaping() {
        // Test control characters (ASCII < 32)
        jsonObject.addProperty("control", "\u0001\u0002\u001F");
        
        String compact = jsonObject.toCompactString();
        
        // Should escape as unicode
        assertTrue(compact.contains("\\u0001"));
        assertTrue(compact.contains("\\u0002"));
        assertTrue(compact.contains("\\u001f"));
    }
    
    @Test
    public void testNullValues() {
        jsonObject.addProperty("nullString", (String) null);
        jsonObject.addProperty("validString", "valid");
        
        String compact = jsonObject.toCompactString();
        
        // Should handle null properly
        assertTrue(compact.contains("\"nullString\":null"));
        assertTrue(compact.contains("\"validString\":\"valid\""));
    }
    
    @Test
    public void testUnicodeHandling() {
        jsonObject.addProperty("chinese", "你好世界");
        jsonObject.addProperty("emoji", "🌍🚀");
        jsonObject.addProperty("accents", "café naïve résumé");
        
        String compact = jsonObject.toCompactString();
        
        // Unicode should be preserved
        assertTrue(compact.contains("你好世界"));
        assertTrue(compact.contains("🌍🚀"));
        assertTrue(compact.contains("café"));
        assertTrue(compact.contains("naïve"));
        assertTrue(compact.contains("résumé"));
    }
    
    @Test
    public void testMethodChaining() {
        JsonObject result = jsonObject
            .addProperty("name", "John")
            .addProperty("age", 30)
            .addProperty("active", true);
        
        // Should return the same object for chaining
        assertSame(jsonObject, result);
        
        String compact = jsonObject.toCompactString();
        assertTrue(compact.contains("\"name\":\"John\""));
        assertTrue(compact.contains("\"age\":30"));
        assertTrue(compact.contains("\"active\":true"));
    }
    
    @Test
    public void testInsertionOrder() {
        jsonObject.addProperty("third", "3");
        jsonObject.addProperty("first", "1");
        jsonObject.addProperty("second", "2");
        
        String compact = jsonObject.toCompactString();
        
        // Should maintain insertion order
        int thirdPos = compact.indexOf("\"third\"");
        int firstPos = compact.indexOf("\"first\"");
        int secondPos = compact.indexOf("\"second\"");
        
        assertTrue(thirdPos < firstPos);
        assertTrue(firstPos < secondPos);
    }
    
    @Test
    public void testComplexNesting() {
        JsonObject person = new JsonObject();
        person.addProperty("name", "John");
        
        JsonObject address = new JsonObject();
        address.addProperty("street", "123 Main St");
        address.addProperty("city", "New York");
        
        JsonArray phones = new JsonArray();
        phones.add("555-1234").add("555-5678");
        
        person.add("address", address);
        person.add("phones", phones);
        
        jsonObject.add("person", person);
        jsonObject.addProperty("timestamp", "2025-01-01");
        
        String compact = jsonObject.toCompactString();
        
        // Should properly format complex nested structure
        assertTrue(compact.contains("\"person\":{"));
        assertTrue(compact.contains("\"address\":{"));
        assertTrue(compact.contains("\"phones\":["));
        assertTrue(compact.contains("\"555-1234\""));
        assertTrue(compact.contains("\"New York\""));
    }
    
    @Test
    public void testPerformance() {
        // Simple performance test
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            JsonObject obj = new JsonObject();
            obj.addProperty("field1", "value" + i);
            obj.addProperty("field2", i);
            obj.addProperty("field3", i % 2 == 0);
            obj.toCompactString();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should create and format 1000 objects in reasonable time
        assertTrue(duration < 1000, "JsonObject performance too slow: " + duration + "ms for 1000 objects");
    }
}