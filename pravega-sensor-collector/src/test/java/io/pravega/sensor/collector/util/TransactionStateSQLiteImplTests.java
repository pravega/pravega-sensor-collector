/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

public class TransactionStateSQLiteImplTests {

    private static final Logger log = LoggerFactory.getLogger(TransactionStateSQLiteImplTests.class);

    @Test
    public void pendingFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateDB state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFileRecord());
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFileRecord());
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file2.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFileRecord());
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file0.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFileRecord());
    }

    @Test
    public void completedFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateInMemoryImpl state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFileRecord());
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 0L), state.getNextPendingFileRecord());
        state.addCompletedFileRecord("file1.csv", 0L, 1000L, 10L);
        final List<FileNameWithOffset> completedFiles = state.getCompletedFileRecords();
        log.info("completedFiles={}", completedFiles);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.csv", 1000L)), new HashSet<>(completedFiles));
        Assertions.assertNull(state.getNextPendingFileRecord());
        // Make sure this is idempotent.
        state.addCompletedFileRecord("file1.csv", 0L, 1000L, 10L);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.csv", 1000L)), new HashSet<>(completedFiles));
        Assertions.assertNull(state.getNextPendingFileRecord());
    }

    @Test
    public void processFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final TransactionStateInMemoryImpl state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        Assertions.assertNull(state.getNextPendingFileRecord());
        // Find 3 new files.
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file2.csv", 0L)));
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file3.csv", 0L)));
        // Re-add a pending file. This should be ignored.
        state.addPendingFileRecords(ImmutableList.of(new FileNameWithOffset("file1.csv", 0L)));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file2.csv", 0L), 0L), state.getNextPendingFileRecord());
        // Complete file.
        state.addCompletedFileRecord("file2.csv", 0L, 1000L, 10L);
        Assertions.assertEquals(ImmutableSet.of(new FileNameWithOffset("file2.csv", 1000L)), new HashSet<>(state.getCompletedFileRecords()));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.csv", 0L), 10L), state.getNextPendingFileRecord());
        // Complete file.
        state.addCompletedFileRecord("file1.csv", 0L, 2000L, 20L);
        Assertions.assertEquals(ImmutableSet.of(
                        new FileNameWithOffset("file2.csv", 1000L),
                        new FileNameWithOffset("file1.csv", 2000L)),
                new HashSet<>(state.getCompletedFileRecords()));
        // Get next pending file.
        Assertions.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file3.csv", 0L), 20L), state.getNextPendingFileRecord());
        // Complete file.
        state.addCompletedFileRecord("file3.csv", 0L, 1500L, 30L);
        Assertions.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file2.csv", 1000L),
                new FileNameWithOffset("file1.csv", 2000L),
                new FileNameWithOffset("file3.csv", 1500L)),                new HashSet<>(state.getCompletedFileRecords()));
        // No more pending files.
        Assertions.assertNull(state.getNextPendingFileRecord());
        // Delete completed file.
        state.deleteCompletedFileRecord("file1.csv");
        Assertions.assertEquals(ImmutableSet.of(
                        new FileNameWithOffset("file2.csv", 1000L),
                        new FileNameWithOffset("file3.csv", 1500L)),
                new HashSet<>(state.getCompletedFileRecords()));
        // Delete completed file.
        state.deleteCompletedFileRecord("file2.csv");
        Assertions.assertEquals(ImmutableSet.of(
                        new FileNameWithOffset("file3.csv", 1500L)),
                new HashSet<>(state.getCompletedFileRecords()));
        // Delete completed file.
        state.deleteCompletedFileRecord("file3.csv");
        Assertions.assertTrue(state.getCompletedFileRecords().isEmpty());
    }
}
