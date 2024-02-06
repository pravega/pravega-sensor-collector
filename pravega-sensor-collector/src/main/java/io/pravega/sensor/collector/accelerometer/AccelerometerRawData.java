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

import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;

/**
 * Stores raw accelerometer data.
 */
public class AccelerometerRawData {
    public final byte[] bytes;

    public AccelerometerRawData(byte[] bytes) {
        this.bytes = Preconditions.checkNotNull(bytes, "bytes");
    }

    @Override
    public String toString() {
        return "AccelerometerRawData{"
                + ", bytes=" + Hex.encodeHexString(bytes)
                + '}';
    }
}
