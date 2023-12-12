package io.pravega.sensor.collector.file.csvfile;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CsvFileSequenceProcessorTests extends FileProcessorTests {

    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }
    @Test
    public void generateEventForCSVFileTests(){
        FileProcessor fileProcessor = new CsvFileSequenceProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
    }
}
