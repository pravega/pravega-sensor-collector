/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.pravega.sensor.collector.simple.memoryless;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.ObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataCollectorService <R> extends AbstractExecutionThreadService {

    private final String instanceName;
    private final SimpleMemorylessDriver<R> driver;
    private final EventWriter<Object> writer;
    private final long readPeriodicityMs;

    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    public DataCollectorService(String instanceName, SimpleMemorylessDriver<R> driver, EventWriter<Object> writer, long readPeriodicityMs) {
        this.instanceName = instanceName;
        this.driver = driver;
        this.writer = writer;
        this.readPeriodicityMs = readPeriodicityMs;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-"+instanceName;
    }

    @Override
    protected void run() throws InterruptedException {
        for(;;)
        {
            try {
                final long t0 = System.nanoTime();
                // Place blocking read request to get sensor data.
                final R rawData = driver.readRawData();
                long timestamp = 0;
                Object event = driver.getEvent(rawData);
                timestamp = Long.max(timestamp, driver.getTimestamp(rawData));
                //Write the data onto Pravega stream
                writer.writeEvent(driver.getRoutingKey(),event);
                writer.flush();
                writer.commit(timestamp);
                final double ms = (System.nanoTime() - t0) * 1e-6;
                log.info(String.format("Done writing event in %.3f ms to Pravega", ms));
            } catch (Exception e) {
                log.error("Error", e);

            }
            Thread.sleep(readPeriodicityMs);
        }

    }
}
