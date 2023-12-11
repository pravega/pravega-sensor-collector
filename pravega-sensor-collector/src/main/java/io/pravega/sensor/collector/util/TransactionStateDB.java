package io.pravega.sensor.collector.util;

import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionStateDB {

    /**
     * Add file name and begin offset to PendingFiles table
     *
     *  @param files      List of file name with Offset.
     *
     */
    public void addPendingFileRecords(List<FileNameWithOffset> files) throws SQLException;

    /**
     * Get next file to process. Read the file name with begin offset from PendingFiles table and sequence number from SequenceNumber table.
     *
     * @return ((file name, begin offset), sequence number) or null if there is no pending file
     */
    public Pair<FileNameWithOffset,Long> getNextPendingFileRecord() throws SQLException;

    /**
     * Update below details
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
    public void addCompletedFileRecord(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber, Optional<UUID> txnId) throws SQLException;

    /**
     * Delete record from pendingFiles table
     *
     * @param fileName          file name of pending file
     * @param beginOffset       begin offset from where file read starts
     */
    void deletePendingFile(String fileName, long beginOffset) throws SQLException;

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
    public void addCompletedFileRecord(String fileName, long beginOffset, long endOffset, long newNextSequenceNumber) throws SQLException;

    /**
    * Delete record from TransactionsToCommit table
    *
    * @param  txnId transaction id
    */
    public void deleteTransactionToCommit(Optional<UUID> txnId);

    /**
     * Get a list of files from completedFiles table
     *
     * @return list of file name and end offset (file size)
     */
    public List<FileNameWithOffset> getCompletedFileRecords() throws SQLException;

    /**
     * Delete completed file record from completedFiles table for given file name
     *
     * @param fileName  file name
     */
    public void deleteCompletedFileRecord(String fileName) throws SQLException;



}
