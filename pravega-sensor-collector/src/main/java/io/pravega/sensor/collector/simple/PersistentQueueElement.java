/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.simple;

import java.nio.charset.StandardCharsets;

public class PersistentQueueElement {
    public final long id;
    public final byte[] bytes;
    public final String routingKey;
    public final long timestamp;

    public PersistentQueueElement(long id, byte[] bytes, String routingKey, long timestamp) {
        this.id = id;
        this.bytes = bytes;
        this.routingKey = routingKey;
        this.timestamp = timestamp;
    }

    public PersistentQueueElement(byte[] bytes, String routingKey, long timestamp) {
        this(0, bytes, routingKey, timestamp);
    }

    @Override
    public String toString() {
        return "PersistentQueueElement{" +
                "id=" + id +
                ", bytes='" + new String(bytes, StandardCharsets.UTF_8) + '\'' +
                ", routingKey=" + routingKey +
                ", timestamp=" + timestamp +
                '}';
    }
}
