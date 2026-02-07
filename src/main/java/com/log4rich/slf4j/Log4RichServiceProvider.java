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

package com.log4rich.slf4j;

import com.log4rich.Version;
import com.log4rich.core.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J 2.x Service Provider implementation for log4Rich.
 *
 * <p>This class is discovered via Java ServiceLoader mechanism.
 * It provides the entry point for SLF4J to use log4Rich as its backend.</p>
 *
 * <h2>Setup:</h2>
 * <p>To use log4Rich with SLF4J, include both log4Rich and slf4j-api in your classpath.
 * SLF4J will automatically discover and use this service provider.</p>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 * @see org.slf4j.spi.SLF4JServiceProvider
 */
public class Log4RichServiceProvider implements SLF4JServiceProvider {

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified during build time via version property.
     */
    public static final String REQUESTED_API_VERSION = "2.0.99";

    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    /**
     * Returns the requested SLF4J API version.
     *
     * @return the requested API version
     */
    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    /**
     * Initializes this service provider.
     * Called by SLF4J during bootstrap.
     */
    @Override
    public void initialize() {
        // Initialize the logging system (loads config, creates default appenders)
        LogManager.initialize();

        loggerFactory = new Log4RichLoggerFactory();
        markerFactory = new BasicMarkerFactory();
        mdcAdapter = new NOPMDCAdapter();

        System.out.println("[log4Rich] SLF4J binding initialized - log4Rich " + Version.getVersion());
    }

    /**
     * Returns the logger factory for this provider.
     *
     * @return the logger factory
     */
    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    /**
     * Returns the marker factory for this provider.
     *
     * @return the marker factory
     */
    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    /**
     * Returns the MDC adapter for this provider.
     * Currently returns a NOP adapter as log4Rich doesn't support MDC.
     *
     * @return the MDC adapter
     */
    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }
}
