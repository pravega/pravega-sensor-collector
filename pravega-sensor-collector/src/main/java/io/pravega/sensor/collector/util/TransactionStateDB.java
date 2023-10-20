package io.pravega.sensor.collector.util;

import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionStateDB {

    public void close() throws SQLException;
    public void addPendingFiles(List<FileNameWithOffset> files) throws SQLException;
    public Pair<FileNameWithOffset,Long> getNextPendingFile() throws SQLException;
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber, Optional<UUID> txnId) throws SQLException;
    public void addCompletedFile(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException;
    public void deleteTransactionToCommit(Optional<UUID> txnId);
    public List<FileNameWithOffset> getCompletedFiles() throws SQLException;
    public void deleteCompletedFile(String fileName) throws SQLException;



}
