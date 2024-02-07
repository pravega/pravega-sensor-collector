/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.csvfile;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CsvFileSequenceProcessorTests extends FileProcessorTests {

    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }

    /*
     * Generating event for CSV file and process for new files when there are no pending files.
     */
    @Test
    public void generateEventForCSVFileTests() throws Exception {
        FileProcessor fileProcessor = new CsvFileSequenceProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        fileProcessor.processNewFiles();
        Mockito.verify(state, Mockito.times(1)).getNextPendingFileRecord();
    }
}
