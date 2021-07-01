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

import java.util.Map;

public class DeviceDriverConfig {
    private final String instanceName;
    private final String className;
    private final Map<String, String> properties;
    private final DeviceDriverManager deviceDriverManager;

    public DeviceDriverConfig(String instanceName, String className, Map<String, String> properties, DeviceDriverManager deviceDriverManager) {
        this.instanceName = instanceName;
        this.className = className;
        this.properties = properties;
        this.deviceDriverManager = deviceDriverManager;
    }

    @Override
    public String toString() {
        return "DeviceDriverConfig{" +
                "instanceName='" + instanceName + '\'' +
                ", className='" + className + '\'' +
                ", properties=" + properties +
                '}';
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public DeviceDriverManager getDeviceDriverManager() {
        return deviceDriverManager;
    }

    public DeviceDriverConfig withProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }
}
