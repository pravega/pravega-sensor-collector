#
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
export pravega_client_auth_method=Bearer
export pravega_client_auth_loadDynamic=true
export KEYCLOAK_SERVICE_ACCOUNT_FILE=/opt/pravega-sensor-collector/conf/keycloak.json
export JAVA_OPTS="-Xmx512m"
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_CLASS=io.pravega.sensor.collector.leap.LeapDriver

# Set this to IP address of your Leap Wireless Gateway. Port 80 is normally used.
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_API_URI=http://127.0.0.1:8083
# You can use the same username and password that you use to login to the Leap Wireless Gateway web site.
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_USERNAME=admin
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_PASSWORD=mypassword

export PRAVEGA_SENSOR_COLLECTOR_LEAP1_POLL_PERIOD_SEC=180
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_PERSISTENT_QUEUE_FILE=/opt/pravega-sensor-collector/leap1.db
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_PERSISTENT_QUEUE_CAPACITY_EVENTS=10000
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_SCOPE=examples
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_CREATE_SCOPE=false
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_STREAM=leap
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_EXACTLY_ONCE=true
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_PRAVEGA_CONTROLLER_URI=tls://pravega-controller.sdp.cluster1.sdp-demo.org:443
export PRAVEGA_SENSOR_COLLECTOR_LEAP1_TRANSACTION_TIMEOUT_MINUTES=2.0
