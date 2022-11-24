# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

# Load environment variables from env-local.sh if it exists.
export ENV_LOCAL_SCRIPT=$(dirname $0)/env-local.sh
if [[ -f ${ENV_LOCAL_SCRIPT} ]]; then
    source ${ENV_LOCAL_SCRIPT}
fi
export APP_VERSION=${APP_VERSION:-0.2.14}
export GRADLE_OPTIONS="${GRADLE_OPTIONS:-"-Pversion=${APP_VERSION}"}"
