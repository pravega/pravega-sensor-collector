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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Maintain state of pending and completed files in in-memory database.
 */
public class TransactionStateInMemoryImpl extends TransactionStateSQLiteImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionStateInMemoryImpl.class);


    public TransactionStateInMemoryImpl(Connection connection, TransactionCoordinator transactionCoordinator) {
        super(connection, transactionCoordinator);
    }
    @VisibleForTesting
    public static TransactionStateInMemoryImpl create(String fileName, EventWriter writer) {
        final Connection connection = SQliteDBUtility.createDatabase(fileName);
        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, writer);
        return new TransactionStateInMemoryImpl(connection, transactionCoordinator);
    }

}
