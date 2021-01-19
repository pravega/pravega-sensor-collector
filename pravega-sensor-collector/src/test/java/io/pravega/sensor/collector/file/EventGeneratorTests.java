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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EventGeneratorTests {
    private static final Logger log = LoggerFactory.getLogger(EventGeneratorTests.class);

    @Test
    public void Test3by2() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 2);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(102L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test3by3() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(101L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test1by3() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(101L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void Test0by3() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void TestEmptyFile() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 3);
        final String csvStr = "";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void test7by3() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1", 3);
        final String csvStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(csvStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(103L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(csvStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }
}
