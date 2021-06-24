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

import io.pravega.sensor.collector.util.AutoRollback;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

/**
 * A persistent queue that uses a SQLite database on disk.
 */
public class PersistentQueue implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PersistentQueue.class);

    /**
     * The connection should not be used concurrently. Currently, this is enforced
     * by synchronizing access to methods that use it.
     */
    private final Connection connection;
    private final TransactionCoordinator transactionCoordinator;
    private final Semaphore semaphore;

    /**
     * @param capacity Maximum number of elements that can be queued.
     */
    public PersistentQueue(Connection connection, TransactionCoordinator transactionCoordinator, long capacity) {
        try {
            this.connection = connection;
            this.transactionCoordinator = transactionCoordinator;
            final long initialSize = getDatabaseRecordCount();
            log.info("Persistent queue has {} elements.", initialSize);
            final int permits = (int) Long.max(Integer.MIN_VALUE, Long.min(Integer.MAX_VALUE, capacity - initialSize));
            log.info("Semaphore Permits: {}", permits);
            semaphore = new Semaphore(permits);
            performRecovery();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param fileName Name of SQLite database file.
     */
    public static Connection createDatabase(String fileName) {
        try {
            final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
            try (final Statement statement = connection.createStatement()) {
                // Use SQLite exclusive locking mode to ensure that another process or device
                // driver instance is not using this database.
                // statement.execute("PRAGMA locking_mode = EXCLUSIVE");
                statement.execute("create table if not exists Queue (" + "id integer primary key autoincrement, "
                        + "bytes blob not null, " + "routingKey string not null, " + "timestamp integer not null)");
            }
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Add an element. Blocks until there is enough capacity.
     */
    public void add(PersistentQueueElement element) throws SQLException, InterruptedException {
        if (element.id != 0) {
            throw new IllegalArgumentException();
        }
        if (!semaphore.tryAcquire(1)) {
            log.warn("Persistent queue is full. No more elements can be added until elements are removed.");
            semaphore.acquire(1);
            log.info("Persistent queue now has capacity.");
        }
        synchronized (this) {
            try (final PreparedStatement insertStatement = connection
                    .prepareStatement("insert into Queue (bytes, routingKey, timestamp) values (?, ?, ?)");
                    final AutoRollback autoRollback = new AutoRollback(connection)) {
                insertStatement.setBytes(1, element.bytes);
                insertStatement.setString(2, element.routingKey);
                insertStatement.setLong(3, element.timestamp);
                insertStatement.execute();
                autoRollback.commit();
            }
        }
    }

    /**
     * Adds an element to the queue without committing. Blocks until there is enough
     * capacity.
     */
    public void addWithoutCommit(PersistentQueueElement element) throws SQLException, InterruptedException {
        if (element.id != 0) {
            throw new IllegalArgumentException();
        }
        if (!semaphore.tryAcquire(1)) {
            log.warn("Persistent queue is full. No more elements can be added until elements are removed.");
            semaphore.acquire(1);
            log.info("Persistent queue now has capacity.");
        }
        synchronized (this) {
            try (final PreparedStatement insertStatement = connection
                    .prepareStatement("insert into Queue (bytes, routingKey, timestamp) values (?, ?, ?)")) {
                insertStatement.setBytes(1, element.bytes);
                insertStatement.setString(2, element.routingKey);
                insertStatement.setLong(3, element.timestamp);
                insertStatement.execute();
            }
        }
    }

    /**
     * Retrieve up to limit elements. Does not remove elements.
     */
    public synchronized List<PersistentQueueElement> peek(long limit) throws SQLException {
        try (final Statement statement = connection.createStatement();
                final ResultSet rs = statement.executeQuery(
                        "select id, bytes, routingKey, timestamp from Queue order by id limit " + limit)) {
            final List<PersistentQueueElement> result = new ArrayList<>();
            while (rs.next()) {
                final PersistentQueueElement element = new PersistentQueueElement(rs.getLong("id"),
                        rs.getBytes("bytes"), rs.getString("routingKey"), rs.getLong("timestamp"));
                result.add(element);
            }
            return result;
        }
    }

    /**
     * Remove elements from the queue. Before this method is called, it is expected
     * that the elements have been written to Pravega in the Pravega transaction
     * txnId, flushed, but not committed.
     */
    public synchronized void remove(List<PersistentQueueElement> elements, Optional<UUID> txnId) throws SQLException {
        if (!elements.isEmpty()) {
            try (final Statement statement = connection.createStatement();
                    final AutoRollback autoRollback = new AutoRollback(connection)) {
                // Create comma-separated list of ids.
                final String idsStr = String.join(",",
                        elements.stream().map(e -> Long.toString(e.id)).collect(Collectors.toList()));
                final String deleteSql = "delete from Queue where id in (" + idsStr + ")";
                statement.execute(deleteSql);
                transactionCoordinator.addTransactionToCommit(txnId);
                autoRollback.commit();
                semaphore.release(elements.size());
            }
        }
    }

    public synchronized void deleteTransactionToCommit(Optional<UUID> txnId) {
        transactionCoordinator.deleteTransactionToCommit(txnId);
    }

    public synchronized void performRecovery() {
        transactionCoordinator.performRecovery();
    }

    private synchronized long getDatabaseRecordCount() throws SQLException {
        try (final Statement statement = connection.createStatement();
                final ResultSet rs = statement.executeQuery("select count(id) from Queue")) {
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new SQLException("Unexpected query response");
            }
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
