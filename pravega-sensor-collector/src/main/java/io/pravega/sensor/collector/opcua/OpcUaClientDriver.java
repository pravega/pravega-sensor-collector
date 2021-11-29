package io.pravega.sensor.collector.opcua;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.memoryless.SimpleMemorylessDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaClientDriver extends SimpleMemorylessDriver<OpcUaRawData> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientDriver.class);

    public OpcUaClientDriver(DeviceDriverConfig config) {
        super(config);
    }

    /**
     * Reads raw data (byte arrays) from a sensor.
     */
    @Override
    public OpcUaRawData readRawData() {
        log.info("Read logic from the OPC server");
        return null;
    }
}
