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

# Check if pravega-controller-tls secret exists
if kubectl get secret pravega-controller-tls -n nautilus-pravega &> /dev/null; then
    kubectl get secret pravega-controller-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravega.crt
    chmod go-rw ~/pravega.crt
else
    echo "Secret pravega-controller-tls not found, extracting cluster-wildcard-tls-secret"
    kubectl get secret cluster-wildcard-tls-secret -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravegawc.crt
    chmod go-rw ~/pravegawc.crt
fi

# Check if keycloak-tls secret exists
if kubectl get secret keycloak-tls -n nautilus-system &> /dev/null; then
    kubectl get secret keycloak-tls -n nautilus-system -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/keycloak.crt
    chmod go-rw ~/keycloak.crt
else
    echo "Secret keycloak-tls not found, extracting cluster-wildcard-tls-secret"
    kubectl get secret cluster-wildcard-tls-secret -n nautilus-system -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/keycloakwc.crt
    chmod go-rw ~/keycloakwc.crt
fi

kubectl get secret pravega-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravegaAll.crt
chmod go-rw ~/pravegaAll.crt 

echo | openssl s_client -showcerts -servername keycloak.${SDP_DOMAIN} -connect keycloak.${SDP_DOMAIN}:443 2>/dev/null |  sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ~/keycloakIng.crt
chmod go-rw ~/keycloakIng.crt 

echo "Extracted files"
ls -l ~/ | grep -E '\.json|\.crt'