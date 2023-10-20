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
import com.google.common.collect.ImmutableSet;
import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.TransactionStateInMemoryImpl;
import io.pravega.sensor.collector.util.TransactionStateSQLiteImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

public class LogFileSequenceProcessorStateTests {
    private static final Logger log = LoggerFactory.getLogger(LogFileSequenceProcessorStateTests.class);

    @Test
    public void pendingFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateInMemoryImpl state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file2.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file0.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFile());
    }

    @Test
    public void completedFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateInMemoryImpl state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFile());
        state.addCompletedFile("file1.csv", 0L, 1000L, 10L);
        final List<FileNameWithOffset> completedFiles = state.getCompletedFiles();
        log.info("completedFiles={}", completedFiles);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.csv", 1000L)), new HashSet<>(completedFiles));
        Assertions.assertNull(state.getNextPendingFile());
        // Make sure this is idempotent.
        state.addCompletedFile("file1.csv", 0L, 1000L, 10L);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.csv", 1000L)), new HashSet<>(completedFiles));
        Assertions.assertNull(state.getNextPendingFile());
    }

    @Test
    public void processFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateInMemoryImpl state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFile());
        // Find 3 new files.
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file2.csv", 0L)));
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file3.csv", 0L)));
        // Re-add a pending file. This should be ignored.
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file2.csv", 0L), 0L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file2.csv", 0L, 1000L, 10L);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file2.csv", 1000L)), new HashSet<>(state.getCompletedFiles()));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 10L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file1.csv", 0L, 2000L, 20L);
        Assertions.assertEquals(ImmutableSet.of(
                    new FileNameWithOffset("file2.csv", 1000L),
                    new FileNameWithOffset("file1.csv", 2000L)),
                new HashSet<>(state.getCompletedFiles()));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file3.csv", 0L), 20L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file3.csv", 0L, 1500L, 30L);
        Assertions.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file2.csv", 1000L),
                new FileNameWithOffset("file1.csv", 2000L),
                new FileNameWithOffset("file3.csv", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // No more pending files.
        Assertions.assertNull(state.getNextPendingFile());
        // Delete completed file.
        state.deleteCompletedFile("file1.csv");
        Assertions.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file2.csv", 1000L),
                new FileNameWithOffset("file3.csv", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // Delete completed file.
        state.deleteCompletedFile("file2.csv");
        Assertions.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file3.csv", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // Delete completed file.
        state.deleteCompletedFile("file3.csv");
        Assertions.assertTrue(state.getCompletedFiles().isEmpty());
    }
}
