package io.pravega.sensor.collector.file.simple;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.network.NetworkDriver;
import io.pravega.sensor.collector.simple.MemoryQueueToPersistentQueueService;
import io.pravega.sensor.collector.simple.PersistentQueue;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import io.pravega.sensor.collector.util.TransactionalEventWriter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.when;

public class MemoryQueueToPersistentQueueServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    TransactionalEventWriter transactionalEventWriter;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    TransactionCoordinator transactionCoordinator;

    PersistentQueue persistentQueue;
    DeviceDriverConfig config;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);
        // Mock behavior for the connection and statement
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        //transactionCoordinator = new TransactionCoordinator(mockConnection, transactionalEventWriter);
        //when(mockStatement.execute(anyString())).thenReturn(true);
        when(mockStatement.executeQuery("select count(id) from Queue")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true);
        persistentQueue = new PersistentQueue(mockConnection, transactionCoordinator, 10);
        config = new DeviceDriverConfig("ins", "DeviceDriverConfig", new HashMap<>(), new DeviceDriverManager(new HashMap<>()));
    }

    @Test
    public void testCreateMemoryQueueToPersistentQueueServiceWithNullInstanceName() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new MemoryQueueToPersistentQueueService<>(null, new LinkedBlockingQueue<>(1), null, null, 1));
        Assert.assertTrue("instanceName".equals(exception.getMessage()));
    }

    @Test
    public void testCreateMemoryQueueToPersistentQueueServiceWithNullMemoryQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new MemoryQueueToPersistentQueueService<>("instance-name", null, null, null, 1));
        Assert.assertTrue("memoryQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreateMemoryQueueToPersistentQueueServiceWithNullPersistentQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new MemoryQueueToPersistentQueueService<>("instance-name", new LinkedBlockingQueue<>(1), null, null, 1));
        Assert.assertTrue("persistentQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreateMemoryQueueToPersistentQueueServiceWithNull() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new MemoryQueueToPersistentQueueService<>("instance-name", new LinkedBlockingQueue<>(1), persistentQueue, null, 1));
        Assert.assertTrue("driver".equals(exception.getMessage()));
    }
}
