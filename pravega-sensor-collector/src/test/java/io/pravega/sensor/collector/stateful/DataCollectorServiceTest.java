/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.stateful;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.MockedConnection;
import io.pravega.sensor.collector.simple.PersistentQueue;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;

public class DataCollectorServiceTest extends MockedConnection {

    private PersistentQueue persistentQueue;

    private DeviceDriverConfig config;

    @BeforeEach
    public void setUp() throws SQLException {
        super.before();
        persistentQueue = new PersistentQueue(mockConnection, transactionCoordinator, 10);
        config = new DeviceDriverConfig("ins", "DeviceDriverConfig", new HashMap<>(), new DeviceDriverManager(new HashMap<>()));
    }

    @Test
    public void testCreateDataCollectorServiceTestWithNullInstanceName() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>(null, persistentQueue, null));
        Assert.assertTrue("instanceName".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceTestWithNullPersistentQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>("instance", null, null));
        Assert.assertTrue("persistentQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceTestWithNullDriver() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>("instance", persistentQueue, null));
        Assert.assertTrue("driver".equals(exception.getMessage()));
    }
}
