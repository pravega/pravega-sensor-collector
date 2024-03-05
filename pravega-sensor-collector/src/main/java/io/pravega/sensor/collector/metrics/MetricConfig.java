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

import io.pravega.common.util.Property;
import io.pravega.sensor.collector.DeviceDriverConfig;

import java.io.File;
import java.net.URI;

/**
 * MetricConfig holding config parameters and
 * defaults for publishing metrics.
 */
public class MetricConfig {
    private static final Property<String> METRIC_FILE_WRITER_INTERVAL_SECONDS = Property.named("METRIC_FILE_WRITER_INTERVAL_SECONDS", "15", "");
    private static final Property<String> METRIC_STREAM_WRITER_INTERVAL_SECONDS = Property.named("METRIC_STREAM_WRITER_INTERVAL_SECONDS", "30", "");
    private static final Property<String> METRIC_STREAM_NAME = Property.named("METRIC_STREAM_NAME", "pscmetricsstream", "");
    private static final Property<String> METRIC_SCOPE_NAME = Property.named("METRIC_SCOPE_NAME", "pscmetricsscope", "");
    private static final Property<String> METRIC_CONTROLLER_URI = Property.named("PRAVEGA_CONTROLLER_URI", "tcp://localhost:9090", "");
    private static final Property<String> METRIC_FILE_PATH = Property.named("METRIC_FILE_PATH", System.getProperty("java.io.tmpdir") + File.separator + "psc_metric.json", "");


    private String instanceName;
    /**
     * Pravega scope to publish metrics to.
     */
    private String metricsScope;
    /**
     * Pravega Stream to publish metrics to.
     */
    private String metricStream;
    /**
     * Interval in seconds to publish metrics to
     * a known file by PSC.
     */
    private int fileWriterIntervalSeconds;
    /**
     * Known file where PSC will dump
     * telemetry data.
     */
    private String metricFilePath;
    /**
     * Controller URI of Pravega.
     */
    private URI controllerURI;
    /**
     * Interval in seconds to publish metrics to
     * a configured stream by PSC.
     */
    private int streamWriterIntervalSeconds;

    public String getInstanceName() {
        return instanceName;
    }

    private void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getMetricStream() {
        return metricStream;
    }

    private void setMetricStream(String metricStream) {
        this.metricStream = metricStream;
    }

    public String getMetricsScope() {
        return metricsScope;
    }

    public void setMetricsScope(String metricsScope) {
        this.metricsScope = metricsScope;
    }

    public String getMetricFilePath() {
        return metricFilePath;
    }

    private void setMetricFilePath(String metricFilePath) {
        this.metricFilePath = metricFilePath;
    }

    public URI getControllerURI() {
        return controllerURI;
    }

    private void setControllerURI(URI controllerURI) {
        this.controllerURI = controllerURI;
    }

    public int getFileWriterIntervalSeconds() {
        return fileWriterIntervalSeconds;
    }

    private void setFileWriterIntervalSeconds(int fileWriterIntervalSeconds) {
        this.fileWriterIntervalSeconds = fileWriterIntervalSeconds;
    }

    public int getStreamWriterIntervalSeconds() {
        return streamWriterIntervalSeconds;
    }

    private void setStreamWriterIntervalSeconds(int streamWriterIntervalSeconds) {
        this.streamWriterIntervalSeconds = streamWriterIntervalSeconds;
    }

    // Static factory
    public static MetricConfig getMetricConfigFrom(DeviceDriverConfig ddrConfig) {
        MetricConfig metricConfig = new MetricConfig();
        String pscId = ddrConfig.getProperties().getOrDefault("PSC_ID", "");
        metricConfig.setFileWriterIntervalSeconds(Integer.parseInt(ddrConfig.getProperties().getOrDefault(METRIC_FILE_WRITER_INTERVAL_SECONDS.getName(), METRIC_FILE_WRITER_INTERVAL_SECONDS.getDefaultValue())));
        metricConfig.setStreamWriterIntervalSeconds(Integer.parseInt(ddrConfig.getProperties().getOrDefault(METRIC_STREAM_WRITER_INTERVAL_SECONDS.getName(), METRIC_STREAM_WRITER_INTERVAL_SECONDS.getDefaultValue())));
        metricConfig.setMetricStream(ddrConfig.getProperties().getOrDefault(METRIC_STREAM_NAME.getName() + pscId, METRIC_STREAM_NAME.getDefaultValue() + pscId));
        metricConfig.setControllerURI(URI.create(ddrConfig.getProperties().getOrDefault(METRIC_CONTROLLER_URI.getName(), METRIC_CONTROLLER_URI.getDefaultValue())));
        metricConfig.setMetricsScope(ddrConfig.getProperties().get("SCOPE"));
        metricConfig.setMetricFilePath(METRIC_FILE_PATH.getDefaultValue());
        return metricConfig;
    }
}
