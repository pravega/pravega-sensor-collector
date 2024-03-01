/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.metrics.MetricPublisher;
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

    @Override
    protected MetricPublisher getMetricPublisher(DeviceDriverConfig config){
        MetricPublisher metricPublisher = new TestMetricPublisher(config);
        return metricPublisher;
    }

    class TestMetricPublisher extends MetricPublisher {
        public TestMetricPublisher(DeviceDriverConfig config) {
            super(config);
        }


        @Override
        protected void doStart() {
            notifyStarted();
        }
    }
}
