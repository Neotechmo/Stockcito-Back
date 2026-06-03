package org.stockcito.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerPurchaseRequest;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.PurchaseRequest;
import org.stockcito.model.PurchaseStatus;

@Path("v1/purchase-requests")
@Produces(MediaType.APPLICATION_JSON)
public class RestPurchaseRequest extends RestSupport {

    @GET public Response getAll(@QueryParam("status") String status) {
        try {
            PurchaseStatus filter = blank(status) ? null : PurchaseStatus.valueOf(status.toUpperCase());
            return Response.ok(gson.toJson(new ControllerPurchaseRequest().getAll(filter))).build();
        } catch (Exception e) { return error(e); }
    }
    @GET @Path("{id}") public Response getById(@PathParam("id") long id) {
        try {
            PurchaseRequest item = new ControllerPurchaseRequest().getById(id);
            return item == null ? notFound("Solicitud no encontrada") : Response.ok(gson.toJson(item)).build();
        } catch (Exception e) { return error(e); }
    }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response save(String json) {
        try {
            PurchaseRequest item = gson.fromJson(json, PurchaseRequest.class);
            Response validation = validate(item); if (validation != null) return validation;
            return Response.status(Response.Status.CREATED).entity(gson.toJson(new ControllerPurchaseRequest().save(item))).build();
        } catch (Exception e) { return error(e); }
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public Response update(@PathParam("id") long id, String json) {
        try {
            PurchaseRequest item = gson.fromJson(json, PurchaseRequest.class);
            Response validation = validate(item); if (validation != null) return validation;
            PurchaseRequest updated = new ControllerPurchaseRequest().update(id, item);
            return updated == null ? notFound("Solicitud no encontrada") : Response.ok(gson.toJson(updated)).build();
        } catch (Exception e) { return error(e); }
    }
    @PATCH @Path("{id}/status") @Consumes(MediaType.APPLICATION_JSON) public Response updateStatus(@PathParam("id") long id, String json) {
        try {
            PurchaseRequest body = gson.fromJson(json, PurchaseRequest.class);
            if (body == null || body.getStatus() == null) return badRequest("El status es obligatorio");
            PurchaseRequest updated = new ControllerPurchaseRequest().updateStatus(id, body.getStatus(), body.getApprovedBy());
            return updated == null ? notFound("Solicitud no encontrada") : Response.ok(gson.toJson(updated)).build();
        } catch (Exception e) { return error(e); }
    }
    @DELETE @Path("{id}") public Response delete(@PathParam("id") long id) {
        try {
            return new ControllerPurchaseRequest().delete(id) ? Response.ok(gson.toJson(new ApiMessage("Solicitud eliminada"))).build() : notFound("Solicitud no encontrada");
        } catch (Exception e) { return error(e); }
    }
    private Response validate(PurchaseRequest item) {
        if (item == null || item.getProductId() <= 0 || item.getRequestedBy() <= 0) return badRequest("Producto y solicitante son obligatorios");
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) return badRequest("La cantidad debe ser positiva");
        return null;
    }
}
