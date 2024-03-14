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

import io.pravega.sensor.collector.writetonfs.RawFileMoveProcessor;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import io.pravega.sensor.collector.util.TransactionStateDB;

/*
 * The FileProcessorFactory class is responsible for creating instances of file processors based on the type of the input file.
 *
 */
public class FileProcessorFactory {

    public static FileProcessor createFileSequenceProcessor(final FileConfig config, TransactionStateDB state,
                                                                    EventWriter<byte[]> writer,
                                                                    TransactionCoordinator transactionCoordinator,
                                                                    String writerId) {

        final String className = config.fileType.substring(config.fileType.lastIndexOf(".") + 1);

            switch (className) {
                case "RawFileMoveService":
                    return new RawFileMoveProcessor(config, state, writer, transactionCoordinator, writerId);

                default :
                    throw new RuntimeException("Unsupported className: " + className);
            }

    }
}
