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
# Simulate an accelerometer device by writing to a FIFO that behaves like the real character device.
set -x
mkfifo /tmp/accelfifo
while [ 1 ]; do
  cp fake-sensor-accel/dev/iio_device3_1600_samples /tmp/accelfifo
  sleep 1s
done
