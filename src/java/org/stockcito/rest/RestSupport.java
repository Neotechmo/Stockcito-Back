package org.stockcito.rest;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import org.stockcito.model.ApiMessage;

abstract class RestSupport {

    protected final Gson gson = new Gson();

    protected Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(new ApiMessage(message))).build();
    }

    protected Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(new ApiMessage(message))).build();
    }

    protected Response serviceUnavailable(String message) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(gson.toJson(new ApiMessage(message))).build();
    }

    protected Response error(Exception e) {
        if (e instanceof IllegalArgumentException) return badRequest(e.getMessage());
        e.printStackTrace();
        return Response.serverError().entity(gson.toJson(new ApiMessage("Error interno del servidor"))).build();
    }

    protected boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
