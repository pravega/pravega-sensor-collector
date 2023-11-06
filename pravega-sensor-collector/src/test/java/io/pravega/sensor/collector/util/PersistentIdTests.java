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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

public class PersistentIdTests {
    private static final Logger log = LoggerFactory.getLogger(PersistentIdTests.class);

    @Test
    public void persistentIdTest() throws SQLException {
        final String fileName = "/tmp/persistent-id-test-" + UUID.randomUUID() + ".db";
        log.info("fileName={}", fileName);

        try {
            final String writerId1;
            try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName)) {
                writerId1 = new PersistentId(connection).getPersistentId().toString();
                log.info("writerId1={}", writerId1);
            }
            final String writerId2;
            try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName)) {
                writerId2 = new PersistentId(connection).getPersistentId().toString();
                log.info("writerId2={}", writerId2);
            }
            Assertions.assertEquals(writerId1, writerId2);
        } finally {
            new File(fileName).delete();
        }
    }
}
