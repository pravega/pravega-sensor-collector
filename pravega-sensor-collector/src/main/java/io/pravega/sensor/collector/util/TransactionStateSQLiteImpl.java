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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
 * Maintain state of pending and completed files in SQLite database.
*/
public class TransactionStateSQLiteImpl  implements AutoCloseable, TransactionStateDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionStateSQLiteImpl.class);

    private final Connection connection;
    private final TransactionCoordinator transactionCoordinator;

    public TransactionStateSQLiteImpl(Connection connection, TransactionCoordinator transactionCoordinator) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
        this.transactionCoordinator = transactionCoordinator;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    /**
     * Add file name and begin offset to PendingFiles table.
     *
     * @param files List of file name with Offset.
     * @throws SQLException
     */
    @Override
    public void addPendingFileRecords(List<FileNameWithOffset> files) throws SQLException {
        try (final PreparedStatement insertStatement = connection.prepareStatement(
                "insert or ignore into PendingFiles (fileName, offset) values (?, ?)");
             final AutoRollback autoRollback = new AutoRollback(connection)) {
            for (FileNameWithOffset file : files) {
                insertStatement.setString(1, file.fileName);
                insertStatement.setLong(2, file.offset);
                insertStatement.execute();
            }
            autoRollback.commit();
        }
    }

    /**
     * Get next file to process. Read the file name with begin offset from PendingFiles table and sequence number from SequenceNumber table.
     *
     * @return (( file name, begin offset), sequence number) or null if there is no pending file
     */
    @Override
    public Pair<FileNameWithOffset, Long> getNextPendingFileRecord() throws SQLException {
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


    /**
     * Update below details
     * 1. Update sequence number into SequenceNumber table
     * 2. Add entry into CompletedFiles table for given file name and end offset
     * 3. Delete all entry from PendingFiles for given file name offset less than equal to given begin offset value
     * 4. Add transaction id to TransactionsToCommit table if provided
     *
     * @param fileName              file name of processed file
     * @param beginOffset           begin offset from where file read starts
     * @param endOffset             end offset where reading ends.
     * @param newNextSequenceNumber next sequence number.
     * @param txnId                 transaction id (Optional value) from Pravega.
     */
    @Override
    public void addCompletedFileRecord(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber, Optional<UUID> txnId) throws SQLException {
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
            if (transactionCoordinator != null) {
                transactionCoordinator.addTransactionToCommit(txnId);
            }
            autoRollback.commit();
        }
    }

    /**
     * Update below details
     *      1. Update sequence number into SequenceNumber table
     *      2. Add entry into CompletedFiles table for given file name and end offset
     *      3. Delete all entry from PendingFiles for given file name offset less than equal to given begin offset value
     * @param fileName               file name of processed file
     * @param beginOffset            begin offset from where file read starts
     * @param endOffset              end offset where reading ends.
     * @param newNextSequenceNumber  next sequence number.
     *
     */
    @Override
    @VisibleForTesting
    public void addCompletedFileRecord(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException {
        addCompletedFileRecord(fileName, beginOffset, endOffset, newNextSequenceNumber, Optional.empty());
    }

    /**
     * Delete record from PendingFiles table.
     *
     * @param  fileName         file name of pending file
     * @param beginOffset       begin offset from where file read starts
     */
    @Override
    public void deletePendingFile(String fileName, long beginOffset) throws SQLException {
        try (final PreparedStatement deletePendingFileStatement = connection.prepareStatement(
                     "delete from PendingFiles where fileName = ? and offset <= ?");) {
            // Remove pending file.
            deletePendingFileStatement.setString(1, fileName);
            deletePendingFileStatement.setLong(2, beginOffset);
            deletePendingFileStatement.execute();
        }
    }

    /**
     * Delete record from TransactionsToCommit table.
     *
     * @param txnId transaction id
     */
    @Override
    public void deleteTransactionToCommit(Optional<UUID> txnId) {
        if (transactionCoordinator != null) {
            transactionCoordinator.deleteTransactionToCommit(txnId);
        }
    }

    /**
     * Get a list of files from completedFiles table.
     *
     * @return list of file name and end offset (file size)
     */
    @Override
    public List<FileNameWithOffset> getCompletedFileRecords() throws SQLException {
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

    /**
     * Delete completed file record from completedFiles table for given file name.
     *
     * @param fileName file name
     */
    @Override
    public void deleteCompletedFileRecord(String fileName) throws SQLException {
        try (final PreparedStatement deleteStatement = connection.prepareStatement(
                "delete from CompletedFiles where fileName = ?");
             final AutoRollback autoRollback = new AutoRollback(connection)) {
            deleteStatement.setString(1, fileName);
            deleteStatement.execute();
            autoRollback.commit();
        }
    }

}
