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
ROOT_DIR=$(readlink -f $(dirname $0)/..)
source ${ROOT_DIR}/scripts/env.sh
pushd ${ROOT_DIR}
cp ${ROOT_DIR}/pravega-sensor-collector/src/main/resources/logback.xml ${ROOT_DIR}/pravega-sensor-collector/src/main/dist/bin/logback.xml
GZIP="--rsyncable" ./gradlew distTar ${GRADLE_OPTIONS}
rm ${ROOT_DIR}/pravega-sensor-collector/src/main/dist/bin/logback.xml

ls -lh ${ROOT_DIR}/pravega-sensor-collector/build/distributions/pravega-sensor-collector-${APP_VERSION}.tgz

./gradlew shadowJar ${GRADLE_OPTIONS}
ls -lh ${ROOT_DIR}/pravega-sensor-collector/build/libs/pravega-sensor-collector-${APP_VERSION}.jar
popd
