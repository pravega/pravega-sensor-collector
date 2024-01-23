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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Parameters {
    private static final Logger log = LoggerFactory.getLogger(Parameters.class);

    private static final String ENV_PREFIX = "PRAVEGA_SENSOR_COLLECTOR_";

    public static String getEnvPrefix() {
        return ENV_PREFIX;
    }

    /**
     * Combines properties from:
     *    1. properties file (if specified)
     *    2. system environment
     *  Values in the system environment will override values in the properties file.
     *  It is intended that properties files only be used when developing in an IDE.
     */
    public static Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        final String fileName = getPropertiesFileName();
        if (!fileName.isEmpty()) {
            log.info("Reading properties from file {}", fileName);
            Properties properties = new Properties();
            try (FileInputStream inputStream = new FileInputStream(fileName)) {
                properties.load(inputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (String key : properties.stringPropertyNames()) {
                map.put(key, properties.getProperty(key));
            }
        }
        map.putAll(System.getenv());
        return map;
    }

    private static String getPropertiesFileName() {
        return getEnvVar(getEnvPrefix() + "PROPERTIES_FILE", "");
    }

    private static String getEnvVar(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
