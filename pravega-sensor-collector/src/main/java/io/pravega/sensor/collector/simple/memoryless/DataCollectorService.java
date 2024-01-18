/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.pravega.sensor.collector.simple.memoryless;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.pravega.sensor.collector.util.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DataCollectorService<R> extends AbstractExecutionThreadService {

    private final String instanceName;
    private final SimpleMemorylessDriver<R> driver;
    private final EventWriter<byte[]> writer;
    private final long readPeriodicityMs;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectorService.class);

    public DataCollectorService(String instanceName, SimpleMemorylessDriver<R> driver, EventWriter<byte[]> writer, long readPeriodicityMs) {
        this.instanceName = instanceName;
        this.driver = driver;
        this.writer = writer;
        this.readPeriodicityMs = readPeriodicityMs;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-" + instanceName;
    }

    @Override
    protected void run() throws InterruptedException {
        for (;;) {
            try {
                final long t0 = System.nanoTime();
                // Place blocking read request to get sensor data.
                final List<R> rawData = driver.readRawData();
                int eventCounter = 0;
                long timestamp = 0;
                for (R dataItr : rawData) {
                    byte[] event = driver.getEvent(dataItr);
                    timestamp = Long.max(timestamp, driver.getTimestamp(dataItr));
                    //Write the data onto Pravega stream
                    writer.writeEvent(driver.getRoutingKey(), event);
                    eventCounter++;
                }
                writer.flush();
                writer.commit(timestamp);
                final double ms = (System.nanoTime() - t0) * 1e-6;
                LOGGER.info(String.format("Done writing %d event in %.3f ms to Pravega", eventCounter, ms));
            } catch (Exception e) {
                LOGGER.error("Error", e);

            }
            Thread.sleep(readPeriodicityMs);
        }

    }
}
