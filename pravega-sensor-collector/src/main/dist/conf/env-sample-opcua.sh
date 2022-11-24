#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/conf/keycloak.json
export PRAVEGA_SENSOR_COLLECTOR_OPC1_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.cluster1.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_OPC1_CLASS=io.pravega.sensor.collector.opcua.OpcUaClientDriver
export PRAVEGA_SENSOR_COLLECTOR_OPC1_SCOPE=edge
export PRAVEGA_SENSOR_COLLECTOR_OPC1_STREAM=opc_sensor_stream
export PRAVEGA_SENSOR_COLLECTOR_OPC1_EXACTLY_ONCE=false
export PRAVEGA_SENSOR_COLLECTOR_OPC1_TRANSACTION_TIMEOUT_MINUTES=2.0
export PRAVEGA_SENSOR_COLLECTOR_OPC1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_OPC1_POLL_PERIODICITY_MS=1000
export PRAVEGA_SENSOR_COLLECTOR_OPC1_ENDPOINT=opc.tcp://127.0.0.1:62541/milo
export PRAVEGA_SENSOR_COLLECTOR_OPC1_NAMESPACE_INDEX=2
export PRAVEGA_SENSOR_COLLECTOR_OPC1_NODE_IDENTIFIER=Dynamic/RandomInt32
export PRAVEGA_SENSOR_COLLECTOR_OPC1_NODE_FILTER_REGEX='.*'
