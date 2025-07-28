/*
 * Copyright (c) 2025 log4Rich
 * 
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.log4rich.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class providing Java 8 compatible alternatives to methods introduced in later Java versions.
 * This class ensures compatibility with Java 8 by providing implementations of methods that were
 * introduced in Java 11 and later.
 * 
 * @author log4Rich
 * @since 1.0.4
 */
public final class Java8Utils {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Java8Utils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Java 8 compatible alternative to String.repeat() which was introduced in Java 11.
     * Creates a string by repeating the given string the specified number of times.
     * 
     * @param str the string to repeat
     * @param count the number of times to repeat the string
     * @return the repeated string
     * @throws IllegalArgumentException if count is negative
     */
    public static String repeat(String str, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 0 || str.isEmpty()) {
            return "";
        }
        if (count == 1) {
            return str;
        }
        
        // Use StringBuilder for efficiency
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * Java 8 compatible alternative to Files.readString() which was introduced in Java 11.
     * Reads all characters from a file into a string, decoding from bytes to characters
     * using the UTF-8 charset.
     * 
     * @param path the path to the file
     * @return a String containing the content of the file
     * @throws IOException if an I/O error occurs reading from the file or a malformed or
     *         unmappable byte sequence is read
     */
    public static String readString(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}