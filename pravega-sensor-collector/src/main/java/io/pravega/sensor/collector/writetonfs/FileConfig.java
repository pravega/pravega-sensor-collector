/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.writetonfs;

/*
 * Configuration file.
 */
public class FileConfig {
    public final String stateDatabaseFileName;
    public final String fileSpec;
    public final String fileExtension;
    public final String nfsMountPath;
    public final String routingKey;
    public final String eventTemplateStr;
    public final String fileType;
    /**
     * Also known as samplesPerEvent.
     */
    public final int maxRecordsPerEvent;

    public final boolean enableDeleteCompletedFiles;
    public final boolean exactlyOnce;
    public final double transactionTimeoutMinutes;

    public final long minTimeInMillisToUpdateFile;

    public FileConfig(String stateDatabaseFileName, String fileSpec, String fileExtension, String nfsMountPath,String routingKey, String eventTemplateStr, int maxRecordsPerEvent, boolean enableDeleteCompletedFiles, boolean exactlyOnce, double transactionTimeoutMinutes, long minTimeInMillisToUpdateFile, String fileType) {
        this.stateDatabaseFileName = stateDatabaseFileName;
        this.fileSpec = fileSpec;
        this.fileExtension = fileExtension;
        this.nfsMountPath = nfsMountPath;
        this.routingKey = routingKey;
        this.eventTemplateStr = eventTemplateStr;
        this.maxRecordsPerEvent = maxRecordsPerEvent;
        this.enableDeleteCompletedFiles = enableDeleteCompletedFiles;
        this.exactlyOnce = exactlyOnce;
        this.transactionTimeoutMinutes = transactionTimeoutMinutes;
        this.minTimeInMillisToUpdateFile = minTimeInMillisToUpdateFile;
        this.fileType = fileType;
    }

    @Override
    public String toString() {
        return "FileConfig{"
                + "stateDatabaseFileName='" + stateDatabaseFileName + '\''
                + ", fileSpec='" + fileSpec + '\''
                + ", fileExtension='" + fileExtension + '\''
                + ", nfsMountPath='" + nfsMountPath + '\''
                + ", fileType='" + fileType + '\''
                + ", routingKey='" + routingKey + '\''
                + ", eventTemplateStr='" + eventTemplateStr + '\''
                + ", maxRecordsPerEvent=" + maxRecordsPerEvent
                + ", enableDeleteCompletedFiles=" + enableDeleteCompletedFiles
                + ", exactlyOnce=" + exactlyOnce
                + ", transactionTimeoutMinutes=" + transactionTimeoutMinutes
                + '}';
    }
}
