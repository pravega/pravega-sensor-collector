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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class PollResponseTest {

    @Test
    public void testCreatePollResponseWithNullEvents() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PollResponse<String>(null, "state"));
        Assert.assertTrue("events".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePollResponseWithNullState() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PollResponse<String>(new ArrayList<>(), null));
        Assert.assertTrue("state".equals(exception.getMessage()));
    }
}
