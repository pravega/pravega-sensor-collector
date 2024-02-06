package io.pravega.sensor.collector.simple;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class PersistentQueueElementTest {

    @Test
    public void testCreatePersistentQueueElementWithNullBytes() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueElement(1, null, "routing-key", 1000000));
        Assert.assertTrue("bytes".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePersistentQueueElementWithNullRoutingKey() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PersistentQueueElement(1, new byte[]{}, null, 1000000));
        Assert.assertTrue("routingKey".equals(exception.getMessage()));
    }
}
