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

import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.util.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SimpleMemorylessDriver<R> extends DeviceDriver {

    private static final Logger log = LoggerFactory.getLogger(SimpleMemorylessDriver.class);
    private static final String SCOPE_KEY = "SCOPE";
    private static final String STREAM_KEY = "STREAM";
    private static final String EXACTLY_ONCE_KEY = "EXACTLY_ONCE";
    private static final String TRANSACTION_TIMEOUT_MINUTES_KEY = "TRANSACTION_TIMEOUT_MINUTES";
    private static final String ROUTING_KEY_KEY = "ROUTING_KEY";
    private static final String SENSOR_POLL_PERIODICITY_MS = "POLL_PERIODICITY_MS";
    private static final String ENABLE_LARGE_EVENT = "ENABLE_LARGE_EVENT";

    private final DataCollectorService<R> dataCollectorService;
    private final EventStreamClientFactory clientFactory;
    private final EventWriter<byte[]> writer;
    private final String writerId;
    private final String routingKey;
    private final long readPeriodicityMs;

    public SimpleMemorylessDriver(DeviceDriverConfig config) {
        super(config);
        final String scopeName = getScopeName();
        final String streamName = getStreamName();
        writerId = java.util.UUID.randomUUID().toString(); //TODO: Bind to meaningful ID if necessary
        clientFactory = getEventStreamClientFactory(scopeName);
        final double transactionTimeoutMinutes = getTransactionTimeoutMinutes();
        final boolean exactlyOnce = getExactlyOnce();
        routingKey = getRoutingKey("");
        readPeriodicityMs = getReadPeriodicityMs();
        createStream(scopeName, streamName);
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
        dataCollectorService = new DataCollectorService<>(config.getInstanceName(), this, writer, readPeriodicityMs);
    }

    @Override
    protected void doStart() {
        dataCollectorService.startAsync();
        dataCollectorService.awaitRunning();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        dataCollectorService.stopAsync();
        dataCollectorService.awaitTerminated();
        notifyStopped();
    }

    @Override
    public void close() throws Exception {
        super.close();
        log.info("Close the streams/events created in constructor");
    }

    protected String getScopeName() {
        return getProperty(SCOPE_KEY);
    }

    String getStreamName() {
        return getProperty(STREAM_KEY);
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

    boolean getExactlyOnce() {
        return Boolean.parseBoolean(getProperty(EXACTLY_ONCE_KEY, Boolean.toString(true)));
    }

    /**
     * Reads raw data (byte arrays) from a sensor.
     * @throws Exception
     */
    abstract public List<R> readRawData() throws Exception;

    /**
     * Create a payload event to be written from raw data.
     *
     * @param rawData
     */
    abstract public byte[] getEvent(R rawData);

    /**
     * Get timestamp value sourced from server for the particular Raw data object.
     *
     * @param rawData : Object to fetch timestamp from
     * @return timestamp in Unix Epoch format
     */
    abstract public long getTimestamp(R rawData);

    private String getRoutingKey(String defaultVal) {
        return getProperty(ROUTING_KEY_KEY, defaultVal);
    }

    private long getReadPeriodicityMs() {
        return Long.parseLong(getProperty(SENSOR_POLL_PERIODICITY_MS, Integer.toString(10)));
    }

    boolean getLargeEventEnable() {
        return Boolean.parseBoolean(getProperty(ENABLE_LARGE_EVENT, Boolean.toString(false)));
    }


    public String getRoutingKey() {
        return routingKey;
    }

}
