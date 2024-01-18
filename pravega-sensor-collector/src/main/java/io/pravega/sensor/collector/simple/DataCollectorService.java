/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.simple;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Read raw data from a device and send it to the memory queue.
 */
public class DataCollectorService<R, S extends Samples> extends AbstractExecutionThreadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectorService.class);

    private final String instanceName;
    private final BlockingQueue<R> memoryQueue;
    private final SimpleDeviceDriver<R, S> driver;

    public DataCollectorService(String instanceName, BlockingQueue<R> memoryQueue, SimpleDeviceDriver<R, S> driver) {
        this.instanceName = instanceName;
        this.memoryQueue = memoryQueue;
        this.driver = driver;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-" + instanceName;
    }

    @Override
    protected void run() throws Exception {
        LOGGER.info("Running");
        for (;;) {
            try {
                final R rawSensorData = driver.readRawData();
                if (!memoryQueue.offer(rawSensorData, 10, TimeUnit.SECONDS)) {
                    LOGGER.warn("Memory queue is full. Data will be discarded. Writing to persistent queue took too long.");
                }
            } catch (EOFException e) {
                LOGGER.warn("EOF");
                Thread.sleep(1000);
            } catch (Exception e) {
                LOGGER.error("Error", e);
                Thread.sleep(10000);
                // Continue on any errors. We will retry on the next iteration.
            }
        }
    }
}
