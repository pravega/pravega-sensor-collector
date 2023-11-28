package io.pravega.sensor.collector.file.rawfile;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import io.pravega.sensor.collector.file.parquet.ParquetFileProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RawFileProcessorTests extends FileProcessorTests {
    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }
    @Test
    public void generateEventForRawFileTests(){
        FileProcessor fileProcessor = new RawFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
    }

}
