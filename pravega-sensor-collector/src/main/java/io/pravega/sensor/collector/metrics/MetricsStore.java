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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

/**
 * Simple Store holding metric data centrally.
 */
public class MetricsStore {
    private static final ImmutableMap<String, Metric> metricStore = ImmutableMap.<String, Metric>builder()
            .put(MetricNames.PSC_FILES_PROCESSED_GAUGE, new Gauge())
            .put(MetricNames.PSC_FILES_DELETED_GAUGE, new Gauge())
            .put(MetricNames.PSC_BYTES_PROCESSED_GAUGE, new Gauge())
            .put(MetricNames.PSC_EXCEPTIONS, new ExceptionMeter())
            .build();

    public static Metric getMetric(String metricName) {
       return metricStore.get(metricName);
    }

    /**
     * Return a json representation of all metrics
     * to be published. Uses Jackson to do so.
     * @return JSON String of all metrics.
     * @throws JsonProcessingException In case of any exception retrieving
     * json string.
     */
    public static String getMetricsAsJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(metricStore);
    }

    public static void clearMetrics() {
        metricStore.get(MetricNames.PSC_FILES_PROCESSED_GAUGE).clear();
        metricStore.get(MetricNames.PSC_FILES_DELETED_GAUGE).clear();
        metricStore.get(MetricNames.PSC_BYTES_PROCESSED_GAUGE).clear();
        metricStore.get(MetricNames.PSC_EXCEPTIONS).clear();
    }
}
