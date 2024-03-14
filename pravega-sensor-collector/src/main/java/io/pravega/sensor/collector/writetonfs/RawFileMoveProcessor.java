/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.writetonfs;

import io.pravega.sensor.collector.writetonfs.EventGenerator;
import io.pravega.sensor.collector.writetonfs.FileConfig;
import io.pravega.sensor.collector.writetonfs.FileProcessor;
import io.pravega.sensor.collector.writetonfs.TransactionCoordinator;
import io.pravega.sensor.collector.writetonfs.TransactionStateDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawFileMoveProcessor extends FileProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawFileMoveProcessor.class);
    private final FileConfig config;
    private final String writerId;

    public RawFileMoveProcessor(FileConfig config, TransactionStateDB state, TransactionCoordinator transactionCoordinator, String writerId) {
        super(config, state, transactionCoordinator);
        this.config = config;
        this.writerId = writerId;
    }

    /** Event generator for Raw file.
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
