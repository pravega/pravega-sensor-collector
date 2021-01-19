/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.simple;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.pravega.sensor.collector.util.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read events from the persistent queue and write them to Pravega.
 */
public class PersistentQueueToPravegaService extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(PersistentQueueToPravegaService.class);

    private final String instanceName;
    private final PersistentQueue persistentQueue;
    private final EventWriter<byte[]> writer;
    private final int maxEventsPerWriteBatch;
    private final long delayBetweenWriteBatchesMs;

    public PersistentQueueToPravegaService(String instanceName, PersistentQueue persistentQueue, EventWriter<byte[]> writer, int maxEventsPerWriteBatch, long delayBetweenWriteBatchesMs) {
        this.instanceName = instanceName;
        this.persistentQueue = persistentQueue;
        this.writer = writer;
        this.maxEventsPerWriteBatch = maxEventsPerWriteBatch;
        this.delayBetweenWriteBatchesMs = delayBetweenWriteBatchesMs;
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
                // In case a previous iteration encountered an error, we need to ensure that
                // previous flushed transactions are committed and any unflushed transactions are aborted.
                persistentQueue.performRecovery();
                writer.abort();

                List<PersistentQueueElement> events = persistentQueue.peek(maxEventsPerWriteBatch);
                if (!events.isEmpty()) {
                    log.info("Writing {} events to Pravega", events.size());
                    final long t0 = System.nanoTime();
                    long byteCount = 0;
                    long timestamp = 0;
                    for (PersistentQueueElement event : events) {
                        log.trace("Writing event={}", event);
                        byte[] eventBytes = event.bytes;
                        byteCount += eventBytes.length;
                        timestamp = Long.max(timestamp, event.timestamp);
                        writer.writeEvent(event.routingKey, eventBytes);
                    }
                    final Optional<UUID> txnId = writer.flush();
                    final double ms = (System.nanoTime() - t0) * 1e-6;
                    persistentQueue.remove(events, txnId);
                    // injectCommitFailure();
                    writer.commit(timestamp);
                    persistentQueue.deleteTransactionToCommit(txnId);
                    log.info(String.format("Done writing %d bytes in %d events in %.3f ms to Pravega",
                            byteCount, events.size(), ms));
                }
                // Only sleep if the batch was not full.
                if (events.size() < maxEventsPerWriteBatch) {
                    Thread.sleep(delayBetweenWriteBatchesMs);
                }
            } catch (Exception e) {
                log.error("Error", e);
                Thread.sleep(10000);
                // Continue on any errors. We will retry on the next iteration.
            }
        }
    }

    /**
     * Inject a failure before commit for testing.
     */
    protected void injectCommitFailure() {
        if (Math.random() < 0.1) {
            throw new RuntimeException("injectCommitFailure: Commit failure test exception");
        }
    }
}
