package io.pravega.sensor.collector.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.Parameters;
import io.pravega.sensor.collector.metrics.writers.MetricFileWriter;
import io.pravega.sensor.collector.metrics.writers.MetricStreamWriter;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class MetricTests {

    @Test
    public void metricFileWriterServiceTest() {
        ImmutableMap<String, String> props = ImmutableMap.<String, String>builder()
                .put("METRIC_FILE_WRITER_INTERVAL_SECONDS", "3")
                .build();
        DeviceDriverManager manager = new DeviceDriverManager(props);
        DeviceDriverConfig deviceDriverConfig = new DeviceDriverConfig("test", "testClass", Parameters.getProperties(), manager);
        MetricConfig metricConfig = MetricConfig.getMetricConfigFrom(deviceDriverConfig);
        MetricFileWriter fileWriter = new MetricFileWriter(metricConfig);
        fileWriter.startAsync();
        fileWriter.awaitRunning();
        Assert.assertTrue(Files.exists(Paths.get(metricConfig.getMetricFilePath())));
    }

    @Test
    public void streamWriterServiceTest() {
        ImmutableMap<String, String> props = ImmutableMap.<String, String>builder()
                .put("METRIC_STREAM_WRITER_INTERVAL_SECONDS", "3")
                .build();
        DeviceDriverManager manager = new DeviceDriverManager(props);
        DeviceDriverConfig deviceDriverConfig = new DeviceDriverConfig("test", "testClass", Parameters.getProperties(), manager);
        MetricConfig metricConfig = MetricConfig.getMetricConfigFrom(deviceDriverConfig);
        TestMetricStreamWriter tms = new TestMetricStreamWriter(metricConfig);
        CompletableFuture<Void> result = new CompletableFuture<>();
        tms.setNotifier(result);
        tms.startAsync();
        tms.awaitRunning();
        Assert.assertTrue(tms.isRunning());
        result.join();
        Assert.assertTrue(result.isDone());
    }

    @Test
    public void testMetricTypes() {
        Gauge guage = new Gauge();
        guage.update(10L);
        Assert.assertEquals(guage.getGauge(), new Long(10L));
        Counter counter = new Counter();
        counter.update(10L);
        Assert.assertEquals(counter.getCounter(), new Long(10L));
        ExceptionMeter exceptionMeter = new ExceptionMeter();
        exceptionMeter.update("test");
        Assert.assertEquals(exceptionMeter.getExceptionClass(), "test");
    }

    @Test
    public void metricStoreTests() {
        try {
            String json = MetricsStore.getMetricsAsJson();
            Assert.assertNotNull(json);
            Assert.assertEquals(((Gauge)MetricsStore.getMetric(MetricNames.PSC_BYTES_PROCESSED_GAUGE)).getGauge(), new Long(0L));
            Assert.assertEquals(((Gauge)MetricsStore.getMetric(MetricNames.PSC_FILES_DELETED_GAUGE)).getGauge(), new Long(0L));
        } catch( JsonProcessingException jpe) {
            Assert.fail("Error retrieving json from metricstore");
        }
    }

    class TestMetricStreamWriter extends MetricStreamWriter {
        MetricConfig config;
        CompletableFuture<Void> notifier;
        TestMetricStreamWriter(MetricConfig config) {
            super(config);
            this.config = config;
        }

        void setNotifier(CompletableFuture<Void> cf) {
            this.notifier = cf;
        }
        @Override
        public PravegaClient initalizePravegaClient() {
            TestPravegaClient client = new TestPravegaClient(config.getMetricsScope(), config.getMetricStream());
            client.setNotifier(notifier);
            return client;
        }
    }

    class TestPravegaClient extends PravegaClient {
        CompletableFuture<Void> notifier;
        TestPravegaClient(String scope, String stream) {
            super(scope, stream);
        }
        void setNotifier(CompletableFuture<Void> cf) {
            this.notifier = cf;
        }

        @Override
        public void writeEvent(String rk, String message) {
            this.notifier.complete(null);
        }
    }
}
