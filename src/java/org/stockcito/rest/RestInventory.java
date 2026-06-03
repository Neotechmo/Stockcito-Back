package org.stockcito.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerInventoryMovement;

@Path("v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class RestInventory extends RestSupport {
    @GET public Response getInventory() {
        try { return Response.ok(gson.toJson(new ControllerInventoryMovement().getInventory())).build(); } catch (Exception e) { return error(e); }
    }
}
