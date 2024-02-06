package io.pravega.sensor.collector.simple;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.MockedConnection;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class MemoryQueueToPersistentQueueServiceTest extends MockedConnection {

    private PersistentQueue persistentQueue;

    private DeviceDriverConfig config;

    @BeforeEach
    public void setUp() throws SQLException {
        super.before();
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
