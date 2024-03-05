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
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


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

    WatchDogService(WatchDogConfig config) {
        this.config = config;
        this.monitor = new PSCWatchdogMonitor(config);
    }

    @Override
    protected void doStart() {
        log.info("Starting WatchDog Service");
        monitor.startAsync();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        log.info("Stopping Watchdog Service");
        monitor.stopAsync();
        notifyStopped();
    }

    public void checkPscServiceStatus() throws IOException {
        Process psc = null;
        log.info("Checking PSC service status {} ", this.config.getServiceName());
        if (SystemUtils.IS_OS_LINUX) {
            psc = Runtime.getRuntime().exec(new String[]{"sh", "-c", "systemctl status " + this.config.getServiceName()});
        } else if ( SystemUtils.IS_OS_WINDOWS) {
            psc = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", this.config.getServiceName() + ".exe", "status"});
        }
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(psc.getInputStream()));

        String s = null;
        Boolean isAlive = true;
        while ((s = stdInput.readLine()) != null) {
            if(s.equalsIgnoreCase("NonExistent")){
                isAlive = false;
            }
        }
        log.debug("Process psc {}, and isAlive value is {} ", psc, isAlive);
        if(!isAlive) {
            log.error("PSC service is not running");
            throw new RuntimeException("PSC service is not running. Please start psc before starting watchdog monitor.");
        }
    }
}
