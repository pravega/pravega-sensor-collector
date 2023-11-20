package io.pravega.sensor.collector.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

public class SQliteDBUtility {

    public static Connection createDatabase(String fileName) {
        try {
            final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
            try (final Statement statement = connection.createStatement()) {
                // Use SQLite exclusive locking mode to ensure that another process or device driver instance is not using this database.
                //statement.execute("PRAGMA locking_mode = EXCLUSIVE");
                statement.execute(
                        "create table if not exists PendingFiles (" +
                                "id integer primary key autoincrement, " +
                                "fileName string unique not null, " +
                                "offset bigint not null)");
                statement.execute(
                        "create table if not exists CompletedFiles (" +
                                "fileName string primary key not null, " +
                                "offset bigint not null)");
                statement.execute(
                        "create table if not exists SequenceNumber (" +
                                "id integer primary key check (id = 0), " +
                                "nextSequenceNumber bigint not null)");
                statement.execute(
                        "insert or ignore into SequenceNumber (id, nextSequenceNumber) values (0, 0)");
            }
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
