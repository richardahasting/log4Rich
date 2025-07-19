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

package com.log4rich;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Version information for the log4Rich logging framework.
 * 
 * This class provides access to version information, build details, and
 * runtime version checking capabilities for the log4Rich framework.
 * 
 * @author log4Rich Contributors
 * @since 1.0.0
 */
public final class Version {
    
    // Core version information
    private static final String MAJOR_VERSION = "1";
    private static final String MINOR_VERSION = "0";
    private static final String PATCH_VERSION = "1";
    private static final String BUILD_TYPE = "RELEASE";
    
    // Full version string
    private static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION;
    private static final String FULL_VERSION = VERSION + "-" + BUILD_TYPE;
    
    // Build information
    private static final String BUILD_DATE = "2025-07-19";
    private static final String BUILD_TIME = "15:20:00 UTC";
    private static final String BUILD_TIMESTAMP = BUILD_DATE + " " + BUILD_TIME;
    
    // Framework information
    private static final String FRAMEWORK_NAME = "log4Rich";
    private static final String FRAMEWORK_DESCRIPTION = "Ultra-High-Performance Java Logging Framework";
    private static final String COPYRIGHT = "Copyright (c) 2025 log4Rich Contributors";
    private static final String LICENSE = "Apache License 2.0";
    private static final String WEBSITE = "https://github.com/richardahasting/log4Rich";
    
    // Java compatibility
    private static final String MIN_JAVA_VERSION = "1.8";
    private static final String RECOMMENDED_JAVA_VERSION = "11+";
    
    // Performance characteristics (from benchmarks)
    private static final String PEAK_THROUGHPUT = "2.3M messages/second";
    private static final String TYPICAL_LATENCY = "Sub-microsecond";
    private static final String MEMORY_FOOTPRINT = "Low (thread-local pools)";
    
    // Feature highlights for this version
    private static final String[] VERSION_FEATURES = {
        "SLF4J-style {} placeholder support (100% compatible)",
        "Environment variable configuration (29 LOG4RICH_* variables)",
        "Enhanced error messages with specific fix guidance",
        "Migration utilities for SLF4J/Log4j users",
        "Asynchronous compression with adaptive management",
        "Lock-free ring buffers for async logging", 
        "Memory-mapped file I/O (5.4x performance)",
        "Intelligent batch processing (23x multi-threaded)",
        "Zero-allocation mode with object pools",
        "Runtime configuration management"
    };
    
    // Prevent instantiation
    private Version() {
        throw new UnsupportedOperationException("Version is a utility class and cannot be instantiated");
    }
    
    /**
     * Gets the major version number.
     * 
     * @return the major version (e.g., "1" for version 1.0.0)
     */
    public static String getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    /**
     * Gets the minor version number.
     * 
     * @return the minor version (e.g., "0" for version 1.0.0)
     */
    public static String getMinorVersion() {
        return MINOR_VERSION;
    }
    
    /**
     * Gets the patch version number.
     * 
     * @return the patch version (e.g., "0" for version 1.0.0)
     */
    public static String getPatchVersion() {
        return PATCH_VERSION;
    }
    
    /**
     * Gets the version string in semantic versioning format.
     * 
     * @return the version string (e.g., "1.0.0")
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * Gets the full version string including build type.
     * 
     * @return the full version string (e.g., "1.0.0-RELEASE")
     */
    public static String getFullVersion() {
        return FULL_VERSION;
    }
    
    /**
     * Gets the build type (RELEASE, SNAPSHOT, etc.).
     * 
     * @return the build type
     */
    public static String getBuildType() {
        return BUILD_TYPE;
    }
    
    /**
     * Gets the build date.
     * 
     * @return the build date in YYYY-MM-DD format
     */
    public static String getBuildDate() {
        return BUILD_DATE;
    }
    
    /**
     * Gets the build time.
     * 
     * @return the build time in HH:MM:SS UTC format
     */
    public static String getBuildTime() {
        return BUILD_TIME;
    }
    
    /**
     * Gets the complete build timestamp.
     * 
     * @return the build timestamp combining date and time
     */
    public static String getBuildTimestamp() {
        return BUILD_TIMESTAMP;
    }
    
    /**
     * Gets the framework name.
     * 
     * @return the framework name
     */
    public static String getFrameworkName() {
        return FRAMEWORK_NAME;
    }
    
    /**
     * Gets the framework description.
     * 
     * @return the framework description
     */
    public static String getFrameworkDescription() {
        return FRAMEWORK_DESCRIPTION;
    }
    
    /**
     * Gets the copyright notice.
     * 
     * @return the copyright notice
     */
    public static String getCopyright() {
        return COPYRIGHT;
    }
    
    /**
     * Gets the license information.
     * 
     * @return the license information
     */
    public static String getLicense() {
        return LICENSE;
    }
    
    /**
     * Gets the project website URL.
     * 
     * @return the website URL
     */
    public static String getWebsite() {
        return WEBSITE;
    }
    
