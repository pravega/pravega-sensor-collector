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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.util.PravegaWriterEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Generate Event from CSV file.
 */
public class CsvFileEventGenerator implements EventGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvFileEventGenerator.class);

    private final String routingKey;
    private final int maxRecordsPerEvent;
    private final ObjectNode eventTemplate;
    private final ObjectMapper mapper;

    public CsvFileEventGenerator(String routingKey, int maxRecordsPerEvent, ObjectNode eventTemplate, ObjectMapper mapper) {
        this.routingKey = Preconditions.checkNotNull(routingKey, "routingKey");
        this.maxRecordsPerEvent = maxRecordsPerEvent;
        this.eventTemplate = eventTemplate;
        this.mapper = Preconditions.checkNotNull(mapper, "objectMapper");
    }

    public static CsvFileEventGenerator create(String routingKey, int maxRecordsPerEvent, String eventTemplateStr, String writerId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode eventTemplate = (ObjectNode) mapper.readTree(eventTemplateStr);
            eventTemplate.put("WriterId", writerId);
            return new CsvFileEventGenerator(routingKey, maxRecordsPerEvent, eventTemplate, mapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CsvFileEventGenerator create(String routingKey, int maxRecordsPerEvent) throws IOException {
        return create(routingKey, maxRecordsPerEvent, "{}", "MyWriterId");
    }

    /** Generate event from input stream. number of records in one event is defined in input config file
     * @param inputStream
     * @param firstSequenceNumber
     * @param consumer
     * @throws IOException If there is any error generating event.
     * @return next sequence number, end offset
     */
    public Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        final CSVParser parser = CSVParser.parse(inputStream, StandardCharsets.UTF_8, format);
        long nextSequenceNumber = firstSequenceNumber;
        int numRecordsInEvent = 0;
        List<HashMap<String, Object>> eventBatch = new ArrayList<>();
        for (CSVRecord record : parser) {
            HashMap<String, Object> recordDataMap = new HashMap<String, Object>();
            for (int i = 0; i < record.size(); i++) {
                recordDataMap.put(parser.getHeaderNames().get(i), convertValue(record.get(i)));
            }
            eventBatch.add(recordDataMap);
            numRecordsInEvent++;
            if (numRecordsInEvent >= maxRecordsPerEvent) {
                consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(eventBatch)));
                nextSequenceNumber++;
                eventBatch.clear();
                numRecordsInEvent = 0;
            }
        }
        if (!eventBatch.isEmpty()) {
            consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(eventBatch)));
            nextSequenceNumber++;
            eventBatch.clear();
        }
        final long endOffset = inputStream.getCount();
        return new ImmutablePair<>(nextSequenceNumber, endOffset);
    }

    public Object convertValue(String s) {
        // TODO: convert timestamp
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) { }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) { }
        return s;
    }
}
