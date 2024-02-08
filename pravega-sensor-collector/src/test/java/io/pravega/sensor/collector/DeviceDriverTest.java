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

import io.pravega.sensor.collector.accelerometer.AccelerometerDriver;
import io.pravega.sensor.collector.file.csvfile.CsvFileIngestService;
import io.pravega.sensor.collector.file.parquet.ParquetFileIngestService;
import io.pravega.sensor.collector.file.rawfile.RawFileIngestService;
import io.pravega.sensor.collector.leap.LeapDriver;
import io.pravega.sensor.collector.network.NetworkDriver;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class DeviceDriverTest {

    @Test
    public void testCreateAccelerometerDriverWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new AccelerometerDriver(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

    @Test
    public void testCreateLeapDriverWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new LeapDriver(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

    @Test
    public void testCreateNetworkDriverWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () ->  new NetworkDriver(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

    @Test
    public void testCreateCsvFileIngestServiceWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () ->  new CsvFileIngestService(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

    @Test
    public void testCreateParquetFileIngestServiceWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new ParquetFileIngestService(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

    @Test
    public void testCreateRawFileIngestServiceWithNullConfig() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new RawFileIngestService(null));
        Assert.assertTrue("config".equals(exception.getMessage()));
    }

}
