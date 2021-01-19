/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.UninterruptibleBlockingStrategy;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.SimpleDeviceDriver;
import io.pravega.sensor.collector.util.SpinBlockingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkDriver extends SimpleDeviceDriver<NetworkRawData, NetworkSamples> {
    private static final Logger log = LoggerFactory.getLogger(NetworkDriver.class);

    private static final String NETWORK_INTERFACE_KEY = "NETWORK_INTERFACE";
    private static final String STATISTICS_KEY = "STATISTICS";
    private static final String SAMPLES_PER_SEC_KEY = "SAMPLES_PER_SEC";

    private final List<NetworkStatisticFile> networkStatisticFiles = new ArrayList<>();
    private final Map<String, Integer> statisticNameToIndex = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Bucket bucket;
    private final UninterruptibleBlockingStrategy blockingStrategy;

    public NetworkDriver(DeviceDriverConfig config) {
        super(config);

        final String interfaceName = getInterfaceName();
        final List<String> statisticNames = getStatisticNames();
        final double samplesPerSec = getSamplesPerSec();
        log.info("Network Interface: {}", interfaceName);
        log.info("Statistics: {}", statisticNames);
        log.info("Samples Per Sec: {}", samplesPerSec);

        final long bucketCapacity = 2;
        final long periodNanos = (long) (bucketCapacity * 1e9 / samplesPerSec);
        bucket = Bucket4j.builder()
                .withNanosecondPrecision()
                .addLimit(Bandwidth.simple(2, Duration.ofNanos(periodNanos)))
                .build();
        log.info("Token Bucket: {}", bucket);

        if (samplesPerSec > 100.0) {
            log.info("Using spin blocking for precise sampling");
            blockingStrategy = new SpinBlockingStrategy();
        } else {
            log.info("Using parking strategy for reduced CPU usage");
            blockingStrategy = UninterruptibleBlockingStrategy.PARKING;
        }

        IntStream.range(0, statisticNames.size()).forEach((i) -> {
            final String statisticName = statisticNames.get(i);
            statisticNameToIndex.put(statisticName, i);
            String fileName = getStatisticFileName(interfaceName, statisticName);
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(new File(fileName), "r");
                networkStatisticFiles.add(new NetworkStatisticFile(interfaceName, statisticName, randomAccessFile));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        bucket.tryConsumeAsMuchAsPossible();
    }

    protected String getInterfaceName() {
        return getProperty(NETWORK_INTERFACE_KEY, "eth0");
    }

    protected List<String> getStatisticNames() {
        return Arrays.stream(getProperty(STATISTICS_KEY, "rx_bytes,tx_bytes").split(",")).collect(Collectors.toList());
    }

    protected double getSamplesPerSec() {
        return Double.parseDouble(getProperty(SAMPLES_PER_SEC_KEY, Double.toString(10.0)));
    }

    protected String getStatisticFileName(String interfaceName, String statisticName) {
        return String.format("/sys/class/net/%s/statistics/%s", interfaceName, statisticName);
    }

    protected String getRemoteAddr() {
        return getRoutingKey();
    }

    @Override
    public NetworkRawData readRawData() throws Exception {
        bucket.asScheduler().consumeUninterruptibly(1, blockingStrategy);
        final long timestampNanos = System.currentTimeMillis() * 1000 * 1000;
        final List<Long> statisticValues = networkStatisticFiles.stream().map((s) -> {
            try {
                s.randomAccessFile.seek(0);
                return Long.parseLong(s.randomAccessFile.readLine());
            } catch (Exception e) {
                log.warn("Exception reading file", e);
                return 0L;
            }
        }).collect(Collectors.toList());
        final NetworkRawData networkRawData = new NetworkRawData(timestampNanos, statisticValues);
        log.trace("networkRawData={}", networkRawData);
        return networkRawData;
    }

    @Override
    public void decodeRawDataToSamples(NetworkSamples samples, NetworkRawData networkRawData) {
        samples.timestampNanos.add(networkRawData.timestampNanos);
        samples.rxBytes.add(getStatistic(networkRawData, "rx_bytes"));
        samples.txBytes.add(getStatistic(networkRawData, "tx_bytes"));
    }

    private long getStatistic(NetworkRawData networkRawData, String statisticName) {
        final Integer index = statisticNameToIndex.get(statisticName);
        if (index == null) {
            return 0L;
        }
        return networkRawData.statisticValues.get(index);
    }

    @Override
    public NetworkSamples createSamples() {
        return new NetworkSamples(getRemoteAddr(), getInterfaceName());
    }

    @Override
    public byte[] serializeSamples(NetworkSamples samples) throws Exception {
        samples.setLastTimestampFormatted();
        log.info("samples={}", samples);
        return mapper.writeValueAsBytes(samples);
    }
}
