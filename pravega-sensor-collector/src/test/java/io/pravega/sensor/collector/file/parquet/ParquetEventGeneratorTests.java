/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.file.parquet;

import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.file.EventGenerator;
import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.FileUtils;
import io.pravega.sensor.collector.util.PravegaWriterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ParquetEventGeneratorTests {
    private static final Logger LOG = LoggerFactory.getLogger(ParquetEventGeneratorTests.class);
    
    @Test
    public void TestFile() throws IOException {
        final EventGenerator eventGenerator = ParquetEventGenerator.create("routingKey1", 100);
        final List<FileNameWithOffset> files = FileUtils.getDirectoryListing("../parquet-file-sample-data", "parquet", Paths.get("."), 5000);
        File parquetData = new File(files.get(0).fileName);

        final CountingInputStream inputStream = new CountingInputStream(new FileInputStream(parquetData));
        final List<PravegaWriterEvent> events = new ArrayList<>();
        Pair<Long, Long> nextSequenceNumberAndOffset = eventGenerator.generateEventsFromInputStream(inputStream, 1, events::add);
        Assert.assertEquals(501L, (long) nextSequenceNumberAndOffset.getLeft());
        Assert.assertEquals(parquetData.length(), (long) nextSequenceNumberAndOffset.getRight());
    }

}
