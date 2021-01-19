/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.accelerometer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.SimpleDeviceDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class AccelerometerDriver extends SimpleDeviceDriver<AccelerometerRawData, AccelerometerSamples> {
    private static final Logger log = LoggerFactory.getLogger(AccelerometerDriver.class);

    private static final String CONFIG_DEVICE_FILE_KEY = "CONFIG_DEVICE_FILE";
    private static final String DATA_DEVICE_FILE_KEY = "DATA_DEVICE_FILE";

    private final static int SAMPLES_PER_QUEUE_ELEMENT = 16;
    private final static int BYTES_PER_SAMPLE = 16;

    // Scale for x, y, z.
    private final List<Double> scales;
    private final RandomAccessFile randomAccessFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public AccelerometerDriver(DeviceDriverConfig config) {
        super(config);

        final String sensorConfigurationDeviceFileName = getSensorConfigurationDeviceFileName();
        final String sensorDataDeviceFileName = getSensorDataDeviceFileName();
        log.info("Sensor Configuration Device File: {}", sensorConfigurationDeviceFileName);
        log.info("Sensor Data Device File: {}", sensorDataDeviceFileName);

        try {
            // TODO: IIO device numbering may change after reboot. Need to scan file system to find desired device.
            final File deviceFile = new File(sensorConfigurationDeviceFileName);
            final ImmutableList<String> relativeFileNames = ImmutableList.of(
                    "in_accel_x_scale",
                    "in_accel_y_scale",
                    "in_accel_z_scale");
            scales = relativeFileNames.stream().map(relativeFileName -> {
                try {
                    final File f = new File(deviceFile, relativeFileName);
                    return Double.parseDouble(Files.asCharSource(f, StandardCharsets.UTF_8).readFirstLine());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            log.info("scales={}", scales);
            randomAccessFile = new RandomAccessFile(new File(sensorDataDeviceFileName), "r");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getSensorConfigurationDeviceFileName() {
        return getProperty(CONFIG_DEVICE_FILE_KEY, "../fake-sensor-accel/sys/bus/iio/devices/iio_device3");
    }

    String getSensorDataDeviceFileName() {
        return getProperty(DATA_DEVICE_FILE_KEY, "/tmp/accelfifo");
    }

    String getRemoteAddr() {
        return getRoutingKey();
    }

    @Override
    public AccelerometerRawData readRawData() throws IOException {
        final byte[] buffer = new byte[BYTES_PER_SAMPLE * SAMPLES_PER_QUEUE_ELEMENT];
        randomAccessFile.readFully(buffer);
        return new AccelerometerRawData(buffer);
    }

    @Override
    public void decodeRawDataToSamples(AccelerometerSamples samples, AccelerometerRawData rawSensorData) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(rawSensorData.bytes).order(ByteOrder.LITTLE_ENDIAN);
        while (byteBuffer.hasRemaining()) {
            // 1 byte for each direction.
            samples.x.add(byteBuffer.get() * scales.get(0));
            samples.y.add(byteBuffer.get() * scales.get(1));
            samples.z.add(byteBuffer.get() * scales.get(2));
            // Skip 5 bytes of padding.
            byteBuffer.get();
            byteBuffer.getInt();
            // 8 bytes for timestamp which is the number of nanoseconds since 1970.
            samples.timestampNanos.add(byteBuffer.getLong());
        }
    }

    @Override
    public AccelerometerSamples createSamples() {
        return new AccelerometerSamples(getRemoteAddr());
    }

    @Override
    public byte[] serializeSamples(AccelerometerSamples samples) throws Exception {
        samples.setLastTimestampFormatted();
        return mapper.writeValueAsBytes(samples);
    }
}
