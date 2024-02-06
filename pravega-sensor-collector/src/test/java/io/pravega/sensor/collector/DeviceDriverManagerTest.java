package io.pravega.sensor.collector;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class DeviceDriverManagerTest {

    @Test
    public void testCreateDeviceDriverManagerWithNullProperties() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () ->  new DeviceDriverManager(null));
        Assert.assertTrue("properties".equals(exception.getMessage()));
    }
}
