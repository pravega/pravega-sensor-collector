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
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

public class OpcUaClientDriver extends SimpleMemorylessDriver<OpcUaRawData> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientDriver.class);
    private static String ENDPOINT = "ENDPOINT";
    private static String NS_INDEX = "NAMESPACE_INDEX";
    private static String NODE_ID  = "NODE_IDENTIFIER";
    private static String NODE_FILTER  = "NODE_FILTER_REGEX";
    private static OpcUaClient opcUaClient;
    private static Pattern sys;
    private static List<NodeId> sensorList ;
    private static NamespaceTable ns;

    public OpcUaClientDriver(DeviceDriverConfig config) {
        super(config);
        try {
            log.info("Trying establishment with OPC server");
            //TODO :connection monitoring and restore
            opcUaClient = buildClient();
            opcUaClient.connect().get();
            log.info("Connection established with OPC server");
            ns = opcUaClient.getNamespaceTable();
            log.info("Creating sensor List");
            sys = Pattern.compile(getNodeFilter());
            sensorList = new LinkedList<>();
            if (opcUaClient.getAddressSpace().getNode(getNodeID()).getNodeClass().getValue() == NodeClass.Variable.getValue())
            {
                sensorList.add(getNodeID());
            }
            browseNode("",opcUaClient,getNodeID());
        } catch (UaException | InterruptedException | ExecutionException e) {
            log.error("Error in connection to UA Server", e);
        }
    }


    @Override
    public List<OpcUaRawData> readRawData() throws UaException {


        List<OpcUaRawData> dataList = new LinkedList<>();
        for (NodeId nodeIt : sensorList)
        {
            UaVariableNode node = opcUaClient.getAddressSpace().getVariableNode(nodeIt);
            DataValue data = node.readValue();
            log.info("Sensor {} with Raw Data {} ",nodeIt.getIdentifier(),data.getValue().getValue());
            dataList.add(new OpcUaRawData(data.getValue().getValue(),data.getSourceTime().getUtcTime()));
        }
        return dataList;
    }

    private void browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        BrowseDescription browse = new BrowseDescription(
                browseRoot,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                uint(BrowseResultMask.All.getValue())
        );

        try {
            BrowseResult browseResult = client.browse(browse).get();

            List<ReferenceDescription> references = toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                if (rd.getBrowseName().getName().equalsIgnoreCase("_Hints"))
                {
                    continue;
                }
                else if (sys.matcher(rd.getBrowseName().getName()).find() && rd.getNodeClass().getValue() == NodeClass.Variable.getValue())
                {
                    //Sensor Node
                    log.info("Qualified Sensor:"+rd.getNodeId().toNodeId(ns).get().getIdentifier());
                    sensorList.add(rd.getNodeId().toNodeId(ns).get());
                }
                //log.info("{} Node={} ::: Regex match : {} :: DataType {}", indent, rd.getBrowseName().getName(), !sys.matcher(rd.getBrowseName().getName()).find(),rd.getNodeClass());
                // recursively browse to children
                rd.getNodeId().toNodeId(client.getNamespaceTable())
                        .ifPresent(nodeId -> browseNode(indent + "  ", client, nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
        }
    }

    //TODO : nodeID
    private NodeId getNodeID() {
        return new NodeId(Integer.parseInt(getProperty(NS_INDEX, "2")),getProperty(NODE_ID));
    }

    private String getNodeFilter(){
        return getProperty(NODE_FILTER , "^(?!_).*");
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
