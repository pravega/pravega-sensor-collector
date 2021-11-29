package io.pravega.sensor.collector.opcua;

import org.apache.commons.codec.binary.Hex;

public class OpcUaRawData {
    public final byte[] bytes;

    public OpcUaRawData(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return "OpcUaRawData{" +
                ", bytes=" + Hex.encodeHexString(bytes) +
                '}';
    }
}
