/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.stateful;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.simple.PersistentQueue;
import io.pravega.sensor.collector.util.AutoRollback;

/**
 */
public class DataCollectorService<S> extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    private final String instanceName;
    private final PersistentQueue persistentQueue;
    private final StatefulSensorDeviceDriver<S> driver;

    public DataCollectorService(String instanceName, PersistentQueue persistentQueue,
            StatefulSensorDeviceDriver<S> driver) {
        this.instanceName = instanceName;
        this.persistentQueue = persistentQueue;
        this.driver = driver;
    }

    @Override
    protected String serviceName() {
        return super.serviceName() + "-" + instanceName;
    }

    @Override
    protected void run() throws Exception {
        log.info("Running");
        for (;;) {
            try {
                // Get state from persistent database
                ReadingState readingState = new ReadingState(persistentQueue.getConnection());
                S state = (S) readingState.getState();
                try (final AutoRollback autoRollback = new AutoRollback(persistentQueue.getConnection())) {
                    // Poll sensor for events.
                    final PollResponse<S> response = driver.pollEvents(state);
                    // Add samples, state atomically to persistent queue.
                    response.events.stream().forEach(event -> {
                        try {
                            log.trace("Adding event {}", event);
                            persistentQueue.addWithoutCommit(event);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    // Write state to database
                    log.info("Last state = {}", state);
                    readingState.updateState((String) response.state);
                    log.info("New State = {}", response.state);
                    // Commit SQL transaction
                    autoRollback.commit();
                }
            } catch (Exception e) {
                log.error("Error", e);
                Thread.sleep(10000);
                // Continue on any errors. We will retry on the next iteration.
            }
        }
    }
}
