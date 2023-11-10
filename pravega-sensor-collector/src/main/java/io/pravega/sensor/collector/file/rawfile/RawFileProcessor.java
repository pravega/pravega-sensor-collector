/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.rawfile;

import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.file.FileConfig;
import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.parquet.ParquetEventGenerator;
import io.pravega.sensor.collector.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RawFileProcessor extends FileProcessor {
    private static final Logger log = LoggerFactory.getLogger(RawFileProcessor.class);
    private final FileConfig config;
    private final String writerId;

    public RawFileProcessor(FileConfig config, TransactionStateSQLiteImpl state, EventWriter<byte[]> writer, TransactionCoordinator transactionCoordinator, String writerId) {
        super(config, state, writer, transactionCoordinator);
        this.config =config;
        this.writerId = writerId;
    }

    /** Event generator for Raw file
     * @param config configurations parameters
     * @return eventGenerator
     */
    @Override
    public EventGenerator getEventGenerator(FileConfig config) {
        return RawEventGenerator.create(
                config.routingKey,
                config.eventTemplateStr,
                writerId);
    }

}
