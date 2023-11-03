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
//import org.junit.Assert;
//import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.FileUtils;

import java.io.IOException;
import java.util.List;
import io.pravega.sensor.collector.util.FileNameWithOffset;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void getDirectoryListingTest() throws IOException {
        final List<FileNameWithOffset> actual = FileUtils.getDirectoryListing(
                "../log-file-sample-data/","csv");
        log.info("actual={}", actual);
    }

    /*    Perform a test to verify the existence of files in a non-existent directory.     */
    @Test
    public void getDirectoryListingTestWithWrongPath() {
        Assertions.assertThrows(IOException.class, () ->
                FileUtils.getDirectoryListing(
                        "../log-file-sample-data1/","csv"));
    }

    /*    Test case to confirm the absence of files in the specified directory path    */
    @Test
    public void getDirectoryListingTestWithNoFileInPath() throws IOException {
        Assertions.assertTrue(
                FileUtils.getDirectoryListing("../log-file-sample-data/","abc").isEmpty(), "List is not empty");
    }

   /* @Test
    public void getDirectoryListingTestWithNoFileInPath1() throws IOException {
        LogFileSequenceProcessor logProcessor = mock(LogFileSequenceProcessor.class);
       when(logProcessor.getDirectoryListing(anyString(),anyString())).thenReturn(ImmutableList.of(new FileNameWithOffset("file3", 0)));
        Assertions.assertEquals(
                LogFileSequenceProcessor.getDirectoryListing("../log-file-sample-data/","csv1").get(0).offset, 0);
    }*/

}
