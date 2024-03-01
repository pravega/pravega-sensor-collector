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
     * @param t value to add.
     */
    void incrementBy(T t);

    /**
     * Clears/resets the underlying data type/structure
     * stored by the Metric implementations.
     */
    void clear();
}

/**
 *  Representation of Counter Metric.
 */
class Counter implements Metric<Long> {
    private Long counter = 0L;
    private String metricType = "COUNTER";

    public Long getCounter() {
        return counter;
    }

    public String getMetricType() {
        return metricType;
    }

    public synchronized void incrementBy(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.counter += value;
    }

    @Override
    public synchronized void clear() {
        this.counter = 0L;
    }
}

/**
 * Representation of Gauge Metric.
 */
class Gauge implements Metric<Long> {
    private Long gauge = 0L;
    private String metricType = "GAUGE";
    public Long getGauge() {
        return gauge;
    }

    public String getMetricType() {
        return metricType;
    }

    public synchronized void incrementBy(Long value) {
        Preconditions.checkArgument(value > 0, "value needs to be positive");
        this.gauge += value;
    }

    @Override
    public synchronized void clear() {
        this.gauge = 0L;
    }

}

/**
 * Metric class to represent a meter
 * storing exception strings.
 */
class ExceptionMeter implements Metric<String> {
    private final StringBuilder exceptionClass = new StringBuilder();
    private final String metricType = "METER";
    private final String separator = ";";

    public String getExceptionClass() {
        return exceptionClass.toString();
    }

    public String getMetricType() {
        return metricType;
    }

    public synchronized void incrementBy(String value) {
        Preconditions.checkArgument(value != null, "value needs to be non-null");
        this.exceptionClass.append(value + separator);
    }

    @Override
    public synchronized void clear() {
        this.exceptionClass.setLength(0);
    }
}
