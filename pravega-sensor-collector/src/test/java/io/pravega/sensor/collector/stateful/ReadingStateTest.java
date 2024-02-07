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

import org.junit.Assert;
import org.junit.Test;

public class ReadingStateTest {

    @Test
    public void testCreateReadingStateWithNullConnection() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new ReadingState(null));
        Assert.assertTrue("connection".equals(exception.getMessage()));
    }
}
