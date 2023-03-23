
package io.pravega.sensor.collector.parquet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CountingInputStream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroSchemaConverter;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.schema.Type;

import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
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


    // PARQUET TO JSON

    /**
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
                                                // field.getType(), 
                                                PrimitiveType.PrimitiveTypeName.valueOf(((PrimitiveType) field).getPrimitiveTypeName().toString()),
                                                field.getName()
                                .replaceAll("[^A-Za-z0-9_]+", "_")))
                                .collect(Collectors.toList());
        MessageType modifiedSchema = new MessageType(schema.getName(), fields);
        
        Schema avroSchema = new AvroSchemaConverter(conf).convert(modifiedSchema);
        
        final AvroParquetReader.Builder<GenericRecord> builder = AvroParquetReader.<GenericRecord>builder(tempFilePath);
        AvroReadSupport.setAvroReadSchema(conf, avroSchema);        
        final ParquetReader<GenericRecord> reader = builder.withDataModel(GenericData.get()).withConf(conf).build();

        long nextSequenceNumber = firstSequenceNumber;
        int numRecordsInEvent = 0;
        ObjectNode jsonEvent = null;
        GenericRecord record;
        while ((record = reader.read())!=null) {
            if (numRecordsInEvent >= maxRecordsPerEvent) {
                consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(jsonEvent)));
                nextSequenceNumber++;
                jsonEvent = null;
                numRecordsInEvent = 0;
            }
            if (jsonEvent == null) {
                jsonEvent = eventTemplate.deepCopy();
            }
            for (Schema.Field field : record.getSchema().getFields()){
                // log.info("Parquet File record : {}", record.toString());
                String key =  field.name();
                String value;
                if(record.get(key)!= null)
                    value = record.get(key).toString();
                else
                    value = null;
                addValueToArray(jsonEvent, key, value);
            }
            numRecordsInEvent++;
        }
        if (jsonEvent != null) {
            consumer.accept(new PravegaWriterEvent(routingKey, nextSequenceNumber, mapper.writeValueAsBytes(jsonEvent)));
            nextSequenceNumber++;
        }
        final long endOffset = inputStream.getCount();
        tempFile.delete();
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
        if(value == null){
            objectNode.putNull(key);
        }
        else{    
            final JsonNode node = objectNode.get(key);
            final JsonNode valueNode = stringValueToJsonNode(value);
            if (node instanceof ArrayNode ) {
                ((ArrayNode) node).add(valueNode);
            } else {
                objectNode.putArray(key).add(valueNode);
            }
        }
    }

}
