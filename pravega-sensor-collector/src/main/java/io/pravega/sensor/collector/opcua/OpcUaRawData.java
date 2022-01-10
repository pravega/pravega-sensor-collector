/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.pravega.sensor.collector.opcua;

public class OpcUaRawData {
    public final String nodeIdentifier;
    public final long timestamp;
    public final String dataType;
    public final Object data;

    public OpcUaRawData(Object data, long timestamp, String nodeIdentifier, String dataType) {
        this.data = data;
        this.timestamp = timestamp;
        this.nodeIdentifier = nodeIdentifier;
        this.dataType = dataType;
    }

}
