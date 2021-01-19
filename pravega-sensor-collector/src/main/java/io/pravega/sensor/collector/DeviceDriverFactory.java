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

public class DeviceDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(DeviceDriverFactory.class);

    /**
     * Instantiate a concrete subclass of DeviceDriver based on key/value properties.
     */
    DeviceDriver create(DeviceDriverConfig config) {
        try {
            log.info("Creating device driver instance {} with class {}", config.getInstanceName(), config.getClassName());
            final Class<?> deviceDriverClass = Class.forName(config.getClassName());
            final DeviceDriver driver = (DeviceDriver) deviceDriverClass.getConstructor(DeviceDriverConfig.class).newInstance(config);
            log.info("Done creating device driver instance {}", config.getInstanceName());
            return driver;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
