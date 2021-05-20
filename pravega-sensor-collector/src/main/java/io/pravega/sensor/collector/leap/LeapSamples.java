/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.leap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.simple.Samples;

public class LeapSamples implements Samples {
    private static final Logger log = LoggerFactory.getLogger(LeapSamples.class);

    public List<LeapRawData> rawData = new ArrayList<>();

    @Override
    public int size() {
        return rawData.size();
    }

    @Override
    public List<Long> getTimestampNanos() {
        return rawData.stream().map(d -> d.timestampNanos).collect(Collectors.toList());
    }
}
