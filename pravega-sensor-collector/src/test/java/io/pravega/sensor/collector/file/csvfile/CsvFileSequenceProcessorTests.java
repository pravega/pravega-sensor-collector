package io.pravega.sensor.collector.file.csvfile;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

public class CsvFileSequenceProcessorTests extends FileProcessorTests {

    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }

    /*
     * Generating event for CSV file and process for new files when there are no pending files.
     */
    @Test
    public void generateEventForCSVFileTests() throws Exception {
        FileProcessor fileProcessor = new CsvFileSequenceProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        fileProcessor.processNewFiles();
        Mockito.verify(state, Mockito.times(1)).getNextPendingFileRecord();
    }
}
