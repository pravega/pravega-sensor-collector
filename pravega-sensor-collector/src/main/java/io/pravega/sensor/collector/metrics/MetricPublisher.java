/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.metrics;

import com.google.common.util.concurrent.AbstractService;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.metrics.writers.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 *  Encompassing service that starts different implementations
 *  of the MetricWriter to publish metrics.
 */
public class MetricPublisher extends AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricPublisher.class);
    List<MetricWriter> writers;
    DeviceDriverConfig config;

    public MetricPublisher(DeviceDriverConfig config) {
        this.config = config;
        this.writers = new ArrayList<>();
        initializeWriters();
    }

    /**
     *  Uses the ServiceLoader to get Impl's of MetricWriter and initializes them
     *  via their constructor.
     * @throws RuntimeException
     */
    private void initializeWriters() {
        ServiceLoader<MetricWriter> writers = ServiceLoader.load(MetricWriter.class);
        MetricConfig metricConfig = MetricConfig.getMetricConfigFrom(this.config);
        try {
            for (MetricWriter writer: writers) {
                final Class<?> writerClass = Class.forName(writer.getClass().getName());
                final MetricWriter w = (MetricWriter) writerClass.getConstructor(MetricConfig.class).newInstance(metricConfig);
                this.writers.add(w);
                LOGGER.info("Initialized MetricWriters.");
            }
        } catch (Exception e) {
            // Throw here to halt PSC if writers are not initialized.
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doStart() {
        doStartWriters();
        notifyStarted();
    }

    /**
     * Start the MetricWriter publishers.
     */
    private void doStartWriters() {
        this.writers.forEach( writer -> writer.startAsync());
        this.writers.forEach( writer -> writer.awaitRunning());
    }

    @Override
    protected void doStop() {
        LOGGER.info("Stopping MetricPublisher");
        this.writers.forEach(writer -> writer.stopAsync());
        notifyStopped();
    }
}
