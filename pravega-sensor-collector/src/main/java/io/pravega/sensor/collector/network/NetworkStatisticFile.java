/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.network;

import java.io.RandomAccessFile;

public class NetworkStatisticFile {
    // eth0, ens33, etc.
    public final String interfaceName;
    // rx_bytes, tx_bytes, etc.
    public final String statisticName;
    public final RandomAccessFile randomAccessFile;

    public NetworkStatisticFile(String interfaceName, String statisticName, RandomAccessFile randomAccessFile) {
        this.interfaceName = interfaceName;
        this.statisticName = statisticName;
        this.randomAccessFile = randomAccessFile;
    }
}
