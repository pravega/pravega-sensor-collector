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

export PRAVEGA_SENSOR_COLLECTOR_PROPERTIES_FILE=${HOME}/Documents/pravega-sensor-collector/pravega-sensor-collector/src/test/resources/LeapTest.properties

./gradlew pravega-sensor-collector::run
