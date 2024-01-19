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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceDriverConfigTest {

    @Test
    void withProperty() {
        Map<String, String> properties = Parameters.getProperties();
        DeviceDriverManager driverManager = new DeviceDriverManager(properties);
        DeviceDriverConfig driverConfig = new DeviceDriverConfig("test", "PRAVEGA_SENSOR_COLLECTOR_", properties, driverManager);
        driverConfig.withProperty("testKey", "testVal");
        assertEquals(driverConfig.getProperties().get("testKey"), "testVal");
        assertEquals(driverConfig.getDeviceDriverManager(), driverManager);
        assertTrue(driverConfig.toString().contains("test"));
    }
}