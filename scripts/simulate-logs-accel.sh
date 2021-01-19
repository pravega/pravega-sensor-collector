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
rm -rf /tmp/watch
mkdir -p /tmp/watch
#rm -f /tmp/accelerometer.db
#rm -f /tmp/plc.db

while [ 1 ]; do
  for f in log-file-sample-data/Accelerometer.*.csv; do
    cp "$f" /tmp/watch
    ls -l /tmp/watch
    sleep 10s
  done
done
