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

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class PersistentId {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentId.class);

    private final UUID persistentId;

    public PersistentId(Connection connection) {
        Preconditions.checkNotNull(connection, "connection");
        try {
            try (final Statement statement = connection.createStatement()) {
                // Create table if needed.
                statement.execute(
                        "create table if not exists PersistentId (" +
                                "id integer primary key check (id = 0), " +
                                "persistentId string not null)");
                // Insert new UUID if table is empty.
                final String defaultPersistentId = UUID.randomUUID().toString();
                statement.execute(
                        "insert or ignore into PersistentId (id, persistentId) values (0, '" + defaultPersistentId + "')");
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                // Read UUID from table.
                try (final ResultSet rs = statement.executeQuery("select persistentId from PersistentId")) {
                    if (rs.next()) {
                        persistentId = UUID.fromString(rs.getString(1));
                    } else {
                        throw new SQLException("Unexpected query response");
                    }
                }
                LOGGER.info("persistentId={}", persistentId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return A persistent UUID that identifies this database instance.
     */
    public UUID getPersistentId() {
        return persistentId;
    }
}
