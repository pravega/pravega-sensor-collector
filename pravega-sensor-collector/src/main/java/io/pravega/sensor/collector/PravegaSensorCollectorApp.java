/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PravegaSensorCollectorApp {
    private static final Logger log = LoggerFactory.getLogger(PravegaSensorCollectorApp.class);

    public static void main(String[] args) {
        try {
            log.info("Collector starting");
            final Map<String, String> properties = Parameters.getProperties();
            log.debug("Properties: {}", properties);
            final DeviceDriverManager deviceDriverManager = new DeviceDriverManager(properties);
            deviceDriverManager.startAsync();
            deviceDriverManager.awaitTerminated();
        } catch (Exception e) {
            log.error("Fatal Error", e);
            System.exit(2);
        }
    }
}
