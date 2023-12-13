package io.pravega.sensor.collector;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PravegaClientConfigTests {

    @Test
    public void testConstructorWithValues(){
        URI uri = URI.create("tcp://localhost:9090");
        String scopeName = "testScope";
        PravegaClientConfig conf = new PravegaClientConfig(uri,scopeName);
        assertEquals(scopeName, conf.getScopeName());
        assertEquals(uri, conf.toClientConfig().getControllerURI());

    }

    @Test
    public void testConstructorWithProperties() {
        URI uri = URI.create("tcp://example.com:9090");
        String scopeName = "testScope";

        Map<String, String> properties = new HashMap<>();
        properties.put("PRAVEGA_CONTROLLER_URI", uri.toString());
        PravegaClientConfig configFile = new PravegaClientConfig(properties, scopeName);
        assertEquals(uri, configFile.toClientConfig().getControllerURI());
        assertEquals(scopeName, configFile.getScopeName());
    }

    @Test
    public void testConstructorWithPropertiesDefaultURI() {
        String scopeName = "testScope";

        Map<String, String> properties = Collections.emptyMap();

        PravegaClientConfig configFile = new PravegaClientConfig(properties, scopeName);

        assertEquals(URI.create("tcp://localhost:9090"), configFile.toClientConfig().getControllerURI());
        assertEquals(scopeName, configFile.getScopeName());
    }

    @Test
    public void testEqualsAndHashCode() {
        URI uri1 = URI.create("tcp://localhost:9090");
        String scopeName1 = "testScope1";
        PravegaClientConfig configFile1 = new PravegaClientConfig(uri1, scopeName1);

        URI uri2 = URI.create("tcp://localhost:9090");
        String scopeName2 = "testScope1";
        PravegaClientConfig configFile2 = new PravegaClientConfig(uri2, scopeName2);

        assertEquals(configFile1, configFile2);
        assertEquals(configFile1.hashCode(), configFile2.hashCode());
    }

}
