package io.pravega.sensor.collector.metrics;

import com.google.common.base.Preconditions;

/**
 * Interface representing a Metric
 * that can be published.
 * @param <T> underlying metric data-type.
 */
public interface Metric<T> {
    /**
     * Implementors to update the internally maintained
     * value by adding the passed value.
     * @param T value to add.
     */
    void updateWith(T t);
    void clear();
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

    public void updateWith(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.counter += value;
    }

    @Override
    public void clear() {
        this.counter = 0L;
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

    public void updateWith(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.gauge += value;
    }

    @Override
    public void clear() {
        this.gauge = 0L;
    }

}


class ExceptionMeter implements Metric<String> {
    private final StringBuilder exceptionClass = new StringBuilder();
    private final String metricType = "METER";
    private final String SEPARATOR = ";";

    public String getExceptionClass() {
        return exceptionClass.toString();
    }

    public String getMetricType() {
        return metricType;
    }

    public void updateWith(String value) {
        Preconditions.checkArgument(value != null, "value needs to be non-null");
        this.exceptionClass.append(value + SEPARATOR);
    }

    @Override
    public void clear() {
        this.exceptionClass.setLength(0);
    }
}
