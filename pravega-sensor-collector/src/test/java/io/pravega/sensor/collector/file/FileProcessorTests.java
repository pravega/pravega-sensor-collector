package io.pravega.sensor.collector.file;

import com.google.common.collect.ImmutableList;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.TransactionalEventStreamWriter;
import io.pravega.client.stream.TxnFailedException;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.client.stream.impl.TransactionalEventStreamWriterImpl;
import io.pravega.sensor.collector.file.rawfile.RawEventGenerator;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import io.pravega.sensor.collector.util.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
        config = new FileConfig(stateDatabaseFileName,"/opt/pravega-sensor-collector/Files/A","parquet","key12",
                "stream1","{}",10, false,
                true,20.0,"RawFileIngestService");

       // state = TransactionStateInMemoryImpl.create(stateDatabaseFileName);
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
                "../log-file-sample-data/","csv");
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
       // Mockito.when(state.getNextPendingFileRecord()).thenReturn(new ImmutablePair<>(new FileNameWithOffset("file1.parquet", 0), 1L));
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        doNothing().when(transactionalEventWriter).writeEvent(anyString(), any());
        //fileProcessor.processNewFiles();
        fileProcessor.processFile(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L);
        verify(transactionalEventWriter).writeEvent(anyString(), any());
    }

    /*
     * Process 3 files in loop
     */
    @Test
    public void processNextFewFiles() throws Exception {
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
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        Mockito.doThrow(TxnFailedException.class).when(transactionalEventWriter).commit();
        assertThrows(RuntimeException.class, () -> fileProcessor.processFile(new FileNameWithOffset("../../pravega-sensor-collector/parquet-file-sample-data/sub1.parquet", 0), 1L));
        // Verify that myMethod was called exactly three times
        Mockito.verify(transactionalEventWriter, Mockito.times(1)).commit();

    }

}
