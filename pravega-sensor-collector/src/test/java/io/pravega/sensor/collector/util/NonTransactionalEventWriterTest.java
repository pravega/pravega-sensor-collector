package io.pravega.sensor.collector.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class NonTransactionalEventWriterTest {

    @Test
    public void testCreateNonTransactionalEventWriterWithNull() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new NonTransactionalEventWriter(null));
        Assert.assertTrue("writer".equals(exception.getMessage()));
    }
}
