package io.pravega.sensor.collector.stateful;

import org.junit.Assert;
import org.junit.Test;

public class ReadingStateTest {

    @Test
    public void testCreateReadingStateWithNullConnection() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new ReadingState(null));
        Assert.assertTrue("connection".equals(exception.getMessage()));
    }
}
