package io.pravega.sensor.collector.stateful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Stores and updates last-read timestamp in the persistent state (SQLite database) *
 */
public class ReadingState {
    private static final Logger log = LoggerFactory.getLogger(ReadingState.class);
    public final Connection connection;
    public ReadingState(Connection connection) {
        this.connection = connection;
        log.info("connection: {}", this.connection);
        try {
            try (final Statement statement = connection.createStatement()) {
                statement.execute("create table if not exists LastReadingState ("
                        + "id integer primary key check(id=0)," + "lastTimestamp string)");
                statement.execute("insert or ignore into LastReadingState(id, lastTimestamp) values (0, '')");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateState(String state) {
        try (final PreparedStatement updateStateStatement = connection
                .prepareStatement("update LastReadingState set lastTimestamp = ?")) {
            updateStateStatement.setString(1, state);
            updateStateStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getState() {
        try {
            try (final Statement statement = connection.createStatement();
                    final ResultSet rs = statement.executeQuery("select lastTimestamp from LastReadingState")) {
                if (rs.next()) {
                    return rs.getString(1);
                } else {
                    throw new SQLException("Unexpected query response");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
