package io.pravega.sensor.collector.metrics;

import com.google.common.base.Preconditions;

/**
 * Interface representing a Metric
 * that can be published.
 * @param <T> underlying metric data-type.
 */
public interface Metric<T> {
    void update(T t);
}

class Counter implements Metric<Long> {
    private Long counter = 0L;
    private String metricType = "COUNTER";

    public Long getCounter() {
        return counter;
    }

    public String getMetricType() {
        return metricType;
    }

    public void update(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.counter = value;
    }
}

class Gauge implements Metric<Long> {
    private Long gauge = 0L;
    private String metricType = "GAUGE";
    public Long getGauge() {
        return gauge;
    }

    public String getMetricType() {
        return metricType;
    }

    public void update(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.gauge = value;
    }
}


class ExceptionMeter implements Metric<String> {
    private String exceptionClass;
    private String metricType = "METER";
    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getMetricType() {
        return metricType;
    }

    public void update(String value) {
        Preconditions.checkArgument(value != null, "value needs to be non-null");
        this.exceptionClass = value;
    }
}
