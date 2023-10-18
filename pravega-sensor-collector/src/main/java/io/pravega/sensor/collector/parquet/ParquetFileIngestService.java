/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.parquet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.client.EventStreamClientFactory;
import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;


/**
 * Ingestion service for parquet file data.  
 */
public class ParquetFileIngestService extends DeviceDriver{
    private static final Logger log = LoggerFactory.getLogger(ParquetFileIngestService.class);
    
    private static final String FILE_SPEC_KEY = "FILE_SPEC";
    private static final String FILE_EXT= "FILE_EXTENSION";
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

    private final ParquetFileProcessor processor;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> watchFileTask;
    private ScheduledFuture<?> processFileTask;

    public ParquetFileIngestService(DeviceDriverConfig config){
        super(config);
        final ParquetFileConfig parquetFileConfig = new ParquetFileConfig(
                getDatabaseFileName(),
                getFileSpec(),
                getFileExtension(),
                getRoutingKey(),
                getStreamName(),
                getEventTemplate(),
                getSamplesPerEvent(),
                getDeleteCompletedFiles(),
                getExactlyOnce(),
                getTransactionTimeoutMinutes());
        log.info("Parquet File Ingest Config: {}", parquetFileConfig);
        final String scopeName = getScopeName();
        log.info("Scope: {}", scopeName);
        createStream(scopeName, getStreamName());

        final EventStreamClientFactory clientFactory = getEventStreamClientFactory(scopeName);
        processor = ParquetFileProcessor.create(parquetFileConfig, clientFactory);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                ParquetFileIngestService.class.getSimpleName() + "-" + config.getInstanceName() + "-%d").build();
        executor = Executors.newScheduledThreadPool(1, namedThreadFactory);

    }

    String getFileSpec() {
        return getProperty(FILE_SPEC_KEY);
    }

    String getFileExtension() {
        return getProperty(FILE_EXT, "");
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

    protected void watchParquetFiles() {
        log.trace("watchParquetFiles: BEGIN");
        try {
            processor.watchParquetFiles();
        } catch (Exception e) {
            log.error("watchParquetFiles: watch file error", e);
            // Continue on any errors. We will retry on the next iteration.
        }
        log.trace("watchParquetFiles: END");
    }
    protected void processParquetFiles() {
        log.trace("processParquetFiles: BEGIN");
        try {
            processor.processParquetFiles();
        } catch (Exception e) {
            log.error("processParquetFiles: Process file error", e);
            // Continue on any errors. We will retry on the next iteration.
        }
        log.trace("processParquetFiles: END");
    }

    @Override
    protected void doStart() {
        watchFileTask = executor.scheduleAtFixedRate(
                this::watchParquetFiles,
                0,
                getIntervalMs(),
                TimeUnit.MILLISECONDS);
        processFileTask = executor.scheduleWithFixedDelay(
                this::processParquetFiles,
                0,
                0,
                TimeUnit.MILLISECONDS);
        notifyStarted();        
    }

    @Override
    protected void doStop() {
        log.info("doStop: Cancelling ingestion task and process file task");
        watchFileTask.cancel(false);
        processFileTask.cancel(false);
    }

}
