/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.simple;

import io.pravega.sensor.collector.MockedConnection;
import io.pravega.sensor.collector.util.EventWriter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.sql.SQLException;

public class PersistentQueueToPravegaServiceTest extends MockedConnection {

    @Mock
    private EventWriter writer;

    private PersistentQueue persistentQueue;

    @BeforeEach
    public void before() throws SQLException {
        super.before();
        persistentQueue = new PersistentQueue(mockConnection, transactionCoordinator, 10);
    }

    @Test
    public void testCreatePersistentQueueToPravegaServiceWithNull() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueToPravegaService(null, persistentQueue, writer, 1, 1));
        Assert.assertTrue("instanceName".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePersistentQueueToPravegaServiceWithNullPersistentQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueToPravegaService("instance-name", null, writer, 1, 1));
        Assert.assertTrue("persistentQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePersistentQueueToPravegaServiceWithNullWriter() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueToPravegaService("instance-name", persistentQueue, null, 1, 1));
        Assert.assertTrue("writer".equals(exception.getMessage()));
    }
}
