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

public class PersistentQueueElementTest {

    @Test
    public void testCreatePersistentQueueElementWithNullBytes() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueElement(1, null, "routing-key", 1000000));
        Assert.assertTrue("bytes".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePersistentQueueElementWithNullRoutingKey() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueElement(1, new byte[]{}, null, 1000000));
        Assert.assertTrue("routingKey".equals(exception.getMessage()));
    }
}
