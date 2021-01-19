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

export CREATE_SCOPE=true
export ROUTING_KEY=${HOSTNAME}
export ENABLE_PRAVEGA=true
export ENABLE_SENSOR_COLLECTOR=false
export LOG_FILE_INGEST_ENABLE=true

export LOG_FILE_INGEST_0_FILE_SPEC="/tmp/watch/Accelerometer.*.csv"
export LOG_FILE_INGEST_0_DATABASE_FILE="/tmp/accelerometer.db"
export LOG_FILE_INGEST_0_EVENT_TEMPLATE="{\"RemoteAddr\":\"$(hostname)\",\"SensorType\":\"Accelerometer\"}"
export LOG_FILE_INGEST_0_SAMPLES_PER_EVENT=200
export LOG_FILE_INGEST_0_PRAVEGA_STREAM=sensors-accelerometer
export LOG_FILE_INGEST_0_ROUTING_KEY=$(hostname)
export LOG_FILE_INGEST_0_DELETE_COMPLETED_FILES=true

export LOG_FILE_INGEST_1_FILE_SPEC="/tmp/watch/PLC.*.csv"
export LOG_FILE_INGEST_1_DATABASE_FILE="/tmp/plc.db"
export LOG_FILE_INGEST_1_EVENT_TEMPLATE="{\"RemoteAddr\":\"$(hostname)\",\"SensorType\":\"PLC\"}"
export LOG_FILE_INGEST_1_SAMPLES_PER_EVENT=20
export LOG_FILE_INGEST_1_PRAVEGA_STREAM=sensors-plc
export LOG_FILE_INGEST_1_ROUTING_KEY=$(hostname)
export LOG_FILE_INGEST_1_DELETE_COMPLETED_FILES=true

./gradlew --no-daemon run
