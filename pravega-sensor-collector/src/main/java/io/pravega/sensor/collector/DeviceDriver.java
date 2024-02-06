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
import com.google.common.util.concurrent.AbstractService;
import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * This is an abstract class for all device drivers.
 */
public abstract class DeviceDriver extends AbstractService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDriver.class);

    private final DeviceDriverConfig config;

    private static final String CREATE_SCOPE_KEY = "CREATE_SCOPE";

    public DeviceDriver(DeviceDriverConfig config) {
        this.config = Preconditions.checkNotNull(config, "config");
        LOGGER.info("Create Scope: {}", isCreateScope());
    }

    public String getProperty(String key) {
        final String value = config.getProperties().get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format("Missing required parameter {0}", key));
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        return config.getProperties().getOrDefault(key, defaultValue);
    }

    @Override
    public void close() throws Exception {
    }

    private boolean isCreateScope() {
        return Boolean.parseBoolean(getProperty(CREATE_SCOPE_KEY, Boolean.toString(false)));
    }

    private PravegaClientConfig getPravegaClientConfig(String scopeName) {
        return new PravegaClientConfig(config.getProperties(), scopeName);
    }

    protected EventStreamClientFactory getEventStreamClientFactory(String scopeName) {
        return config.getDeviceDriverManager().getPravegaClientPool().getEventStreamClientFactory(getPravegaClientConfig(scopeName));
    }

    private ClientConfig getClientConfig(String scopeName) {
        return config.getDeviceDriverManager().getPravegaClientPool().getClientConfig(getPravegaClientConfig(scopeName));
    }

    protected void createStream(String scopeName, String streamName) {
        try (final StreamManager streamManager = StreamManager.create(getClientConfig(scopeName))) {
            if (isCreateScope()) {
                streamManager.createScope(scopeName);
            }
            final StreamConfiguration streamConfig = StreamConfiguration.builder().build();
            streamManager.createStream(scopeName, streamName, streamConfig);
        }
    }
}
