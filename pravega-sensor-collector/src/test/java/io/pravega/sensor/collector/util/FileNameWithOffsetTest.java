package io.pravega.sensor.collector.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class FileNameWithOffsetTest {

    @Test
    public void testCreateFileNameWithOffsetWithNullFileName() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new FileNameWithOffset(null, 0));
        Assert.assertTrue("fileName".equals(exception.getMessage()));
    }
}
