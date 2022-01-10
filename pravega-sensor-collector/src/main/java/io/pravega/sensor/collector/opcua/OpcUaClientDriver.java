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

import com.google.gson.Gson;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.simple.memoryless.SimpleMemorylessDriver;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.eclipse.milo.opcua.stack.core.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

public class OpcUaClientDriver extends SimpleMemorylessDriver<OpcUaRawData> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientDriver.class);
    private static final Gson jsonParser = new Gson();

    private static String ENDPOINT = "ENDPOINT";
    private static String NS_INDEX = "NAMESPACE_INDEX";
    private static String NODE_ID = "NODE_IDENTIFIER";
    private static String NODE_FILTER = "NODE_FILTER_REGEX";
    private static OpcUaClient opcUaClient;
    private static Pattern nodeFilter;
    private static List<NodeId> sensorList;
    private static List<ReadValueId> readValueIds;
    private static NamespaceTable ns;

    public OpcUaClientDriver(DeviceDriverConfig config) throws UaException, ExecutionException, InterruptedException {
        super(config);
        log.info("Trying to establish connection with OPC server");
        //TODO :connection monitoring and restore
        opcUaClient = buildClient();
        opcUaClient.connect().get();
        log.info("Connection established with OPC server");
        ns = opcUaClient.getNamespaceTable();
        log.info("Creating sensor List");
        nodeFilter = Pattern.compile(getNodeFilter());
        sensorList = new LinkedList<>();
        if (opcUaClient.getAddressSpace().getNode(getNodeID()).getNodeClass().getValue() == NodeClass.Variable.getValue()) {
            sensorList.add(getNodeID());
        }
        filterNode(opcUaClient, getNodeID());
        readValueIds = sensorList.stream().map(nodeId -> new ReadValueId(nodeId, AttributeId.Value.uid(), null, null)).collect(Collectors.toUnmodifiableList());
    }


    @Override
    public List<OpcUaRawData> readRawData() throws ExecutionException, InterruptedException {
        List<OpcUaRawData> dataList = new LinkedList<>();
        ReadResponse readResp = opcUaClient.read(0, TimestampsToReturn.Source, readValueIds).get();
        DataValue[] aggregatedData = readResp.getResults();
        int i = 0;
        for (DataValue data : aggregatedData) {
            // As bulk read operation is in-place read , the list ordering of the input nodes will match the data fetched from responses.
            Variant rawVariant = data.getValue();
            String dataTypeClass = TypeUtil.getBackingClass(rawVariant.getDataType().get().toNodeId(ns).get()).getName();
            log.trace("Sensor name {} : Raw Data {}", sensorList.get(i).getIdentifier(), rawVariant.getValue());
            dataList.add(new OpcUaRawData(rawVariant.getValue(), data.getSourceTime().getUtcTime(), sensorList.get(i++).getIdentifier().toString(), dataTypeClass));
        }
        return dataList;
    }

    private void filterNode(OpcUaClient client, NodeId rootNode) {
        BrowseDescription browse = new BrowseDescription(
                rootNode,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()), // Get both Objects and Variable types while browsing
                uint(BrowseResultMask.All.getValue())
        );

        try {
            BrowseResult browseResult = client.browse(browse).get();
            List<ReferenceDescription> references = toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                if (rd.getBrowseName().getName().equalsIgnoreCase("_Hints")) {
                    //Skip node iteration if the node name is _Hints as it contains hints about variables creation functions supported by server.
                    continue;
                } else if (nodeFilter.matcher(rd.getBrowseName().getName()).find() && rd.getNodeClass().getValue() == NodeClass.Variable.getValue()) {
                    //Sensor which matches RegEx and node type being a Variable.
                    log.info("Qualified Sensor: {}", rd.getNodeId().toNodeId(ns).get().getIdentifier());
                    sensorList.add(rd.getNodeId().toNodeId(ns).get());
                }

                rd.getNodeId().toNodeId(client.getNamespaceTable())
                        .ifPresent(nodeId -> filterNode(client, nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Browsing nodeId={} failed: {}", rootNode, e.getMessage(), e);
        }
    }

    private NodeId getNodeID() {
        return new NodeId(Integer.parseInt(getProperty(NS_INDEX, "2")), getProperty(NODE_ID));
    }

    private String getNodeFilter() {
        return getProperty(NODE_FILTER, "^(?!_).*");
    }


    @Override
    public byte[] getEvent(OpcUaRawData rawData) {
        return jsonParser.toJson(rawData).getBytes(StandardCharsets.UTF_8);
    }

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
                                .setRequestTimeout(UInteger.valueOf(5000))
                                .build()); //Insecure Client connection creation.
    }


    //TODO : Fetch appropriate security policy : Current -NONE
    private Predicate<EndpointDescription> endpointFilter() {
        return e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri());
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

    private String getEndpointUrl() {
        return getProperty(ENDPOINT, "opc.tcp://127.0.0.1:49320");
    }

}
