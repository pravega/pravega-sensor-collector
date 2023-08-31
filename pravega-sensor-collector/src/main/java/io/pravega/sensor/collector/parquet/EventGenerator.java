
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.avro.AvroSchemaConverter;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
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
     * Convert Parquet to Json
     * @param inputStream
     * @param firstSequenceNumber
     * @return next sequence number, end offset
     */
    protected Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException {
        File tempFile = File.createTempFile("temp", ".parquet");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        IOUtils.copy(inputStream,outputStream);
        outputStream.close();
        Path tempFilePath = new Path(tempFile.toString());
        Configuration conf = new Configuration();
        MessageType schema = ParquetFileReader.readFooter(HadoopInputFile.fromPath(tempFilePath, conf),ParquetMetadataConverter.NO_FILTER).getFileMetaData().getSchema();
        
        //Modifying field names in extracted schema (removing special characters) 
        List<Type> fields = schema.getFields().stream()
                                .map(field -> new PrimitiveType(field.getRepetition(),  
                                                PrimitiveType.PrimitiveTypeName.valueOf(((PrimitiveType) field).getPrimitiveTypeName().toString()),
                                                field.getName()
                                .replaceAll("[^A-Za-z0-9_]+", "_")))
                                .collect(Collectors.toList());
        MessageType modifiedSchema = new MessageType(schema.getName(), fields);
        Schema avroSchema = new AvroSchemaConverter(conf).convert(modifiedSchema);

        // Add original field names as aliases to avroSchema
        for (int i = 0; i < modifiedSchema.getFieldCount(); i++) {
            String originalFieldName = schema.getFields().get(i).getName();
            avroSchema.getFields().get(i).addAlias(originalFieldName);
        }

        final AvroParquetReader.Builder<GenericRecord> builder = AvroParquetReader.<GenericRecord>builder(tempFilePath);
        AvroReadSupport.setAvroReadSchema(conf, avroSchema);        
        final ParquetReader<GenericRecord> reader = builder.withDataModel(GenericData.get()).withConf(conf).build();

        long nextSequenceNumber = firstSequenceNumber;
        int numRecordsInEvent = 0;
        List<HashMap<String,Object>> eventBatch = new ArrayList<>();
        GenericRecord record;
        while ((record=reader.read())!=null){
            HashMap<String,Object> dataMap = new HashMap<String,Object>();
            for(Schema.Field field : record.getSchema().getFields()){
                String key = field.name();
                Object value;
                value = record.get(key);
                dataMap.put(key,value);
            }
            eventBatch.add(dataMap);
            numRecordsInEvent++;
            if(numRecordsInEvent>=maxRecordsPerEvent){
                byte[] batchJsonEvent = mapper.writeValueAsBytes(eventBatch);
                consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, batchJsonEvent));
                nextSequenceNumber++;
                numRecordsInEvent = 0;
                eventBatch.clear();
            }
        }
        if (!eventBatch.isEmpty()){
            byte[] batchJsonEvent = mapper.writeValueAsBytes(eventBatch);
            consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, batchJsonEvent));
            nextSequenceNumber++;
        }
        final long endOffset = inputStream.getCount();
        tempFile.delete();
        return new ImmutablePair<>(nextSequenceNumber, endOffset);
    }

}
