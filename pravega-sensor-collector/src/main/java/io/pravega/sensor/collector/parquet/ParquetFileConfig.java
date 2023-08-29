/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.parquet;

public class ParquetFileConfig {
    public final String stateDatabaseFileName;
    public final String fileSpec;
    public final String fileExtension;
    public final String routingKey;
    public final String streamName;
    public final String eventTemplateStr;

    /**
     * Also known as samplesPerEvent.
     */
    public final int maxRecordsPerEvent;

    public final boolean enableDeleteCompletedFiles;
    public final boolean exactlyOnce;
    public final double transactionTimeoutMinutes;

    public ParquetFileConfig(String stateDatabaseFileName, String fileSpec, String fileExtension, String routingKey, String streamName, String eventTemplateStr, int maxRecordsPerEvent, boolean enableDeleteCompletedFiles, boolean exactlyOnce, double transactionTimeoutMinutes) {
        this.stateDatabaseFileName = stateDatabaseFileName;
        this.fileSpec = fileSpec;
        this.fileExtension = fileExtension;
        this.routingKey = routingKey;
        this.streamName = streamName;
        this.eventTemplateStr = eventTemplateStr;
        this.maxRecordsPerEvent = maxRecordsPerEvent;
        this.enableDeleteCompletedFiles = enableDeleteCompletedFiles;
        this.exactlyOnce = exactlyOnce;
        this.transactionTimeoutMinutes = transactionTimeoutMinutes;
    }

    @Override
    public String toString() {
        return "ParquetFileConfig{" +
                "stateDatabaseFileName='" + stateDatabaseFileName + '\'' +
                ", fileSpec='" + fileSpec + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", streamName='" + streamName + '\'' +
                ", eventTemplateStr='" + eventTemplateStr + '\'' +
                ", maxRecordsPerEvent=" + maxRecordsPerEvent +
                ", enableDeleteCompletedFiles=" + enableDeleteCompletedFiles +
                ", exactlyOnce=" + exactlyOnce +
                ", transactionTimeoutMinutes=" + transactionTimeoutMinutes +
                '}';
    }


}
