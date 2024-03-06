/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.watchdog;

import io.pravega.common.util.Property;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

public class WatchDogConfig {

    public static final Property<String> PSC_WATCHDOG_WATCH_INTERVAL_SECONDS = Property.named("WATCHDOG_WATCH_INTERVAL_SECONDS", "15", "");
    public static final Property<String> PSC_WATCHDOG_FILE_MONITOR_PATH = Property.named("WATCHDOG_FILE_MONITOR_PATH", System.getProperty("java.io.tmpdir") + File.separator + "psc_metric.json", "");

    public static final Property<String> PSC_WATCHDOG_FILE_MONITOR_UPDATE_MISSED_THRESHOLD = Property.named("WATCHDOG_FILE_MONITOR_UPDATE_MISSED_THRESHOLD", "3", "");

    public static final Property<String> PSC_WATCHDOG_RESTART_TRIGGER_PATH = Property.named("WATCHDOG_RESTART_TRIGGER_PATH", ".", "");

    public static final Property<String> PSC_SERVICE_NAME = Property.named("PSC_SERVICE_NAME", "pravega-sensor-collector.service", "");

    private final int watchDogWatchIntervalSeconds;
    private final String watchdogFileMonitorPath;
    private final int watchDogFileUpdateMissedThreshold;
    private final String restartTriggerPath;
    private final String serviceName;

    public WatchDogConfig(Map<String, String> properties) {
        this.serviceName = getProperty(properties, PSC_SERVICE_NAME.toString());
        this.watchDogWatchIntervalSeconds = Integer.parseInt(properties.getOrDefault(PSC_WATCHDOG_WATCH_INTERVAL_SECONDS.toString(), PSC_WATCHDOG_WATCH_INTERVAL_SECONDS.getDefaultValue()));
        this.watchdogFileMonitorPath = properties.getOrDefault(PSC_WATCHDOG_FILE_MONITOR_PATH.toString(), PSC_WATCHDOG_FILE_MONITOR_PATH.getDefaultValue());
        this.restartTriggerPath = properties.getOrDefault(PSC_WATCHDOG_RESTART_TRIGGER_PATH.toString(), PSC_WATCHDOG_RESTART_TRIGGER_PATH.getDefaultValue());
        this.watchDogFileUpdateMissedThreshold = Integer.parseInt(properties.getOrDefault(PSC_WATCHDOG_FILE_MONITOR_UPDATE_MISSED_THRESHOLD.toString(), PSC_WATCHDOG_FILE_MONITOR_UPDATE_MISSED_THRESHOLD.getDefaultValue()));
    }

    /**
     * PSC service name, Watchdog can
     * use to trigger restart.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * The file path Watchdog will monitor to
     * determine PSC liveness.
     */
    public String getWatchdogFileMonitorPath() {
        return watchdogFileMonitorPath;
    }

    /**
     * Interval at which Watchdog has to monitor
     * the configured monitor file path.
     */
    public int getWatchDogWatchInterval() {
        return watchDogWatchIntervalSeconds;
    }

    /**
     * Threshold to determine the number of updates
     * missed on the configured file path, beyond
     * which Watchdog will trigger restart of PSC.
     */
    public int getWatchDogFileUpdateMissedThreshold() {
        return watchDogFileUpdateMissedThreshold;
    }

    /**
     * Not used. Will be removed.
     */
    public String getRestartTriggerPath() {
        return restartTriggerPath;
    }


    /**
     * Retrieves the value of a property from the given map of properties.
     *
     * @param  properties  the map of properties to retrieve the value from
     * @param  key         the key of the property to retrieve
     * @return             the value of the property
     */
    public String getProperty(Map<String, String> properties, String key) {
        final String value = properties.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format("Missing required parameter {0}", key));
        }
        return value;
    }

}
