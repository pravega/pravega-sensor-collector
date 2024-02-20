package io.pravega.sensor.collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PravegaSensorCollectorAppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PravegaSensorCollectorAppTest.class);

    @Test
    public void testPravegaSensorCollector(){
        PravegaSensorCollectorApp app = new PravegaSensorCollectorApp();
        Assertions.assertNotNull(app.toString());
    }
}
