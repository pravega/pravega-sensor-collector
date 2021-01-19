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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public class EventGenerator {
    private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);

    private final String routingKey;
    private final int maxRecordsPerEvent;
    private final ObjectNode eventTemplate;
    private final ObjectMapper mapper;

    public EventGenerator(String routingKey, int maxRecordsPerEvent, ObjectNode eventTemplate, ObjectMapper mapper) {
        this.routingKey = routingKey;
        this.maxRecordsPerEvent = maxRecordsPerEvent;
        this.eventTemplate = eventTemplate;
        this.mapper = mapper;
    }

    public static EventGenerator create(String routingKey, int maxRecordsPerEvent, String eventTemplateStr, String writerId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode eventTemplate = (ObjectNode) mapper.readTree(eventTemplateStr);
            eventTemplate.put("WriterId", writerId);
            return new EventGenerator(routingKey, maxRecordsPerEvent, eventTemplate, mapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventGenerator create(String routingKey, int maxRecordsPerEvent) throws IOException {
        return create(routingKey, maxRecordsPerEvent, "{}", "MyWriterId");
    }

    /**
     * @param inputStream
     * @param firstSequenceNumber
     * @return next sequence number, end offset
     */
    protected Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        final CSVParser parser = CSVParser.parse(inputStream, StandardCharsets.UTF_8, format);
        long nextSequenceNumber = firstSequenceNumber;
        int numRecordsInEvent = 0;
        ObjectNode jsonEvent = null;
        for (CSVRecord record: parser) {
            if (numRecordsInEvent >= maxRecordsPerEvent) {
                consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(jsonEvent)));
                nextSequenceNumber++;
                jsonEvent = null;
                numRecordsInEvent = 0;
            }
            if (jsonEvent == null) {
                jsonEvent = eventTemplate.deepCopy();
            }
            for (Map.Entry<String,String> entry: record.toMap().entrySet()) {
                addValueToArray(jsonEvent, entry.getKey(), entry.getValue());
            }
            numRecordsInEvent++;
        }
        if (jsonEvent != null) {
            consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(jsonEvent)));
            nextSequenceNumber++;
        }
        final long endOffset = inputStream.getCount();
        return new ImmutablePair<>(nextSequenceNumber, endOffset);
    }

    protected JsonNode stringValueToJsonNode(String s) {
        // TODO: convert timestamp
        try {
            return mapper.getNodeFactory().numberNode(Long.parseLong(s));
        } catch (NumberFormatException ignored) {}
        try {
            return mapper.getNodeFactory().numberNode(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {}
        return mapper.getNodeFactory().textNode(s);
    }

    protected void addValueToArray(ObjectNode objectNode, String key, String value) {
        final JsonNode node = objectNode.get(key);
        final JsonNode valueNode = stringValueToJsonNode(value);
        if (node instanceof ArrayNode ) {
            ((ArrayNode) node).add(valueNode);
        } else {
            objectNode.putArray(key).add(valueNode);
        }
    }
}
