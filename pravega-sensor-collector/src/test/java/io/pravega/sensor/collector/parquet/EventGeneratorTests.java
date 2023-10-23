package io.pravega.sensor.collector.parquet;

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
    public void TestFile() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1",2);
        final String parquetfileStr =
                "\"first_name\",\"last_name\",\"timestamp\",\"id\",\"email\",\"phone_num\",\"location\",\"subscription_status\"\n" +
                "\"Jacob\",\"Jones\",\"1987-01-03 09:00:00\",\"1\",\"yzonfi@example.com\",\"1153133580\",\"Chicago\",\"active\"\n" +
                "\"Andrea\",\"Jackson\",\"1987-01-03 09:01:00\",\"2\",\"ndxcsn@example.com\",\"8122103672\",\"San Francisco\",\"active\"\n" +
                "\"Chase\",\"Ramirez\",\"1987-01-03 09:02:00\",\"3\",\"rohnpl@example.com\",\"5608142576\",\"Denver\",\"unsubscribed\"\n" +
                "\"Ronald\",\"Stout\",\"1987-01-03 09:03:00\",\"4\",\"wkiepc@example.com\",\"7608948167\",\"Miami\",\"unsubscribed\"\n" ;
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(parquetfileStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(102L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(parquetfileStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void TestEmptyFile() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1",2);
        final String parquetfileStr = "";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(parquetfileStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(parquetfileStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }
}
