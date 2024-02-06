/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.parquet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.FileUtils;
import io.pravega.sensor.collector.util.PravegaWriterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ParquetEventGeneratorTests {
    private static final Logger LOG = LoggerFactory.getLogger(ParquetEventGeneratorTests.class);
    
    @Test
    public void TestFile() throws IOException {
        final EventGenerator eventGenerator = ParquetEventGenerator.create("routingKey1",100);
        final List<FileNameWithOffset> files = FileUtils.getDirectoryListing("../parquet-file-sample-data","parquet", Paths.get("."), 5000);
        File parquetData= new File(files.get(0).fileName);

        final CountingInputStream inputStream = new CountingInputStream(new FileInputStream(parquetData));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 1, events::add);
        Assert.assertEquals(501L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(parquetData.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void testCreateParquetEventGeneratorWithNullRoutingKey() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = (ObjectNode) objectMapper.readTree("{}");
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new ParquetEventGenerator(null, 1, objectNode, objectMapper));
        Assert.assertTrue("routingKey".equals(exception.getMessage()));
    }

    @Test
    public void testCreateParquetEventGeneratorWithNullObjectMapper() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = (ObjectNode) objectMapper.readTree("{}");
        Exception exception = Assert.assertThrows(NullPointerException.class, () -> new ParquetEventGenerator("routing-key", 1, objectNode, null));
        Assert.assertTrue("objectMapper".equals(exception.getMessage()));
    }

}
