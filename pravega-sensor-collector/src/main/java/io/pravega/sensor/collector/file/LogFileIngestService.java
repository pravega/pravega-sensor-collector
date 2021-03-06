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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class LogFileIngestService extends DeviceDriver {
    private static final Logger log = LoggerFactory.getLogger(LogFileIngestService.class);

    private static final String FILE_SPEC_KEY = "FILE_SPEC";
    private static final String DELETE_COMPLETED_FILES_KEY = "DELETE_COMPLETED_FILES";
    private static final String DATABASE_FILE_KEY = "DATABASE_FILE";
    private static final String EVENT_TEMPLATE_KEY = "EVENT_TEMPLATE";
    private static final String SAMPLES_PER_EVENT_KEY = "SAMPLES_PER_EVENT";
    private static final String INTERVAL_MS_KEY = "INTERVAL_MS";

    private static final String SCOPE_KEY = "SCOPE";
    private static final String STREAM_KEY = "STREAM";
    private static final String ROUTING_KEY_KEY = "ROUTING_KEY";
    private static final String EXACTLY_ONCE_KEY = "EXACTLY_ONCE";
    private static final String TRANSACTION_TIMEOUT_MINUTES_KEY = "TRANSACTION_TIMEOUT_MINUTES";

    private final LogFileSequenceProcessor processor;
    private final ScheduledExecutorService executor;

    private ScheduledFuture<?> task;

    public LogFileIngestService(DeviceDriverConfig config) {
        super(config);
        final LogFileSequenceConfig logFileSequenceConfig = new LogFileSequenceConfig(
                getDatabaseFileName(),
                getFileSpec(),
                getRoutingKey(),
                getStreamName(),
                getEventTemplate(),
                getSamplesPerEvent(),
                getDeleteCompletedFiles(),
                getExactlyOnce(),
                getTransactionTimeoutMinutes());
        log.info("Log File Ingest Config: {}", logFileSequenceConfig);
        final String scopeName = getScopeName();
        log.info("Scope: {}", scopeName);
        createStream(scopeName, getStreamName());
        final EventStreamClientFactory clientFactory = getEventStreamClientFactory(scopeName);
        processor = LogFileSequenceProcessor.create(logFileSequenceConfig, clientFactory);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                LogFileIngestService.class.getSimpleName() + "-" + config.getInstanceName() + "-%d").build();
        executor = Executors.newScheduledThreadPool(1, namedThreadFactory);
    }

    String getFileSpec() {
        return getProperty(FILE_SPEC_KEY);
    }

    boolean getDeleteCompletedFiles() {
        return Boolean.parseBoolean(getProperty(DELETE_COMPLETED_FILES_KEY, Boolean.toString(true)));
    }

    String getDatabaseFileName() {
        return getProperty(DATABASE_FILE_KEY);
    }

    String getEventTemplate() {
        return getProperty(EVENT_TEMPLATE_KEY, "{}");
    }

    int getSamplesPerEvent() {
        return Integer.parseInt(getProperty(SAMPLES_PER_EVENT_KEY, Integer.toString(100)));
    }

    long getIntervalMs() {
        return Long.parseLong(getProperty(INTERVAL_MS_KEY, Long.toString(10000)));
    }

    String getScopeName() {
        return getProperty(SCOPE_KEY);
    }

    String getStreamName() {
        return getProperty(STREAM_KEY);
    }

    protected String getRoutingKey() {
        return getProperty(ROUTING_KEY_KEY, "");
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

    protected void ingestLogFiles() {
        log.info("ingestLogFiles: BEGIN");
        try {
            processor.ingestLogFiles();
        } catch (Exception e) {
            log.error("Error", e);
            // Continue on any errors. We will retry on the next iteration.
        }
        log.info("ingestLogFiles: END");
    }

    @Override
    protected void doStart() {
        task = executor.scheduleAtFixedRate(
                this::ingestLogFiles,
                0,
                getIntervalMs(),
                TimeUnit.MILLISECONDS);
        notifyStarted();
    }

    @Override
    protected void doStop() {
        task.cancel(false);
    }
}
