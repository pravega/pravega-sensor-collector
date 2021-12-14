/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.pravega.sensor.collector.opcua;

public class OpcUaRawData {
    public final Object data;
    public final long timestamp;
    public final String nodeIdentifier;

    public OpcUaRawData(Object data, long timestamp, String nodeIdentifier) {
        this.data = data;
        this.timestamp = timestamp;
        this.nodeIdentifier = nodeIdentifier;
    }

}
