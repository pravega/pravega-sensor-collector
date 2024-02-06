package io.pravega.sensor.collector.file.simple;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.leap.LeapDriver;
import io.pravega.sensor.collector.simple.PersistentQueue;
import io.pravega.sensor.collector.stateful.DataCollectorService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class DataCollectorServiceTest {

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
    public void testCreateDataCollectorServiceTestWithNullInstanceName() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>(null, persistentQueue, null));
        Assert.assertTrue("instanceName".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceTestWithNullPersistentQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>("instance", null, null));
        Assert.assertTrue("persistentQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceTestWithNullDriver() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService<>("instance", persistentQueue, null));
        Assert.assertTrue("driver".equals(exception.getMessage()));
    }

}
