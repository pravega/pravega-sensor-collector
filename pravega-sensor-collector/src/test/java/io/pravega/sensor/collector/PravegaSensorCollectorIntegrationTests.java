package io.pravega.sensor.collector;

import com.google.common.util.concurrent.Service;
import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.*;
import io.pravega.client.stream.impl.UTF8StringSerializer;
import io.pravega.sensor.collector.util.FileNameWithOffset;
import io.pravega.sensor.collector.util.SQliteDBUtility;
import io.pravega.sensor.collector.util.TransactionStateDB;
import io.pravega.sensor.collector.util.TransactionStateSQLiteImpl;
import io.pravega.test.integration.utils.SetupUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PravegaSensorCollectorIntegrationTests {
    private static final Logger log = LoggerFactory.getLogger(PravegaSensorCollectorIntegrationTests.class);
    private final SetupUtils setupUtils = new SetupUtils();
    static String fileName = "./src/test/resources/RawFileIngest-integration-test.properties";
    Map<String, String> properties = null;
    @BeforeEach
    public void setup() {
        log.info("Setup");
        properties = Parameters.getProperties(fileName);
        try {
            setupUtils.startAllServices();

            Files.deleteIfExists(Paths.get(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        properties.put("PRAVEGA_SENSOR_COLLECTOR_RAW1_PRAVEGA_CONTROLLER_URI", setupUtils.getControllerUri().toString());
        log.debug("Properties: {}", properties);
    }

    @AfterEach
    public void tearDown() {
        log.info("TearDown");
        try {
            setupUtils.stopAllServices();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Files.deleteIfExists(Paths.get(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        properties = null;
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void testPSCDataIntegration() {
        try {
            copyHelloWorldFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        URI controllerURI = setupUtils.getControllerUri();
        String scope = "test-psc-data-integration";
        String streamName ="test-psc-data-integration-stream";

        properties.put("PRAVEGA_SENSOR_COLLECTOR_RAW1_SCOPE",scope);
        properties.put("PRAVEGA_SENSOR_COLLECTOR_RAW1_STREAM",streamName);

        final DeviceDriverManager deviceDriverManager = new DeviceDriverManager(properties);
        Service startService = deviceDriverManager.startAsync();
        try {
            startService.awaitRunning(Duration.ofSeconds(30));
            Thread.sleep(25000);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        final Connection connection = SQliteDBUtility.createDatabase(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE"));
        final TransactionStateDB state = new TransactionStateSQLiteImpl(connection, null);

        try {
            List<FileNameWithOffset> completedFiles = state.getCompletedFileRecords();
            Assertions.assertEquals(1, completedFiles.size());

            validateStreamData(controllerURI, scope, streamName, new String(Files.readAllBytes(Paths.get("../parquet-file-sample-data/test_file/hello-world.parquet"))));

            Thread.sleep(50000);

            Service stopService = deviceDriverManager.stopAsync();
            stopService.awaitTerminated(Duration.ofSeconds(30));

            // Till this time all the completed files should get deleted
            completedFiles = state.getCompletedFileRecords();
            Assertions.assertEquals(0, completedFiles.size());
            connection.close();
        } catch (SQLException | InterruptedException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateStreamData(URI controllerURI, String scope, String streamName, String content) {
        StreamManager streamManager = StreamManager.create(controllerURI);

        final String readerGroup = UUID.randomUUID().toString().replace("-", "");
        final ReaderGroupConfig readerGroupConfig = ReaderGroupConfig.builder()
                .stream(Stream.of(scope, streamName))
                .build();
        try (ReaderGroupManager readerGroupManager = ReaderGroupManager.withScope(scope, controllerURI)) {
            readerGroupManager.createReaderGroup(readerGroup, readerGroupConfig);
        }

        try (EventStreamClientFactory clientFactory = EventStreamClientFactory.withScope(scope,
                ClientConfig.builder().controllerURI(controllerURI).build());
             EventStreamReader<String> reader = clientFactory.createReader("reader",
                     readerGroup,
                     new UTF8StringSerializer(),
                     ReaderConfig.builder().build())) {
            System.out.format("Reading all the events from %s/%s%n", scope, streamName);
            EventRead<String> eventRead = null;
            try {
                while ((eventRead = reader.readNextEvent(2000)).getEvent() != null) {
                    String event = eventRead.getEvent();
                    System.out.format("Read event: %s", event);
                    Assertions.assertNotNull(event);
                    Assertions.assertFalse(event.isEmpty());
                    Assertions.assertEquals(content, event);
                }
            } catch (ReinitializationRequiredException e) {
                //There are certain circumstances where the reader needs to be reinitialized
                e.printStackTrace();
            }
            System.out.format("No more events from %s/%s%n", scope, streamName);
        }
    }

    public void copyHelloWorldFile() throws IOException {
        Path sourcePath = Paths.get("../parquet-file-sample-data/test_file/hello-world.parquet");
        Path targetPath = Paths.get("../parquet-file-sample-data/integration-test/hello-world.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void testRawFile() {
        try {
            copyFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final DeviceDriverManager deviceDriverManager = new DeviceDriverManager(properties);
        Service startService = deviceDriverManager.startAsync();
        try {
            startService.awaitRunning(Duration.ofSeconds(30));
            Thread.sleep(25000);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        final Connection connection = SQliteDBUtility.createDatabase(properties.get("PRAVEGA_SENSOR_COLLECTOR_RAW1_DATABASE_FILE"));
        final TransactionStateDB state = new TransactionStateSQLiteImpl(connection, null);

        try {
            List<FileNameWithOffset> completedFiles = state.getCompletedFileRecords();
            Assertions.assertEquals(3, completedFiles.size());

            Thread.sleep(50000);

            Service stopService = deviceDriverManager.stopAsync();
            stopService.awaitTerminated(Duration.ofSeconds(30));

            // Till this time all the completed files should get deleted
            completedFiles = state.getCompletedFileRecords();
            Assertions.assertEquals(0, completedFiles.size());
            connection.close();
        } catch (SQLException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyFile() throws IOException {
        Path sourcePath = Paths.get("../parquet-file-sample-data/test_file/sub1.parquet");
        Path targetPath = Paths.get("../parquet-file-sample-data/integration-test/sub1.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        sourcePath = Paths.get("../parquet-file-sample-data/test_file/sub2.parquet");
        targetPath = Paths.get("../parquet-file-sample-data/integration-test/sub2.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        sourcePath = Paths.get("../parquet-file-sample-data/test_file/sub3.parquet");
        targetPath = Paths.get("../parquet-file-sample-data/integration-test/sub3.parquet");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("copyFile: Files copied successfully");
    }
}
