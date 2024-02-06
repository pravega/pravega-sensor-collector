package io.pravega.sensor.collector.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TransactionalEventWriterTest {

    @Test
    public void testCreateTransactionalEventWriterWithNullWriter() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new TransactionalEventWriter(null));
        Assert.assertTrue("writer".equals(exception.getMessage()));
    }
}
