/*
 * Copyright (c) 2025 log4Rich
 * 
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.log4rich.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Java8Utils compatibility methods.
 * 
 * @author log4Rich
 * @since 1.0.4
 */
class Java8UtilsTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testRepeat() {
        // Test basic functionality
        assertEquals("", Java8Utils.repeat("abc", 0));
        assertEquals("abc", Java8Utils.repeat("abc", 1));
        assertEquals("abcabc", Java8Utils.repeat("abc", 2));
        assertEquals("abcabcabc", Java8Utils.repeat("abc", 3));
        
        // Test with special characters
        assertEquals("===", Java8Utils.repeat("=", 3));
        assertEquals("---", Java8Utils.repeat("-", 3));
        
        // Test empty string
        assertEquals("", Java8Utils.repeat("", 5));
        
        // Test negative count throws exception
        assertThrows(IllegalArgumentException.class, () -> Java8Utils.repeat("abc", -1));
    }
    
    @Test
    void testReadString() throws IOException {
        // Create a test file with UTF-8 content
        Path testFile = tempDir.resolve("test.txt");
        String originalContent = "Hello, World!\nThis is a test file with UTF-8 content: café, naïve, résumé";
        Files.write(testFile, originalContent.getBytes(StandardCharsets.UTF_8));
        
        // Read using our utility method
        String readContent = Java8Utils.readString(testFile);
        
        assertEquals(originalContent, readContent);
    }
    
    @Test
    void testReadStringEmptyFile() throws IOException {
        // Create an empty test file
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.write(emptyFile, new byte[0]);
        
        // Read using our utility method
        String readContent = Java8Utils.readString(emptyFile);
        
        assertEquals("", readContent);
    }
    
    @Test
    void testReadStringNonExistentFile() {
        // Test with non-existent file
        Path nonExistentFile = tempDir.resolve("does-not-exist.txt");
        
        assertThrows(IOException.class, () -> Java8Utils.readString(nonExistentFile));
    }
}