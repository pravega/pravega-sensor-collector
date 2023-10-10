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

import com.google.common.base.Preconditions;
import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TransactionalEventStreamWriter;
import io.pravega.client.stream.TxnFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionalEventWriter<T> implements EventWriter<T> {
    private static final Logger log = LoggerFactory.getLogger(TransactionalEventWriter.class);

    private final TransactionalEventStreamWriter<T> writer;

    /**
     * The currently running transaction to which we write
     */
    private Transaction<T> currentTxn = null;

    public TransactionalEventWriter(TransactionalEventStreamWriter<T> writer) {
        this.writer = writer;
    }

    public void writeEvent(String routingKey, T event) throws TxnFailedException {
        if (currentTxn == null) {
            currentTxn = writer.beginTxn();
            log.info("writeEvent: began transaction {}", currentTxn.getTxnId());
        }
        currentTxn.writeEvent(routingKey, event);
    }

    public Optional<UUID> flush() throws TxnFailedException {
        if (currentTxn == null) {
            return Optional.empty();
        } else {
            log.info("flush: flushing transaction {}", currentTxn.getTxnId());
            currentTxn.flush();
            return Optional.of(currentTxn.getTxnId());
        }
    }

    public void commit() throws TxnFailedException {
        if (currentTxn != null) {
            log.info("commit: committing transaction {}", currentTxn.getTxnId());
            currentTxn.commit();
            currentTxn = null;
        }
    }

    public void commit(long timestamp) throws TxnFailedException {
        if (currentTxn != null) {
            log.info("commit: committing transaction {} with timestamp {}", currentTxn.getTxnId(), timestamp);
            currentTxn.commit(timestamp);
            currentTxn = null;
        }
    }
    private boolean canCommitTransaction(UUID txnId){
        Transaction.Status transactionStatus = writer.getTxn(txnId).checkStatus();
        log.info("canCommitTransaction: Status of Transaction id {} is {}", txnId, transactionStatus);
        return transactionStatus == Transaction.Status.OPEN;
    }

    public void commit(UUID txnId) throws TxnFailedException {
        /*Check the transaction status before committing transaction
        Only transactions which rea in open status can be committed */
        if(canCommitTransaction(txnId)){
            log.info("commit: committing transaction {}", txnId);
            writer.getTxn(txnId).commit();
        }
    }

    public void abort() {
        if (currentTxn != null) {
            log.info("abort: aborting transaction {}", currentTxn.getTxnId());
            currentTxn.abort();
            currentTxn = null;
        }
    }

        public Transaction.Status getTransactionStatus() {
        if (currentTxn != null){
            return currentTxn.checkStatus();
        }
        return null;
    }

    public void close() {
        try {
            abort();
        } catch (Exception e) {
            log.warn("Error aborting transaction", e);
        }
        writer.close();
    }
}
