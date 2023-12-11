package io.pravega.sensor.collector.file;
/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.google.common.io.CountingInputStream;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TxnFailedException;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Get list of files obtained from config. Process each file for ingestion.
 * Keep track of new files and delete ingested files if "DELETE_COMPLETED_FILES"=true.
 */
public abstract class FileProcessor {
    private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);

    private final FileConfig config;
    private final TransactionStateDB state;
    private final EventWriter<byte[]> writer;
    private final TransactionCoordinator transactionCoordinator;
    private final EventGenerator eventGenerator;

    public FileProcessor(FileConfig config, TransactionStateDB state, EventWriter<byte[]> writer, TransactionCoordinator transactionCoordinator) {
        this.config = config;
        this.state = state;
        this.writer = writer;
        this.transactionCoordinator = transactionCoordinator;
        this.eventGenerator = getEventGenerator(config);
    }

    public static FileProcessor create(
            FileConfig config, EventStreamClientFactory clientFactory){

        final Connection connection = SQliteDBUtility.createDatabase(config.stateDatabaseFileName);

        final String writerId = new PersistentId(connection).getPersistentId().toString();
        log.info("Writer ID: {}", writerId);

        final EventWriter<byte[]> writer = EventWriter.create(
                clientFactory,
                writerId,
                config.streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder()
                        .enableConnectionPooling(true)
                        .transactionTimeoutTime((long) (config.transactionTimeoutMinutes * 60.0 * 1000.0))
                        .build(),
                config.exactlyOnce);

        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, writer);
        transactionCoordinator.performRecovery();

        final TransactionStateDB state = new TransactionStateSQLiteImpl(connection, transactionCoordinator);
        return FileProcessorFactory.createFileSequenceProcessor(config, state, writer, transactionCoordinator,writerId);

    }

    /* The abstract method serves as an Event Generator responsible for generating events.
     * This logic is tailored to specific file types, and as such, it will be implemented in their respective classes.
     * @param config configurations parameters
     * @return eventGenerator
     */
    public abstract EventGenerator getEventGenerator(FileConfig config);
    public void watchFiles() throws Exception {
        findAndRecordNewFiles();
    }
    public void processFiles() throws Exception {
        log.debug("processFiles: BEGIN");
        if (config.enableDeleteCompletedFiles) {
            log.debug("processFiles: Deleting completed files");
            deleteCompletedFiles();
        }
        processNewFiles();
        log.debug("processFiles: END");
    }

    public void processNewFiles() throws Exception {
        for (;;) {
            // If nextFile is null then check for new files to process is handled as part of scheduleWithDelay
            final Pair<FileNameWithOffset, Long> nextFile = state.getNextPendingFileRecord();
            if (nextFile == null) {
                log.debug("processNewFiles: No more files to watch");
                break;
            } else {
                processFile(nextFile.getLeft(), nextFile.getRight());
            }
        }
    }

    protected void findAndRecordNewFiles() throws Exception {
        final List<FileNameWithOffset> directoryListing = getDirectoryListing();
        final List<FileNameWithOffset> completedFiles = state.getCompletedFileRecords();
        final List<FileNameWithOffset> newFiles = getNewFiles(directoryListing, completedFiles);
        state.addPendingFileRecords(newFiles);
    }

    /**
     * @return list of file name and file size in bytes
     */
    protected List<FileNameWithOffset> getDirectoryListing() throws IOException {
        log.debug("getDirectoryListing: fileSpec={}", config.fileSpec);
        //Invalid files will be moved to a separate folder Failed_Files next to the database file
        Path failedFilesDirectory = Paths.get(config.stateDatabaseFileName).getParent();
        final List<FileNameWithOffset> directoryListing = FileUtils.getDirectoryListing(config.fileSpec, config.fileExtension, failedFilesDirectory);
        log.debug("getDirectoryListing: directoryListing={}", directoryListing);
        return directoryListing;
    }

    /**
     * @return sorted list of file name and file size in bytes
     */
    static protected List<FileNameWithOffset> getNewFiles(List<FileNameWithOffset> directoryListing, List<FileNameWithOffset> completedFiles) {
        final ArrayList<FileNameWithOffset> sortedDirectoryListing = new ArrayList<>(directoryListing);
        Collections.sort(sortedDirectoryListing);
        final List<FileNameWithOffset> newFiles = new ArrayList<>();
        final Set<FileNameWithOffset> setCompletedFiles = new HashSet<>(completedFiles);
        log.trace("setCompletedFiles={}", setCompletedFiles);
        sortedDirectoryListing.forEach(dirFile -> {
            if (!setCompletedFiles.contains(dirFile)) {
                newFiles.add(new FileNameWithOffset(dirFile.fileName, 0));
            }
        });
        log.info("getNewFiles: new file lists = {}", newFiles);
        return newFiles;
    }

    void processFile(FileNameWithOffset fileNameWithBeginOffset, long firstSequenceNumber) throws Exception {
        log.info("processFile: Ingesting file {}; beginOffset={}, firstSequenceNumber={}",
                fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset, firstSequenceNumber);

        AtomicLong numofbytes = new AtomicLong(0);
        long timestamp = System.nanoTime();
        // In case a previous iteration encountered an error, we need to ensure that
        // previous flushed transactions are committed and any unflushed transactions as aborted.
        transactionCoordinator.performRecovery();
        /* Check if transactions can be aborted.
         * Will fail with {@link TxnFailedException} if the transaction has already been committed or aborted.
         */
        log.debug("processFile: Transaction status {} ", writer.getTransactionStatus());
        if(writer.getTransactionStatus() == Transaction.Status.OPEN){
            writer.abort();
        }

        File pendingFile = new File(fileNameWithBeginOffset.fileName);
        if(!pendingFile.exists()){
            log.warn("File {} does not exist. It was deleted before processing", fileNameWithBeginOffset.fileName);
            state.deletePendingFile(fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset);
            return;
        }

        try (final InputStream inputStream = new FileInputStream(fileNameWithBeginOffset.fileName)) {
            final CountingInputStream countingInputStream = new CountingInputStream(inputStream);
            countingInputStream.skip(fileNameWithBeginOffset.offset);
            final Pair<Long,Long> result = eventGenerator.generateEventsFromInputStream(countingInputStream, firstSequenceNumber,
                    e -> {
                        log.trace("processFile: event={}", e);
                        try {
                             writer.writeEvent(e.routingKey, e.bytes);
                            numofbytes.addAndGet(e.bytes.length);
                        } catch (TxnFailedException ex) {
                            log.error("processFile: Write event to transaction failed with exception {} while processing file: {}, event: {}", ex, fileNameWithBeginOffset.fileName, e);

                           /* TODO while writing event if we get Transaction failed exception then should we abort the trasaction and process again?
                            This will occur only if Transaction state is not open*/

                            throw new RuntimeException(ex);
                        }
                    });
            final Optional<UUID> txnId = writer.flush();
            final long nextSequenceNumber = result.getLeft();
            final long endOffset = result.getRight();
            log.debug("processFile: Adding completed file: {}",  fileNameWithBeginOffset.fileName);
            state.addCompletedFileRecord(fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset, endOffset, nextSequenceNumber, txnId);
            // injectCommitFailure();
            try {
                // commit fails only if Transaction is not in open state.
                log.info("processFile: Commit transaction for Id: {}; file: {}", txnId.orElse(null), fileNameWithBeginOffset.fileName);
                writer.commit();
            } catch (TxnFailedException ex) {
                log.error("processFile: Commit transaction for id: {}, file : {}, failed with exception: {}", txnId, fileNameWithBeginOffset.fileName, ex);
                throw new RuntimeException(ex);
            }
            // Add to completed file list only if commit is successfull else it will be taken care as part of recovery
            if(txnId.isPresent()){
                Transaction.Status status = writer.getTransactionStatus(txnId.get());
                if(status == Transaction.Status.COMMITTED || status == Transaction.Status.ABORTED)
                    state.deleteTransactionToCommit(txnId);
            }

            double elapsedSec = (System.nanoTime() - timestamp) / 1_000_000_000.0;
            double megabyteCount = numofbytes.getAndSet(0) / 1_000_000.0;
            double megabytesPerSec = megabyteCount / elapsedSec;
            log.info("Sent {} MB in {} sec. Transfer rate: {} MB/sec ", megabyteCount, elapsedSec, megabytesPerSec );
            log.info("processFile: Finished ingesting file {}; endOffset={}, nextSequenceNumber={}",
                    fileNameWithBeginOffset.fileName, endOffset, nextSequenceNumber);
        }
        // Delete file right after ingesting
        if (config.enableDeleteCompletedFiles) {
            deleteCompletedFiles();
        }
    }

    void deleteCompletedFiles() throws Exception {
        final List<FileNameWithOffset> completedFiles = state.getCompletedFileRecords();
        completedFiles.forEach(file -> {
            //Obtain a lock on file
            try(FileChannel channel = FileChannel.open(Paths.get(file.fileName), StandardOpenOption.WRITE)){
                try(FileLock lock = channel.tryLock()) {
                    if(lock!=null){
                        Files.deleteIfExists(Paths.get(file.fileName));
                        log.info("deleteCompletedFiles: Deleted file {}", file.fileName);
                        lock.release();
                        // Only remove from database if we could delete file.
                        state.deleteCompletedFileRecord(file.fileName);
                    }
                    else{
                        log.warn("Unable to obtain lock on file {}. File is locked by another process.", file.fileName);
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to delete ingested file {}", e.getMessage());
                log.warn("Deletion will be retried on the next iteration.");
                // We can continue on this error. Deletion will be retried on the next iteration.
            }
        });
    }

    /**
     * Inject a failure before commit for testing.
     */
    protected void injectCommitFailure() {
        if (Math.random() < 0.3) {
            throw new RuntimeException("injectCommitFailure: Commit failure test exception");
        }
    }
}
