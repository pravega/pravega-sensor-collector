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
}
