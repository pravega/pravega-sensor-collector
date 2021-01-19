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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Connection;

/**
 * Based on https://stackoverflow.com/a/37122747/5890553.
 */
public class AutoRollback implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AutoRollback.class);

    private Connection connection;
    private boolean committed;

    public AutoRollback(Connection connection) {
        this.connection = connection;
    }

    public void commit() throws SQLException {
        log.debug("Committing transaction on connection {}", connection);
        connection.commit();
        committed = true;
    }

    @Override
    public void close() throws SQLException {
        if (!committed) {
            log.warn("Rolling back transaction on connection {}", connection);
            connection.rollback();
        }
    }
}
