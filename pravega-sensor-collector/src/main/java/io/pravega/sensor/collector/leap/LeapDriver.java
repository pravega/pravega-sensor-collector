/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.leap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.SimpleDeviceDriver;

public class LeapDriver extends SimpleDeviceDriver<LeapRawData, LeapSamples> {
    private static final Logger log = LoggerFactory.getLogger(LeapDriver.class);

    private static final String POLL_PERIOD_SEC_KEY = "POLL_PERIOD_SEC";
    private static final String API_URI_KEY = "API_URI";

    private final Bucket bucket;
    private final String apiUri;

    public LeapDriver(DeviceDriverConfig config) {
        super(config.withProperty(SAMPLES_PER_EVENT_KEY, "1"));

        final double pollPeriodSec = getPollPeriodSec();
        apiUri = getProperty(API_URI_KEY);
        log.info("Poll Period Sec: {}", pollPeriodSec);
        log.info("API URI: {}", apiUri);

        final long bucketCapacity = 2;
        final long periodMillis = (long) (bucketCapacity * 1000 * pollPeriodSec);
        bucket = Bucket4j.builder()
                .withMillisecondPrecision()
                .addLimit(Bandwidth.simple(bucketCapacity, Duration.ofMillis(periodMillis)))
                .build();
        log.info("Token Bucket: {}", bucket);

        bucket.tryConsume(bucketCapacity - 1);
    }

    protected double getPollPeriodSec() {
        return Double.parseDouble(getProperty(POLL_PERIOD_SEC_KEY, Double.toString(60.0)));
    }

    @Override
    public LeapRawData readRawData() throws Exception {
        log.info("readRawData: BEGIN");
        bucket.asScheduler().consume(1);
        HttpClient client = HttpClient.newHttpClient();
        String uri = apiUri;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .build();
        log.info("request={}", request);
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        log.info("response={}", response);
        if (response.statusCode() != 200) {
            throw new RuntimeException(MessageFormat.format("HTTP server returned status code {0}", response.statusCode()));
        };
        log.info("body={}", response.body());
        final long timestampNanos = System.currentTimeMillis() * 1000 * 1000;
        byte[] data = response.body().getBytes(StandardCharsets.UTF_8);
        final LeapRawData rawData = new LeapRawData(timestampNanos, data);
        log.info("rawData={}", rawData);
        log.info("readRawData: END");
        return rawData;
    }

    @Override
    public void decodeRawDataToSamples(LeapSamples samples, LeapRawData rawData) {
        assert(samples.rawData.isEmpty());
        samples.rawData.add(rawData);
    }

    @Override
    public LeapSamples createSamples() {
        return new LeapSamples();
    }

    @Override
    public byte[] serializeSamples(LeapSamples samples) throws Exception {
        assert(samples.rawData.size() == 1);
        return samples.rawData.get(0).data;
    }
}
