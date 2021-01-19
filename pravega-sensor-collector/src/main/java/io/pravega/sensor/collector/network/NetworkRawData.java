/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.network;

import java.util.List;

public class NetworkRawData {
    public final long timestampNanos;
    public final List<Long> statisticValues;

    public NetworkRawData(long timestampNanos, List<Long> statisticValues) {
        this.timestampNanos = timestampNanos;
        this.statisticValues = statisticValues;
    }

    @Override
    public String toString() {
        return "NetworkRawData{" +
                "timestampNanos=" + timestampNanos +
                ", statisticValues=" + statisticValues +
                '}';
    }
}
