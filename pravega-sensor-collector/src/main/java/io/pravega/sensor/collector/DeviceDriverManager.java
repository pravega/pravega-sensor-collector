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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeviceDriverManager extends AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDriverManager.class);

    private static final String PREFIX = Parameters.getEnvPrefix();
    private static final String SEPARATOR = "_";
    private static final String CLASS_KEY = "CLASS";

    private final List<DeviceDriverConfig> configs;
    private final PravegaClientPool pravegaClientPool = new PravegaClientPool();
    private List<DeviceDriver> drivers;

    public DeviceDriverManager(Map<String, String> properties) {
        configs = configFromProperties(PREFIX, SEPARATOR, Preconditions.checkNotNull(properties, "properties"));
    }

    @Override
    protected void doStart() {
        LOGGER.info("Starting device drivers");
        final DeviceDriverFactory factory = new DeviceDriverFactory();
        drivers = configs.stream().map(factory::create).collect(Collectors.toList());
        drivers.stream().forEach((driver) -> driver.startAsync());
        drivers.stream().forEach((driver) -> driver.awaitRunning());
        LOGGER.info("All device drivers started successfully");
        notifyStarted();
    }

    @Override
    protected void doStop() {
        drivers.stream().forEach((driver) -> driver.stopAsync());
        drivers.stream().forEach((driver) -> driver.awaitTerminated());
        drivers = null;
    }

    /**
     * Returns a list of DeviceDriverConfig instances from key/value properties.
     * A key such as PRAVEGA_SENSOR_COLLECTOR_NET1_CLASS is parsed as identifying the Java CLASS
     * for the instance named NET1. The string in the position of NET1 is arbitrary.
     * Multiple instances can be specified by specifying additional classes such as
     * PRAVEGA_SENSOR_COLLECTOR_ACCELEROMETER1_CLASS.
     * Properties that do not have an instance name (e.g. PRAVEGA_SENSOR_COLLECTOR_PRAVEGA_CONTROLLER_URI)
     * will be applied to all instances.
     *
     * @param prefix All keys must be prefixed with this value.
     * @param sep The character that separates the prefix, instance name, and the rest of the key.
     * @param properties A mapping of key/value properties.
     *                   This will usually come from the system environment.
     * @return A list of DeviceDriverConfig. The properties map omits the prefix and the instance name.
     */
    private List<DeviceDriverConfig> configFromProperties(String prefix, String sep, Map<String, String> properties) {
        // Find instance names.
        final List<String> instanceNames = properties.keySet().stream().flatMap((key) -> {
            if (key.startsWith(prefix) && key.endsWith(sep + CLASS_KEY)) {
                return Stream.of(key.substring(prefix.length(), key.length() - CLASS_KEY.length() - sep.length()));
            }
            return Stream.empty();
        }).collect(Collectors.toList());
        LOGGER.debug("configFromProperties: instanceNames={}", instanceNames);
        // Copy properties with prefix to keys without a prefix.
        final List<DeviceDriverConfig> config = instanceNames.stream().map((instanceName) -> {
            final String className = properties.get(prefix + instanceName + sep + CLASS_KEY);
            assert (className != null);
            final Map<String, String> instanceProperties = new HashMap<>();
            properties.entrySet().stream().forEach((entry) -> {
                final String key = entry.getKey();
                final String instancePrefix = prefix + instanceName + sep;
                if (key.startsWith(instancePrefix)) {
                    instanceProperties.put(key.substring(instancePrefix.length()), entry.getValue());
                } else if (key.startsWith(prefix)) {
                    instanceProperties.put(key.substring(prefix.length()), entry.getValue());
                }
            });
            return new DeviceDriverConfig(instanceName, className, instanceProperties, this);
        }).collect(Collectors.toList());
        LOGGER.info("configFromProperties: config={}", config);
        return config;
    }

    public PravegaClientPool getPravegaClientPool() {
        return pravegaClientPool;
    }
}
