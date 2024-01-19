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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametersTest {

    @Test
    void getEnvPrefix() {
        assertEquals(Parameters.getEnvPrefix(), "PRAVEGA_SENSOR_COLLECTOR_");
    }

    @Test
    void testGetProperties() {
        Map<String, String>  parameters = Parameters.getProperties();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry: parameters.entrySet()) {
            list.add(entry.getKey());
        }
        assertTrue(list.contains("DESKTOP_SESSION"));
    }
}
