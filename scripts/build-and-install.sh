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
sudo systemctl stop pravega-sensor-collector.service || true
./gradlew --no-daemon distTar
sudo mkdir -p /opt/pravega-sensor-collector
sudo tar -C /opt/pravega-sensor-collector --strip-components 1 \
    -xvf pravega-sensor-collector/build/distributions/pravega-sensor-collector-${APP_VERSION}.tar
sudo /opt/pravega-sensor-collector/bin/install-service.sh
