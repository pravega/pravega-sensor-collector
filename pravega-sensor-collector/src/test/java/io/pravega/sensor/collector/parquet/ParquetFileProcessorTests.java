package io.pravega.sensor.collector.parquet;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.FileUtils;

public class ParquetFileProcessorTests {
    private static final Logger log = LoggerFactory.getLogger(ParquetFileProcessorTests.class);
    
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
        final List<FileNameWithOffset> actual = ParquetFileProcessor.getNewFiles(directoryListing, completedFiles);
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void getDirectoryListingTest() throws IOException {
        final List<FileNameWithOffset> actual = FileUtils.getDirectoryListing("../parquet-file-sample-data","parquet");
        log.info("actual={}", actual);
    }
}
