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
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public String authenticateMethod(@Context Request request, String data) throws Exception{
        final AuthTokenDto authTokenDto = new AuthTokenDto("1", "admin", "abcdJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImFkbWluIiwibmFtZWlkIjoiMSIsImp0aSI6IjhFNjQ0MTdENDhCRjBFODU3OEZDQkNGNzBBNjRFMUJEIiwiZGV2aWNlR3JvdXBzIjoiW10iLCJyb2xlIjoiQWRtaW4iLCJuYmYiOjE2MjE1MTc5MzEsImV4cCI6MTYyMjEyMjczMSwiaWF0IjoxNjIxNTE3OTMxfQ.xlgn8w7eAIMXfe7hwIc4m5K0P4YD1wWC5JTIeKsyz", Arrays.asList("Admin"), 604799);
        String jsonAuthToken = mapper.writeValueAsString(authTokenDto);
        return jsonAuthToken;
    }

    @GET
    @Path("ClientApi/V1/DeviceReadings")
    @Produces({"application/json"})
    public String deviceReadingsMethod(@Context Request request, @QueryParam("startDate") String startDate) throws Exception{
        try{        
            List<DeviceReadingsDto> allReadings = new ArrayList<>();

            // //start from Jun 1 00:00 UTC
            // Date timeStamp = dateFormat.parse("2021-06-08T00:00:00.000+00:00");
            Date timeStamp;
            if(startDate==null)
                //Past 24 hours
                timeStamp = Date.from(Instant.now().minus(Duration.ofHours(24)));
            else
                //From startDate to now
                timeStamp = dateFormat.parse(startDate);
            Date current = new Date(System.currentTimeMillis());
            log.info("Working on readings starting from {}",dateFormat.format(timeStamp));
            log.info("Current date= {}",dateFormat.format(current));
            while(timeStamp.getTime() < current.getTime())  //till today, compares in milliseconds
            {
                timeStamp = Date.from(timeStamp.toInstant().plus(Duration.ofMinutes(1))); //adds 1 minute
                ReadingValueDto readingValue1 = new ReadingValueDto(1, 0, 6, 5, "label", "iconUrl", "units", 5.637376656633329, "status");
                DeviceReadingsDto deviceReading = new DeviceReadingsDto(timeStamp, Arrays.asList(readingValue1 , readingValue1), 
                                                                    "deviceId",timeStamp);
                allReadings.add(deviceReading);
            }
            String jsonReadings = mapper.writeValueAsString(allReadings);
            return jsonReadings;
        }
        catch (Exception e) {
            log.info("Error", e);
            throw e;
        }

    }
    
}
