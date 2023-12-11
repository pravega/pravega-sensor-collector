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

export PRAVEGA_SENSOR_COLLECTOR_PARQ2_CLASS=io.pravega.sensor.collector.file.parquet.ParquetFileIngestService
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_FILE_SPEC="/opt/pravega-sensor-collector/Parquet_Files/A,/opt/pravega-sensor-collector/Parquet_Files/B"
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_FILE_EXTENSION=parquet
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_DATABASE_FILE=/opt/pravega-sensor-collector/datafile.db
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_SAMPLES_PER_EVENT=200
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.cluster1.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_SCOPE=project1
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_STREAM=stream-p
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_DELETE_COMPLETED_FILES=false
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_TRANSACTION_TIMEOUT_MINUTES=2.0
export PRAVEGA_SENSOR_COLLECTOR_PARQ2_CREATE_SCOPE=false

# windows - location of bin/winutils.exe
export HADOOP_HOME=${HOME}/dev

./gradlew --no-daemon run
