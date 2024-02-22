package io.pravega.sensor.collector.util;

import io.pravega.sensor.collector.DeviceDriverConfig;
import io.pravega.sensor.collector.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to contain convenient utilities for writing test cases.
 */
public final class TestUtils {


    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final String CLASS_KEY = "CLASS";
    protected DeviceDriverConfig config;

    public static Map<String, String> configFromProperties(String prefix, String sep, Map<String, String> properties) {
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
}