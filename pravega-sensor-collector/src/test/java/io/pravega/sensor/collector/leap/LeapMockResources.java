package io.pravega.sensor.collector.leap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import org.glassfish.grizzly.http.server.Request;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Path("/")
public class LeapMockResources {

    final static Logger log = LoggerFactory.getLogger(LeapMockResources.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private final ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("api/Auth/authenticate")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public String authenticateMethod(@Context Request request, String data) throws Exception {
        final AuthTokenDto authTokenDto = new AuthTokenDto("1", "admin",
                "abcdJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImFkbWluIiwibmFtZWlkIjoiMSIsImp0aSI6IjhFNjQ0MTdENDhCRjBFODU3OEZDQkNGNzBBNjRFMUJEIiwiZGV2aWNlR3JvdXBzIjoiW10iLCJyb2xlIjoiQWRtaW4iLCJuYmYiOjE2MjE1MTc5MzEsImV4cCI6MTYyMjEyMjczMSwiaWF0IjoxNjIxNTE3OTMxfQ.xlgn8w7eAIMXfe7hwIc4m5K0P4YD1wWC5JTIeKsyz",
                Arrays.asList("Admin"), 604799);
        String jsonAuthToken = mapper.writeValueAsString(authTokenDto);
        return jsonAuthToken;
    }

    /** 
     * Generate readings from time 0:00 UTC of the previous day until the currenttime 
     * */
    public List<DeviceReadingsDto> getAllReadings() throws Exception {
        List<DeviceReadingsDto> allReadings = new ArrayList<>();
        Date current = new Date(System.currentTimeMillis());

        Long offsetDay = Date.from(current.toInstant().minus(Duration.ofDays(1))).getTime(); // reduces 1 day
        Date start = new Date(offsetDay - offsetDay % (24 * 60 * 60 * 1000)); // midnight of previous day
        log.info("Current date= {}", dateFormat.format(current));
        log.info("Previous day 00:00 UTC= {}", dateFormat.format(start));
        log.info("Getting all readings starting from {}", dateFormat.format(start));
        int countAll = 0;
        while (start.getTime() < current.getTime()) // till today, compares in milliseconds
        {
            start = Date.from(start.toInstant().plus(Duration.ofMinutes(1)));
            ReadingValueDto readingValue1 = new ReadingValueDto(1, 0, 6, 5, "label", "iconUrl", "units",
                    5.637376656633329, "status");
            DeviceReadingsDto deviceReading = new DeviceReadingsDto(start, Arrays.asList(readingValue1, readingValue1),
                    "deviceId", start);
            allReadings.add(deviceReading);
            countAll++;
        }
        log.info("Total {} readings", countAll);
        return allReadings;
    }

    /** 
     * Gets readings from getAllReadings and filters out any readings prior to startDate.
     * */
    @GET
    @Path("ClientApi/V1/DeviceReadings")
    @Produces({ "application/json" })
    public String deviceReadingsMethod(@Context Request request, @QueryParam("startDate") String startDate)
            throws Exception {
        try {
            List<DeviceReadingsDto> allReadings = getAllReadings();
            List<DeviceReadingsDto> filteredReadings = new ArrayList<>();
            String jsonReadings;
            if (startDate == null)
                jsonReadings = mapper.writeValueAsString(allReadings);
            else {
                Date timeStamp = dateFormat.parse(startDate);

                log.info("Filtering readings");
                int finalCount = 0;
                for (DeviceReadingsDto deviceReading : allReadings) {
                    if (deviceReading.getReadingTimestamp().getTime() >= timeStamp.getTime()) {
                        filteredReadings.add(deviceReading);
                        finalCount++;
                    }
                }
                log.info("Final {} readings", finalCount);
                jsonReadings = mapper.writeValueAsString(filteredReadings);
            }
            return jsonReadings;
        } catch (Exception e) {
            log.info("Error", e);
            throw e;
        }
    }
}
