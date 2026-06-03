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
import org.stockcito.controller.ControllerCategory;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.Category;

@Path("v1/categories")
public class RestCategory extends RestSupport {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            ControllerCategory controller = new ControllerCategory();
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
            ControllerCategory controller = new ControllerCategory();
            Category category = controller.getById(id);
            if (category == null) {
                return notFound("Categoria no encontrada");
            }
            return Response.ok(gson.toJson(category)).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(String json) {
        try {
            Category category = gson.fromJson(json, Category.class);
            if (category == null || isBlank(category.getName())) {
                return badRequest("El nombre es obligatorio");
            }
            ControllerCategory controller = new ControllerCategory();
            return Response.status(Response.Status.CREATED).entity(gson.toJson(controller.save(category))).build();
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
            Category category = gson.fromJson(json, Category.class);
            if (category == null || isBlank(category.getName())) {
                return badRequest("El nombre es obligatorio");
            }
            ControllerCategory controller = new ControllerCategory();
            Category updated = controller.update(id, category);
            if (updated == null) {
                return notFound("Categoria no encontrada");
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
            ControllerCategory controller = new ControllerCategory();
            if (!controller.delete(id)) {
                return notFound("Categoria no encontrada");
            }
            return Response.ok(gson.toJson(new ApiMessage("Categoria eliminada"))).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
