/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.rawfile;

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

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.AutoRollback;
import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.TransactionCoordinator;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

/**
 * Maintain state of pending and completed files in SQLite database.  
 */
public class RawFileState implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(RawFileState.class);

    private final Connection connection;
    private final TransactionCoordinator transactionCoordinator;

    public RawFileState(Connection connection, TransactionCoordinator transactionCoordinator) {
        this.connection = connection;
        this.transactionCoordinator = transactionCoordinator;
    }

    public static Connection createDatabase(String fileName) {
        try {
            final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
            try (final Statement statement = connection.createStatement()) {
                // Use SQLite exclusive locking mode to ensure that another process or device driver instance is not using this database.
                //statement.execute("PRAGMA locking_mode = EXCLUSIVE");
                statement.execute(
                        "create table if not exists PendingFiles (" +
                                "id integer primary key autoincrement, " +
                                "fileName string unique not null, " +
                                "offset bigint not null)");
                statement.execute(
                        "create table if not exists CompletedFiles (" +
                                "fileName string primary key not null, " +
                                "offset bigint not null)");
                statement.execute(
                        "create table if not exists SequenceNumber (" +
                                "id integer primary key check (id = 0), " +
                                "nextSequenceNumber bigint not null)");
                statement.execute(
                        "insert or ignore into SequenceNumber (id, nextSequenceNumber) values (0, 0)");
            }
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public static RawFileState create(String fileName) {
        final Connection connection = createDatabase(fileName);
        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, null);
        return new RawFileState(connection, transactionCoordinator);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public void addPendingFiles(List<FileNameWithOffset> files) throws SQLException {
        try (final PreparedStatement insertStatement = connection.prepareStatement(
                "insert or ignore into PendingFiles (fileName, offset) values (?, ?)");
             final AutoRollback autoRollback = new AutoRollback(connection)) {
            for (FileNameWithOffset file: files) {
                insertStatement.setString(1, file.fileName);
                insertStatement.setLong(2, file.offset);
                insertStatement.execute();
            }
            autoRollback.commit();
        }
    }

    /**
     * @return ((file name, begin offset), sequence number) or null if there is no pending file
     */
    public Pair<FileNameWithOffset,Long> getNextPendingFile() throws SQLException {
        try (final Statement statement = connection.createStatement();
             final ResultSet rs = statement.executeQuery("select fileName, offset from PendingFiles order by id limit 1")) {
            if (rs.next()) {
                final FileNameWithOffset fileNameWithOffset = new FileNameWithOffset(rs.getString("fileName"), rs.getLong("offset"));
                try (final ResultSet rsSequenceNumber = statement.executeQuery("select nextSequenceNumber from SequenceNumber")) {
                    rsSequenceNumber.next();
                    final long nextSequenceNumber = rsSequenceNumber.getLong(1);
                    return new ImmutablePair<>(fileNameWithOffset, nextSequenceNumber);
                }
            } else {
                return null;
            }
        } finally {
            connection.commit();
        }
    }

    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber, Optional<UUID> txnId) throws SQLException {
        try (final PreparedStatement updateSequenceNumberStatement = connection.prepareStatement(
                "update SequenceNumber set nextSequenceNumber = ?");
             final PreparedStatement insertCompletedFileStatement = connection.prepareStatement(
                     "insert or ignore into CompletedFiles (fileName, offset) values (?, ?)");
             final PreparedStatement deletePendingFileStatement = connection.prepareStatement(
                     "delete from PendingFiles where fileName = ? and offset <= ?");
             final AutoRollback autoRollback = new AutoRollback(connection)) {
            // Update sequence number.
            updateSequenceNumberStatement.setLong(1, newNextSequenceNumber);
            updateSequenceNumberStatement.execute();
            // Add completed file.
            insertCompletedFileStatement.setString(1, fileName);
            insertCompletedFileStatement.setLong(2, endOffset);
            insertCompletedFileStatement.execute();
            // Remove pending file.
            deletePendingFileStatement.setString(1, fileName);
            deletePendingFileStatement.setLong(2, beginOffset);
            deletePendingFileStatement.execute();
            transactionCoordinator.addTransactionToCommit(txnId);
            autoRollback.commit();
        }
    }

    @VisibleForTesting
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException {
        addCompletedFile(fileName, beginOffset, endOffset, newNextSequenceNumber, Optional.empty());
    }

    public void deleteTransactionToCommit(Optional<UUID> txnId) {
        transactionCoordinator.deleteTransactionToCommit(txnId);
    }

    /**
     * @return list of file name and end offset (file size)
     */
    public List<FileNameWithOffset> getCompletedFiles() throws SQLException {
        try (final Statement statement = connection.createStatement();
             final ResultSet rs = statement.executeQuery("select fileName, offset from completedFiles")) {
            final List<FileNameWithOffset> files = new ArrayList<>();
            while (rs.next()) {
                final FileNameWithOffset fileNameWithOffset = new FileNameWithOffset(rs.getString("fileName"), rs.getLong("offset"));
                files.add(fileNameWithOffset);
            }
            return files;
        } finally {
            connection.commit();
        }
    }

    public void deleteCompletedFile(String fileName) throws SQLException {
        try (final PreparedStatement deleteStatement = connection.prepareStatement(
                     "delete from CompletedFiles where fileName = ?");
             final AutoRollback autoRollback = new AutoRollback(connection)) {
            deleteStatement.setString(1, fileName);
            deleteStatement.execute();
            autoRollback.commit();
        }
    }
    
}
