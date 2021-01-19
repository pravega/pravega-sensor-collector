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
: ${1?"You must specify the certificate to import"}
CERT_FILE=$1
openssl x509 -text -fingerprint -in ${CERT_FILE}
CERT_NAME=$(basename -s .crt ${CERT_FILE})
mkdir -p /usr/local/share/ca-certificates/${CERT_NAME}
chmod 755 /usr/local/share/ca-certificates/${CERT_NAME}
cp ${CERT_FILE} /usr/local/share/ca-certificates/${CERT_NAME}/
chmod 644 /usr/local/share/ca-certificates/${CERT_NAME}/${CERT_NAME}.crt
update-ca-certificates -f
