/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector;

import com.google.common.base.Preconditions;
import io.pravega.client.ClientConfig;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class PravegaClientConfig {

    private static final String PRAVEGA_CONTROLLER_URI_KEY = "PRAVEGA_CONTROLLER_URI";
    private final URI controllerURI;
    private final String scopeName;

    public PravegaClientConfig(URI controllerURI, String scopeName) {
        this.controllerURI = Preconditions.checkNotNull(controllerURI, "controllerURI");
        this.scopeName = Preconditions.checkNotNull(scopeName, "scopeName");
    }

    public PravegaClientConfig(Map<String, String> properties, String scopeName) {
        this(URI.create(properties.getOrDefault(PRAVEGA_CONTROLLER_URI_KEY, "tcp://localhost:9090")), scopeName);
    }

    public String getScopeName() {
        return scopeName;
    }

    public ClientConfig toClientConfig() {
        return ClientConfig.builder().controllerURI(controllerURI).build();
    }

    @Override
    public String toString() {
        return "PravegaClientConfig{" +
                "controllerURI=" + controllerURI +
                ", scopeName='" + scopeName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PravegaClientConfig that = (PravegaClientConfig) o;
        return controllerURI.equals(that.controllerURI)
                && scopeName.equals(that.scopeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(controllerURI, scopeName);
    }
}
