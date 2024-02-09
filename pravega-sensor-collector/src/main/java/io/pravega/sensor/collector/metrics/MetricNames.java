package io.pravega.sensor.collector.metrics;

public class MetricNames {
    public static final String PREFIX = "psc" + ".";
    public static final String PSC_FILES_PROCESSED_GAUGE = PREFIX + "files.processed";
    public static final String PSC_FILES_DELETED_GAUGE = PREFIX + "files.deleted";
    public static final String PSC_BYTES_PROCESSED_GAUGE = PREFIX + "bytes.processed";
    public static final String PSC_EXCEPTIONS = PREFIX + "exceptions";
}