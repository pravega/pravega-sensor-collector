package io.pravega.sensor.collector.file.parquet;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.DeviceDriverManager;
import io.pravega.sensor.collector.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class ParquetFileIngestServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParquetFileIngestServiceTest.class);

    private static final String PREFIX = Parameters.getEnvPrefix();
    private static final String SEPARATOR = "_";
    private static final String CLASS_KEY = "CLASS";
    protected DeviceDriverConfig config;
    static String filename = "./src/test/resources/ParquetFileIngest.properties";
    DeviceDriverConfig deviceDriverConfig;
    Map<String, String> properties;

    private DeviceDriverManager driverManager;

    @BeforeEach
    void setUp(){
        properties = Parameters.getProperties(filename);
        driverManager = new DeviceDriverManager(properties);
        deviceDriverConfig = new DeviceDriverConfig("PARQ2", "ParquetFileIngestService",
                configFromProperties(PREFIX, SEPARATOR, properties), driverManager);
    }
    @Test
    public void testParquetFileIngestService(){
        ParquetFileIngestService parquetFileIngestService = new TestParquetFileIngestService(deviceDriverConfig);
        try {
            parquetFileIngestService.startAsync();
            parquetFileIngestService.awaitRunning(Duration.ofSeconds(10));
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(parquetFileIngestService.isRunning());
        Assertions.assertEquals(parquetFileIngestService.getProperty("PERSISTENT_QUEUE_FILE"),
                properties.get(PREFIX + "PARQ2" + SEPARATOR + "PERSISTENT_QUEUE_FILE"));
        try {
            parquetFileIngestService.stopAsync();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        Assertions.assertFalse(parquetFileIngestService.isRunning());
    }

    Map<String, String> configFromProperties(String prefix, String sep, Map<String, String> properties) {
        Map<String, String> instanceProperties = new HashMap<>();
        // Find instance names.
        final List<String> instanceNames = properties.keySet().stream().flatMap((key) -> {
            if (key.startsWith(prefix) && key.endsWith(sep + CLASS_KEY)) {
                return Stream.of(key.substring(prefix.length(), key.length() - CLASS_KEY.length() - sep.length()));
            }
            return Stream.empty();
        }).collect(Collectors.toList());
        LOGGER.info("configFromProperties: instanceNames={}", instanceNames);
        // Copy properties with prefix to keys without a prefix.
        String instanceName = instanceNames.get(0);

        for(Map.Entry<String, String> e : properties.entrySet()){
            final String key = e.getKey();
            final String instancePrefix = prefix + instanceName + sep;
            if (key.startsWith(instancePrefix)) {
                LOGGER.info("key"+ key.substring(instancePrefix.length()) + " value " + e.getValue());
                instanceProperties.put(key.substring(instancePrefix.length()), e.getValue());
            } else if (key.startsWith(prefix)) {
                instanceProperties.put(key.substring(prefix.length()), e.getValue());
            }
            //LOGGER.info("configFromProperties: instanceProperties={}", instanceProperties);
        }
        LOGGER.info("configFromProperties: instanceProperties={}", instanceProperties);
        return instanceProperties;
    }

    class TestParquetFileIngestService extends ParquetFileIngestService {

        public TestParquetFileIngestService(DeviceDriverConfig config) {
            super(config);
        }

        @Override
        protected void createStream(String scopeName, String streamName) {
            System.out.println("Create Stream do nothing");
        }
    }
}
