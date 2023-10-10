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

import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TxnFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinate SQLite and Pravega transactions using a two-phase commit protocol.
 * The ID of a Pravega transaction that is flushed and ready to commit is stored in SQLite until it is committed.
 * When used with {@link io.pravega.sensor.collector.util.TransactionalEventWriter}, this class can be
 * used to implement exactly-once semantics by using Pravega transactions in a two-phase commit protocol.
 *
 * The steps of the exactly-once algorithm are:
 *
 *   1. Update the state in the SQLite database to indicate the workload to process.
 *      Usually this will consist of inserting data or metadata records into a table used as an ordered queue.
 *   2. Using the state in the SQLite database, generate Pravega events.
 *      Usually this will consist of reading from the queue table and deterministically or non-deterministically
 *      mapping them to Pravega events.
 *   3. Write the Pravega events in a Pravega transaction.
 *   4. Flush the Pravega transaction.
 *   5. Atomically update the state in the SQLite database.
 *      a. Update the state in the SQLite database to indicate that this workload has been processed.
 *         Usually this will consist of deleting the processed records from the queue table.
 *      b. Record the Pravega transaction ID by inserting it into the SQLite TransactionsToCommit table.
 *         This will be used if recovery is necessary.
 *   6. Commit the Pravega transaction.
 *   7. Delete the Pravega transaction ID from the SQLite database.
 *
 * If the application terminates unexpectedly, recovery is as follows:
 *
 *  1. Retrieve the Pravega transaction ID from the SQLite database table TransactionsToCommit
 *     If the table is empty, no recovery is required.
 *  2. Commit the Pravega transaction.
 *     Note that committing a transaction multiple times is not an error.
 *     The only scenario in which this will fail (other than retryable network errors)
 *     is when the transaction times out.
 *     The transaction timeout should be set to a large enough value so that data loss
 *     in such a scenario is acceptable.
 *
 */
public class TransactionCoordinator {
    private static final Logger log = LoggerFactory.getLogger(TransactionCoordinator.class);

    private final Connection connection;
    private final EventWriter<byte[]> writer;

    public TransactionCoordinator(Connection connection, EventWriter<byte[]> writer) {
        this.connection = connection;
        this.writer = writer;
        try {
            try (final Statement statement = connection.createStatement()) {
                statement.execute(
                        "create table if not exists TransactionsToCommit (" +
                                "txnId string primary key not null)");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is intended to run as part of a larger transaction.
     * The SQL transaction is not committed.
     * @param txnId the Pravega transaction ID
     */
    public void addTransactionToCommit(Optional<UUID> txnId) {
        try {
            if (txnId.isPresent()) {
                try (final PreparedStatement statement = connection.prepareStatement(
                        "insert into TransactionsToCommit (txnId) values (?)")) {
                    statement.setString(1, txnId.get().toString());
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param txnId the Pravega transaction ID
     */
    public void deleteTransactionToCommit(Optional<UUID> txnId) {
        try {
            if (txnId.isPresent()) {
                try (final PreparedStatement deleteStatement = connection.prepareStatement(
                        "delete from TransactionsToCommit where txnId = ?");
                     final AutoRollback autoRollback = new AutoRollback(connection)) {
                    deleteStatement.setString(1, txnId.get().toString());
                    deleteStatement.execute();
                    autoRollback.commit();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the list of Pravega transactions that have been flushed and are ready to commit.
     * Under normal circumstances, this should return 0 or 1 transactions.
     * @return List of Pravega transaction IDs
     */
    protected List<UUID> getTransactionsToCommit() {
        try {
            try (final Statement statement = connection.createStatement();
                 final ResultSet rs = statement.executeQuery("select txnId from TransactionsToCommit")) {
                final List<UUID> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(UUID.fromString(rs.getString("txnId")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void performRecovery() {
        final List<UUID> transactionsToCommit = getTransactionsToCommit();
        if (transactionsToCommit.isEmpty()) {
            log.debug("Transaction recovery not needed");
        } else {
            log.warn("Transaction recovery needed on {} transactions", transactionsToCommit.size());
            transactionsToCommit.forEach((txnId) -> {
                log.info("Committing transaction {} from a previous process", txnId);
                try {
                    writer.commit(txnId);
                } catch (TxnFailedException e) {
                    log.error(
                            "Unable to commit transaction {} from a previous process. Events may have been lost. " +
                            "Try increasing the transaction timeout.", txnId, e);
                    // Continue recovery and run as normal.
                } catch (RuntimeException e) {
                    if (e.getMessage().startsWith("Unknown transaction")) {
                        // This may occur if an entire Pravega cluster fails and has been recreated, losing prior state.
                        // There is no point in retrying if this error occurs.
                        log.error(
                                "Unable to commit transaction {} from a previous process. Events may have been lost.",
                                txnId, e);
                        // Continue recovery and run as normal.
                    } else {
                        log.error(
                                "Unable to commit transaction {} from a previous process. Events may have been lost. " +
                                        "Try increasing the transaction timeout.", txnId, e);
                        throw e;
                    }
                }
                deleteTransactionToCommit(Optional.of(txnId));
            });
            log.info("Transaction recovery completed");
        }
    }
}
