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
export SENSOR_CONFIG_DEVICE_FILE="/sys/bus/iio/devices/iio:device3"
export SENSOR_DATA_DEVICE_FILE="/dev/iio:device3"
export PRAVEGA_CONTROLLER=tcp://10.42.0.11:9090
export PRAVEGA_SCOPE=edge
export PRAVEGA_STREAM=pravega-sensor-collector-test1
export CREATE_SCOPE=false
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=${HOME}/keycloak.json
export SAMPLE_RATE_HZ=1600
export SAMPLES_PER_EVENT=1600
export ROUTING_KEY=${HOSTNAME}
export ENABLE_PRAVEGA=true
export ENABLE_SENSOR_COLLECTOR=true
./gradlew --no-daemon run
