/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.rawfile;

/**
 * Config passed to Pravega Sensor Collector
 */
public class RawFileConfig {
    public final String stateDatabaseFileName;
    public final String fileSpec;
    public final String routingKey;
    public final String streamName;
    public final String eventTemplateStr;


    public final boolean enableDeleteCompletedFiles;
    public final boolean exactlyOnce;
    public final double transactionTimeoutMinutes;

    public RawFileConfig(String stateDatabaseFileName, String fileSpec, String routingKey, String streamName, String eventTemplateStr, boolean enableDeleteCompletedFiles, boolean exactlyOnce, double transactionTimeoutMinutes) {
        this.stateDatabaseFileName = stateDatabaseFileName;
        this.fileSpec = fileSpec;
        this.routingKey = routingKey;
        this.streamName = streamName;
        this.eventTemplateStr = eventTemplateStr;
        this.enableDeleteCompletedFiles = enableDeleteCompletedFiles;
        this.exactlyOnce = exactlyOnce;
        this.transactionTimeoutMinutes = transactionTimeoutMinutes;
    }

    @Override
    public String toString() {
        return "RawFileConfig{" +
                "stateDatabaseFileName='" + stateDatabaseFileName + '\'' +
                ", fileSpec='" + fileSpec + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", streamName='" + streamName + '\'' +
                ", eventTemplateStr='" + eventTemplateStr + '\'' +
                ", enableDeleteCompletedFiles=" + enableDeleteCompletedFiles +
                ", exactlyOnce=" + exactlyOnce +
                ", transactionTimeoutMinutes=" + transactionTimeoutMinutes +
                '}';
    }

}
