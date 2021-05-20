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
- Generic CSV file import

## Build the installation archive

This must be executed on a build host that has Internet access.
This will download all dependencies and create a single archive that can be copied to an offline system.

1.  On the build host, build the installation archive.

    ```shell script   
    user@build-host:~$
    git clone https://github.com/pravega/pravega-sensor-collector
    cd pravega-sensor-collector
    scripts/build-installer.sh
    ```
    
    This will create the installation archive
    `pravega-sensor-collector/build/distributions/pravega-sensor-collector-${APP_VERSION}.tgz`.

## Install Pravega Sensor Collector

1.  The only prerequisite on the target system is Java 8.x.
    On Ubuntu, this can be installed with:
    ```shell script
    sudo apt-get install openjdk-8-jre
    ```
   
2.  Copy the installation archive to the target system in `/tmp`.

3.  Extract the archive.
    ```shell script
    sudo mkdir -p /opt/pravega-sensor-collector
    sudo tar -C /opt/pravega-sensor-collector --strip-components 1 \
       -xzvf /tmp/pravega-sensor-collector-*.tgz
    ```
   
4.  Create the configuration file, starting from a sample configuration file.
    ```shell script
    cp /opt/pravega-sensor-collector/conf/env-sample-network-standalone.sh /opt/pravega-sensor-collector/conf/env-local.sh
    ```

5.  Install and start as a Systemd service.
    ```shell script
    sudo /opt/pravega-sensor-collector/bin/install-service.sh
    ```

6.  To view the status of the service:
    ```shell script
    sudo systemctl status pravega-sensor-collector.service
    sudo journalctl -u pravega-sensor-collector.service
    ```

## How to Configure

Pravega Sensor Collector is conveniently configured using only environment variables.
This avoids the need to manage site-specific configuration files and allows a simple shell script
to be used to customize the configuration.
All environment variables and properties begin with the prefix PRAVEGA_SENSOR_COLLECTOR.

For convenience when debugging in an IDE, configuration values can also be specified in a properties file.
To use a properties file, you must set the environment variable PRAVEGA_SENSOR_COLLECTOR_PROPERTIES_FILE
to the path of the properties file.

For a list of commonly-used configuration values, see the
[sample environment files](pravega-sensor-collector/src/main/dist/conf).

## Data File Ingestion

### Overview

Pravega Sensor Collector can be configured to read data from files and write the data to Pravega.

Periodically, new files that match the file name pattern in LOG_FILE_INGEST_FILE_SPEC will be identified and ingested.
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
This can be disabled by setting LOG_FILE_INGEST_DELETE_COMPLETED_FILES to false.

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
These can be defined in the parameter LOG_FILE_INGEST_EVENT_TEMPLATE which accepts a JSON object.

## Development

```shell
cd
git clone https://github.com/pravega/pravega
cd pravega
git checkout r0.9
./gradlew startStandalone \
  -Dcontroller.transaction.lease.count.max=2592000000 \
  -Dcontroller.transaction.execution.timeBound.days=30
```

## References

- https://www.pravega.io/

## About

Pravega Sensor Collector is 100% open source and community-driven. All components are available
under [Apache 2 License](https://www.apache.org/licenses/LICENSE-2.0.html) on GitHub.
