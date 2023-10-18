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
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class LogFileSequenceProcessorTests {
    private static final Logger log = LoggerFactory.getLogger(LogFileSequenceProcessorTests.class);

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
        final List<FileNameWithOffset> actual = LogFileSequenceProcessor.getNewFiles(directoryListing, completedFiles);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getDirectoryListingTest() throws IOException {
        final List<FileNameWithOffset> actual = LogFileSequenceProcessor.getDirectoryListing(
                "../log-file-sample-data/","csv");
        log.info("actual={}", actual);
    }
}
