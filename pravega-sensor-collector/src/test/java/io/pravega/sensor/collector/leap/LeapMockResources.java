package io.pravega.sensor.collector.leap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
// import javax.servlet.http.HttpServletResponse;

import org.glassfish.grizzly.http.server.Request;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Path("/")
public class LeapMockResources {

    final static Logger log = LoggerFactory.getLogger(LeapMockResources.class);
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss+hh:mm");
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("CST"));
    }
    private final ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("api/Auth/authenticate")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public String authenticateMethod(@Context Request request, String data) throws Exception{
        
        // log.info("Data: {}",data);
        final AuthTokenDto authTokenDto = new AuthTokenDto("1", "admin", "abcdJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImFkbWluIiwibmFtZWlkIjoiMSIsImp0aSI6IjhFNjQ0MTdENDhCRjBFODU3OEZDQkNGNzBBNjRFMUJEIiwiZGV2aWNlR3JvdXBzIjoiW10iLCJyb2xlIjoiQWRtaW4iLCJuYmYiOjE2MjE1MTc5MzEsImV4cCI6MTYyMjEyMjczMSwiaWF0IjoxNjIxNTE3OTMxfQ.xlgn8w7eAIMXfe7hwIc4m5K0P4YD1wWC5JTIeKsyz", Arrays.asList("Admin"), 604799);
        // log.info("getAuthToken: authTokenDto={}", authTokenDto);   
        String jsonAuthToken = mapper.writeValueAsString(authTokenDto);
        return jsonAuthToken;

        // data to json object using Jackson, put into authTOken Dto and return
        // Configure Device readings method the same way
    }

    @GET
    @Path("ClientApi/V1/DeviceReadings")
    @Produces({"application/json"})
    public String deviceReadingsMethod(@Context Request request) throws Exception{
        try{
        
            List<DeviceReadingsDto> allReadings = new ArrayList<>();
            Date timeStamp = dateFormat.parse("2021-06-01T00:00:00.000+00:00");
            // log.info("Initial = {}", timeStamp.toString());
            // while(timeStamp.getTime() != System.currentTimeMillis())  //till today, compares in milliseconds
            for(int i=0;i<4;i++)
            {
                ReadingValueDto readingValue1 = new ReadingValueDto(1, 0, 6, 5, "label", "iconUrl", "units", 5.637376656633329, "status");
                DeviceReadingsDto deviceReading = new DeviceReadingsDto(timeStamp, Arrays.asList(readingValue1 , readingValue1), 
                    "deviceId",timeStamp);
                allReadings.add(deviceReading);
                timeStamp = Date.from(timeStamp.toInstant().plus(Duration.ofMinutes(1))); //adds 1 minute
                // log.info(timeStamp.toString());       
            }

            log.info("DeviceReadings: {}",allReadings);
            String jsonReadings = mapper.writeValueAsString(allReadings);

            return jsonReadings;

        }
        catch (Exception e) {
            log.info("Error", e);
            throw e;
        }

    }
    
}
