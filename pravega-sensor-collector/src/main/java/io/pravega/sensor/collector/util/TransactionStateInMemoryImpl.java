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

public class TransactionStateInMemoryImpl implements TransactionStateDB{

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

    @Override
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
    @Override
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


    @Override
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
    @Override
    @VisibleForTesting
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException {
        addCompletedFile(fileName, beginOffset, endOffset, newNextSequenceNumber, Optional.empty());
    }

    @Override
    public void deleteTransactionToCommit(Optional<UUID> txnId) {
        transactionCoordinator.deleteTransactionToCommit(txnId);
    }

    /**
     * @return list of file name and end offset (file size)
     */
    @Override
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
    @Override
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
