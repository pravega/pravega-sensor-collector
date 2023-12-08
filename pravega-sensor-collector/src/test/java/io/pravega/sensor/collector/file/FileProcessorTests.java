package io.pravega.sensor.collector.file;

import com.google.common.collect.ImmutableList;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.file.rawfile.RawEventGenerator;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import io.pravega.sensor.collector.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class FileProcessorTests {
    private static final Logger log = LoggerFactory.getLogger(FileProcessorTests.class);

    private FileConfig config;
    private TransactionStateSQLiteImpl state;

    private EventWriter writer;
    @Mock
    private TransactionCoordinator transactionCoordinator;
    @Mock
    private EventGenerator eventGenerator;
    @Mock
    private EventStreamClientFactory clientFactory;


    @BeforeEach
    public void setup(){
        MockitoAnnotations.initMocks(this);
        config = new FileConfig("tset.db","/opt/pravega-sensor-collector/Files/A","parquet","key12",
                "stream1","{}",10, false,
                true,20.0,"RawFileIngestService");
        /*writer = EventWriter.create(
                clientFactory,
                "writerId",
                config.streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder()
                        .enableConnectionPooling(true)
                        .transactionTimeoutTime((long) (20.0 * 60.0 * 1000.0))
                        .build(),
                config.exactlyOnce);*/
        String stateDatabaseFileName = ":memory:";
        state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
        //rawFileProcessor = new RawFileProcessor(config,state, writer, transactionCoordinator, "writerId");

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
        final List<FileNameWithOffset> actual = FileProcessor.getNewFiles(directoryListing, completedFiles);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void getDirectoryListingTest() throws IOException {
        final List<FileNameWithOffset> actual = FileUtils.getDirectoryListing(
                "../log-file-sample-data/","csv",Paths.get("."));
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
}
