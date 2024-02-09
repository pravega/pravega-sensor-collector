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

