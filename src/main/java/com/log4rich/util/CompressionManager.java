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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages external compression of log files.
 * Supports configurable compression programs and arguments.
 * This class handles the execution of external compression programs
 * with timeout support and proper error handling.
 */
public class CompressionManager {
    
    private static final String DEFAULT_PROGRAM = "gzip";
    private static final String DEFAULT_ARGS = "";
    private static final long DEFAULT_TIMEOUT = 30000; // 30 seconds
    
    private final String program;
    private final String arguments;
    private final long timeoutMillis;
    
    /**
     * Creates a new CompressionManager with default settings.
     * Uses gzip as the compression program with no additional arguments
     * and a 30-second timeout.
     */
    public CompressionManager() {
        this(DEFAULT_PROGRAM, DEFAULT_ARGS, DEFAULT_TIMEOUT);
    }
    
    /**
     * Creates a new CompressionManager with the specified settings.
     * 
     * @param program the compression program to use (e.g., "gzip", "bzip2", "xz")
     * @param arguments additional arguments for the compression program
     * @param timeoutMillis the timeout in milliseconds for compression operations
     */
    public CompressionManager(String program, String arguments, long timeoutMillis) {
        this.program = program != null ? program : DEFAULT_PROGRAM;
        this.arguments = arguments != null ? arguments : DEFAULT_ARGS;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT;
    }
    
    /**
     * Compresses a file using the configured compression program.
     * If compression fails, the original file is returned.
     * 
     * @param sourceFile the file to compress
     * @return the compressed file, or the original file if compression failed
     */
    public File compressFile(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            return sourceFile;
        }
        
        try {
            // Build command
            List<String> command = buildCommand(sourceFile);
            
            // Execute compression
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(sourceFile.getParentFile());
            
            Process process = pb.start();
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                logError("Compression timeout for file: " + sourceFile.getName());
                return sourceFile;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Success - return the compressed file
                File compressedFile = getCompressedFileName(sourceFile);
                if (compressedFile.exists()) {
                    return compressedFile;
                } else {
                    logError("Compression succeeded but compressed file not found: " + compressedFile.getName());
                    return sourceFile;
                }
            } else {
                // Compression failed
                String errorOutput = readProcessError(process);
                logError("Compression failed for file: " + sourceFile.getName() + 
                        ", exit code: " + exitCode + ", error: " + errorOutput);
                return sourceFile;
            }
            
        } catch (IOException e) {
            logError("IOException during compression of file: " + sourceFile.getName() + 
                    ", program: " + program + ", error: " + e.getMessage());
            return sourceFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Compression interrupted for file: " + sourceFile.getName());
            return sourceFile;
        }
    }
    
    /**
     * Builds the command to execute for compression.
     * 
     * @param sourceFile the file to compress
     * @return list of command arguments
     */
    private List<String> buildCommand(File sourceFile) {
        List<String> command = new ArrayList<>();
        command.add(program);
        
        // Add arguments if specified
        if (!arguments.trim().isEmpty()) {
            String[] args = arguments.trim().split("\\s+");
            for (String arg : args) {
                if (!arg.isEmpty()) {
                    command.add(arg);
                }
            }
        }
        
        // Add the file to compress
        command.add(sourceFile.getName());
        
        return command;
    }
    
    /**
     * Gets the expected compressed file name based on the compression program.
     * 
     * @param sourceFile the original file
     * @return the expected compressed file
     */
    private File getCompressedFileName(File sourceFile) {
        String baseName = sourceFile.getName();
        String extension = getCompressionExtension();
        
        return new File(sourceFile.getParentFile(), baseName + extension);
    }
    
    /**
     * Gets the file extension for the compression program.
     * 
     * @return the appropriate file extension
     */
    private String getCompressionExtension() {
        switch (program.toLowerCase()) {
            case "gzip":
                return ".gz";
            case "bzip2":
                return ".bz2";
            case "xz":
                return ".xz";
            case "zip":
                return ".zip";
            case "7z":
                return ".7z";
            default:
                return ".compressed";
        }
    }
    
    /**
     * Reads error output from a process.
     * 
     * @param process the process to read from
     * @return error output as string
     */
    private String readProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "Unable to read error output: " + e.getMessage();
        }
    }
    
    /**
     * Logs an error message to stderr.
     * This avoids potential infinite loops by not using the logging framework.
     * 
     * @param message the error message
     */
    private void logError(String message) {
        System.err.println("[log4Rich] Compression error: " + message);
    }
    
    /**
     * Checks if the compression program is available.
     * Tests if the program can be executed by running it with --version.
     * 
     * @return true if the program can be executed, false otherwise
     */
    public boolean isProgramAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(program, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished) {
                return process.exitValue() == 0;
            } else {
                process.destroyForcibly();
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    // Getters
    /**
     * Gets the compression program name.
     * 
     * @return the compression program name
     */
    public String getProgram() {
        return program;
    }
    
    /**
     * Gets the compression program arguments.
     * 
     * @return the compression program arguments
     */
    public String getArguments() {
        return arguments;
    }
    
    /**
     * Gets the compression timeout in milliseconds.
     * 
     * @return the compression timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}