/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.metrics.writers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.pravega.sensor.collector.metrics.MetricConfig;
import io.pravega.sensor.collector.metrics.MetricsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 *  Metric File Writer implementation of MetricWriter to dump
 *  metric to a known file. Regular updates to this file will
 *  be monitored by a watchdog process.
 */
@AutoService(MetricWriter.class)
public class MetricFileWriter extends AbstractService implements MetricWriter {

    private final Logger log = LoggerFactory.getLogger(MetricFileWriter.class);
    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
            MetricFileWriter.class.getSimpleName() + "-%d").build();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, namedThreadFactory);

    private final MetricConfig config;

    public MetricFileWriter(MetricConfig config) {
        this.config = config;
    }

    /**
     *  Used by ServiceLoader.
     */
    public MetricFileWriter() {
        config = null;
    }

    @Override
    public void writeMetric() {
        try {
            String jsonMetrics = MetricsStore.getMetricsAsJson();
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(config.getMetricFilePath()), StandardCharsets.US_ASCII)) {
                log.info("Finished writing metric data at {}", config.getMetricFilePath());
                bw.write(jsonMetrics);
            }
        } catch (JsonProcessingException jpe) {
            // Log. Dont throw/panic. Watchdog watching.
            log.error("Error fetching metrics as json string {}", jpe);
        } catch (Exception ioe) {
            // Log. Dont throw/panic. Watchdog watching.
            log.error("Error writing metrics file at {}", ioe);
        }
    }

    @Override
    public void doStart() {
        log.info("Starting MetricFileWriter");
        executor.scheduleAtFixedRate(this::writeMetric, 0, config.getFileWriterIntervalSeconds(), TimeUnit.SECONDS);
        notifyStarted();
    }

    @Override
    public void doStop() {
        log.info("Stopping Watchdog monitor.");
        executor.shutdown();
        notifyStopped();
    }
}
