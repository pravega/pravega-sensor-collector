
/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.rawfile;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate Event from file  
 */
public class EventGenerator {
    private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);

    private final String routingKey;
    private final ObjectNode eventTemplate;
    private final ObjectMapper mapper;

    public EventGenerator(String routingKey, ObjectNode eventTemplate, ObjectMapper mapper) {
        this.routingKey = routingKey;
        this.eventTemplate = eventTemplate;
        this.mapper = mapper;
    }

    public static EventGenerator create(String routingKey, String eventTemplateStr, String writerId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode eventTemplate = (ObjectNode) mapper.readTree(eventTemplateStr);
            eventTemplate.put("WriterId", writerId);
            return new EventGenerator(routingKey, eventTemplate, mapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventGenerator create(String routingKey) throws IOException {
        return create(routingKey, "{}", "MyWriterId");
    }


    /**
     * Convert File to byteArray
     * @param inputStream
     * @param firstSequenceNumber
     * @return next sequence number, end offset
     */
    protected Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<RawFileWriterEvent> consumer) throws IOException {
        
        long nextSequenceNumber = firstSequenceNumber;
        try{    
            byte[] byteArray = inputStream.readAllBytes();
            //TODO: Batching

            consumer.accept(new RawFileWriterEvent(routingKey, nextSequenceNumber, byteArray));
            nextSequenceNumber++;			        
            final long endOffset = inputStream.getCount();

            return new ImmutablePair<>(nextSequenceNumber, endOffset);
        } catch (Exception e){
            log.error("Exception = {}",e);
            throw e;
        }
    }   
}
