/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 Main App to start the Watchdog service
 for Pravega Sensor Collector(PSC).
 */
public class PscWatchdogApp {
    private static final Logger log = LoggerFactory.getLogger(PscWatchdogApp.class);

    public static void main(String[] args) {
        try {
            log.info("Watchdog starting");
            final Map<String, String> properties = System.getenv();
            log.debug("Properties: {}", properties);
            final WatchDogService service = new WatchDogService(properties);
            service.startAsync();
            service.awaitTerminated();
        } catch (Exception e) {
            log.error("Error starting Watchdog ", e);
            System.exit(1);
        }
    }
}
