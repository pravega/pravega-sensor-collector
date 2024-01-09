/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.accelerometer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.pravega.sensor.collector.simple.Samples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class AccelerometerSamples implements Samples {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccelerometerSamples.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Timestamps as nanoseconds since 1970-01-01.
    @JsonProperty("TimestampNanos")
    public final List<Long> timestampNanos = new ArrayList<>();
    @JsonProperty("X")
    public final List<Double> x = new ArrayList<>();
    @JsonProperty("Y")
    public final List<Double> y = new ArrayList<>();
    @JsonProperty("Z")
    public final List<Double> z = new ArrayList<>();
    @JsonProperty("RemoteAddr")
    public final String remoteAddr;
    // The last timestamp formatted as a string.
    @JsonProperty("LastTimestampFormatted")
    public String lastTimestampFormatted;

    public AccelerometerSamples(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    @Override
    public String toString() {
        return "Samples{"
                + "remoteAddr='" + remoteAddr + '\''
                + ", lastTimestampFormatted='" + lastTimestampFormatted + '\''
                + ", timestampNanos=" + timestampNanos + ", x="
                + x + ", y=" + y + ", z=" + z + '}';
    }

    public long lastTimestamp() {
        return timestampNanos.get(timestampNanos.size() - 1);
    }

    public void setLastTimestampFormatted() {
        lastTimestampFormatted = DATE_FORMAT.format(new Date(lastTimestamp() / 1000 / 1000));
    }

    @Override
    public int size() {
        return timestampNanos.size();
    }

    @Override
    public List<Long> getTimestampNanos() {
        return timestampNanos;
    }
}
