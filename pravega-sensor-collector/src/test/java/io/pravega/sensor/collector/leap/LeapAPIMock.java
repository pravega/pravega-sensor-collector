package io.pravega.sensor.collector.leap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class LeapAPIMock {

    private static final Logger log = LoggerFactory.getLogger(LeapAPIMock.class);
    private static final String SERVER_URI = "http://0.0.0.0";
    private static final String PORT_NUM = "8083";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     *
     * @return Grizzly HTTP server.
     */
    private static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig().packages("io.pravega.sensor.collector.leap");
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(SERVER_URI + ":" + PORT_NUM), rc);
    }

    public static void main(String[] args) throws Exception {

        final HttpServer server = startServer();
        log.info("Server running at {}", SERVER_URI + ":" + PORT_NUM);
    }
}
