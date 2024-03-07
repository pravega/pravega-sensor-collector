/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.Parameters;
import io.pravega.sensor.collector.util.TestUtils;
import org.apache.hadoop.yarn.client.api.impl.FileSystemTimelineWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;


/*
* Test class for FileIngestService
*/
public class FileIngestServiceTest {

    static final String FILE_NAME = "./src/test/resources/RawFileIngestService.properties";
    private static final String PREFIX = Parameters.getEnvPrefix();
    private static final String SEPARATOR = "_";
    DeviceDriverConfig config;
    DeviceDriverConfig deviceDriverConfig;
    Map<String, String> properties;

    private DeviceDriverManager driverManager;

    @BeforeEach
    void setUp() throws IOException {
        properties = Parameters.getProperties(FILE_NAME);
        Files.deleteIfExists(Paths.get(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE")));
        driverManager = new DeviceDriverManager(properties);
        deviceDriverConfig = new DeviceDriverConfig("RAW1", "RawFileIngestService",
                TestUtils.configFromProperties(PREFIX, SEPARATOR, properties), driverManager);
    }

    @AfterEach
    public void tearDown() {
        try {
            Files.deleteIfExists(Paths.get(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        properties = null;
    }

    @Test
    public void testFileIngestService() {
        FileIngestService fileIngestService = new MockFileIngestService(deviceDriverConfig);
        try {
            fileIngestService.startAsync();
            fileIngestService.awaitRunning(Duration.ofSeconds(10));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(fileIngestService.isRunning());
        Assertions.assertEquals(fileIngestService.getProperty("PERSISTENT_QUEUE_FILE"),
                properties.get(PREFIX + "RAW1" + SEPARATOR + "PERSISTENT_QUEUE_FILE"));
        try {
            fileIngestService.stopAsync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assertions.assertFalse(fileIngestService.isRunning());
    }
}
