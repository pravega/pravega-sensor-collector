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

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.memoryless.SimpleMemorylessDriver;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public class OpcUaClientDriver extends SimpleMemorylessDriver<OpcUaRawData> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientDriver.class);
    private static OpcUaClient opcUaClient;

    public OpcUaClientDriver(DeviceDriverConfig config) {
        super(config);
        try {
            //TODO :connection monitoring and restore
            opcUaClient = buildClient();
            opcUaClient.connect().get();
        } catch (UaException | InterruptedException | ExecutionException e) {
            log.error("Error in connection to UA Server", e);
        }
    }


    @Override
    public OpcUaRawData readRawData() throws UaException {
        //TODO : multi reads ?
        UaVariableNode node = opcUaClient.getAddressSpace().getVariableNode(getNodeID());
        DataValue data = node.readValue();
        return new OpcUaRawData((byte[]) data.getValue().getValue(),System.nanoTime());
    }

    //TODO : nodeID
    private NodeId getNodeID() {
        return null;
    }

    /**
     * Create a payload event to be written from raw data.
     *
     * @param rawData
     */
    @Override
    public byte[] getEvent(OpcUaRawData rawData) {
        return rawData.bytes;
    }

    /**
     * @param rawData
     * @return
     */
    @Override
    public long getTimestamp(OpcUaRawData rawData) {
        return rawData.timestamp;
    }

    private OpcUaClient buildClient() throws UaException {
        return OpcUaClient.create(
                getEndpointUrl(),
                endpoints ->
                        endpoints.stream()
                                .filter(endpointFilter())
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("Pravega Sensor Collector opc-ua client"))
                                .setApplicationUri("urn:pravega:sensor:collector:client")
                                .setKeyPair(getClientKeyPair())
                                .setCertificate(getClientCertificate())
                                .setCertificateChain(getClientCertificateChain())
                                .setCertificateValidator(getCertificateValidator())
                                .setIdentityProvider(getIdentityProvider())
                                .setRequestTimeout(UInteger.valueOf(5000))
                                .build());
    }

    //TODO : Fetch appropriate security policy
    private Predicate<EndpointDescription> endpointFilter() {
        return e-> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri());
    }

    //TODO : Keystore
    private ClientCertificateValidator getCertificateValidator() {
        return null;
    }

    //TODO : Keystore
    private X509Certificate[] getClientCertificateChain() {
        return null;
    }

    //TODO : Keystore
    private X509Certificate getClientCertificate() {
        return null;
    }

    //TODO : Keystore
    private KeyPair getClientKeyPair() {
        return null;
    }

    //TODO : Keystore
    private IdentityProvider getIdentityProvider() {
        return null;
    }

    //TODO : EP Config fetch
    private String getEndpointUrl() {
        return "opc.tcp://localhost:12686/milo";
    }

}
