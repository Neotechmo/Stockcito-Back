package org.stockcito.rest;

import com.google.gson.Gson;
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
import org.stockcito.controller.ControllerSupplier;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.Supplier;

@Path("v1/suppliers")
public class RestSupplier {

    private final Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            ControllerSupplier controller = new ControllerSupplier();
            return Response.ok(gson.toJson(controller.getAll())).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") long id) {
        try {
            ControllerSupplier controller = new ControllerSupplier();
            Supplier supplier = controller.getById(id);
            if (supplier == null) {
                return notFound("Proveedor no encontrado");
            }
            return Response.ok(gson.toJson(supplier)).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(String json) {
        try {
            Supplier supplier = gson.fromJson(json, Supplier.class);
            if (supplier == null || isBlank(supplier.getName())) {
                return badRequest("El nombre es obligatorio");
            }
            ControllerSupplier controller = new ControllerSupplier();
            return Response.status(Response.Status.CREATED).entity(gson.toJson(controller.save(supplier))).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") long id, String json) {
        try {
            Supplier supplier = gson.fromJson(json, Supplier.class);
            if (supplier == null || isBlank(supplier.getName())) {
                return badRequest("El nombre es obligatorio");
            }
            ControllerSupplier controller = new ControllerSupplier();
            Supplier updated = controller.update(id, supplier);
            if (updated == null) {
                return notFound("Proveedor no encontrado");
            }
            return Response.ok(gson.toJson(updated)).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") long id) {
        try {
            ControllerSupplier controller = new ControllerSupplier();
            if (!controller.delete(id)) {
                return notFound("Proveedor no encontrado");
            }
            return Response.ok(gson.toJson(new ApiMessage("Proveedor eliminado"))).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(new ApiMessage(message))).build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(new ApiMessage(message))).build();
    }

    private Response error(Exception e) {
        e.printStackTrace();
        return Response.serverError().entity(gson.toJson(new ApiMessage("Error interno del servidor"))).build();
    }
}
