package io.pravega.sensor.collector.rawfile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.FileNameWithOffset;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

public class RawFileStateTests {
    private static final Logger log = LoggerFactory.getLogger(RawFileStateTests.class);
    
    @Test
    public void pendingFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final RawFileState state = RawFileState.create(stateDatabaseFileName);
        Assert.assertNull(state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.txt", 0L)));
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.txt", 0L), 0L), state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file2.txt", 0L)));
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.txt", 0L), 0L), state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file0.txt", 0L)));
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.txt", 0L), 0L), state.getNextPendingFile());
    }

    @Test
    public void completedFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final RawFileState state = RawFileState.create(stateDatabaseFileName);
        Assert.assertNull(state.getNextPendingFile());
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.txt", 0L)));
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.txt", 0L), 0L), state.getNextPendingFile());
        state.addCompletedFile("file1.txt", 0L, 1000L, 10L);
        final List<FileNameWithOffset> completedFiles = state.getCompletedFiles();
        log.info("completedFiles={}", completedFiles);
        Assert.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.txt", 1000L)), new HashSet<>(completedFiles));
        Assert.assertNull(state.getNextPendingFile());
        // Make sure this is idempotent.
        state.addCompletedFile("file1.txt", 0L, 1000L, 10L);
        Assert.assertEquals(ImmutableSet.of(new FileNameWithOffset("file1.txt", 1000L)), new HashSet<>(completedFiles));
        Assert.assertNull(state.getNextPendingFile());
    }

    @Test
    public void processFilesTest() throws SQLException {
        final String stateDatabaseFileName = ":memory:";
        final RawFileState state = RawFileState.create(stateDatabaseFileName);
        Assert.assertNull(state.getNextPendingFile());
        // Find 3 new files.
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file2.txt", 0L)));
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.txt", 0L)));
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file3.txt", 0L)));
        // Re-add a pending file. This should be ignored.
        state.addPendingFiles(ImmutableList.of(new FileNameWithOffset("file1.txt", 0L)));
        // Get next pending file.
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file2.txt", 0L), 0L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file2.txt", 0L, 1000L, 10L);
        Assert.assertEquals(ImmutableSet.of(new FileNameWithOffset("file2.txt", 1000L)), new HashSet<>(state.getCompletedFiles()));
        // Get next pending file.
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file1.txt", 0L), 10L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file1.txt", 0L, 2000L, 20L);
        Assert.assertEquals(ImmutableSet.of(
                    new FileNameWithOffset("file2.txt", 1000L),
                    new FileNameWithOffset("file1.txt", 2000L)),
                new HashSet<>(state.getCompletedFiles()));
        // Get next pending file.
        Assert.assertEquals(new ImmutablePair<>(new FileNameWithOffset("file3.txt", 0L), 20L), state.getNextPendingFile());
        // Complete file.
        state.addCompletedFile("file3.txt", 0L, 1500L, 30L);
        Assert.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file2.txt", 1000L),
                new FileNameWithOffset("file1.txt", 2000L),
                new FileNameWithOffset("file3.txt", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // No more pending files.
        Assert.assertNull(state.getNextPendingFile());
        // Delete completed file.
        state.deleteCompletedFile("file1.txt");
        Assert.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file2.txt", 1000L),
                new FileNameWithOffset("file3.txt", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // Delete completed file.
        state.deleteCompletedFile("file2.txt");
        Assert.assertEquals(ImmutableSet.of(
                new FileNameWithOffset("file3.txt", 1500L)),
                new HashSet<>(state.getCompletedFiles()));
        // Delete completed file.
        state.deleteCompletedFile("file3.txt");
        Assert.assertTrue(state.getCompletedFiles().isEmpty());
    }
}
