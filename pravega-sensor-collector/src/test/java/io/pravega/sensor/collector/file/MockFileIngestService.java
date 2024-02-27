package io.pravega.sensor.collector.file;

import io.pravega.sensor.collector.DeviceDriverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockFileIngestService extends FileIngestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockFileIngestService.class);
    public MockFileIngestService(DeviceDriverConfig config) {
        super(config);
    }

    /*
     * Mocking the behaviour of create stream
     */
    @Override
    protected void createStream(String scopeName, String streamName) {
        LOGGER.info("Do nothing for create stream");
    }
}
