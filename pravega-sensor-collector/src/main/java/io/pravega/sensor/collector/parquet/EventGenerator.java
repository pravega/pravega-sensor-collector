
/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.parquet;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
     * Convert Parquet to byteArray
     * @param inputStream
     * @param firstSequenceNumber
     * @return next sequence number, end offset
     */
    protected Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException {
        
        long nextSequenceNumber = firstSequenceNumber;
        try{    
            byte[] byteArray = inputStream.readAllBytes();

            consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, byteArray));
            nextSequenceNumber++;			        
            final long endOffset = inputStream.getCount();

            return new ImmutablePair<>(nextSequenceNumber, endOffset);
        } catch (Exception e){
            log.error("Exception = {}",e);
            throw e;
        }  
    }   
}
