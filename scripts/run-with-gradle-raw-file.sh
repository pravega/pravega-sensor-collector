#!/usr/bin/env bash
#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
set -ex

export CREATE_SCOPE=false
export ROUTING_KEY=${HOSTNAME}
export ENABLE_PRAVEGA=true
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/conf/keycloak.json
export JAVA_OPTS="-Xmx512m"

export PRAVEGA_SENSOR_COLLECTOR_RAW1_CLASS=io.pravega.sensor.collector.rawfile.RawFileIngestService
export PRAVEGA_SENSOR_COLLECTOR_RAW1_FILE_SPEC=/opt/pravega-sensor-collector/Files
export PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE=/opt/pravega-sensor-collector/datafile.db
export PRAVEGA_SENSOR_COLLECTOR_RAW1_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.cluster1.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_RAW1_SCOPE=project1
export PRAVEGA_SENSOR_COLLECTOR_RAW1_STREAM=stream-p
export PRAVEGA_SENSOR_COLLECTOR_RAW1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_RAW1_DELETE_COMPLETED_FILES=false
export PRAVEGA_SENSOR_COLLECTOR_RAW1_TRANSACTION_TIMEOUT_MINUTES=2.0
export PRAVEGA_SENSOR_COLLECTOR_RAW1_CREATE_SCOPE=false

./gradlew --no-daemon run
