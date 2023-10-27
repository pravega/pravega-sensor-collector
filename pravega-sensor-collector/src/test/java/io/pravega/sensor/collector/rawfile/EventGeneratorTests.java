package io.pravega.sensor.collector.rawfile;

import com.google.common.io.CountingInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.PravegaWriterEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EventGeneratorTests {
    private static final Logger log = LoggerFactory.getLogger(EventGeneratorTests.class);

    @Test
    public void TestFile() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1");
        final String rawfileStr =
                "\"Time\",\"X\",\"Y\",\"Z\",\"IN_PROGRESS\"\n" +
                "\"2020-07-15 23:59:50.352\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.362\",\"1.305966\",\"0.1\",\"1.331963\",\"0\"\n" +
                "\"2020-07-15 23:59:50.415\",\"0.305966\",\"0.0\",\"9.331963\",\"0\"\n";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(rawfileStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(101L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(rawfileStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

    @Test
    public void TestEmptyFile() throws IOException {
        final EventGenerator eventGenerator = EventGenerator.create("routingKey1");
        final String rawfileStr = "";
        final CountingInputStream inputStream = new CountingInputStream(new ByteArrayInputStream(rawfileStr.getBytes(StandardCharsets.UTF_8)));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 100, events::add);
        log.info("events={}", events);
        Assert.assertEquals(100L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(rawfileStr.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

}
