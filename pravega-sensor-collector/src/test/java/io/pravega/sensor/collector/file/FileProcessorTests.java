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

import com.google.common.collect.ImmutableList;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.TxnFailedException;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import io.pravega.sensor.collector.util.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;


public class FileProcessorTests {
    private static final Logger log = LoggerFactory.getLogger(FileProcessorTests.class);

    protected FileConfig config;
    @Mock
    protected TransactionStateSQLiteImpl state;

    @Mock
    private EventWriter writer;

    @Mock
    protected TransactionalEventWriter transactionalEventWriter;

    @Mock
    protected TransactionCoordinator transactionCoordinator;
    @Mock
    private EventGenerator eventGenerator;
    @Mock
    private EventStreamClientFactory clientFactory;


    @BeforeEach
    public void setup(){
        MockitoAnnotations.initMocks(this);
        String stateDatabaseFileName = ":memory:";
        config = new FileConfig("./psc.db","/opt/pravega-sensor-collector/Files/A","parquet","key12",
                "stream1","{}",10, false,
                true,20.0, 5000,"RawFileIngestService");
    }

    @Test
    public void getNewFilesTest() {
        final List<FileNameWithOffset> directoryListing = ImmutableList.of(
                new FileNameWithOffset("file2", 10),
                new FileNameWithOffset("file4", 10),
                new FileNameWithOffset("file3", 10));
        final List<FileNameWithOffset> completedFiles = ImmutableList.of(
                new FileNameWithOffset("file1", 10),
                new FileNameWithOffset("file2", 10));
        final List<FileNameWithOffset> expected = ImmutableList.of(
                new FileNameWithOffset("file3", 0),
                new FileNameWithOffset("file4", 0));
        RawFileProcessor fileProcessor = new RawFileProcessor(config,state, writer, transactionCoordinator, "writerId");
        final List<FileNameWithOffset> actual = fileProcessor.getNewFiles(directoryListing, completedFiles);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void getDirectoryListingTest() throws IOException {
        final List<FileNameWithOffset> actual = FileUtils.getDirectoryListing(
                "../log-file-sample-data/","csv", Paths.get("."), 5000);
        log.info("actual={}", actual);
    }

    /*
     * When there is no new files in SqlLite DB to process. which returns empty file set for nextFiles() call.
     */
    @Test
    public void getEmptyNextFileSet() throws Exception {
       FileProcessor fileProcessor = FileProcessor.create(config, clientFactory);
        fileProcessor.processFiles();
    }

    /*
     * Process the single file for Raw file processor.
     */
    @Test
    public void processNextFile() throws Exception {
        copyFile();
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        doNothing().when(transactionalEventWriter).writeEvent(anyString(), any());
        fileProcessor.processFile(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L);
        verify(transactionalEventWriter).writeEvent(anyString(), any());
    }

    /*
     * Process 3 files in loop
     */
    @Test
    public void processNextFewFiles() throws Exception {
        copyFile();
        // Define different return values for the first three invocations and from 4th invocation onwards null
        Mockito.when(state.getNextPendingFileRecord())
                .thenReturn(new ImmutablePair<>(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L))
                .thenReturn(new ImmutablePair<>(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub2.parquet", 0), 2L))
                .thenReturn(new ImmutablePair<>(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub3.parquet", 0), 3L))
                .thenAnswer(invocation -> null);

        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        doNothing().when(transactionalEventWriter).writeEvent(anyString(), any());
        fileProcessor.processNewFiles();

        // Verify that myMethod was called exactly three times
        Mockito.verify(transactionalEventWriter, Mockito.times(3)).writeEvent(anyString(), any());

    }

    /*
     * Process the single file .
     * Throw transaction failed exception while writing events
     */
    @Test
    public void processNextFile_WriteEventException() throws Exception {
        copyFile();
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        Mockito.doThrow(TxnFailedException.class).when(transactionalEventWriter).writeEvent(anyString(), any());
        assertThrows(RuntimeException.class, () -> fileProcessor.processFile(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L));
        // Verify that myMethod was called exactly three times
        Mockito.verify(transactionalEventWriter, Mockito.times(1)).writeEvent(anyString(), any());

    }
    /*
     * Process the single file .
     * Throw transaction failed exception while commiting transaction
     */
    @Test
    public void processNextFile_CommitException() throws Exception {
        copyFile();
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        Mockito.doThrow(TxnFailedException.class).when(transactionalEventWriter).commit();
        assertThrows(RuntimeException.class, () -> fileProcessor.processFile(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L));
        // Verify that myMethod was called exactly three times
        Mockito.verify(transactionalEventWriter, Mockito.times(1)).commit();
    }

    /*
     * Before each test we need to copy the files to parquet file directory so that files are available for processing.
     * Post process these files are moved to different directory, so it is important to add them back to the current directory path.
     */
    public void copyFile() throws IOException {
        Path sourcePath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/test_file/sub1.parquet");
        Path targetPath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        sourcePath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/test_file/sub2.parquet");
        targetPath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/sub2.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        sourcePath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/test_file/sub3.parquet");
        targetPath = Paths.get("../../pravega-sensor-collector/parquet-file-sample-data/sub3.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

}
