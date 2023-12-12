package io.pravega.sensor.collector.file.rawfile;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RawFileProcessorTests extends FileProcessorTests {
    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }

    /*
     * Generating event for Raw file and check for process new files when there are no pending files.
     */
    @Test
    public void generateEventForRawFileTests() throws Exception {
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
        fileProcessor.processNewFiles();
        Mockito.verify(state, Mockito.times(1)).getNextPendingFileRecord();
    }

}
