package io.pravega.sensor.collector.file.parquet;

import io.pravega.sensor.collector.file.FileProcessor;
import io.pravega.sensor.collector.file.FileProcessorTests;
import io.pravega.sensor.collector.file.rawfile.RawFileProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ParquetFileProcessorTests extends FileProcessorTests {


    @BeforeEach
    public void before() throws Exception {
        super.setup();

    }
    @Test
    public void generateEventForParquetTests(){
        FileProcessor fileProcessor = new ParquetFileProcessor(config, state, transactionalEventWriter,transactionCoordinator, "test");
    }
}
