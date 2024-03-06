package io.pravega.sensor.collector.watchdog;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class WatchdogServiceTests {

    @Test
    public void testWatchDogServiceStart() {
        WatchDogConfig config = new WatchDogConfig(ImmutableMap.<String, String>builder().put("PSC_SERVICE_NAME", "test").build());
        WatchDogService service = new WatchDogService(config);
        service.startAsync();
        Assert.assertTrue(service.isRunning());
        service.stopAsync();
        Assert.assertFalse(service.isRunning());
    }

    @Test
    public void testWatchdogMonitorStart() {
        WatchDogConfig config = new WatchDogConfig(ImmutableMap.<String, String>builder().put("PSC_SERVICE_NAME", "test").build());
        PSCWatchdogMonitor monitor = new PSCWatchdogMonitor(config);
        monitor.startAsync();
        Assert.assertTrue(monitor.isRunning());
        monitor.stopAsync();
        Assert.assertFalse(monitor.isRunning());
    }

    @Test
    public void testWatchdogRestartPSC() {
        WatchDogConfig config = new WatchDogConfig(ImmutableMap.<String, String>builder().put("PSC_SERVICE_NAME", "test").build());
        PSCWatchdogMonitor monitor = new PSCWatchdogMonitor(config);
        monitor.onUpdateMissed();
        monitor.onUpdateMissed();
        monitor.onUpdateMissed();
        monitor.onUpdateMissed();
        // threshold crossed, updates missed resets to zero and restart PSC.
        Assert.assertFalse(monitor.areUpdatesMissedGreaterThanThreshold());
    }

    @Test
    public void testWatchdogResetCounterOnUpdates() {
        WatchDogConfig config = new WatchDogConfig(ImmutableMap.<String, String>builder().put("PSC_SERVICE_NAME", "test").build());
        PSCWatchdogMonitor monitor = new PSCWatchdogMonitor(config);
        monitor.onUpdateMissed();
        monitor.onUpdateMissed();
        monitor.onUpdateMissed();
        monitor.onUpdate(Instant.now());
        monitor.onUpdateMissed();
        Assert.assertFalse(monitor.areUpdatesMissedGreaterThanThreshold());
    }

    @Test (timeout = 15000)
    public void testWatchdogRestartPSCWhenNoFileExists() throws Exception {
        ImmutableMap<String, String> props = ImmutableMap.<String, String>builder()
                .put("WATCHDOG_WATCH_INTERVAL_SECONDS", "3")
                .put("WATCHDOG_FILE_MONITOR_UPDATE_MISSED_THRESHOLD", "2")
                .put("PSC_SERVICE_NAME", "test")
                .build();
        WatchDogConfig config = new WatchDogConfig(props);
        PSCWatchdogMonitor monitor = new PSCWatchdogMonitor(config);
        monitor.startAsync();
        System.out.println(System.getProperty("user.name"));
        // Threshold crossed at 6th second...stop executor at 7th second.
        CompletableFuture<Void> cf = new CompletableFuture<>();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                monitor.doStop();
                cf.complete(null);
            }
        }, 7000);
        cf.get(); // wait for task to run
        Assert.assertFalse(monitor.areUpdatesMissedGreaterThanThreshold());
    }
}




