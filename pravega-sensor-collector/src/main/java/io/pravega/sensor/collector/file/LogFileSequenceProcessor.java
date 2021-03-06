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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    public void ingestLogFiles() throws Exception {
        log.info("ingestLogFiles: BEGIN");
        findAndRecordNewFiles();
        processNewFiles();
        if (config.enableDeleteCompletedFiles) {
            deleteCompletedFiles();
        }
        log.info("ingestLogFiles: END");
    }

    public void processNewFiles() throws Exception {
        for (;;) {
            final Pair<FileNameWithOffset, Long> nextFile = state.getNextPendingFile();
            if (nextFile == null) {
                log.info("No more files to ingest");
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
        final List<FileNameWithOffset> directoryListing = getDirectoryListing(config.fileSpec);
        log.trace("getDirectoryListing: directoryListing={}", directoryListing);
        return directoryListing;
    }

    /**
     * @return list of file name and file size in bytes
     */
    static protected List<FileNameWithOffset> getDirectoryListing(String fileSpec) throws IOException {
        final Path pathSpec = Paths.get(fileSpec);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathSpec.getParent(), pathSpec.getFileName().toString())) {
            return StreamSupport.stream(dirStream.spliterator(), false)
                    .map(f -> new FileNameWithOffset(f.toAbsolutePath().toString(), f.toFile().length()))
                    .collect(Collectors.toList());
        }
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
        log.info("getNewFiles={}", newFiles);
        return newFiles;
    }

    void processFile(FileNameWithOffset fileNameWithBeginOffset, long firstSequenceNumber) throws Exception {
        log.info("processFile: Ingesting file {}; beginOffset={}, firstSequenceNumber={}",
                fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset, firstSequenceNumber);

        // In case a previous iteration encountered an error, we need to ensure that
        // previous flushed transactions are committed and any unflushed transactions as aborted.
        transactionCoordinator.performRecovery();
        writer.abort();

        try (final InputStream inputStream = new FileInputStream(fileNameWithBeginOffset.fileName)) {
            final CountingInputStream countingInputStream = new CountingInputStream(inputStream);
            countingInputStream.skip(fileNameWithBeginOffset.offset);
            final Pair<Long,Long> result = eventGenerator.generateEventsFromInputStream(countingInputStream, firstSequenceNumber,
                    e -> {
                        log.trace("processFile: event={}", e);
                        try {
                            writer.writeEvent(e.routingKey, e.bytes);
                        } catch (TxnFailedException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
            final Optional<UUID> txnId = writer.flush();
            final long nextSequenceNumber = result.getLeft();
            final long endOffset = result.getRight();
            state.addCompletedFile(fileNameWithBeginOffset.fileName, fileNameWithBeginOffset.offset, endOffset, nextSequenceNumber, txnId);
            // injectCommitFailure();
            writer.commit();
            state.deleteTransactionToCommit(txnId);
            log.info("processFile: Finished ingesting file {}; endOffset={}, nextSequenceNumber={}",
                    fileNameWithBeginOffset.fileName, endOffset, nextSequenceNumber);
        }
    }

    void deleteCompletedFiles() throws Exception {
        final List<FileNameWithOffset> completedFiles = state.getCompletedFiles();
        completedFiles.forEach(file -> {
            try {
                Files.deleteIfExists(Paths.get(file.fileName));
                log.info("deleteCompletedFiles: Deleted file {}", file.fileName);
                // Only remove from database if we could delete file.
                state.deleteCompletedFile(file.fileName);
            } catch (Exception e) {
                log.warn("Unable to delete ingested file {}", e);
                // We can continue on this error. It will be retried on the next iteration.
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
