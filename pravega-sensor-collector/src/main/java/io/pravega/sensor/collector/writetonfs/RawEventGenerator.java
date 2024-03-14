/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.writetonfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.util.PravegaWriterEvent;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Generate Event from RAW file.
 */
public class RawEventGenerator implements EventGenerator{
    private static final Logger LOGGER = LoggerFactory.getLogger(RawEventGenerator.class);

    private final String routingKey;
    private final ObjectNode eventTemplate;
    private final ObjectMapper mapper;

    public RawEventGenerator(String routingKey, ObjectNode eventTemplate, ObjectMapper mapper) {
        this.routingKey = routingKey;
        this.eventTemplate = eventTemplate;
        this.mapper = mapper;
    }

    public static RawEventGenerator create(String routingKey, String eventTemplateStr, String writerId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode eventTemplate = (ObjectNode) mapper.readTree(eventTemplateStr);
            eventTemplate.put("WriterId", writerId);
            return new RawEventGenerator(routingKey, eventTemplate, mapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RawEventGenerator create(String routingKey) throws IOException {
        return create(routingKey, "{}", "MyWriterId");
    }


    /**
     * Convert File to byteArray.
     * @param inputStream
     * @param firstSequenceNumber
     * @param consumer
     * @throws IOException
     * @return next sequence number, end offset
     */
    public Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException {
        long nextSequenceNumber = firstSequenceNumber;
        try {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte[] byteArray = IOUtils.toByteArray(bis);

            if (byteArray.length > 0) {         //non-empty file
                consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, byteArray));
                nextSequenceNumber++;
            }
            final long endOffset = inputStream.getCount();
            return new ImmutablePair<>(nextSequenceNumber, endOffset);
        } catch (Exception e) {
            LOGGER.error("Exception = {}", e);
            throw e;
        }
    }
}
