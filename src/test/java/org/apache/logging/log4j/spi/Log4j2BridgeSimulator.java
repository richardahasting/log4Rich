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

package org.apache.logging.log4j.spi;

import com.log4rich.core.Logger;

/**
 * Test-only helper that simulates a log4j2 bridge class in the
 * {@code org.apache.logging.log4j.spi} package.
 *
 * <p>When {@link com.log4rich.util.LocationInfo#getCaller()} walks the stack,
 * this class's package ({@code org.apache.logging.log4j.}) should be recognized
 * as a logging framework frame and skipped, so the real application caller
 * is reported instead of this simulator.</p>
 *
 * <p>This class is in the test source tree only and is never shipped.</p>
 */
public class Log4j2BridgeSimulator {

    /**
     * Simulates a log4j2 bridge wrapper calling through to log4Rich.
     */
    public static void logViaSimulatedBridge(Logger logger, String message) {
        logger.info(message);
    }

    /**
     * Two-level wrapper to simulate AbstractLogger → Log4RichLogger chain.
     */
    public static void logViaTwoLevelBridge(Logger logger, String message) {
        innerBridgeCall(logger, message);
    }

    private static void innerBridgeCall(Logger logger, String message) {
        logger.info(message);
    }
}
