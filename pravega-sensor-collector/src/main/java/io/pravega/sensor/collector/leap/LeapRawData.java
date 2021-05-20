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

import java.nio.charset.StandardCharsets;

public class LeapRawData {
    public final long timestampNanos;
    public final byte[] data;

    public LeapRawData(long timestampNanos, byte[] data) {
        this.timestampNanos = timestampNanos;
        this.data = data;
    }

    @Override
    public String toString() {
        return "LeapRawData{" +
                "timestampNanos=" + timestampNanos +
                ", data='" + new String(data, StandardCharsets.UTF_8) + "'" +
                '}';
    }
}
