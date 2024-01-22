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

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/**
 * Watchdog service for Pravega Sensor Collector(PSC).
 * Watchdog service looks for liveness of Pravega Sensor Collector and attempts
 * to keep it up and running if not live.
 * The definition(can be extended with time) of liveness is watchdog looking
 * for regular/live updates on a configured file made by PSC.
 * If not live Watchdog restarts PSC service as a corrective measure.
 * There could be other corrective measures added in future.
 */
public class WatchDogService extends AbstractService {

    private static final Logger log = LoggerFactory.getLogger(WatchDogService.class);
    private final WatchDogConfig config;
    private final Monitor monitor; // one monitor for now

    WatchDogService(Map<String, String> properties) {
        config = new WatchDogConfig(properties);
        monitor = new PSCWatchdogMonitor();
    }

    @Override
    protected void doStart() {
        log.info("Starting WatchDog Service");
        monitor.startAsync();
        monitor.awaitTerminated();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        log.info("Stopping Watchdog Service");
        notifyStopped();
    }

    /**
     * Monitoring service to be started by watchdog.
     * Implementations need to define actions for updates received
     * or missed while monitoring.
     */
    private interface Monitor extends Service {

        /**
         * Define action on update to resource
         * being monitored.
         * @param modifiedTime
         */
        void onUpdate(Instant modifiedTime);

        /**
         * Define action on updates missed on
         * resource being monitored.
         * @throws Exception throw Exception if any when missing updates.
         */
        void onUpdateMissed() throws Exception;
    }

    private class PSCWatchdogMonitor extends AbstractService implements Monitor {

        private final Logger log = LoggerFactory.getLogger(PSCWatchdogMonitor.class);
        private Instant lastModified = Instant.EPOCH;
        private int updateMissedCount = 0;
        private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                PSCWatchdogMonitor.class.getSimpleName() + "-%d").build();
        private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, namedThreadFactory);

        @Override
        public void onUpdate(Instant modifiedTime) {
            updateMissedCount = 0;
            this.lastModified = modifiedTime;
            log.info("PSC Watchdog Monitor file has been updated at {}", config.getWatchdogFileMonitorPath());
        }

        @Override
        public void onUpdateMissed() {
            log.info("PSC Watchdog Monitor File has not been updated at {}", config.getWatchdogFileMonitorPath());
            updateMissedCount++;
            log.debug("Update missed count {} and threshold is {}", updateMissedCount, config.getWatchDogFileUpdateMissedThreshold());
            // if no. of updates missed is greater that set threshold then take action.
            if (updateMissedCount > config.getWatchDogFileUpdateMissedThreshold()) {
                log.debug("Triggering restart of PSC.");
                try {
                    restartPSC();
                } catch (IOException ioe) {
                    log.error("Error restarting PSC. Exception: {}", ioe);
                }
                updateMissedCount = 0;
            }
        }

        @Override
        protected void doStart() {
            log.info("Starting WatchdogService");
            executor.scheduleAtFixedRate(this::process, 0, config.getWatchDogWatchInterval(), TimeUnit.SECONDS);
            notifyStarted();
        }

        /**
         * Restart PSC. Handle Platforms.
         * TODO: use a Manager that determines platform and have separate platform specific implementations.
         * @throws IOException in case of any exception.
         */
        private void restartPSC() throws IOException {
            String serviceName = config.getServiceName();
            if (SystemUtils.IS_OS_LINUX) {
                Runtime.getRuntime().exec(new String[]{"sh", "-c", "systemctl restart " + serviceName});
            } else if ( SystemUtils.IS_OS_WINDOWS) {
                Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", serviceName + ".exe", "restart"});
            } else {
                throw new IOException("Unsupported operating-system");
            }
        }

        /**
         * TODO: Make this part of the Monitor interface.
         */
        private void process() {
            log.debug("process called");
            Path path = Paths.get(config.getWatchdogFileMonitorPath());
            try {
                Instant modifiedTime = Files.getLastModifiedTime(path).toInstant();
                handleUpdates(modifiedTime);
            } catch (NoSuchFileException fne) {
                log.error("No monitor file exists at {}.", config.getWatchdogFileMonitorPath());
                handleUpdates(this.lastModified);
            } catch (IOException e) {
                log.error("Could not get the last modified time for monitor file at {}. exception {}", config.getWatchdogFileMonitorPath(), e);
            }
        }

        private void handleUpdates(Instant modifiedTime) {
            log.debug("handleUpdates called");
            if (modifiedTime.isAfter(this.lastModified)) {
                onUpdate(modifiedTime);
            } else {
                onUpdateMissed();
            }
        }

        @Override
        protected void doStop() {
            log.info("Stopping WatchdogSrevice");
            notifyStopped();
        }
    }

}