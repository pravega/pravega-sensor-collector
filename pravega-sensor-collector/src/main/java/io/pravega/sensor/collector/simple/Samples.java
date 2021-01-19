/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.simple;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface Samples {
    /**
     * @return the number of samples
     */
    int size();

    /**
     * @return the list of sample timestamps as nanoseconds since 1970-01-01
     */
    List<Long> getTimestampNanos();

    /**
     * @return the maximum timestamp as nanoseconds since 1970-01-01
     */
    @JsonIgnore
    default long getMaxTimestampNanos() {
        final List<Long> timestampsNanos = getTimestampNanos();
        return timestampsNanos.get(timestampsNanos.size() - 1);
    }
}
