/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.csvfile;

import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.util.PravegaWriterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CSVFileEventGeneratorTests {
    private static final Logger LOG = LoggerFactory.getLogger(CSVFileEventGeneratorTests.class);

    @Test
    public void test3by2() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 2);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(102L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test3by3() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(101L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test1by3() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(101L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test0by3() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void TestEmptyFile() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 3);
        final String csvStr = "";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void test7by3() throws IOException {
        final EventGenerator eventGenerator = CsvFileEventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n"
                + "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        LOG.info("events={}", events);
        Assertions.assertEquals(103L, (long) nextSequenceNumberAndOffset.getLeft());
        Assertions.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }
}
