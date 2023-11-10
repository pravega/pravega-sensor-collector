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

import com.google.common.io.CountingInputStream;
import io.pravega.sensor.collector.util.PravegaWriterEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.function.Consumer;

/**
 *
 */
public interface EventGenerator{

    Pair<Long, Long> generateEventsFromInputStream(CountingInputStream inputStream, long firstSequenceNumber, Consumer<PravegaWriterEvent> consumer) throws IOException;
    /*JsonNode stringValueToJsonNode(String s);
    void addValueToArray(ObjectNode objectNode, String key, String value);*/



}
