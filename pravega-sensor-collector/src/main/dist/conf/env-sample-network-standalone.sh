#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Collect network statistics and write to a local standalone Pravega.
export PRAVEGA_SENSOR_COLLECTOR_NET1_CLASS=io.pravega.sensor.collector.network.NetworkDriver
export PRAVEGA_SENSOR_COLLECTOR_NET1_NETWORK_INTERFACE=ens33
export PRAVEGA_SENSOR_COLLECTOR_NET1_MEMORY_QUEUE_CAPACITY_ELEMENTS=10000   # number of samples
export PRAVEGA_SENSOR_COLLECTOR_NET1_SAMPLES_PER_EVENT=1000
export PRAVEGA_SENSOR_COLLECTOR_NET1_SAMPLES_PER_SEC=1000
export PRAVEGA_SENSOR_COLLECTOR_NET1_PERSISTENT_QUEUE_FILE=/tmp/pravega-sensor-collector-NET1.db
export PRAVEGA_SENSOR_COLLECTOR_NET1_PERSISTENT_QUEUE_CAPACITY_EVENTS=86400   # 1 day x 1 event/sec
export PRAVEGA_SENSOR_COLLECTOR_NET1_PRAVEGA_CONTROLLER_URI=tcp://localhost:9090
export PRAVEGA_SENSOR_COLLECTOR_NET1_SCOPE=examples
export PRAVEGA_SENSOR_COLLECTOR_NET1_CREATE_SCOPE=true
export PRAVEGA_SENSOR_COLLECTOR_NET1_STREAM=network
export PRAVEGA_SENSOR_COLLECTOR_NET1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_NET1_EXACTLY_ONCE=true
export PRAVEGA_SENSOR_COLLECTOR_NET1_TRANSACTION_TIMEOUT_MINUTES=10080    # 1 week
