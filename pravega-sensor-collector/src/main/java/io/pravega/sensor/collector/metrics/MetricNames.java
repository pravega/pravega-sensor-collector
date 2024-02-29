/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.metrics;

public class MetricNames {
    public static final String PREFIX = "psc" + ".";
    public static final String PSC_FILES_PROCESSED_GAUGE = PREFIX + "files.processed";
    public static final String PSC_FILES_DELETED_GAUGE = PREFIX + "files.deleted";
    public static final String PSC_BYTES_PROCESSED_GAUGE = PREFIX + "bytes.processed";
    public static final String PSC_EXCEPTIONS = PREFIX + "exceptions";
}