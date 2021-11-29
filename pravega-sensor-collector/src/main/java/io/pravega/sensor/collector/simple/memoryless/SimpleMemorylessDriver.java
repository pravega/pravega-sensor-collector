package io.pravega.sensor.collector.simple.memoryless;

import io.pravega.sensor.collector.DeviceDriver;
import io.pravega.sensor.collector.DeviceDriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleMemorylessDriver <R> extends DeviceDriver {

    private static final Logger log = LoggerFactory.getLogger(SimpleMemorylessDriver.class);
    private final DataCollectorService<R> dataCollectorService;
    public SimpleMemorylessDriver(DeviceDriverConfig config) {
        super(config);
        dataCollectorService = new DataCollectorService<>(config.getInstanceName(),this);
        log.info("This space is for queue init if any");
    }

    @Override
    protected void doStart() {
        dataCollectorService.startAsync();
        dataCollectorService.awaitRunning();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        dataCollectorService.stopAsync();
        dataCollectorService.awaitTerminated();
        notifyStopped();
    }

    @Override
    public void close() throws Exception {
        super.close();
        log.info("Close the streams/events created in constructor");
    }

    /**
     * Reads raw data (byte arrays) from a sensor.
     */
    abstract public R readRawData() throws Exception;
}
