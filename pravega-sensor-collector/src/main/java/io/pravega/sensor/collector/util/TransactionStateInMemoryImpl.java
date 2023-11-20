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
public class TransactionStateInMemoryImpl extends TransactionStateSQLiteImpl{

    private static final Logger log = LoggerFactory.getLogger(TransactionStateInMemoryImpl.class);


    public TransactionStateInMemoryImpl(Connection connection, TransactionCoordinator transactionCoordinator) {
        super(connection, transactionCoordinator);
    }
    @VisibleForTesting
    public static TransactionStateInMemoryImpl create(String fileName) {
        final Connection connection = SQliteDBUtility.createDatabase(fileName);
        final TransactionCoordinator transactionCoordinator = new TransactionCoordinator(connection, null);
        return new TransactionStateInMemoryImpl(connection, transactionCoordinator);
    }

}
