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

import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.Serializer;
import io.pravega.client.stream.TxnFailedException;

import java.util.Optional;
import java.util.UUID;

/**
 * A wrapper for a Pravega transactional or non-transactional writer.
 * When using the {@link TransactionalEventWriter} implementation, it wraps the commit, and abort methods
 * from {@link io.pravega.client.stream.TransactionalEventStreamWriter}
 * but these are ignored when using the {@link NonTransactionalEventWriter} implementation.
 */
public interface EventWriter<T> extends AutoCloseable {
    static <T> EventWriter<T> create(
            EventStreamClientFactory clientFactory,
            String writerId,
            String streamName,
            Serializer<T> serializer,
            EventWriterConfig config,
            boolean exactlyOnce) {
        final EventWriter<T> writer;
        if (exactlyOnce) {
            writer = new TransactionalEventWriter<T>(
                    clientFactory.createTransactionalEventWriter(writerId, streamName, serializer, config));
        } else {
            writer = new NonTransactionalEventWriter<T>(
                    clientFactory.createEventWriter(writerId, streamName, serializer, config));
        }
        return writer;
    }

    void writeEvent(String routingKey, T event) throws TxnFailedException;

    /**
     * @return a transaction ID that can be passed to commit(UUID txnId), even in another process.
     *         The non-transactional implementation will always return Optional.empty().
     */
    Optional<UUID> flush() throws TxnFailedException;

    void commit() throws TxnFailedException;

    /**
     * @param timestamp is the number of nanoseconds since 1970-01-01
     */
    void commit(long timestamp) throws TxnFailedException;

    /**
     * Commit a transaction given a transaction ID.
     * This method should only be used only when recovering from a failure.
     *
     * @param txnId a return value from flush(), even from another process.
     */
    void commit(UUID txnId) throws TxnFailedException;

    /**
     * This should called be prior to writing events to ensure that they go into a new transaction.
     * This is a no-op if a transaction is not open.
     */
    void abort();

    void close();
}
