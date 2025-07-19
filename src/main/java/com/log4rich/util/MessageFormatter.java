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

/**
 * Utility class for formatting log messages with SLF4J-style placeholders.
 * 
 * <p>This formatter provides 100% compatibility with SLF4J's message formatting,
 * supporting {} placeholders for parameter substitution. It also handles special
 * cases like array arguments and automatic throwable detection.</p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>SLF4J Compatibility</strong>: Identical {} placeholder syntax</li>
 *   <li><strong>Array Support</strong>: Automatically formats arrays using Arrays.toString()</li>
 *   <li><strong>Throwable Detection</strong>: Automatically extracts Throwable from varargs</li>
 *   <li><strong>Escaped Placeholders</strong>: Supports \{} for literal {} in output</li>
 *   <li><strong>Null Safety</strong>: Handles null arguments gracefully</li>
 * </ul>
 * 
 * <h3>Examples:</h3>
 * <pre>{@code
 * // Basic placeholder substitution
 * format("User {} logged in", "john") // → "User john logged in"
 * 
 * // Multiple placeholders
 * format("User {} failed {} times", "john", 3) // → "User john failed 3 times"
 * 
 * // Array handling
 * format("Items: {}", new String[]{"a", "b"}) // → "Items: [a, b]"
 * 
 * // Escaped placeholders
 * format("Literal \\{} and value {}", "test") // → "Literal {} and value test"
 * 
 * // Throwable detection (last argument)
 * Object[] args = {"user123", new RuntimeException("error")};
 * extractThrowable(args); // Returns the RuntimeException
 * }</pre>
 * 
 * @since 1.0.0
 */
public final class MessageFormatter {
    
    private static final String PLACEHOLDER = "{}";
    private static final int PLACEHOLDER_LENGTH = 2;
    
    private MessageFormatter() {
        // Utility class
    }
    
    /**
     * Formats a message using SLF4J-style {} placeholders.
     * 
     * @param messagePattern the message pattern with {} placeholders
     * @param arguments the arguments to substitute
     * @return the formatted message
     */
    public static String format(String messagePattern, Object... arguments) {
        if (messagePattern == null) {
            return null;
        }
        
        if (arguments == null || arguments.length == 0) {
            return messagePattern;
        }
        
        // Fast path - if no placeholders, return original
        if (messagePattern.indexOf(PLACEHOLDER) == -1) {
            return messagePattern;
        }
        
        StringBuilder result = new StringBuilder(messagePattern.length() + 50);
        int messageStart = 0;
        int argumentIndex = 0;
        
        while (argumentIndex < arguments.length) {
            int placeholderIndex = messagePattern.indexOf(PLACEHOLDER, messageStart);
            
            if (placeholderIndex == -1) {
                // No more placeholders
                break;
            }
            
            // Check if placeholder is escaped with backslash
            if (placeholderIndex > 0 && messagePattern.charAt(placeholderIndex - 1) == '\\') {
                // Escaped placeholder - append text including the escaped placeholder
                result.append(messagePattern, messageStart, placeholderIndex - 1);
                result.append(PLACEHOLDER);
                messageStart = placeholderIndex + PLACEHOLDER_LENGTH;
                continue;
            }
            
            // Normal placeholder - append text before placeholder
            result.append(messagePattern, messageStart, placeholderIndex);
            
            // Append the argument
            Object argument = arguments[argumentIndex++];
            if (argument != null) {
                if (argument instanceof Object[]) {
                    // Handle array arguments
                    result.append(java.util.Arrays.toString((Object[]) argument));
                } else if (argument.getClass().isArray()) {
                    // Handle primitive arrays
                    result.append(arrayToString(argument));
                } else {
                    result.append(argument.toString());
                }
            } else {
                result.append("null");
            }
            
            messageStart = placeholderIndex + PLACEHOLDER_LENGTH;
        }
        
        // Append any remaining text
        result.append(messagePattern, messageStart, messagePattern.length());
        
        return result.toString();
    }
    
    /**
     * Extracts the throwable from arguments if the last argument is a Throwable.
     * This supports the SLF4J pattern where the throwable is passed as the last parameter.
     * 
     * @param arguments the arguments array
     * @return the throwable if found, null otherwise
     */
    public static Throwable extractThrowable(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return null;
        }
        
        Object lastArgument = arguments[arguments.length - 1];
        return lastArgument instanceof Throwable ? (Throwable) lastArgument : null;
    }
    
    /**
     * Removes the throwable from the arguments array if present.
     * 
     * @param arguments the original arguments
     * @param throwable the throwable to remove (if present)
     * @return arguments without the throwable
     */
    public static Object[] removeThrowable(Object[] arguments, Throwable throwable) {
        if (throwable == null || arguments == null || arguments.length == 0) {
            return arguments;
        }
        
        Object lastArgument = arguments[arguments.length - 1];
        if (lastArgument == throwable) {
            // Create new array without the last element
            Object[] result = new Object[arguments.length - 1];
            System.arraycopy(arguments, 0, result, 0, arguments.length - 1);
            return result;
        }
        
        return arguments;
    }
    
    /**
     * Converts primitive arrays to string representation.
     */
    private static String arrayToString(Object array) {
        Class<?> type = array.getClass().getComponentType();
        
        if (type == boolean.class) {
            return java.util.Arrays.toString((boolean[]) array);
        } else if (type == byte.class) {
            return java.util.Arrays.toString((byte[]) array);
        } else if (type == char.class) {
            return java.util.Arrays.toString((char[]) array);
        } else if (type == short.class) {
            return java.util.Arrays.toString((short[]) array);
        } else if (type == int.class) {
            return java.util.Arrays.toString((int[]) array);
        } else if (type == long.class) {
            return java.util.Arrays.toString((long[]) array);
        } else if (type == float.class) {
            return java.util.Arrays.toString((float[]) array);
        } else if (type == double.class) {
            return java.util.Arrays.toString((double[]) array);
        }
        
        return String.valueOf(array);
    }
}