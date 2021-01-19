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
CONF_DIR=$(readlink -f $(dirname $0)/../conf)
# Load environment variables from env-local.sh if it exists.
if [ -f ${CONF_DIR}/env-local.sh ]; then
    . ${CONF_DIR}/env-local.sh
fi
# Load environment variables from env-HOSTNAME.sh if it exists.
if [ -f ${CONF_DIR}/env-$(hostname).sh ]; then
    . ${CONF_DIR}/env-$(hostname).sh
fi
