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

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;

public class PersistentQueueTest {

    @Mock
    private Connection mockConnection;

    @Test
    public void testCreatePersistentQueueWithNullConnection() {
        Exception exception = Assert.assertThrows(RuntimeException.class, () -> new PersistentQueue(null, null, 1));
        Assert.assertTrue(exception.getMessage().contains("connection"));
    }

    @Test
    public void testCreatePersistentQueueWithNullTransactionCoordinator() {
        MockitoAnnotations.initMocks(this);
        Exception exception = Assert.assertThrows(RuntimeException.class, () -> new PersistentQueue(mockConnection, null, 1));
        Assert.assertTrue(exception.getMessage().contains("transactionCoordinator"));
    }
}
