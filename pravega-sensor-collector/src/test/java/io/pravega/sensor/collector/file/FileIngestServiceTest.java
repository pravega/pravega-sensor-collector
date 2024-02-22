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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

/*
* Test class for FileIngestService
*/
public class FileIngestServiceTest {
    
    private static final String PREFIX = Parameters.getEnvPrefix();
    private static final String SEPARATOR = "_";
    protected DeviceDriverConfig config;
    static String filename = "./src/test/resources/RawFileIngestService.properties";
    DeviceDriverConfig deviceDriverConfig;
    Map<String, String> properties;

    private DeviceDriverManager driverManager;

    @BeforeEach
    void setUp(){
        properties = Parameters.getProperties(filename);
        driverManager = new DeviceDriverManager(properties);
        deviceDriverConfig = new DeviceDriverConfig("RAW1", "RawFileIngestService",
                TestUtils.configFromProperties(PREFIX, SEPARATOR, properties), driverManager);
    }

    @Test
    public void testFileIngestService(){
        FileIngestService fileIngestService = new MockFileIngestService(deviceDriverConfig);
        try {
            fileIngestService.startAsync();
            fileIngestService.awaitRunning(Duration.ofSeconds(10));
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(fileIngestService.isRunning());
        Assertions.assertEquals(fileIngestService.getProperty("PERSISTENT_QUEUE_FILE"),
                properties.get(PREFIX + "RAW1" + SEPARATOR + "PERSISTENT_QUEUE_FILE"));
        try {
            fileIngestService.stopAsync();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        Assertions.assertFalse(fileIngestService.isRunning());
    }
}
