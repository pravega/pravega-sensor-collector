package io.pravega.sensor.collector.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class PravegaWriterEventTest {

    @Test
    public void testCreatePravegaWriterEventWithNullRoutingKey() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PravegaWriterEvent(null, 1, new byte[]{}));
        Assert.assertTrue("routingKey".equals(exception.getMessage()));
    }

    @Test
    public void testCreatePravegaWriterEventWithNullBytes() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new PravegaWriterEvent("routing-key", 1, null));
        Assert.assertTrue("bytes".equals(exception.getMessage()));
    }
}
