/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.stateful;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.PersistentQueue;
import io.pravega.sensor.collector.simple.PersistentQueueToPravegaService;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.PersistentId;
import io.pravega.sensor.collector.util.TransactionCoordinator;

abstract public class StatefulSensorDeviceDriver<S> extends DeviceDriver {
    private static final Logger log = LoggerFactory.getLogger(StatefulSensorDeviceDriver.class);

    private static final String PERSISTENT_QUEUE_FILE_KEY = "PERSISTENT_QUEUE_FILE";
    private static final String PERSISTENT_QUEUE_CAPACITY_EVENTS_KEY = "PERSISTENT_QUEUE_CAPACITY_EVENTS";
    private static final String SCOPE_KEY = "SCOPE";
    private static final String STREAM_KEY = "STREAM";
    private static final String ROUTING_KEY_KEY = "ROUTING_KEY";
    private static final String MAX_EVENTS_PER_WRITE_BATCH_KEY = "MAX_EVENTS_PER_WRITE_BATCH";
    private static final String DELAY_BETWEEN_WRITE_BATCHES_MS_KEY = "DELAY_BETWEEN_WRITE_BATCHES_MS";
    private static final String EXACTLY_ONCE_KEY = "EXACTLY_ONCE";
    private static final String TRANSACTION_TIMEOUT_MINUTES_KEY = "TRANSACTION_TIMEOUT_MINUTES";
    private static final String ENABLE_LARGE_EVENT = "ENABLE_LARGE_EVENT";

    private final String routingKey;
    private final DataCollectorService<S> dataCollectorService;
    private final PersistentQueue persistentQueue;
    private final EventStreamClientFactory clientFactory;
    private final EventWriter<byte[]> writer;
    private final PersistentQueueToPravegaService persistentQueueToPravegaService;

    public StatefulSensorDeviceDriver(DeviceDriverConfig config) {
        super(config);

        final String persistentQueueFileName = getPersistentQueueFileName();
        final int persistentQueueCapacityEvents = getPersistentQueueCapacityEvents();
        log.info("Persistent Queue File: {}", persistentQueueFileName);
        log.info("Persistent Queue Capacity Events: {}", persistentQueueCapacityEvents);

        final String scopeName = getScopeName();
        final String streamName = getStreamName();
        routingKey = getRoutingKey();
        final int maxEventsPerWriteBatch = getMaxEventsPerWriteBatch();
        final long delayBetweenWriteBatchesMs = getDelayBetweenWriteBatchesMs();
        final boolean exactlyOnce = getExactlyOnce();
        final double transactionTimeoutMinutes = getTransactionTimeoutMinutes();
        log.info("Stream: {}/{}", scopeName, streamName);
        log.info("Routing Key: {}", routingKey);
        log.info("Max Events Per Write Batch: {}", maxEventsPerWriteBatch);
        log.info("Delay Between Write Batches: {} ms", delayBetweenWriteBatchesMs);
        log.info("Exactly Once: {}", exactlyOnce);
        log.info("Transaction Timeout: {} minutes", transactionTimeoutMinutes);

        createStream(scopeName, streamName);

        final Connection connection = PersistentQueue.createDatabase(getPersistentQueueFileName());

        final String writerId = new PersistentId(connection).getPersistentId().toString();
        log.info("Writer ID: {}", writerId);

        clientFactory = getEventStreamClientFactory(scopeName);

        writer = EventWriter.create(
            clientFactory,
            writerId,
            streamName,
            new ByteArraySerializer(),
            EventWriterConfig.builder()
                    .enableConnectionPooling(true)
                    .retryAttempts(Integer.MAX_VALUE)
                    .transactionTimeoutTime((long) (transactionTimeoutMinutes * 60.0 * 1000.0))
                    .enableLargeEvents(getLargeEventEnable())
                    .build(),
            exactlyOnce);

        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, writer);

        persistentQueue = new PersistentQueue(connection, transactionCoordinator, persistentQueueCapacityEvents);

        persistentQueueToPravegaService = new PersistentQueueToPravegaService(
                config.getInstanceName(),
                persistentQueue,
                writer,
                maxEventsPerWriteBatch,
                delayBetweenWriteBatchesMs);

        dataCollectorService = new DataCollectorService<>(config.getInstanceName(), persistentQueue, this);
    }

    int getPersistentQueueCapacityEvents() {
        return Integer.parseInt(getProperty(PERSISTENT_QUEUE_CAPACITY_EVENTS_KEY, Integer.toString(1000 * 1000)));
    }

    String getPersistentQueueFileName() {
        return getProperty(PERSISTENT_QUEUE_FILE_KEY);
    }

    protected String getRoutingKey() {
        return getProperty(ROUTING_KEY_KEY, "");
    }

    int getMaxEventsPerWriteBatch() {
        return Integer.parseInt(getProperty(MAX_EVENTS_PER_WRITE_BATCH_KEY, Integer.toString(100)));
    }

    long getDelayBetweenWriteBatchesMs() {
        return Long.parseLong(getProperty(DELAY_BETWEEN_WRITE_BATCHES_MS_KEY, Long.toString(1000)));
    }

    boolean getExactlyOnce() {
        return Boolean.parseBoolean(getProperty(EXACTLY_ONCE_KEY, Boolean.toString(true)));
    }

    /**
     * This time duration must not exceed the controller property controller.transaction.maxLeaseValue (milliseconds).
     */
    double getTransactionTimeoutMinutes() {
        return Double.parseDouble(getProperty(TRANSACTION_TIMEOUT_MINUTES_KEY, Double.toString(18.0 * 60.0)));
    }

    protected String getScopeName() {
        return getProperty(SCOPE_KEY);
    }

    String getStreamName() {
        return getProperty(STREAM_KEY);
    }

    boolean getLargeEventEnable() {
        return Boolean.parseBoolean(getProperty(ENABLE_LARGE_EVENT, Boolean.toString(false)));
    }

    @Override
    protected void doStart() {
        persistentQueueToPravegaService.startAsync();
        persistentQueueToPravegaService.awaitRunning();
        dataCollectorService.startAsync();
        dataCollectorService.awaitRunning();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        dataCollectorService.stopAsync();
        dataCollectorService.awaitTerminated();
        persistentQueueToPravegaService.stopAsync();
        persistentQueueToPravegaService.awaitTerminated();
    }

    @Override
    public void close() throws Exception {
        writer.close();
        clientFactory.close();
        persistentQueue.close();
    }

    abstract public S initialState();

    abstract public PollResponse<S> pollEvents(S state) throws Exception;
}
