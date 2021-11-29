package io.pravega.sensor.collector.simple.memoryless;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCollectorService <R> extends AbstractExecutionThreadService {

    private final String instanceName;
    private final SimpleMemorylessDriver<R> driver;

    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    public DataCollectorService(String instanceName, SimpleMemorylessDriver<R> driver) {
        this.instanceName = instanceName;
        this.driver = driver;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-"+instanceName;
    }

    @Override
    protected void run() {
        log.info("This shall be infinite loop and poll the data from OPA Servers  using driver");
        for(;;)
        {
            //body of the Server polling.
        }

    }
}
