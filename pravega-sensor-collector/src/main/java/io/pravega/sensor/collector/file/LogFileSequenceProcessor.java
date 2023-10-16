/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file;

import com.google.common.io.CountingInputStream;
import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.Transaction;
import io.pravega.client.stream.TxnFailedException;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.sensor.collector.util.EventWriter;
import io.pravega.sensor.collector.util.PersistentId;
import io.pravega.sensor.collector.util.TransactionCoordinator;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LogFileSequenceProcessor {
    private static final Logger log = LoggerFactory.getLogger(LogFileSequenceProcessorState.class);

    private final LogFileSequenceConfig config;
    private final LogFileSequenceProcessorState state;
    private final EventWriter<byte[]> writer;
    private final TransactionCoordinator transactionCoordinator;
    private final EventGenerator eventGenerator;

    public LogFileSequenceProcessor(LogFileSequenceConfig config, LogFileSequenceProcessorState state, EventWriter<byte[]> writer, TransactionCoordinator transactionCoordinator, EventGenerator eventGenerator) {
        this.config = config;
        this.state = state;
        this.writer = writer;
        this.transactionCoordinator = transactionCoordinator;
        this.eventGenerator = eventGenerator;
    }

    public static LogFileSequenceProcessor create(
            LogFileSequenceConfig config, EventStreamClientFactory clientFactory){

        final Connection connection = LogFileSequenceProcessorState.createDatabase(config.stateDatabaseFileName);

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

        final EventGenerator eventGenerator = EventGenerator.create(
                config.routingKey,
                config.maxRecordsPerEvent,
                config.eventTemplateStr,
                writerId);
        final LogFileSequenceProcessorState state = new LogFileSequenceProcessorState(connection, transactionCoordinator);
        return new LogFileSequenceProcessor(config, state, writer, transactionCoordinator, eventGenerator);
    }

    public void watchLogFiles() throws Exception {
        findAndRecordNewFiles();
    }
    public void processLogFiles() throws Exception {
        log.info("processLogFiles: BEGIN");
        processNewFiles();
        if (config.enableDeleteCompletedFiles) {
            log.debug("processLogFiles: Deleting completed files");
            deleteCompletedFiles();
        }
        log.info("processLogFiles: END");
    }

    public void processNewFiles() throws Exception {
        for (;;) {
            /*
            Todo
                1. Can we get list of files instead of reading one by one?
                2. If nextFile is null then check the db again after given interval(1 sec)
                    Handled with as part of scheduleWithDelay
             */
            final Pair<FileNameWithOffset, Long> nextFile = state.getNextPendingFile();
            if (nextFile == null) {
                log.info("processNewFiles: No more files to watch");
                break;
            } else {
                processFile(nextFile.getLeft(), nextFile.getRight());
            }
        }
    }

    protected void findAndRecordNewFiles() throws Exception {
        final List<FileNameWithOffset> directoryListing = getDirectoryListing();
        final List<FileNameWithOffset> completedFiles = state.getCompletedFiles();
        final List<FileNameWithOffset> newFiles = getNewFiles(directoryListing, completedFiles);
        state.addPendingFiles(newFiles);
    }

    /**
     * @return list of file name and file size in bytes
     */
    protected List<FileNameWithOffset> getDirectoryListing() throws IOException {
        log.info("getDirectoryListing: fileSpec={}", config.fileSpec);
        final List<FileNameWithOffset> directoryListing = getDirectoryListing(config.fileSpec, config.fileExtension);
        log.trace("getDirectoryListing: directoryListing={}", directoryListing);
        return directoryListing;
    }

    /**
     * @return list of file name and file size in bytes
     * Todo handle the below cases
     *  1. If given file path does not exist then log the message and abort the task
     *  2. If directory does not exist and no file with given extn like .csv then log the message and abort the task
     *  3. check for empty file, log the message and continue with valid files
     *  How to abort the task? By throwing an exception back to caller?
     *
     */
    static protected List<FileNameWithOffset> getDirectoryListing(String fileSpec, String fileExtension) throws IOException {
        final Path pathSpec = Paths.get(fileSpec);
        if (!Files.isDirectory(pathSpec.toAbsolutePath())) {
            log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
            throw new IOException("Directory does not exist or spec is not valid");
        }
        List<FileNameWithOffset> directoryListing = new ArrayList<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathSpec)) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path))         //traverse subdirectories
                    directoryListing.addAll(getDirectoryListing(path.toString(), fileExtension));
                else {
                    FileNameWithOffset fileEntry = new FileNameWithOffset(path.toAbsolutePath().toString(), path.toFile().length());
                    if (isValidFile(fileEntry, fileExtension)) {
                        directoryListing.add(fileEntry);
                    }
                }
            }
        }catch(Exception ex){
            if(ex instanceof IOException){
                log.error("getDirectoryListing: Directory does not exist or spec is not valid : {}", pathSpec.toAbsolutePath());
                throw new IOException("Directory does not exist or spec is not valid");
            }else{
                log.error("getDirectoryListing: Exception while listing files: {}", pathSpec.toAbsolutePath());
                throw new IOException(ex);
            }
        }
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
        log.info("getNewFiles: new file lists ={}", newFiles);
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

        try (final InputStream inputStream = new FileInputStream(fileNameWithBeginOffset.fileName)) {
            final CountingInputStream countingInputStream = new CountingInputStream(inputStream);
            countingInputStream.skip(fileNameWithBeginOffset.offset);
            final Pair<Long,Long> result = eventGenerator.generateEventsFromInputStream(countingInputStream, firstSequenceNumber,
                    e -> {
                        log.trace("processFile: event={}", e);
                        try {
                            writer.writeEvent(e.routingKey, e.bytes);
                        } catch (TxnFailedException ex) {
                            log.error("processFile: Write event to transaction failed with exception {} while processing file: {}, event: {}", ex, fileNameWithBeginOffset.fileName, e);
                            /*
                            TODO while writing event if we get Transaction failed exception then should we abort the trasaction and process again?
                            This will occur only if Transaction state is not open
                             */
                            throw new RuntimeException(ex);
                        }
                    });
            final Optional<UUID> txnId = writer.flush();
            final long nextSequenceNumber = result.getLeft();
            final long endOffset = result.getRight();
            log.debug("processFile: Adding completed file: {}",  fileNameWithBeginOffset.fileName);
            state.addCompletedFile(fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset, endOffset, nextSequenceNumber, txnId);
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
        final List<FileNameWithOffset> completedFiles = state.getCompletedFiles();
        completedFiles.forEach(file -> {
            //Obtain a lock on file
            try(FileChannel channel = FileChannel.open(Paths.get(file.fileName), StandardOpenOption.WRITE)){
                try(FileLock lock = channel.tryLock()) {
                    if(lock!=null){
                        Files.deleteIfExists(Paths.get(file.fileName));
                        log.info("deleteCompletedFiles: Deleted file {}", file.fileName);
                        lock.release();
                        // Only remove from database if we could delete file.
                        state.deleteCompletedFile(file.fileName);
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

    /*
    Check for below file validation
        1. Is File empty
        2. If extension is null or extension is valid ingest all file
     */
    public static boolean isValidFile(FileNameWithOffset fileEntry, String fileExtension ){

        if(fileEntry.offset<=0){
            log.warn("isValidFile: Empty file {} can not be processed ",fileEntry.fileName);
        }
        // If extension is null, ingest all files
        else if(fileExtension.isEmpty() || fileExtension.equals(fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf(".")+1)))
            return true;
        else
            log.warn("isValidFile: File format {} is not supported ", fileEntry.fileName);

        return false;
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
