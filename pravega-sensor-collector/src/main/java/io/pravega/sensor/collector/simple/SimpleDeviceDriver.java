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

import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.PersistentId;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is an abstract class that uses a memory queue and a SQLite persistent queue, and writes to a single Pravega stream.
 */
public abstract class SimpleDeviceDriver<R, S extends Samples> extends DeviceDriver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDeviceDriver.class);

    private static final String MEMORY_QUEUE_CAPACITY_ELEMENTS_KEY = "MEMORY_QUEUE_CAPACITY_ELEMENTS";

    private static final String PERSISTENT_QUEUE_FILE_KEY = "PERSISTENT_QUEUE_FILE";
    private static final String PERSISTENT_QUEUE_CAPACITY_EVENTS_KEY = "PERSISTENT_QUEUE_CAPACITY_EVENTS";
    protected static final String SAMPLES_PER_EVENT_KEY = "SAMPLES_PER_EVENT";

    private static final String SCOPE_KEY = "SCOPE";
    private static final String STREAM_KEY = "STREAM";
    private static final String ROUTING_KEY_KEY = "ROUTING_KEY";
    private static final String MAX_EVENTS_PER_WRITE_BATCH_KEY = "MAX_EVENTS_PER_WRITE_BATCH";
    private static final String DELAY_BETWEEN_WRITE_BATCHES_MS_KEY = "DELAY_BETWEEN_WRITE_BATCHES_MS";
    private static final String EXACTLY_ONCE_KEY = "EXACTLY_ONCE";
    private static final String TRANSACTION_TIMEOUT_MINUTES_KEY = "TRANSACTION_TIMEOUT_MINUTES";
    private static final String ENABLE_LARGE_EVENT = "ENABLE_LARGE_EVENT";

    private final String routingKey;
    private final DataCollectorService<R, S> dataCollectorService;
    private final PersistentQueue persistentQueue;
    private final MemoryQueueToPersistentQueueService<R, S> memoryQueueToPersistentQueueService;
    private final EventStreamClientFactory clientFactory;
    private final EventWriter<byte[]> writer;
    private final PersistentQueueToPravegaService persistentQueueToPravegaService;

    public SimpleDeviceDriver(DeviceDriverConfig config) {
        super(config);

        final int memoryQueueCapacityElements = getMemoryQueueCapacityElements();
        LOGGER.info("Memory Queue Capacity: {} elements", memoryQueueCapacityElements);

        final String persistentQueueFileName = getPersistentQueueFileName();
        final int persistentQueueCapacityEvents = getPersistentQueueCapacityEvents();
        final int samplesPerEvent = getSamplesPerEvent();
        LOGGER.info("Persistent Queue File: {}", persistentQueueFileName);
        LOGGER.info("Persistent Queue Capacity Events: {}", persistentQueueCapacityEvents);
        LOGGER.info("Samples Per Event: {}", samplesPerEvent);

        final String scopeName = getScopeName();
        final String streamName = getStreamName();
        routingKey = getRoutingKey();
        final int maxEventsPerWriteBatch = getMaxEventsPerWriteBatch();
        final long delayBetweenWriteBatchesMs = getDelayBetweenWriteBatchesMs();
        final boolean exactlyOnce = getExactlyOnce();
        final double transactionTimeoutMinutes = getTransactionTimeoutMinutes();
        LOGGER.info("Stream: {}/{}", scopeName, streamName);
        LOGGER.info("Routing Key: {}", routingKey);
        LOGGER.info("Max Events Per Write Batch: {}", maxEventsPerWriteBatch);
        LOGGER.info("Delay Between Write Batches: {} ms", delayBetweenWriteBatchesMs);
        LOGGER.info("Exactly Once: {}", exactlyOnce);
        LOGGER.info("Transaction Timeout: {} minutes", transactionTimeoutMinutes);

        final BlockingQueue<R> memoryQueue = new LinkedBlockingQueue<>(memoryQueueCapacityElements);

        dataCollectorService = new DataCollectorService<>(config.getInstanceName(), memoryQueue, this);

        createStream(scopeName, streamName);

        final Connection connection = PersistentQueue.createDatabase(getPersistentQueueFileName());

        final String writerId = new PersistentId(connection).getPersistentId().toString();
        LOGGER.info("Writer ID: {}", writerId);

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

        memoryQueueToPersistentQueueService = new MemoryQueueToPersistentQueueService<>(
                config.getInstanceName(),
                memoryQueue,
                persistentQueue,
                this,
                samplesPerEvent);

        persistentQueueToPravegaService = new PersistentQueueToPravegaService(
                config.getInstanceName(),
                persistentQueue,
                writer,
                maxEventsPerWriteBatch,
                delayBetweenWriteBatchesMs);
    }

    int getMemoryQueueCapacityElements() {
        return Integer.parseInt(getProperty(MEMORY_QUEUE_CAPACITY_ELEMENTS_KEY, Integer.toString(100)));
    }

    int getPersistentQueueCapacityEvents() {
        return Integer.parseInt(getProperty(PERSISTENT_QUEUE_CAPACITY_EVENTS_KEY, Integer.toString(1000 * 1000)));
    }

    int getSamplesPerEvent() {
        return Integer.parseInt(getProperty(SAMPLES_PER_EVENT_KEY, Integer.toString(100)));
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

    boolean getLargeEventEnable() {
        return Boolean.parseBoolean(getProperty(ENABLE_LARGE_EVENT, Boolean.toString(false)));
    }

    /**
     * This time duration must not exceed the controller property controller.transaction.maxLeaseValue (milliseconds).
     */
    double getTransactionTimeoutMinutes() {
        // TODO: Values 24 hours or greater result in the following error: WARN  [2020-11-09 04:18:33.837] [grpc-default-executor-0]
        // i.p.c.control.impl.ControllerImpl: PingTransaction 00000000-0000-0000-0000-000000000036 failed:
        // java.util.concurrent.CompletionException: io.pravega.client.stream.PingFailedException:
        // Ping transaction for StreamImpl(scope=examples, streamName=network) 00000000-0000-0000-0000-000000000036 failed with status MAX_EXECUTION_TIME_EXCEEDED
        return Double.parseDouble(getProperty(TRANSACTION_TIMEOUT_MINUTES_KEY, Double.toString(18.0 * 60.0)));
    }

    protected String getScopeName() {
        return getProperty(SCOPE_KEY);
    }

    String getStreamName() {
        return getProperty(STREAM_KEY);
    }

    @Override
    protected void doStart() {
        persistentQueueToPravegaService.startAsync();
        persistentQueueToPravegaService.awaitRunning();
        memoryQueueToPersistentQueueService.startAsync();
        memoryQueueToPersistentQueueService.awaitRunning();
        dataCollectorService.startAsync();
        dataCollectorService.awaitRunning();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        dataCollectorService.stopAsync();
        dataCollectorService.awaitTerminated();
        memoryQueueToPersistentQueueService.stopAsync();
        memoryQueueToPersistentQueueService.awaitTerminated();
        persistentQueueToPravegaService.stopAsync();
        persistentQueueToPravegaService.awaitTerminated();
    }

    @Override
    public void close() throws Exception {
        writer.close();
        clientFactory.close();
        persistentQueue.close();
    }

    /**
     * Reads raw data (byte arrays) from a sensor.
     * @throws Exception
     */
    abstract public R readRawData() throws Exception;

    /**
     * Decode raw data (byte arrays) and append to Samples.
     * @param samples
     * @param rawSensorData
     */
    abstract public void decodeRawDataToSamples(S samples, R rawSensorData);

    /**
     * Create a new empty Samples instance.
     */
    abstract public S createSamples();

    /**
     * Serialize Samples to a byte array. This will be written to Pravega as an event.
     * @param samples
     * @throws Exception
     */
    abstract public byte[] serializeSamples(S samples) throws Exception;

    public String getRoutingKey(S samples) {
        return routingKey;
    }

    /**
     * Get the timestamp that will be sent to Pravega with {@link io.pravega.client.stream.EventStreamWriter#noteTime}.
     * Generally, this should be the number of nanoseconds since 1970-01-01.
     * All future events should have a timestamp greater or equal to this value.
     * @param samples
     */
    public long getTimestamp(S samples) {
        return samples.getMaxTimestampNanos();
    }
}
