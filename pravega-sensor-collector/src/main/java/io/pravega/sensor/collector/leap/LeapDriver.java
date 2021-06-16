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
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.PersistentQueueElement;
import io.pravega.sensor.collector.stateful.PollResponse;
import io.pravega.sensor.collector.stateful.StatefulSensorDeviceDriver;

public class LeapDriver extends StatefulSensorDeviceDriver<String> {
    private static final Logger log = LoggerFactory.getLogger(LeapDriver.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String POLL_PERIOD_SEC_KEY = "POLL_PERIOD_SEC";
    private static final String API_URI_KEY = "API_URI";
    private static final String USERNAME_KEY = "USERNAME";
    private static final String PASSWORD_KEY = "PASSWORD";

    private final Bucket bucket;
    private final String apiUri;
    private final ObjectMapper mapper;
    private final String userName;
    private final String password;

    public LeapDriver(DeviceDriverConfig config) {
        super(config);

        mapper = new ObjectMapper();

        final double pollPeriodSec = Double.parseDouble(getProperty(POLL_PERIOD_SEC_KEY, Double.toString(60.0)));
        apiUri = getProperty(API_URI_KEY);
        userName = getProperty(USERNAME_KEY);
        password = getProperty(PASSWORD_KEY);
        log.info("Poll Period Sec: {}", pollPeriodSec);
        log.info("API URI: {}", apiUri);
        log.info("User Name: {}", userName);
        log.info("Password: {}", password);

        final long bucketCapacity = 2;
        final long periodMillis = (long) (bucketCapacity * 1000 * pollPeriodSec);
        bucket = Bucket4j.builder()
                .withMillisecondPrecision()
                .addLimit(Bandwidth.simple(bucketCapacity, Duration.ofMillis(periodMillis)))
                .build();
        log.info("Token Bucket: {}", bucket);

        bucket.tryConsume(bucketCapacity - 1);
    }

    @Override
    public String initialState() {
        return "";
    }

    @Override
    public PollResponse<String> pollEvents(String state) throws Exception {
        log.info("pollEvents: BEGIN");
        bucket.asScheduler().consume(1);
        final List<PersistentQueueElement> events = new ArrayList<>();
        final HttpClient client = HttpClient.newHttpClient();
        final String uri = apiUri + "/ClientApi/V1/DeviceReadings";
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Authorization", "Bearer " + getAuthToken())
            .build();
        log.info("pollEvents: request={}", request);
        final HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        log.info("pollEvents: response={}", response);
        if (response.statusCode() != 200) {
            throw new RuntimeException(MessageFormat.format("HTTP server returned status code {0}", response.statusCode()));
        };
        log.trace("pollEvents: body={}", response.body());        
        JsonNode jsonNode = mapper.readTree(response.body());        
        final ArrayNode arrayNode = (ArrayNode) jsonNode;
        Date maxTime = mapper.convertValue(arrayNode.get(0).get("receivedTimestamp"), Date.class);

        final long timestampNanos = System.currentTimeMillis() * 1000 * 1000;
        for (JsonNode node : arrayNode) 
        {            
            final byte[] bytes = node.toString().getBytes(StandardCharsets.UTF_8);
            final String routingKey = getRoutingKey();
            final PersistentQueueElement event = new PersistentQueueElement(bytes, routingKey, timestampNanos);
            events.add(event);
            Date curReading = mapper.convertValue(node.get("receivedTimestamp"),Date.class);
            if(maxTime.getTime() < curReading.getTime())
                maxTime = curReading;
        }
        state = dateFormat.format(maxTime);
        final PollResponse<String> pollResponse = new PollResponse<String>(events, state);
        log.trace("pollEvents: pollResponse={}", pollResponse);
        log.info("pollEvents: END");
        return pollResponse;
    }

    protected String getAuthToken() throws Exception {
        log.info("getAuthToken: BEGIN");
        final HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
        final String uri = apiUri + "/api/Auth/authenticate";
        final AuthCredentialsDto authCredentialsDto = new AuthCredentialsDto(userName, password);
        final String requestBody = mapper.writeValueAsString(authCredentialsDto);
        log.info("getAuthToken: requestBody={}", requestBody);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .timeout(Duration.ofMinutes(1))
            .header("Accept", "*/*")
            .header("Content-Type", "application/json")
            // .POST(BodyPublishers.ofString(requestBody))
            .build();
        log.info("getAuthToken: request={}", request);
        final HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        log.info("getAuthToken: response={}", response);
        log.info("getAuthToken: body={}", response.body());
        if (response.statusCode() != 200) {
            throw new RuntimeException(MessageFormat.format("HTTP server returned status code {0} for request {1}",
                response.statusCode(), request));
        };
        final AuthTokenDto authTokenDto = mapper.readValue(response.body(), AuthTokenDto.class);
        log.info("getAuthToken: authTokenDto={}", authTokenDto);
        log.info("getAuthToken: END");
        return authTokenDto.token;
    }
}
