package io.pravega.sensor.collector.simple;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;

public class DataCollectorServiceTest {


    @Test
    public void testCreateDataCollectorServiceWithNullInstanceName() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService(null, new LinkedBlockingQueue<>(1), null));
        Assert.assertTrue("instanceName".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceWithNullMemoryQueue() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService("instance-name", null, null));
        Assert.assertTrue("memoryQueue".equals(exception.getMessage()));
    }

    @Test
    public void testCreateDataCollectorServiceWithNullDriver() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new DataCollectorService("instance-name", new LinkedBlockingQueue<>(1), null));
        Assert.assertTrue("driver".equals(exception.getMessage()));
    }

}
