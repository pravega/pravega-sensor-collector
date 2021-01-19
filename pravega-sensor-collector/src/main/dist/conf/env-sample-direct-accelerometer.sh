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
# Direct accelerometer sensor access
export SENSOR_CONFIG_DEVICE_FILE="/sys/bus/iio/devices/iio:device3"
export SENSOR_DATA_DEVICE_FILE="/dev/iio:device3"
export PRAVEGA_CONTROLLER=tcp://10.42.0.11:9090
export PRAVEGA_SCOPE=edge
export PRAVEGA_STREAM=pravega-sensor-collector-test3
export CREATE_SCOPE=false
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/etc/keycloak.json
export SAMPLE_RATE_HZ=1600
export SAMPLES_PER_EVENT=1600
export ROUTING_KEY=$(hostname)
export ENABLE_PERSISTENT_QUEUE_TO_PRAVEGA=true
export ENABLE_SENSOR_COLLECTOR=true
export LOG_FILE_INGEST_ENABLE=false
export JAVA_OPTS="-Xmx768m"

PREFIX=PRAVEGA_SENSOR_COLLECTOR_
INSTANCE_NAME=ACCEL1
export ${PREFIX}${INSTANCE_NAME}_CLASS=io.pravega.sensor.collector.accelerometer.AccelerometerDriver
export ${PREFIX}${INSTANCE_NAME}_SAMPLE_RATE_HZ=1600