    /**
     * Gets the minimum required Java version.
     * 
     * @return the minimum Java version
     */
    public static String getMinJavaVersion() {
        return MIN_JAVA_VERSION;
    }
    
    /**
     * Gets the recommended Java version.
     * 
     * @return the recommended Java version
     */
    public static String getRecommendedJavaVersion() {
        return RECOMMENDED_JAVA_VERSION;
    }
    
    /**
     * Gets the peak throughput performance metric.
     * 
     * @return the peak throughput string
     */
    public static String getPeakThroughput() {
        return PEAK_THROUGHPUT;
    }
    
    /**
     * Gets the typical latency performance metric.
     * 
     * @return the typical latency string
     */
    public static String getTypicalLatency() {
        return TYPICAL_LATENCY;
    }
    
    /**
     * Gets the memory footprint characteristic.
     * 
     * @return the memory footprint description
     */
    public static String getMemoryFootprint() {
        return MEMORY_FOOTPRINT;
    }
    
    /**
     * Gets the key features introduced in this version.
     * 
     * @return array of feature descriptions
     */
    public static String[] getVersionFeatures() {
        return VERSION_FEATURES.clone();
    }
    
    /**
     * Checks if the current Java version meets the minimum requirements.
     * 
     * @return true if Java version is compatible, false otherwise
     */
    public static boolean isJavaVersionCompatible() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) return false;
        
        // Parse major version from java.version property
        String majorVersion;
        if (javaVersion.startsWith("1.")) {
            // Java 8 format: 1.8.0_XXX
            majorVersion = javaVersion.substring(2, 3);
        } else {
            // Java 9+ format: 11.0.1, 17.0.2, etc.
            int dotIndex = javaVersion.indexOf('.');
            if (dotIndex > 0) {
                majorVersion = javaVersion.substring(0, dotIndex);
            } else {
                majorVersion = javaVersion;
            }
        }
        
        try {
            int current = Integer.parseInt(majorVersion);
            int required = Integer.parseInt(MIN_JAVA_VERSION.replace("1.", ""));
            return current >= required;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Gets the current Java runtime version information.
     * 
     * @return formatted Java version string
     */
    public static String getJavaRuntimeInfo() {
        return String.format("Java %s (%s %s)", 
            System.getProperty("java.version", "unknown"),
            System.getProperty("java.vendor", "unknown"),
            System.getProperty("java.vm.name", "unknown"));
    }
    
    /**
     * Gets a complete version information string suitable for logging or display.
     * 
     * @return comprehensive version information
     */
    public static String getVersionInfo() {
        StringBuilder info = new StringBuilder();
        info.append(FRAMEWORK_NAME).append(" ").append(FULL_VERSION).append("\n");
        info.append(FRAMEWORK_DESCRIPTION).append("\n");
        info.append("Build: ").append(BUILD_TIMESTAMP).append("\n");
        info.append("Java: ").append(getJavaRuntimeInfo()).append("\n");
        info.append("Performance: ").append(PEAK_THROUGHPUT).append(", ").append(TYPICAL_LATENCY).append(" latency\n");
        info.append(COPYRIGHT).append("\n");
        info.append("License: ").append(LICENSE).append("\n");
        info.append("Website: ").append(WEBSITE);
        
        return info.toString();
    }
    
    /**
     * Gets a compact banner suitable for application startup.
     * 
     * @return compact version banner
     */
    public static String getBanner() {
        return String.format("%s %s - %s (%s)", 
            FRAMEWORK_NAME, VERSION, FRAMEWORK_DESCRIPTION, BUILD_DATE);
    }
    
    /**
     * Prints version information to standard output.
     * Useful for command-line version checking.
     */
    public static void printVersionInfo() {
        System.out.println(getVersionInfo());
    }
    
    /**
     * Prints a compact banner to standard output.
     * Useful for application startup logging.
     */
    public static void printBanner() {
        System.out.println(getBanner());
    }
    
    /**
     * Main method for command-line version checking.
     * 
     * @param args command line arguments (ignored)
     */
    public static void main(String[] args) {
        if (args.length > 0 && ("--version".equals(args[0]) || "-v".equals(args[0]))) {
            System.out.println(getVersion());
        } else if (args.length > 0 && ("--banner".equals(args[0]) || "-b".equals(args[0]))) {
            printBanner();
        } else {
            printVersionInfo();
            
            // Also show compatibility information
            System.out.println("\nCompatibility:");
            System.out.println("  Minimum Java: " + MIN_JAVA_VERSION);
            System.out.println("  Recommended: " + RECOMMENDED_JAVA_VERSION);
            System.out.println("  Current Java: " + (isJavaVersionCompatible() ? "✓ Compatible" : "✗ Incompatible"));
            
            System.out.println("\nKey Features:");
            for (String feature : VERSION_FEATURES) {
                System.out.println("  • " + feature);
            }
        }
    }
}