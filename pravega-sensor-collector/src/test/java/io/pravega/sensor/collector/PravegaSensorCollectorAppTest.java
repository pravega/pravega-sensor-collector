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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PravegaSensorCollectorAppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PravegaSensorCollectorAppTest.class);

    @Test
    public void testPravegaSensorCollector() {
        PravegaSensorCollectorApp app = new PravegaSensorCollectorApp();
        Assertions.assertNotNull(app.toString());
    }
}
