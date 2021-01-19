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
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

# Build Pravega Sensor Collector locally, then copy and install it on a remote system.

set -ex
ROOT_DIR=$(readlink -f $(dirname $0)/..)
source ${ROOT_DIR}/scripts/env.sh
INSTALLER_TGZ=${APP_NAME}-${APP_VERSION}.tgz

: ${SSH_HOST?"You must export SSH_HOST"}

${ROOT_DIR}/scripts/build-installer.sh
INSTALLER_TGZ=pravega-sensor-collector-${APP_VERSION}.tgz
rsync -e "ssh ${SSH_OPTS}" -v -c --progress ${ROOT_DIR}/pravega-sensor-collector/build/distributions/${INSTALLER_TGZ} ${SSH_HOST}:/tmp

ssh -t ${SSH_OPTS} ${SSH_HOST} \
"sudo mkdir -p /opt/pravega-sensor-collector && \
sudo tar -C /opt/pravega-sensor-collector --strip-components 1 -xzvf /tmp/${INSTALLER_TGZ} && sudo /opt/pravega-sensor-collector/bin/install-service.sh"
