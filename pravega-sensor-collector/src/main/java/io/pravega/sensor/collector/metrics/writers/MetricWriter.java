/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.metrics.writers;

import com.google.common.util.concurrent.Service;

/**
 *MetricWriter service to publish metrics.
 * Implementations of this interface will have 
 * the specifics to write to different sinks
 * e.g: file, stream
 */
public interface MetricWriter extends Service {

    /**
     * Writes metric. Implementations will vary
     * based on the sink to publish metric to.
     */
    public abstract void writeMetric();
}

