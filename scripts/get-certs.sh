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
NAMESPACE=${NAMESPACE:-project1}
SDP_DOMAIN=${SDP_DOMAIN:-sdp.sdp-demo.org}

kubectl get secret ${NAMESPACE}-ext-pravega -n ${NAMESPACE} -o jsonpath="{.data.keycloak\.json}" |base64 -d ;echo > ~/keycloak.json
chmod go-rw ~/keycloak.json

kubectl get secret pravega-controller-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravega.crt
chmod go-rw ~/pravega.crt

kubectl get secret keycloak-tls -n nautilus-system -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/keycloak.crt
chmod go-rw ~/keycloak.crt

kubectl get secret pravega-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravegaAll.crt
chmod go-rw ~/pravegaAll.crt 

echo | openssl s_client -showcerts -servername keycloak.${SDP_DOMAIN} -connect keycloak.${SDP_DOMAIN}:443 2>/dev/null |  sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ~/keycloakIng.crt
chmod go-rw ~/keycloakIng.crt 
