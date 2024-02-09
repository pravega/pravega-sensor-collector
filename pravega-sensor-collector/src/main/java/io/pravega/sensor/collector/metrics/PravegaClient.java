package io.pravega.sensor.collector.metrics;

import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.impl.UTF8StringSerializer;

import java.net.URI;

/**
 * Pravega client wrapper to talk
 * to Pravega.
 */
public class PravegaClient {
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
        StreamManager streamManager = StreamManager.create(controllerURI);
        final boolean scopeIsNew = streamManager.createScope(scope);

        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.fixed(1))
                .build();
        final boolean streamIsNew = streamManager.createStream(scope, streamName, streamConfig);
        EventStreamClientFactory clientFactory = EventStreamClientFactory.withScope(scope,
                ClientConfig.builder().controllerURI(controllerURI).build());
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