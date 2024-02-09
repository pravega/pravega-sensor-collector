package io.pravega.sensor.collector.metrics.writers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.pravega.sensor.collector.metrics.MetricConfig;
import io.pravega.sensor.collector.metrics.MetricNames;
import io.pravega.sensor.collector.metrics.MetricsStore;
import io.pravega.sensor.collector.metrics.PravegaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 *  Metric publisher that writes metrics retrieved from
 *  a metrics store to Pravega Stream.
 */
@AutoService(MetricWriter.class)
public class MetricStreamWriter extends AbstractService implements MetricWriter {
    private final Logger log = LoggerFactory.getLogger(MetricStreamWriter.class);
    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
            MetricStreamWriter.class.getSimpleName() + "-%d").build();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, namedThreadFactory);
    private MetricConfig config;
    private PravegaClient client;

    public MetricStreamWriter(MetricConfig config) {
        this.config = config;
    }

    /**
     * Used by ServiceLoader.
     */
    public MetricStreamWriter() {
    }

    @VisibleForTesting
    protected PravegaClient initalizePravegaClient() {
        return new PravegaClient(config.getMetricsScope(), config.getMetricStream(), config.getControllerURI());
    }

    @Override
    public void writeMetric() {
        try {
            String jsonMetrics = MetricsStore.getMetricsAsJson();
            this.client.writeEvent("", jsonMetrics);
            log.info("Published metric blob to Pravega Stream {}", config.getMetricStream());
        } catch (JsonProcessingException jpe) {
            // Just log . do not stop the scheduler
            log.error("Error fetching metrics as json string {}", jpe);
        } catch (Exception ioe) {
            // Just log . do not stop the scheduler
            log.error("Error writing metrics file at {}", ioe);
        }
    }

    @Override
    public void doStart() {
        log.info("Starting MetricStreamWriter {}");
        this.client = initalizePravegaClient();
        executor.scheduleAtFixedRate(this::writeMetric, 0, config.getStreamWriterIntervalSeconds(), TimeUnit.SECONDS);
        notifyStarted();
    }

    @Override
    public void doStop() {
        log.info("Stopping Watchdog monitor.");
        executor.shutdown();
        this.client.close();
        notifyStopped();
    }
}
