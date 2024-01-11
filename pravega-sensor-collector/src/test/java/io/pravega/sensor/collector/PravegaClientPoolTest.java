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

import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PravegaClientPoolTest {

    @Test
    void testGetClientConfig() {
        PravegaClientPool pravegaClientPool = new PravegaClientPool();
        ClientConfig clientConfig = pravegaClientPool.getClientConfig(new PravegaClientConfig(URI.create("tcp://localhost/"), "scopeName"));
        assertTrue(clientConfig.toString().contains("localhost"));
    }

    @Test
    void testGetEventStreamClientFactory() {
        PravegaClientPool pravegaClientPool = new PravegaClientPool();
        EventStreamClientFactory clientFactory = pravegaClientPool.getEventStreamClientFactory(new PravegaClientConfig(URI.create("tcp://localhost:12345"), "testScope"));
        System.out.println("clientConfig = " + clientFactory.toString());
    }
}