/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.util;

import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.TxnFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * commit and abort methods are ignored. Writes are performed without transactions.
 */
public class NonTransactionalEventWriter<T> implements EventWriter<T> {
    private static final Logger log = LoggerFactory.getLogger(NonTransactionalEventWriter.class);

    private final EventStreamWriter<T> writer;

    public NonTransactionalEventWriter(EventStreamWriter<T> writer) {
        this.writer = writer;
    }

    public void writeEvent(String routingKey, T event) {
        writer.writeEvent(routingKey, event);
    }

    public Optional<UUID> flush() {
        writer.flush();
        return Optional.empty();
    }

    public void commit() {
    }

    public void commit(long timestamp) {
        log.info("commit: noting timestamp {}", timestamp);
        writer.noteTime(timestamp);
    }

    public void commit(UUID txnId) throws TxnFailedException {
        throw new UnsupportedOperationException("Non-transactional writer cannot commit transactions");
    }

    public void abort() {
    }

    public void close() {
        writer.close();
    }
}
