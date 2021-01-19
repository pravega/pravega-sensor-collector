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
import io.pravega.sensor.collector.util.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Read raw data from the memory queue, decode it to Samples, serialize to JSON, and write to the persistent queue.
 * Serialized JSON bytes will be exactly what gets written to Pravega.
 */
public class MemoryQueueToPersistentQueueService<R, S extends Samples> extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(MemoryQueueToPersistentQueueService.class);

    private final String instanceName;
    private final BlockingQueue<R> memoryQueue;
    private final PersistentQueue persistentQueue;
    private final SimpleDeviceDriver<R, S> driver;
    private final int samplesPerEvent;

    public MemoryQueueToPersistentQueueService(String instanceName, BlockingQueue<R> memoryQueue, PersistentQueue persistentQueue, SimpleDeviceDriver<R, S> driver, int samplesPerEvent) {
        this.instanceName = instanceName;
        this.memoryQueue = memoryQueue;
        this.persistentQueue = persistentQueue;
        this.driver = driver;
        this.samplesPerEvent = samplesPerEvent;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-" + instanceName;
    }

    @Override
    protected void run() throws Exception {
        log.info("Running");
        final Statistics statsSinceStart = new Statistics("Since start");
        int warmupEvents = 1;
        for (;;) {
            try {
                final S samples = driver.createSamples();
                while (samples.size() < samplesPerEvent) {
                    final R rawSensorData = memoryQueue.poll(10, TimeUnit.SECONDS);
                    if (rawSensorData == null) {
                        log.warn("Timeout reading from sensor device (underflow)");
                    } else {
                        driver.decodeRawDataToSamples(samples, rawSensorData);
                    }
                }

                // Do not include first event in statistics because the FIFO buffer may have had some very old samples.
                if (warmupEvents == 0) {
                    final Statistics statsForEvent = new Statistics("For event");
                    samples.getTimestampNanos().forEach(statsForEvent::addTimestampNanos);
                    samples.getTimestampNanos().forEach(statsSinceStart::addTimestampNanos);
                    statsForEvent.logStatistics();
                    statsSinceStart.logStatistics();
                } else {
                    warmupEvents--;
                }

                final byte[] eventBytes = driver.serializeSamples(samples);
                final String routingKey = driver.getRoutingKey(samples);
                final long timestamp = driver.getTimestamp(samples);
                final PersistentQueueElement element = new PersistentQueueElement(eventBytes, routingKey, timestamp);
                log.trace("Adding element {}", element);
                persistentQueue.add(element);
            } catch (Exception e) {
                log.error("Error", e);
                Thread.sleep(10000);
                // Continue on any errors. We will retry on the next iteration.
            }
        }
    }
}
