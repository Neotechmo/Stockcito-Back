package org.stockcito.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerInventoryMovement;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.InventoryMovement;

@Path("v1/inventory-movements")
@Produces(MediaType.APPLICATION_JSON)
public class RestInventoryMovement extends RestSupport {

    @GET public Response getAll() {
        try { return Response.ok(gson.toJson(new ControllerInventoryMovement().getAll())).build(); } catch (Exception e) { return error(e); }
    }
    @GET @Path("{id}") public Response getById(@PathParam("id") long id) {
        try {
            InventoryMovement item = new ControllerInventoryMovement().getById(id);
            return item == null ? notFound("Movimiento no encontrado") : Response.ok(gson.toJson(item)).build();
        } catch (Exception e) { return error(e); }
    }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response save(String json) {
        try {
            InventoryMovement item = gson.fromJson(json, InventoryMovement.class);
            Response validation = validate(item); if (validation != null) return validation;
            return Response.status(Response.Status.CREATED).entity(gson.toJson(new ControllerInventoryMovement().save(item))).build();
        } catch (Exception e) { return error(e); }
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public Response update(@PathParam("id") long id, String json) {
        try {
            InventoryMovement item = gson.fromJson(json, InventoryMovement.class);
            Response validation = validate(item); if (validation != null) return validation;
            InventoryMovement updated = new ControllerInventoryMovement().update(id, item);
            return updated == null ? notFound("Movimiento no encontrado") : Response.ok(gson.toJson(updated)).build();
        } catch (Exception e) { return error(e); }
    }
    @DELETE @Path("{id}") public Response delete(@PathParam("id") long id) {
        try {
            return new ControllerInventoryMovement().delete(id) ? Response.ok(gson.toJson(new ApiMessage("Movimiento eliminado"))).build() : notFound("Movimiento no encontrado");
        } catch (Exception e) { return error(e); }
    }
    private Response validate(InventoryMovement item) {
        if (item == null || item.getProductId() <= 0 || item.getUserId() <= 0 || item.getMovementType() == null) return badRequest("Producto, usuario y tipo de movimiento son obligatorios");
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) return badRequest("La cantidad debe ser positiva");
        return null;
    }
}
