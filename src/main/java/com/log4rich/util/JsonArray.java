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

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight JSON array implementation for log4Rich JSON formatting.
 * 
 * This class provides basic JSON array functionality without external dependencies.
 * It maintains insertion order and supports proper JSON escaping.
 * 
 * @author log4Rich Contributors
 * @since 1.0.2
 */
public class JsonArray {
    
    private final List<Object> elements;
    
    /**
     * Creates a new empty JSON array.
     */
    public JsonArray() {
        this.elements = new ArrayList<>();
    }
    
    /**
     * Adds a string element to this JSON array.
     * 
     * @param value the string value (will be properly escaped)
     * @return this JsonArray for method chaining
     */
    public JsonArray add(String value) {
        elements.add(value);
        return this;
    }
    
    /**
     * Adds a numeric element to this JSON array.
     * 
     * @param value the numeric value
     * @return this JsonArray for method chaining
     */
    public JsonArray add(Number value) {
        elements.add(value);
        return this;
    }
    
    /**
     * Adds a boolean element to this JSON array.
     * 
     * @param value the boolean value
     * @return this JsonArray for method chaining
     */
    public JsonArray add(Boolean value) {
        elements.add(value);
        return this;
    }
    
    /**
     * Adds a JSON object to this JSON array.
     * 
     * @param value the JsonObject
     * @return this JsonArray for method chaining
     */
    public JsonArray add(JsonObject value) {
        elements.add(value);
        return this;
    }
    
    /**
     * Adds a nested JSON array to this JSON array.
     * 
     * @param value the JsonArray
     * @return this JsonArray for method chaining
     */
    public JsonArray add(JsonArray value) {
        elements.add(value);
        return this;
    }
    
    /**
     * Creates a JSON array from a Java array of strings.
     * 
     * @param strings the string array
     * @return a new JsonArray containing the strings
     */
    public static JsonArray from(String[] strings) {
        JsonArray array = new JsonArray();
        if (strings != null) {
            for (String str : strings) {
                array.add(str);
            }
        }
        return array;
    }
    
    /**
     * Creates a JSON array from the stack trace of a throwable.
     * 
     * @param throwable the throwable to extract stack trace from
     * @return a new JsonArray containing the stack trace elements
     */
    public static JsonArray fromStackTrace(Throwable throwable) {
        JsonArray array = new JsonArray();
        if (throwable != null) {
            for (StackTraceElement element : throwable.getStackTrace()) {
                array.add(element.toString());
            }
        }
        return array;
    }
    
    /**
     * Returns the pretty-printed JSON representation of this array.
     * 
     * @return formatted JSON string with indentation and newlines
     */
    @Override
    public String toString() {
        return toJsonString(true, 0);
    }
    
    /**
     * Returns the compact JSON representation of this array.
     * 
     * @return compact JSON string without extra whitespace
     */
    public String toCompactString() {
        return toJsonString(false, 0);
    }
    
    /**
     * Converts this JSON array to a string representation.
     * 
     * @param pretty whether to format with indentation and newlines
     * @param indentLevel the current indentation level
     * @return JSON string representation
     */
    protected String toJsonString(boolean pretty, int indentLevel) {
        if (elements.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        boolean first = true;
        for (Object element : elements) {
            if (!first) {
                sb.append(",");
            }
            
            if (pretty) {
                sb.append("\n");
                appendIndent(sb, indentLevel + 1);
            }
            
            appendValue(sb, element, pretty, indentLevel + 1);
            first = false;
        }
        
        if (pretty) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    /**
     * Appends a value to the JSON string, handling different types appropriately.
     * 
     * @param sb the StringBuilder to append to
     * @param value the value to append
     * @param pretty whether to format prettily
     * @param indentLevel the current indentation level
     */
    private void appendValue(StringBuilder sb, Object value, boolean pretty, int indentLevel) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJsonString((String) value)).append("\"");
        } else if (value instanceof JsonObject) {
            sb.append(((JsonObject) value).toJsonString(pretty, indentLevel));
        } else if (value instanceof JsonArray) {
            sb.append(((JsonArray) value).toJsonString(pretty, indentLevel));
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value.toString());
        } else {
            // Fallback: convert to string and escape
            sb.append("\"").append(escapeJsonString(value.toString())).append("\"");
        }
    }
    
    /**
     * Appends the appropriate indentation for the given level.
     * 
     * @param sb the StringBuilder to append to
     * @param level the indentation level
     */
    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
    }
    
    /**
     * Escapes a string for safe inclusion in JSON.
     * 
     * @param input the string to escape
     * @return the escaped string, or null if input is null
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < ' ') {
                        // Control characters
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
    
    /**
     * Returns the number of elements in this JSON array.
     * 
     * @return the number of elements
     */
    public int size() {
        return elements.size();
    }
    
    /**
     * Returns whether this JSON array is empty.
     * 
     * @return true if this array has no elements
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }
}