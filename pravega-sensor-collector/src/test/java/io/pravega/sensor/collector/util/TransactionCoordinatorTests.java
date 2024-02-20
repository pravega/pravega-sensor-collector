/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.util;

import io.pravega.client.stream.TxnFailedException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionCoordinatorTests {

    private static final Logger log = LoggerFactory.getLogger(TransactionCoordinatorTests.class);

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPrepareStatement;
    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private EventWriter eventWriter;

    @Mock
    private TransactionalEventWriter transactionalEventWriter;

    private TransactionCoordinator transactionProcessor;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);

        // Mock behavior for the connection and statement
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);
        /*when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);*/
        transactionProcessor = new TransactionCoordinator(mockConnection, transactionalEventWriter);
    }

    @Test
    public void testAddTransactionToCommit() throws SQLException {

        UUID mockTransactionId = UUID.randomUUID();
        Optional<UUID> optionalTransactionId = Optional.of(mockTransactionId);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);
        transactionProcessor.addTransactionToCommit(optionalTransactionId);
        // Assert
        // Verify that prepareStatement was called with the correct SQL query
        verify(mockConnection).prepareStatement("insert into TransactionsToCommit (txnId) values (?)");
        verify(mockStatement).execute(anyString());

    }

    /*
     * SQLExcption while Adding trasaction id to TransactionsToCommit table
     */
    @Test
    public void testAddTransactionToCommitThrowSQLException() throws SQLException {

        UUID mockTransactionId = UUID.randomUUID();
        Optional<UUID> optionalTransactionId = Optional.of(mockTransactionId);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        // Mock behavior: when preparedStatement.execute is called, throw a SQLException
        doThrow(new SQLException("Test exception")).when(mockPrepareStatement).execute();

        // Use assertThrows to verify that SQLException is thrown
        assertThrows(RuntimeException.class, () -> transactionProcessor.addTransactionToCommit(optionalTransactionId));

        // Verify that prepareStatement was called with the correct SQL query
        verify(mockConnection).prepareStatement("insert into TransactionsToCommit (txnId) values (?)");
        verify(mockStatement).execute(anyString());

    }

    @Test
    public void testDeleteTransactionToCommit() throws SQLException {

        UUID mockTransactionId = UUID.randomUUID();
        Optional<UUID> optionalTransactionId = Optional.of(mockTransactionId);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);
        transactionProcessor.deleteTransactionToCommit(optionalTransactionId);
        // Assert
        // Verify that prepareStatement was called with the correct SQL query
        verify(mockConnection).prepareStatement("delete from TransactionsToCommit where txnId = ?");
        verify(mockStatement).execute(anyString());

    }

    /*
     * SQLExcption while deleting trasaction id from TransactionsToCommit table
     */
    @Test
    public void testDeleteTransactionToCommitThrowSQLException() throws SQLException {

        UUID mockTransactionId = UUID.randomUUID();
        Optional<UUID> optionalTransactionId = Optional.of(mockTransactionId);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        // Mock behavior: when preparedStatement.execute is called, throw a SQLException
        doThrow(new SQLException("Test exception")).when(mockPrepareStatement).execute();

        // Use assertThrows to verify that SQLException is thrown
        assertThrows(RuntimeException.class, () -> transactionProcessor.deleteTransactionToCommit(optionalTransactionId));

        // Verify that prepareStatement was called with the correct SQL query
        verify(mockConnection).prepareStatement("delete from TransactionsToCommit where txnId = ?");
        verify(mockStatement).execute(anyString());
    }

    /*
     * Test to verify getTransactionsToCommit method.
     *   Verify number of transaction id's matching with result set
     *
     */
    @Test
    public void testGetTransactionToCommit() throws SQLException {
        // Mock behavior: when statement.executeQuery is called, return the mock result set
        when(mockStatement.executeQuery("select txnId from TransactionsToCommit")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("txnId")).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Get List of transaction ID's from TransactionToCommit table
        List<UUID> uuidList = transactionProcessor.getTransactionsToCommit();

        // Assert
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getString("txnId");
        //verify result contains 2 UUIDs
        assertEquals(2, uuidList.size());
    }

    /*
     * Test to verify perform recovery method.
     */
    @Test
    public void testPerformRecovery() throws SQLException, TxnFailedException {
        // Mock behavior: when statement.executeQuery is called, return the mock result set
        when(mockStatement.executeQuery("select txnId from TransactionsToCommit")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("txnId")).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        //mock for delete transaction call
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);

        doNothing().when(transactionalEventWriter).commit(any());

        // Get List of transaction ID's from TransactionToCommit table
        transactionProcessor.performRecovery();

        // Assert
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getString("txnId");
        verify(mockConnection, times(2)).prepareStatement("delete from TransactionsToCommit where txnId = ?");

    }


    /*
     * Test to verify perform recovery method.
     * Verify the scenario where transaction commit throw the TxnFailedException
     */
    @Test
    public void testPerformRecoveryWithCommitFail() throws SQLException, TxnFailedException {
        // Mock behavior: when statement.executeQuery is called, return the mock result set
        when(mockStatement.executeQuery("select txnId from TransactionsToCommit")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("txnId")).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        //mock for delete transaction call
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            throw new TxnFailedException("Simulated transaction failure");
        }).when(transactionalEventWriter).commit(Mockito.any());
        //doNothing().when(transactionalEventWriter).commit(any());

        // Perform recovery
        transactionProcessor.performRecovery();

        // Assert
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getString("txnId");
        //verify(mockConnection, times(2)).prepareStatement("delete from TransactionsToCommit where txnId = ?");

    }

    /*
     * Test to verify perform recovery method.
     * Verify the scenario where transaction commit throw the Unknown Transaction as message
     */
    @Test
    public void testPerformRecoveryCommitWithUnknownTransactionFail() throws SQLException, TxnFailedException {
        // Mock behavior: when statement.executeQuery is called, return the mock result set
        when(mockStatement.executeQuery("select txnId from TransactionsToCommit")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("txnId")).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        //mock for delete transaction call
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            throw new RuntimeException("Unknown transaction");
        }).when(transactionalEventWriter).commit(Mockito.any());

        // Perform recovery
        transactionProcessor.performRecovery();

        // Assert
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getString("txnId");

    }

    /*
     * Test to verify perform recovery method.
     * Verify the scenario where transaction commit throw the other runtime exception as message
     */
    @Test
    public void testPerformRecoveryCommitWithOtherException() throws SQLException, TxnFailedException {
        // Mock behavior: when statement.executeQuery is called, return the mock result set
        when(mockStatement.executeQuery("select txnId from TransactionsToCommit")).thenReturn(mockResultSet);
        // Mock behavior: simulate the result set having two rows with different UUIDs
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("txnId")).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        //mock for delete transaction call
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPrepareStatement);
        when(mockPrepareStatement.execute()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            throw new RuntimeException("Other Runtime Exception");
        }).when(transactionalEventWriter).commit(Mockito.any());

        // Perform recovery
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionProcessor.performRecovery();
        });

        // Assert
        String expectedMessage = "Other Runtime Exception";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message mismatch");

    }

    @Test
    public void testCreateTransactionCoordinatorWithNullConnection() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new TransactionCoordinator(null, eventWriter));
        Assert.assertTrue("connection".equals(exception.getMessage()));
    }

    @Test
    public void testCreateTransactionCoordinatorWithNullWriter() {
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new TransactionCoordinator(mockConnection, null));
        Assert.assertTrue("writer".equals(exception.getMessage()));
    }
}

