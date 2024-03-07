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

import io.pravega.sensor.collector.file.csvfile.CsvFileSequenceProcessor;
import io.pravega.sensor.collector.file.parquet.ParquetFileProcessor;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import io.pravega.sensor.collector.util.TransactionStateInMemoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FileProcessorFactoryTest {


    private FileConfig config;
    @Mock
    private EventWriter writer;
    @Mock
    private TransactionCoordinator transactionCoordinator;
    @Mock
    private TransactionStateInMemoryImpl state;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

    }

    /*
     * Test for creating Raw file processor
     */
    @Test
    public void createRAWFileProcessorTest() throws Exception {
        String stateDatabaseFileName = ":memory:";
        config = new FileConfig(stateDatabaseFileName, "/opt/pravega-sensor-collector/Files/A", "parquet", "key12",
                "stream1", "{}", 10, false,
                true,20.0, 5000l, "RawFileIngestService", true, "psc1");
        FileProcessor rawFileProcessor = FileProcessorFactory.createFileSequenceProcessor(config, state, writer, transactionCoordinator, "writerId");
        Assertions.assertTrue(rawFileProcessor instanceof RawFileProcessor);
    }

    /*
     * Test for creating CSV file processor
     */
    @Test
    public void createCSVFileProcessorTest() throws Exception {
        String stateDatabaseFileName = ":memory:";
        config = new FileConfig(stateDatabaseFileName, "/opt/pravega-sensor-collector/Files/A", "parquet", "key12",
                "stream1", "{}", 10, false,
                true,20.0, 5000L, "CsvFileIngestService", false, "psc1");
        FileProcessor csvFileProcessor = FileProcessorFactory.createFileSequenceProcessor(config, state, writer, transactionCoordinator, "writerId");
        Assertions.assertTrue(csvFileProcessor instanceof CsvFileSequenceProcessor);

    }

    /*
     * Test for creating PARQUET file processor
     */
    @Test
    public void createParquetFileProcessorTest() throws Exception {
        String stateDatabaseFileName = ":memory:";
        config = new FileConfig(stateDatabaseFileName, "/opt/pravega-sensor-collector/Files/A", "parquet", "key12",
                "stream1", "{}", 10, false,
                true,20.0, 5000L,"ParquetFileIngestService", false, "psc1");
        FileProcessor parquetFileProcessor = FileProcessorFactory.createFileSequenceProcessor(config, state, writer, transactionCoordinator, "writerId");

        Assertions.assertTrue(parquetFileProcessor instanceof ParquetFileProcessor);

    }
}
