package io.pravega.sensor.collector.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Maintain state of pending and completed files in in-memory database.
 */
public class TransactionStateInMemoryImpl implements AutoCloseable, TransactionStateDB{

    private static final Logger log = LoggerFactory.getLogger(TransactionStateInMemoryImpl.class);

    private final Connection connection;
    private final TransactionCoordinator transactionCoordinator;

    public TransactionStateInMemoryImpl(Connection connection, TransactionCoordinator transactionCoordinator) {
        this.connection = connection;
        this.transactionCoordinator = transactionCoordinator;
    }
    @VisibleForTesting
    public static TransactionStateInMemoryImpl create(String fileName) {
        final Connection connection = SQliteDBUtility.createDatabase(fileName);
        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, null);
        return new TransactionStateInMemoryImpl(connection, transactionCoordinator);
    }

   @Override
    public void close() throws SQLException {
        connection.close();
    }

    /**
     * Add file name and begin offset to PendingFiles table in in-memory state
     *
     *  @param files      List of file name with Offset.
     *
     */
    @Override
    public void addPendingFileRecords(List<FileNameWithOffset> files) throws SQLException {
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
     * Get next file to process. Read the file name with begin offset from PendingFiles table and sequence number from SequenceNumber table from in-memory state.
     *
     * @return ((file name, begin offset), sequence number) or null if there is no pending file
     */
    @Override
    public Pair<FileNameWithOffset,Long> getNextPendingFileRecord() throws SQLException {
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
     * Update below details in in-memory state table
     *      1. Update sequence number into SequenceNumber table
     *      2. Add entry into CompletedFiles table for given file name and end offset
     *      3. Delete all entry from PendingFiles for given file name offset less than equal to given begin offset value
     *      4. Add transaction id to TransactionsToCommit table if provided
     *
     * @param fileName               file name of processed file
     * @param beginOffset            begin offset from where file read starts
     * @param endOffset              end offset where reading ends.
     * @param newNextSequenceNumber  next sequence number.
     * @param txnId                  transaction id (Optional value) from Pravega.
     *
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
            transactionCoordinator.addTransactionToCommit(txnId);
            autoRollback.commit();
        }
    }

    /**
     * Update below details in -in-memory state tables
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
     * Delete record from TransactionsToCommit table
     *
     * @param  txnId transaction id
     */
    @Override
    public void deleteTransactionToCommit(Optional<UUID> txnId) {
        transactionCoordinator.deleteTransactionToCommit(txnId);
    }

    /**
     * Get a list of files from completedFiles table in -in-memory state
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
     * Delete completed file record from completedFiles table for given file name in in-memory state
     *
     * @param fileName  file name
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
