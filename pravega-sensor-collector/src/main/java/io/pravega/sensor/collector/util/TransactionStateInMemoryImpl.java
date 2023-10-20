package io.pravega.sensor.collector.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionStateInMemoryImpl implements TransactionStateDB{


    @VisibleForTesting
    public static TransactionStateSQLiteImpl create(String fileName) {
        final Connection connection = SQliteDBUtility.createDatabase(fileName);
        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, null);
        return new TransactionStateSQLiteImpl(connection, transactionCoordinator);
    }
    @Override
    public void close() throws SQLException {

    }

    @Override
    public void addPendingFiles(List<FileNameWithOffset> files) throws SQLException {

    }

    @Override
    public Pair<FileNameWithOffset, Long> getNextPendingFile() throws SQLException {
        return null;
    }

    @Override
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber, Optional<UUID> txnId) throws SQLException {

    }

    @Override
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException {

    }

    @Override
    public void deleteTransactionToCommit(Optional<UUID> txnId) {

    }

    @Override
    public List<FileNameWithOffset> getCompletedFiles() throws SQLException {
        return null;
    }

    @Override
    public void deleteCompletedFile(String fileName) throws SQLException {

    }
}
