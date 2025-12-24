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

package com.log4rich.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Manages JMX registration for log4Rich MBeans.
 *
 * <p>This class provides utility methods to register and unregister log4Rich
 * management beans with the platform MBean server.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Enable JMX monitoring
 * JMXManager.register();
 *
 * // Later, to clean up
 * JMXManager.unregister();
 * }</pre>
 *
 * <h2>JConsole/VisualVM Access:</h2>
 * <p>Once registered, the MBean is accessible at:</p>
 * <pre>com.log4rich:type=Log4Rich</pre>
 *
 * @author log4Rich Contributors
 * @since 1.0.5
 */
public class JMXManager {

    private static final String OBJECT_NAME = "com.log4rich:type=Log4Rich";
    private static volatile boolean registered = false;
    private static ObjectName objectName;

    private JMXManager() {
        // Utility class
    }

    /**
     * Registers the log4Rich MBean with the platform MBean server.
     *
     * <p>After registration, log4Rich can be monitored and managed through
     * JConsole, VisualVM, or any JMX-compatible monitoring tool.</p>
     *
     * @return true if registration was successful
     */
    public static synchronized boolean register() {
        if (registered) {
            return true;
        }

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            objectName = new ObjectName(OBJECT_NAME);

            // Unregister if already exists (e.g., from previous run in same JVM)
            if (mbs.isRegistered(objectName)) {
                mbs.unregisterMBean(objectName);
            }

            mbs.registerMBean(Log4RichMXBeanImpl.getInstance(), objectName);
            registered = true;

            System.out.println("[log4Rich] JMX MBean registered: " + OBJECT_NAME);
            return true;

        } catch (Exception e) {
            System.err.println("[log4Rich] Failed to register JMX MBean: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregisters the log4Rich MBean from the platform MBean server.
     */
    public static synchronized void unregister() {
        if (!registered || objectName == null) {
            return;
        }

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(objectName)) {
                mbs.unregisterMBean(objectName);
            }
            registered = false;
            System.out.println("[log4Rich] JMX MBean unregistered: " + OBJECT_NAME);

        } catch (Exception e) {
            System.err.println("[log4Rich] Failed to unregister JMX MBean: " + e.getMessage());
        }
    }

    /**
     * Checks if the JMX MBean is currently registered.
     *
     * @return true if registered
     */
    public static boolean isRegistered() {
        return registered;
    }

    /**
     * Gets the ObjectName used for the log4Rich MBean.
     *
     * @return the ObjectName string
     */
    public static String getObjectName() {
        return OBJECT_NAME;
    }

    /**
     * Gets the MBean implementation instance.
     *
     * <p>This can be used to directly record statistics or access configuration
     * without going through JMX.</p>
     *
     * @return the MBean implementation
     */
    public static Log4RichMXBeanImpl getMBean() {
        return Log4RichMXBeanImpl.getInstance();
    }
}
