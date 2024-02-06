package io.pravega.sensor.collector.file.simple;

import io.pravega.sensor.collector.simple.PersistentQueue;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;

public class PersistentQueueTest {

    @Mock
    private Connection mockConnection;

    @Test
    public void testCreatePersistentQueueWithNullConnection() {
        Exception exception = Assert.assertThrows(RuntimeException.class, () -> new PersistentQueue(null, null, 1));
        Assert.assertTrue(exception.getMessage().contains("connection"));
    }

    @Test
    public void testCreatePersistentQueueWithNullTransactionCoordinator() {
        MockitoAnnotations.initMocks(this);
        Exception exception = Assert.assertThrows(RuntimeException.class, () -> new PersistentQueue(mockConnection, null, 1));
        Assert.assertTrue(exception.getMessage().contains("transactionCoordinator"));
    }
}
