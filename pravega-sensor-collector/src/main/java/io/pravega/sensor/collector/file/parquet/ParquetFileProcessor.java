/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.parquet;

import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.file.FileConfig;
import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.util.TransactionStateDB;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ParquetFileProcessor extends FileProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParquetFileProcessor.class);

    private final FileConfig config;
    private final String writerId;

    public ParquetFileProcessor(FileConfig config, TransactionStateDB state, EventWriter<byte[]> writer, TransactionCoordinator transactionCoordinator, String writerId) {
       super(config, state, writer, transactionCoordinator);
        this.config = config;
        this.writerId = writerId;
    }

    /** Event generator for Parquet file.
     * @param config configurations parameters
     * @return eventGenerator
     */
    @Override
    public EventGenerator getEventGenerator(FileConfig config) {
        return ParquetEventGenerator.create(
                config.routingKey,
                config.maxRecordsPerEvent,
                config.eventTemplateStr,
                writerId);
    }

}
