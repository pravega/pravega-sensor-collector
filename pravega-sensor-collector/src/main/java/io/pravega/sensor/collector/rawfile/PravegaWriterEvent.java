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

/**
 * Event generated from file and its sequence number
 */
public class PravegaWriterEvent {
    public final String routingKey;
    public final long sequenceNumber;
    public final byte[] bytes;

    public PravegaWriterEvent(String routingKey, long sequenceNumber, byte[] bytes) {
        this.routingKey = routingKey;
        this.sequenceNumber = sequenceNumber;
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return "PravegaWriterEvent{" +
                "routingKey='" + routingKey + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", bytes=" + new String(bytes) +
                '}';
    }
}
