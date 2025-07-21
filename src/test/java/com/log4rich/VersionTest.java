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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for version information and compatibility checking.
 * 
 * @author log4Rich Contributors
 * @since 1.0.0
 */
public class VersionTest {
    
    @Test
    void testVersionFormat() {
        String version = Version.getVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
        assertEquals("1.0.2", version);
    }
    
    @Test
    void testVersionComponents() {
        assertEquals("1", Version.getMajorVersion());
        assertEquals("0", Version.getMinorVersion());
        assertEquals("2", Version.getPatchVersion());
    }
    
    @Test
    void testFullVersion() {
        String fullVersion = Version.getFullVersion();
        assertNotNull(fullVersion);
        assertTrue(fullVersion.startsWith("1.0.2"));
        assertTrue(fullVersion.contains("RELEASE"));
    }
    
    @Test
    void testBuildInformation() {
        assertNotNull(Version.getBuildDate());
        assertNotNull(Version.getBuildTime());
        assertNotNull(Version.getBuildTimestamp());
        
        // Build date should be in YYYY-MM-DD format
        assertTrue(Version.getBuildDate().matches("\\d{4}-\\d{2}-\\d{2}"));
        
        // Build time should contain time information
        assertTrue(Version.getBuildTime().contains(":"));
    }
    
    @Test
    void testFrameworkInformation() {
        assertEquals("log4Rich", Version.getFrameworkName());
        assertEquals("Ultra-High-Performance Java Logging Framework", Version.getFrameworkDescription());
        assertTrue(Version.getCopyright().contains("log4Rich Contributors"));
        assertEquals("Apache License 2.0", Version.getLicense());
        assertTrue(Version.getWebsite().contains("github.com"));
    }
    
    @Test
    void testJavaCompatibility() {
        assertEquals("1.8", Version.getMinJavaVersion());
        assertEquals("11+", Version.getRecommendedJavaVersion());
        
        // Current Java should be compatible since we're running tests
        assertTrue(Version.isJavaVersionCompatible());
        
        // Runtime info should contain version information
        String runtimeInfo = Version.getJavaRuntimeInfo();
        assertNotNull(runtimeInfo);
        assertTrue(runtimeInfo.toLowerCase().contains("java"));
    }
    
    @Test
    void testPerformanceMetrics() {
        assertNotNull(Version.getPeakThroughput());
        assertNotNull(Version.getTypicalLatency());
        assertNotNull(Version.getMemoryFootprint());
        
        assertTrue(Version.getPeakThroughput().contains("messages/second"));
        assertTrue(Version.getTypicalLatency().toLowerCase().contains("microsecond"));
    }
    
    @Test
    void testVersionFeatures() {
        String[] features = Version.getVersionFeatures();
        assertNotNull(features);
        assertTrue(features.length > 0);
        
        // Check for key features
        boolean hasAsyncCompression = false;
        boolean hasRingBuffers = false;
        boolean hasMemoryMapped = false;
        
        for (String feature : features) {
            if (feature.toLowerCase().contains("async") && feature.toLowerCase().contains("compression")) {
                hasAsyncCompression = true;
            }
            if (feature.toLowerCase().contains("ring buffer")) {
                hasRingBuffers = true;
            }
            if (feature.toLowerCase().contains("memory-mapped")) {
                hasMemoryMapped = true;
            }
        }
        
        assertTrue(hasAsyncCompression, "Should include async compression feature");
        assertTrue(hasRingBuffers, "Should include ring buffer feature");
        assertTrue(hasMemoryMapped, "Should include memory-mapped feature");
    }
    
    @Test
    void testVersionInfo() {
        String versionInfo = Version.getVersionInfo();
        assertNotNull(versionInfo);
        assertFalse(versionInfo.isEmpty());
        
        // Should contain key information
        assertTrue(versionInfo.contains("log4Rich"));
        assertTrue(versionInfo.contains("1.0.2"));
        assertTrue(versionInfo.contains("Build:"));
        assertTrue(versionInfo.contains("Java:"));
        assertTrue(versionInfo.contains("Performance:"));
        assertTrue(versionInfo.contains("Copyright"));
        assertTrue(versionInfo.contains("License:"));
        assertTrue(versionInfo.contains("Website:"));
    }
    
    @Test
    void testBanner() {
        String banner = Version.getBanner();
        assertNotNull(banner);
        assertFalse(banner.isEmpty());
        
        // Should be compact and contain essential information
        assertTrue(banner.contains("log4Rich"));
        assertTrue(banner.contains("1.0"));
        assertTrue(banner.contains("Ultra-High-Performance"));
        assertTrue(banner.contains("2025"));
    }
    
    @Test
    void testLog4RichVersionMethods() {
        // Test version methods in main Log4Rich class
        assertEquals(Version.getVersion(), Log4Rich.getVersion());
        assertEquals(Version.getVersionInfo(), Log4Rich.getVersionInfo());
        assertEquals(Version.getBanner(), Log4Rich.getBanner());
        assertEquals(Version.isJavaVersionCompatible(), Log4Rich.isJavaVersionCompatible());
    }
    
    @Test
    void testVersionConstants() {
        // Ensure version is consistent with what we expect for 1.0.2
        assertEquals("1.0.2", Version.getVersion());
        assertEquals("RELEASE", Version.getBuildType());
        assertEquals("2025-07-19", Version.getBuildDate());
    }
    
    @Test
    void testVersionUtilityClass() {
        // Version should not be instantiable due to private constructor
        assertThrows(IllegalAccessException.class, () -> {
            Version.class.getDeclaredConstructor().newInstance();
        });
    }
    
    @Test
    void testCommandLineVersionChecking() {
        // Test that main method handles version arguments
        // This is more of a smoke test since we can't easily capture stdout
        assertDoesNotThrow(() -> {
            Version.main(new String[]{"--version"});
            Version.main(new String[]{"--banner"});
            Version.main(new String[]{});
        });
        
        assertDoesNotThrow(() -> {
            Log4Rich.main(new String[]{"--version"});
            Log4Rich.main(new String[]{"--banner"});
            Log4Rich.main(new String[]{"--info"});
        });
    }
}