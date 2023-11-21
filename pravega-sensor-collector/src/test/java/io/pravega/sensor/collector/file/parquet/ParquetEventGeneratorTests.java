package io.pravega.sensor.collector.file.parquet;

import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.file.EventGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.FileUtils;
import io.pravega.sensor.collector.util.PravegaWriterEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ParquetEventGeneratorTests {
    private static final Logger log = LoggerFactory.getLogger(ParquetEventGeneratorTests.class);
    
    @Test
    public void TestFile() throws IOException {
        final EventGenerator eventGenerator = ParquetEventGenerator.create("routingKey1",100);
        final List<FileNameWithOffset> files = FileUtils.getDirectoryListing("../parquet-file-sample-data","parquet");
        File parquetData= new File(files.get(0).fileName);

        final CountingInputStream inputStream = new CountingInputStream(new FileInputStream(parquetData));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 1, events::add);
        log.info("events={}", events);
        Assert.assertEquals(501L, (long) nextSequenceNumberAndOffset.getLeft());        
        Assert.assertEquals(parquetData.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

}
