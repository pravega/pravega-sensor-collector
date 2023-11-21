package io.pravega.sensor.collector.file;

import io.pravega.sensor.collector.util.TransactionCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class FileIngestServiceTest {

    @Mock
    private FileProcessor processor;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void watchFileTests() throws Exception {
        doNothing().when(processor).watchFiles();

    }
}
