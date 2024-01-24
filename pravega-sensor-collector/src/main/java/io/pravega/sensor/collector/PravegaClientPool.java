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

import com.google.common.base.Preconditions;
import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PravegaClientPool implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PravegaClientPool.class);

    private final Map<PravegaClientConfig, ClientConfig> clientConfigs = new HashMap<>();
    private final Map<PravegaClientConfig, EventStreamClientFactory> eventStreamClientFactories = new HashMap<>();

    public synchronized ClientConfig getClientConfig(PravegaClientConfig config) {
        final ClientConfig clientConfig = clientConfigs.get(Preconditions.checkNotNull(config, "pravegaClientConfig"));
        if (clientConfig != null) {
            log.info("Reusing client config for {}", config);
            return clientConfig;
        }
        log.info("Creating new client config for {}", config);
        final ClientConfig newClientConfig = config.toClientConfig();
        clientConfigs.put(config, newClientConfig);
        return newClientConfig;
    }

    public synchronized EventStreamClientFactory getEventStreamClientFactory(PravegaClientConfig config) {
        final EventStreamClientFactory factory = eventStreamClientFactories.get(Preconditions.checkNotNull(config, "pravegaClientConfig"));
        if (factory != null) {
            log.info("Reusing client factory for {}", config);
            return factory;
        }
        log.info("Creating new client factory for {}", config);
        final EventStreamClientFactory newFactory = EventStreamClientFactory.withScope(config.getScopeName(), getClientConfig(config));
        eventStreamClientFactories.put(config, newFactory);
        return newFactory;
    }

    @Override
    public void close() throws Exception {
        eventStreamClientFactories.values().forEach(EventStreamClientFactory::close);
    }
}
