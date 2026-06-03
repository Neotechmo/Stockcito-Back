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
import org.stockcito.controller.ControllerProduct;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.Product;

@Path("v1/products")
public class RestProduct {

    private final Gson gson = new Gson();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            ControllerProduct controller = new ControllerProduct();
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
            ControllerProduct controller = new ControllerProduct();
            Product product = controller.getById(id);
            if (product == null) {
                return notFound("Producto no encontrado");
            }
            return Response.ok(gson.toJson(product)).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(String json) {
        try {
            Product product = gson.fromJson(json, Product.class);
            Response validation = validate(product);
            if (validation != null) {
                return validation;
            }
            ControllerProduct controller = new ControllerProduct();
            return Response.status(Response.Status.CREATED).entity(gson.toJson(controller.save(product))).build();
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
            Product product = gson.fromJson(json, Product.class);
            Response validation = validate(product);
            if (validation != null) {
                return validation;
            }
            ControllerProduct controller = new ControllerProduct();
            Product updated = controller.update(id, product);
            if (updated == null) {
                return notFound("Producto no encontrado");
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
            ControllerProduct controller = new ControllerProduct();
            if (!controller.delete(id)) {
                return notFound("Producto no encontrado");
            }
            return Response.ok(gson.toJson(new ApiMessage("Producto eliminado"))).build();
        } catch (Exception e) {
            return error(e);
        }
    }

    private Response validate(Product product) {
        if (product == null) {
            return badRequest("El producto es obligatorio");
        }
        if (product.getCategoryId() <= 0) {
            return badRequest("La categoria es obligatoria");
        }
        if (isBlank(product.getName())) {
            return badRequest("El nombre es obligatorio");
        }
        if (isBlank(product.getUnitOfMeasure())) {
            return badRequest("La unidad de medida es obligatoria");
        }
        if (product.getMinStock() == null || product.getMinStock().signum() < 0) {
            return badRequest("El stock minimo no puede ser negativo");
        }
        if (product.getCurrentStock() == null || product.getCurrentStock().signum() < 0) {
            return badRequest("El stock actual no puede ser negativo");
        }
        if (product.getUnitPrice() != null && product.getUnitPrice().signum() < 0) {
            return badRequest("El precio unitario no puede ser negativo");
        }
        return null;
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
