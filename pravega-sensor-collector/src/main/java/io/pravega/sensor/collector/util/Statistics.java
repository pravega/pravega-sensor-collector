/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.util;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Statistics {
    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    private final String description;
    private final Mean meanMs = new Mean();
    private final StandardDeviation stdDevMs = new StandardDeviation();
    private final Min minMs = new Min();
    private final Max maxMs = new Max();
    private long count = 0;
    private long lastNanos = 0;

    public Statistics(String description) {
        this.description = description;
    }

    public void addTimestampNanos(long nanos) {
        if (lastNanos != 0) {
            final double deltaMs = (nanos - lastNanos) * 1e-6;
            meanMs.increment(deltaMs);
            stdDevMs.increment(deltaMs);
            minMs.increment(deltaMs);
            maxMs.increment(deltaMs);
        }
        lastNanos = nanos;
        count++;
    }

    public void logStatistics() {
        final double meanFreqHz = 1000.0 / meanMs.getResult();
        log.info(String.format("%s: meanFreqHz=%.3f, meanMs=%.3f, stdDevMs=%.3f, minMs=%.3f, maxMs=%.3f, count=%d",
                description, meanFreqHz, meanMs.getResult(), stdDevMs.getResult(), minMs.getResult(), maxMs.getResult(), count));
    }
}
