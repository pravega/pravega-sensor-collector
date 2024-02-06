package io.pravega.sensor.collector.util;

import io.pravega.sensor.collector.stateful.ReadingState;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class AutoRollbackTest {

    @Test
    public void testCreateAutoRollbackWithNullConnection() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new AutoRollback(null));
        Assert.assertTrue("connection".equals(exception.getMessage()));
    }
}
