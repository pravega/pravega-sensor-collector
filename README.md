<!--
Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Pravega Sensor Collector

Pravega Sensor Collector collects data from sensors and ingests the data into
[Pravega](https://www.pravega.io/) streams.

- [Pravega Sensor Collector](#pravega-sensor-collector)
  - [Overview](#overview)
  - [About Pravega](#about-pravega)
  - [Supported Devices and Interfaces](#supported-devices-and-interfaces)
  - [About this Guide](#about-this-guide)
  - [Obtain the Installation Archive](#obtain-the-installation-archive)
    - [Download the Installation Archive](#download-the-installation-archive)
    - [Build the Installation Archive](#build-the-installation-archive)
  - [Installation](#installation)
    - [Configuration Overview](#configuration-overview)
    - [Install the Service](#install-the-service)
    - [Configure Keycloak Credentials](#configure-keycloak-credentials)
    - [Using a Private TLS Certificate Authority](#using-a-private-tls-certificate-authority)
    - [Update Hosts File](#update-hosts-file)
    - [Maintain the Service](#maintain-the-service)
  - [Data File Ingestion](#data-file-ingestion)
    - [Overview](#overview-1)
    - [Supported File Formats](#supported-file-formats)
      - [CSV](#csv)
      - [Parquet](#parquet)
    - [Write Sample Events](#write-sample-events)
  - [Phase IV Leap Wireless Gateway Integration](#phase-iv-leap-wireless-gateway-integration)
  - [Troubleshooting](#troubleshooting)
    - [Logging](#logging)
  - [Development Tips](#development-tips)
    - [Configuration for Debugging](#configuration-for-debugging)
    - [Adding Additional Devices](#adding-additional-devices)
    - [Start Pravega Server](#start-pravega-server)
    - [Start Leap API Mock Server](#start-leap-api-mock-server)
    - [Start Pravega Sensor Collector](#start-pravega-sensor-collector)
    - [View the SQLite Database](#view-the-sqlite-database)
  - [Release Procedure](#release-procedure)
  - [References](#references)
  - [About](#about)

## Overview

Pravega Sensor Collector can continuously collect high-resolution samples without interruption,
even if the network connection to the Pravega server is unavailable for long periods.
For instance, in a connected train use case, there may be long periods of time between cities where there is no network access.
During this time, Pravega Sensor Collector will store collected sensor data on a local disk and
periodically attempt to reconnect to the Pravega server.
It stores this sensor data in local SQLite database files.
When transferring samples from a SQLite database file to Pravega, it coordinates a SQLite transaction and a Pravega transaction.
This technique allows it to guarantee that all samples are sent in order, without gaps, and without duplicates,
even in the presence of computer, network, and power failures.

Pravega Sensor Collector is designed to collect samples at a rate of up to 1000 samples per second (1 kHz).
This makes it possible, for instance, to measure high frequency vibrations from accelerometers.
It batches multiple samples into larger events that are periodically sent to Pravega.
The batch size is configurable to allow you to tune the trade-off between latency and throughput that is
appropriate for your use case.

Pravega Sensor Collector is a Java application.
It has a plug-in architecture to allow Java developers to easily add drivers for different types of sensors
or to transform the sensor data in different ways.

## About Pravega

[Pravega](https://www.pravega.io/) is an open-source storage system for streams built from the ground up to
ingest data from continuous data sources and meet the stringent requirements of streaming workloads.
It provides the ability to store an unbounded amount of data per stream using tiered storage while being elastic,
durable and consistent.
Both the write and read paths of Pravega have been designed to provide low latency along
with high throughput for event streams in addition to features such as long-term retention and stream scaling.
Pravega provides an append-only write path to multiple partitions (referred to as segments) concurrently.
For reading, Pravega is optimized for both low latency sequential reads from the tail and high throughput
sequential reads of historical data.

## Supported Devices and Interfaces

Pravega Sensor Collector supports the following devices:

- ST Micro lng2dm 3-axis Femto accelerometer, directly connected via I2C
- Linux network interface card (NIC) statistics (byte counters, packet counters, error counters, etc.)
- Phase IV Leap Wireless Gateway
- Generic CSV file import
- OPC UA Client

## About this Guide

In the instructions that follow, the host where you should run each command.

- build-host: This is the host that you will use to build the installer archive.
- gw1: This is the host that will run Pravega Sensor Collector.
- edge1: This is the first host that is running the Pravega cluster.
  This may be a node running Streaming Data Platform (SDP).

## Obtain the Installation Archive

Use either of the following methods to obtain the installation archive.

### Download the Installation Archive

1.  Visit https://github.com/pravega/pravega-sensor-collector/releases.

2.  Download the latest version of pravega-sensor-collector-*.tgz.

### Build the Installation Archive

This must be executed on a build host that has Internet access.
This will download all dependencies and create a single archive that can be copied to an offline system.

1.  On the build host, build the installation archive.

    ```shell
    user@build-host:~$
    sudo apt-get install openjdk-11-jdk
    git clone https://github.com/pravega/pravega-sensor-collector
    cd pravega-sensor-collector
    scripts/build-installer.sh
    ```

    This will create the installation archive
    `pravega-sensor-collector/build/distributions/pravega-sensor-collector-${APP_VERSION}.tgz`.

## Installation

### Configuration Overview

Pravega Sensor Collector is conveniently configured using only environment variables.
This avoids the need to manage site-specific configuration files and allows a simple shell script
to be used to customize the configuration.
All environment variables and properties begin with the prefix `PRAVEGA_SENSOR_COLLECTOR`.

For a list of commonly-used configuration values, see the
[sample configuration files](pravega-sensor-collector/src/main/dist/conf).

### Install the Service

1.  The only prerequisite on the target system is Java 11.
    On Ubuntu, this can be installed with:
    ```shell
    admin@gw1:~$
    sudo apt-get install openjdk-11-jre
    ```

2.  Copy the installation archive to the target system in `/tmp`.

3.  Extract the archive.
    ```shell
    admin@gw1:~$
    sudo mkdir -p /opt/pravega-sensor-collector
    sudo tar -C /opt/pravega-sensor-collector --strip-components 1 \
       -xzvf /tmp/pravega-sensor-collector-*.tgz
    ```

4.  Create the configuration file, starting from a sample configuration file.
    Start with the sample configuration file that is most similar to your environment.
    See the [sample configuration files](pravega-sensor-collector/src/main/dist/conf).

    ```shell
    admin@gw1:~$
    sudo cp /opt/pravega-sensor-collector/conf/env-sample-network.sh /opt/pravega-sensor-collector/conf/env-local.sh
    ```

5.  Edit the configuration file `/opt/pravega-sensor-collector/conf/env-local.sh`.
    ```shell
    admin@gw1:~$
    sudo nano /opt/pravega-sensor-collector/conf/env-local.sh
    ```

6. At a minimum, you will need to change the following fields:

   1.  PRAVEGA_SENSOR_COLLECTOR_NET1_PRAVEGA_CONTROLLER_URI: This should have the value 
       `tls://pravega-controller.${sdp_domain_name}:443`, replacing `${sdp_domain_name}`
       with the corresponding value in env.yaml.

   2.  PRAVEGA_SENSOR_COLLECTOR_NET1_CREATE_SCOPE: If Pravega is on SDP, set this to `false`.

7.  Install and start as a Systemd service.
    ```shell
    admin@gw1:~$
    sudo /opt/pravega-sensor-collector/bin/install-service.sh

### Configure Keycloak Credentials

Keycloak is used to authenticate to Pravega on Streaming Data Platform (SDP).

1. Run the command below on the first SDP host to obtain the Keycloak credentials, which is a JSON object.
   Set the NAMESPACE variable to the name of your SDP project.
    ```shell
    edge@edge1:~$
    NAMESPACE=edge
    kubectl get secret ${NAMESPACE}-ext-pravega -n ${NAMESPACE} -o jsonpath="{.data.keycloak\.json}" | base64 -d ; echo
    ```

2. On the target, copy the Keycloak credentials to the file /opt/pravega-sensor-collector/conf/keycloak.json.
    ```shell
    admin@gw1:~$
    sudo nano /opt/pravega-sensor-collector/conf/keycloak.json
    ```

### Using a Private TLS Certificate Authority

If the TLS Certificate Authority (CA) used by Pravega is not trusted by a well-known public CA, such as Let's Encrypt, you must import the CA certificate.

1. Copy the CA certificate to the target system.
    ```shell
    edge@edge1:~/desdp$
    scp ~/desdp/certs/* admin@gw:
    ```
    OR
    ```
    kubectl get secret pravega-controller-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravega.crt
    kubectl get secret keycloak-tls -n nautilus-system -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/keycloak.crt
    kubectl get secret pravega-tls -n nautilus-pravega -o jsonpath="{.data.tls\.crt}" | base64 --decode > ~/pravegaAll.crt
    ```
2. On the target system, add the CA certificate to the operating system.
    ```shell
    admin@gw1:~$
    sudo /opt/pravega-sensor-collector/bin/import-ca-certificate.sh ~/*.crt
    ```

### Update Hosts File

If DNS is not configured throughout your network, you may need to edit the /etc/hosts file manually as described in this section.

1. On the first SDP host, run the following commands to obtain the correct IP addresses for the required FQDNs.
    ```shell
    edge@edge1:~/desdp$
    SDP_DOMAIN=sdp.sdp-demo.org
    echo $(dig +short keycloak.${SDP_DOMAIN})                                 keycloak.${SDP_DOMAIN} ; \
    echo $(dig +short pravega-controller.${SDP_DOMAIN})                       pravega-controller.${SDP_DOMAIN} ; \
    echo $(dig +short nautilus-pravega-segment-store-0.pravega.${SDP_DOMAIN}) nautilus-pravega-segment-store-0.pravega.${SDP_DOMAIN}
    ```

2. Ensure that the previous command returned an IP address for each host name. For example:
    ```
    10.42.0.10 keycloak.sdp.cluster1.sdp-demo.org
    10.42.0.10 pravega-controller.sdp.cluster1.sdp-demo.org
    10.42.0.12 nautilus-pravega-segment-store-0.pravega.sdp.cluster1.sdp-demo.org
    ```

3. On the target device, add the output from the previous command to the end of the file /etc/hosts.
    ```shell
    admin@gw1:~$
    sudo nano /etc/hosts
    ```

### Maintain the Service

1. Restart the service.
    ```shell
    admin@gw1:~$
    sudo systemctl restart pravega-sensor-collector.service
    ```

2. View the status of the service.
    ```shell
    admin@gw1:~$
    sudo systemctl status pravega-sensor-collector.service
    sudo journalctl -u pravega-sensor-collector.service -n 1000 --follow

### Running as a Windows Service

1.  Download winsw.exe from https://github.com/winsw/winsw/releases and rename it as PravegaSensorCollectorApp.exe.

2.  Modify [PravegaSensorCollectorApp.xml](windows-service/PravegaSensorCollectorApp.xml). Check PRAVEGA_SENSOR_COLLECTOR_PARQ1_PRAVEGA_CONTROLLER_URI.
    For parquet files, make sure PRAVEGA_SENSOR_COLLECTOR_PARQ1_FILE_SPEC is set correctly.

3.  Install and run the service using following commands:
    ```
    PravegaSensorCollectorApp.exe install 
    PravegaSensorCollectorApp.exe start 
    PravegaSensorCollectorApp.exe restart 
    PravegaSensorCollectorApp.exe status
    PravegaSensorCollectorApp.exe stop 
    PravegaSensorCollectorApp.exe uninstall 
    ```
    The logs for the sensor collector wil be available under windows-service/logs/PravegaSensorCollectorApp.wrapper.log 
    If there are any errors during service execution, the error log will be in windows-service/logs/PravegaSensorCollectorApp.out.log 

## Data File Ingestion

### Overview

Pravega Sensor Collector can be configured to read data from files and write the data to Pravega.

Periodically, new files that match the file name pattern in `LOG_FILE_INGEST_FILE_SPEC` will be identified and ingested.
The names of files can be in any format.
When multiple files match the file name pattern, the files will be ingested in alphabetical order.
For this reason, it is important that the file names are generated in alphabetical order.
This can be done by using a zero-padded counter (e.g. 0000000001.csv, 0000000002.csv, ...)
or a timestamp (2020-07-29-16-00-02.123.csv).

It is assumed that files matching the pattern are immediately readable in their entirety.
For this reason, it is critical that ingested files are created atomically.
This can be accomplished by writing to a file with a ".tmp" extension and then renaming it
to have a ".csv" extension after the file has been written in its entirety.

After being durably saved to the Pravega stream, the files will be deleted.
This can be disabled by setting `LOG_FILE_INGEST_DELETE_COMPLETED_FILES` to false.

Each instance of Pravega Sensor Collector will have a unique writer ID.
The writer ID will be a UUID that is generated the first time the instance starts.
The writer ID will be persisted to a local SQLite database file and subsequent executions will use the same writer ID.

The SQLite database stores the writer ID and the list of files being ingested.
SQL transactions are used to ensure database consistency even in the event of failures.

CSV files are deleted only after flushing events to Pravega.

Pravega Sensor Collector is installed as a Linux systemd service.
Systemd will start the service when the system boots up and it will restart the service if it fails.

### Supported File Formats

#### CSV

CSV files must have exactly one header. There are no other restrictions on the CSV file.
Data from multiple rows will be combined to efficiently produce events in JSON format.
When possible, integers and floating point values will be converted to their corresponding JSON data types.

Each JSON object may have additional static fields.
These can be defined in the parameter `LOG_FILE_INGEST_EVENT_TEMPLATE` which accepts a JSON object.

### Write Sample Events

Edit the configuration file /opt/pravega-sensor-collector/conf/env-local.sh
(See [env-sample-telit.sh](pravega-sensor-collector/src/main/dist/conf/env-sample-telit.sh) for a sample configuration)

If using the CSV file driver, you can simulate the functionality of it by using the procedure in this section.

1. On the target device, create the file named /opt/dw/staging/Accelerometer.0000000001.tmp.
    ```shell
    admin@gw1:~$
    cd /opt/dw/staging
    sudo nano Accelerometer.0000000001.tmp
    ```

2. Copy and paste the following contents, then save and exit:

    ```
    "T","X","Y","Z"
    "2020-08-19 19:35:44.029","0.458949","9.637929","0.611932"
    "2020-08-19 19:35:44.031","0.458949","9.484945","0.611932"
    "2020-08-19 19:35:44.033","0.458949","9.637929","0.611932"
    "2020-08-19 19:35:44.035","0.611932","9.484945","0.611932"
    ```

3. Rename the file to have a .csv extension. (This must be an atomic operation.)

    ```shell
    admin@gw1:~$
    sudo mv Accelerometer.0000000001.tmp Accelerometer.0000000001.csv
    ```

4. If you have deployed Flink Tools, within 2 minutes, on the SDP host, you should see the sample events written
   to the directory `/desdp/lts/edge-data-project-pvc-*/streaming-data-platform/$(hostname)/sensors-parquet/`.

#### Parquet

Parquet files must not have special characters or spaces in their header.
Data is parsed to efficiently produce events in JSON format.
When possible, integers and floating point values will be converted to their corresponding JSON data types.

The script [run-with-gradle-parquet-files-ingest.sh](pravega-sensor-collector\scripts\run-with-gradle-parquet-file-ingest.sh) can be edited for testing. 

Note: For windows, Hadoop requires native libraries on Windows to work properly. You can download `Winutils.exe` to fix this. See [here](https://cwiki.apache.org/confluence/display/HADOOP2/WindowsProblems).

## Phase IV Leap Wireless Gateway Integration

Edit the configuration file /opt/pravega-sensor-collector/conf/env-local.sh
(See [env-sample-leap.sh](pravega-sensor-collector/src/main/dist/conf/env-sample-leap.sh) for a sample configuration)

The following are example records that are written to the Pravega stream.

```json
{"deviceId":"000072FFFEF0000","readingTimestamp":"2021-11-24T18:42:23Z","receivedTimestamp":"2021-11-24T18:42:33.649728Z","values":[{"componentIndex":0,"sensorIndex":0,"valueIndex":0,"sensorValueDefinitionId":7,"value":23.88,"status":"Success","label":"Temperature  ","iconUrl":"/images/Thermometer_16x16.png","units":""},{"componentIndex":0,"sensorIndex":0,"valueIndex":1,"sensorValueDefinitionId":8,"value":23.87,"status":"Success","label":"Temperature Weighted Average ","iconUrl":"/images/Thermometer_16x16.png","units":""},{"componentIndex":0,"sensorIndex":1,"valueIndex":0,"sensorValueDefinitionId":9,"value":1,"status":"Success","label":"Door Status (Open / Closed)","iconUrl":"/images/Switch_16x16.png","units":""},{"componentIndex":0,"sensorIndex":1,"valueIndex":1,"sensorValueDefinitionId":10,"value":0,"status":"Success","label":"Door Open Time (sec)","iconUrl":"/images/Clock_16x16.png","units":""}]}
```

```json
{"deviceId":"000072FFFEF0000","readingTimestamp":"2021-11-24T18:40:23Z","receivedTimestamp":"2021-11-24T18:42:33.649728Z","values":[{"componentIndex":0,"sensorIndex":0,"valueIndex":0,"sensorValueDefinitionId":7,"value":23.88,"status":"Success","label":"Temperature  ","iconUrl":"/images/Thermometer_16x16.png","units":""},{"componentIndex":0,"sensorIndex":0,"valueIndex":1,"sensorValueDefinitionId":8,"value":23.87,"status":"Success","label":"Temperature Weighted Average ","iconUrl":"/images/Thermometer_16x16.png","units":""},{"componentIndex":0,"sensorIndex":1,"valueIndex":0,"sensorValueDefinitionId":9,"value":1,"status":"Success","label":"Door Status (Open / Closed)","iconUrl":"/images/Switch_16x16.png","units":""},{"componentIndex":0,"sensorIndex":1,"valueIndex":1,"sensorValueDefinitionId":10,"value":0,"status":"Success","label":"Door Open Time (sec)","iconUrl":"/images/Clock_16x16.png","units":""}]}
```

## Linux network interface card (NIC) statistics

Edit the configuration file /opt/pravega-sensor-collector/conf/env-local.sh
(See [env-sample-network.sh](pravega-sensor-collector/src/main/dist/conf/env-sample-network.sh) for a sample configuration)

To get `PRAVEGA_SENSOR_COLLECTOR_NET1_NETWORK_INTERFACE` value
```shell
admin@gw1:~$
ls /sys/class/net/
```

## OPC UA Client

Edit the configuration file /opt/pravega-sensor-collector/conf/env-local.sh
(See [env-sample-opcua.sh](pravega-sensor-collector/src/main/dist/conf/env-sample-opcua.sh) for a sample configuration)

To Simulate use OPC UA Servers like Kepware or opensource servers [opc-ua-demo-server](https://github.com/digitalpetri/opc-ua-demo-server)

## Troubleshooting

### Logging

You can adjust the logging level by editing the environment variable `JAVA_OPTS` and adding `-Droot.log.level=DEBUG`.
The default level is INFO.

For example:
```shell
export JAVA_OPTS="-Xmx512m -Droot.log.level=DEBUG"
```

## Development Tips

This information may be useful for developers.

### Configuration for Debugging

For convenience when debugging in an IDE, configuration values can also be specified in a properties file.
To use a properties file, you must set the environment variable `PRAVEGA_SENSOR_COLLECTOR_PROPERTIES_FILE`
to the path of the properties file.

### Adding Additional Devices

To add support for additional types of devices, you should create a subclass of [SimpleDeviceDriver.java](https://github.com/pravega/pravega-sensor-collector/blob/master/pravega-sensor-collector/src/main/java/io/pravega/sensor/collector/simple/SimpleDeviceDriver.java) and implement the methods `readRawData`, `createSamples`, `decodeRawDataToSamples`, and `serializeSamples`. Refer to the implementation of [NetworkDriver.java](https://github.com/pravega/pravega-sensor-collector/blob/master/pravega-sensor-collector/src/main/java/io/pravega/sensor/collector/network/NetworkDriver.java) as a guide.

### Start Pravega Server

```shell
admin@gw1:~$
cd
git clone https://github.com/pravega/pravega
cd pravega
git checkout r0.12
./gradlew startStandalone \
  -Dcontroller.transaction.lease.count.max=2592000000 \
  -Dcontroller.transaction.execution.timeBound.days=30
```

### Start Leap API Mock Server

To run the leap mock server
```shell
admin@gw1:~$
./gradlew pravega-sensor-collector::runLeapAPIMockServer
```

### Start Pravega Sensor Collector

```shell
admin@gw1:~$
PRAVEGA_SENSOR_COLLECTOR_PROPERTIES_FILE=src/test/resources/LeapTest.properties \
./gradlew pravega-sensor-collector::run
```

### View the SQLite Database

```shell
admin@gw1:~$
docker run --rm -it -v /tmp/leap1.db:/tmp/leap1.db keinos/sqlite3 sqlite3 /tmp/leap1.db .dump
```

## Release Procedure

Committers of the project should use the following procedure to release a new version.

1.  Increment `APP_VERSION` in `scripts/env.sh`.

2.  Commit the changes to the master branch using the normal Github pull request procedure.

3.  `git tag v0.2.14`

4.  `git push --tag`

5.  Open https://github.com/pravega/pravega-sensor-collector/releases and publish the draft release.

## References

- https://www.pravega.io/

## About

Pravega Sensor Collector is 100% open source and community-driven. All components are available
under [Apache 2 License](https://www.apache.org/licenses/LICENSE-2.0.html) on GitHub.
