package io.pravega.sensor.collector.metrics;

import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.impl.UTF8StringSerializer;
import io.pravega.sensor.collector.PravegaClientConfig;
import io.pravega.sensor.collector.PravegaClientPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Pravega client wrapper to talk
 * to Pravega.
 */
public class PravegaClient {

    private final Logger log = LoggerFactory.getLogger(PravegaClient.class);
    private final String scope;
    private final String streamName;    
    private final URI controllerURI;
    private final EventStreamWriter<String> writer;

    public PravegaClient(String scope, String streamName, URI controllerURI) {
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = controllerURI;
        this.writer = initializeWriter();
    }

    public PravegaClient(String scope, String streamName) {
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = null;
        this.writer = null;
    }

    private EventStreamWriter<String> initializeWriter() {
        log.info("Initializing writer with {} {} {}", this.scope, this.streamName, this.controllerURI.toString());
        ClientConfig clientConfig = ClientConfig.builder().controllerURI(this.controllerURI).build();
        StreamManager streamManager = StreamManager.create(clientConfig);
        final boolean scopeIsNew = streamManager.createScope(scope);

        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(1))
                .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);
        EventStreamClientFactory clientFactory = EventStreamClientFactory.withScope(scope,
                clientConfig);
        EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName,
                     new UTF8StringSerializer(),
                     EventWriterConfig.builder().build());
        return writer;
    }

       public void writeEvent(String routingKey, String message) {
           writer.writeEvent(routingKey, message);
           writer.flush();
       }

       public void close() {
            this.writer.close();
       }
}
