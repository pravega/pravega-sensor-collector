#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Ingest from CSV files produced by Telit deviceWise.
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/conf/keycloak.json
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_CLASS=io.pravega.sensor.collector.file.LogFileIngestService
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_FILE_SPEC="/opt/dw/staging/*.csv"
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_DELETE_COMPLETED_FILES=true
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_DATABASE_FILE=/tmp/accelerometer.db
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_EVENT_TEMPLATE="{\"RemoteAddr\":\"$(hostname)\"}"
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_SAMPLES_PER_EVENT=200
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_SCOPE=edge
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_CREATE_SCOPE=false
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_STREAM=sensors
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_ACCEL1_TRANSACTION_TIMEOUT_MINUTES=2.0
export JAVA_OPTS="-Xmx512m"
