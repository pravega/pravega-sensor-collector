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
export KEYCLOAK_SERVICE_ACCOUNT_FILE=~/Documents/sensor_collector/opt/pravega-sensor-collector/conf/keycloak.json
export JAVA_OPTS="-Xmx512m"

export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_CLASS=io.pravega.sensor.collector.parquet.ParquetFileIngestService
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_FILE_SPEC=C:/Users/apoorva_shivpuriya/Documents/sensor_collector_fork/ParquetFiles/Modified.Coil_A1712_0125.parquet
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_DATABASE_FILE=C:/Users/apoorva_shivpuriya/Documents/sensor_collector_fork/datafile.db
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_SAMPLES_PER_EVENT=200
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.shinobi-shaw.ns.sdp.hop.lab.emc.com:443
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_SCOPE=project1
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_STREAM=stream-p
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_DELETE_COMPLETED_FILES=false
export PRAVEGA_SENSOR_COLLECTOR_ACCEL2_TRANSACTION_TIMEOUT_MINUTES=2.0

# windows - location of winutils.exe
export HADOOP_HOME=C:/Users/apoorva_shivpuriya/dev

./gradlew --no-daemon run
