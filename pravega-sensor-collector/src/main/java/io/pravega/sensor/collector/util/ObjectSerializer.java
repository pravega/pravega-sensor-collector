package io.pravega.sensor.collector.util;

import io.pravega.client.stream.Serializer;

import java.io.*;
import java.nio.ByteBuffer;

public class ObjectSerializer implements Serializer<Object>, Serializable {
    /**
     * Serializes the given event.
     *
     * @param value The event to be serialized.
     * @return The serialized form of the event.
     * NOTE: buffers returned should not exceed {@link #MAX_EVENT_SIZE}.
     */
    @Override
    public ByteBuffer serialize(Object value) {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(value);
            }
            return ByteBuffer.wrap(b.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the given ByteBuffer into an event.
     *
     * @param serializedValue A event that has been previously serialized.
     * @return The event object.
     */
    @Override
    public Object deserialize(ByteBuffer serializedValue) {
        byte[] result = new byte[serializedValue.remaining()];
        serializedValue.get(result);
        try(ByteArrayInputStream b = new ByteArrayInputStream(result)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
