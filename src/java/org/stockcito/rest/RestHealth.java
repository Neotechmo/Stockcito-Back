package org.stockcito.rest;

import com.google.gson.Gson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("v1/health")
public class RestHealth {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "stockcito-api");
        return Response.ok(new Gson().toJson(data)).build();
    }
}
