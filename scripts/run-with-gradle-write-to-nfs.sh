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
export JAVA_OPTS="-Xmx512m"

export PRAVEGA_SENSOR_COLLECTOR_RAW1_CLASS=io.pravega.sensor.collector.writetonfs.RawFileMoveService
export PRAVEGA_SENSOR_COLLECTOR_RAW1_FILE_SPEC="C:\\Users\\winuser\\PSC\\writeToNFS\\files3"
export PRAVEGA_SENSOR_COLLECTOR_RAW1_FILE_EXTENSION=parquet
export PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE=/c/Users/winuser/PSC/writeToNFS/datafile.db
export PRAVEGA_SENSOR_COLLECTOR_RAW1_NFS_MOUNT_PATH="Z:"
export PRAVEGA_SENSOR_COLLECTOR_RAW1_ROUTING_KEY=$(hostname)
export PRAVEGA_SENSOR_COLLECTOR_RAW1_DELETE_COMPLETED_FILES=false
export PRAVEGA_SENSOR_COLLECTOR_RAW1_TRANSACTION_TIMEOUT_MINUTES=2.0

./gradlew --no-daemon run
