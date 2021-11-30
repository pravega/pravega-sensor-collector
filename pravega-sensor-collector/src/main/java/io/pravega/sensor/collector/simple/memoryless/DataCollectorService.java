package io.pravega.sensor.collector.simple.memoryless;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.pravega.sensor.collector.util.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCollectorService <R> extends AbstractExecutionThreadService {

    private final String instanceName;
    private final SimpleMemorylessDriver<R> driver;
    private final EventWriter<byte[]> writer;

    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    public DataCollectorService(String instanceName, SimpleMemorylessDriver<R> driver, EventWriter<byte[]> writer) {
        this.instanceName = instanceName;
        this.driver = driver;
        this.writer = writer;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-"+instanceName;
    }

    @Override
    protected void run() {
        for(;;)
        {
            //TODO : body of the Server polling and write to pravega stream using Eventwriter
        }

    }
}
