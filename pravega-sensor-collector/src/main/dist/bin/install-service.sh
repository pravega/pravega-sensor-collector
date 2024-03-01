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
set -x
ROOT_DIR=$(readlink -f $(dirname $0)/..)
SERVICE_NAME=${SERVICE_NAME:-$(basename ${ROOT_DIR})}
echo Installing service ${SERVICE_NAME} located in ${ROOT_DIR}.
sed -e "s:\${ROOT_DIR}:${ROOT_DIR}:g" ${ROOT_DIR}/etc/pravega-sensor-collector-TEMPLATE.service \
    > ${ROOT_DIR}/etc/${SERVICE_NAME}.service
sed -e "s:\${ROOT_DIR}:${ROOT_DIR}:g" ${ROOT_DIR}/etc/psc-watchdog-TEMPLATE.service \
    > ${ROOT_DIR}/etc/psc-watchdog.service
systemctl stop ${SERVICE_NAME}.service
systemctl stop psc-watchdog.service
ln -svf ${ROOT_DIR}/etc/${SERVICE_NAME}.service /etc/systemd/system/${SERVICE_NAME}.service
ln -svf ${ROOT_DIR}/etc/psc-watchdog.service /etc/systemd/system/psc-watchdog.service
systemctl daemon-reload
systemctl enable ${SERVICE_NAME}.service
systemctl enable psc-watchdog.service
systemctl start ${SERVICE_NAME}.service
systemctl start psc-watchdog.service
