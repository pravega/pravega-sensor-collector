#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# TODO: This sample config file is out-of-date.
# Ingest from CSV files produced by Telit deviceWise.
export PRAVEGA_CONTROLLER=tls://pravega-controller.sdp1.example.com:443
export PRAVEGA_SCOPE=edge
export PRAVEGA_STREAM=sensors
export CREATE_SCOPE=false
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/conf/keycloak.json
export ROUTING_KEY=$(hostname)
export ENABLE_SENSOR_COLLECTOR=false
export ENABLE_PERSISTENT_QUEUE_TO_PRAVEGA=false
export SAMPLES_PER_EVENT=200
export LOG_FILE_INGEST_ENABLE=true
export LOG_FILE_INGEST_EVENT_TEMPLATE="{\"RemoteAddr\":\"$(hostname)\"}"
export LOG_FILE_INGEST_FILE_SPEC="/opt/dw/staging/*.csv"
export JAVA_OPTS="-Xmx512m"
