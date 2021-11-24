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
export JAVA_OPTS="-Xmx512m"
export PRAVEGA_SENSOR_COLLECTOR_NET1_CLASS=io.pravega.sensor.collector.network.NetworkDriver
export PRAVEGA_SENSOR_COLLECTOR_NET1_NETWORK_INTERFACE=ens33
export PRAVEGA_SENSOR_COLLECTOR_NET1_MEMORY_QUEUE_CAPACITY_ELEMENTS=10000
export PRAVEGA_SENSOR_COLLECTOR_NET1_SAMPLES_PER_EVENT=100
export PRAVEGA_SENSOR_COLLECTOR_NET1_SAMPLES_PER_SEC=100
export PRAVEGA_SENSOR_COLLECTOR_NET1_PERSISTENT_QUEUE_FILE=/opt/pravega-sensor-collector/network-ens33.db
export PRAVEGA_SENSOR_COLLECTOR_NET1_PERSISTENT_QUEUE_CAPACITY_EVENTS=100
export PRAVEGA_SENSOR_COLLECTOR_NET1_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.cluster1.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_NET1_SCOPE=edge
export PRAVEGA_SENSOR_COLLECTOR_NET1_CREATE_SCOPE=false
export PRAVEGA_SENSOR_COLLECTOR_NET1_STREAM=sensors
export PRAVEGA_SENSOR_COLLECTOR_NET1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_NET1_EXACTLY_ONCE=true
export PRAVEGA_SENSOR_COLLECTOR_NET1_TRANSACTION_TIMEOUT_MINUTES=2.0
