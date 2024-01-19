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

import com.google.common.util.concurrent.Service;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeviceDriverManagerTest {

    @Test
    void doStart() {
        Map<String, String> properties = Parameters.getProperties();
        DeviceDriverManager driverManager = new DeviceDriverManager(properties);
        assertEquals(driverManager.state(), Service.State.NEW);

        driverManager.startAsync();
        assertEquals(driverManager.state(), Service.State.RUNNING);

        driverManager.stopAsync();
        assertFalse(driverManager.isRunning());

    }

    @Test
    void doStartWithProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("PRAVEGA_SENSOR_COLLECTOR_ACCEL1_CREATE_SCOPE", "true");
        DeviceDriverManager driverManager = new DeviceDriverManager(properties);
        assertEquals(driverManager.state(), Service.State.NEW);

        driverManager.startAsync();
        assertEquals(driverManager.state(), Service.State.RUNNING);

        driverManager.stopAsync();
        assertFalse(driverManager.isRunning());

    }
}
