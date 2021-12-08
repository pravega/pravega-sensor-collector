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

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public class OpcUaClientDriver extends SimpleMemorylessDriver<OpcUaRawData> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientDriver.class);
    private static String ENDPOINT = "ENDPOINT";
    private static String NS_INDEX = "NAMESPACE_INDEX";
    private static String NODE_ID  = "NODE_IDENTIFIER";
    private static OpcUaClient opcUaClient;

    public OpcUaClientDriver(DeviceDriverConfig config) {
        super(config);
        try {
            log.info("Trying establishment with OPC server");
            //TODO :connection monitoring and restore
            opcUaClient = buildClient();
            opcUaClient.connect().get();
            log.info("Connection established with OPC server");
        } catch (UaException | InterruptedException | ExecutionException e) {
            log.error("Error in connection to UA Server", e);
        }
    }


    @Override
    public OpcUaRawData readRawData() throws UaException, IOException {
        UaVariableNode node = opcUaClient.getAddressSpace().getVariableNode(getNodeID());
        DataValue data = node.readValue();
        log.info("Raw-Data of sensor "+ data.getValue().getValue());
        return new OpcUaRawData(data.getValue().getValue(),data.getSourceTime().getUtcTime());
    }

    //TODO : nodeID
    private NodeId getNodeID() {
        return new NodeId(Integer.parseInt(getProperty(NS_INDEX, "2")),getProperty(NODE_ID));
    }

    /**
     * Create a payload event to be written from raw data.
     *
     * @param rawData
     */
    @Override
    public Object getEvent(OpcUaRawData rawData) {
        return rawData.data;
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
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("Pravega Sensor Collector opc-ua client"))
                                .setApplicationUri("urn:pravega:sensor:collector:client")
                                .setRequestTimeout(UInteger.valueOf(5000))
                                .build()); //Insecure Client connection creation.
    }

    //TODO : Fetch appropriate security policy : Current -NONE
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
        return getProperty(ENDPOINT,"opc.tcp://127.0.0.1:49320");
    }

}
