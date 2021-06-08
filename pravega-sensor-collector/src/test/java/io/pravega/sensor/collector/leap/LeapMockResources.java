package io.pravega.sensor.collector.leap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import io.pravega.sensor.collector.leap.AuthCredentialsDto;

@Path("")
public class LeapMockResources {

    final static Logger log = LoggerFactory.getLogger(LeapMockResources.class);

    @POST
    @Path("/api/Auth/authenticate")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public String authenticateMethod(@Context Request request, String data) throws Exception{
        
        // AuthCredentialsDto authCreds = new AuthCredentialsDto("admin", "mypassword");
        // log.info("authenticateMethod: authCreds={}", authCreds.toString());
        log.info("Data: {}",data);

        
        return "{}";


    }
    
}
