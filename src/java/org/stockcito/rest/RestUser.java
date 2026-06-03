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
import org.stockcito.controller.ControllerUser;
import org.stockcito.model.ApiMessage;
import org.stockcito.model.User;

@Path("v1/users")
@Produces(MediaType.APPLICATION_JSON)
public class RestUser extends RestSupport {

    @GET
    public Response getAll() {
        try { return Response.ok(gson.toJson(new ControllerUser().getAll())).build(); } catch (Exception e) { return error(e); }
    }

    @GET @Path("{id}")
    public Response getById(@PathParam("id") long id) {
        try {
            User user = new ControllerUser().getById(id);
            return user == null ? notFound("Usuario no encontrado") : Response.ok(gson.toJson(user)).build();
        } catch (Exception e) { return error(e); }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON)
    public Response save(String json) {
        try {
            User user = gson.fromJson(json, User.class);
            Response validation = validate(user, true);
            if (validation != null) return validation;
            return Response.status(Response.Status.CREATED).entity(gson.toJson(new ControllerUser().save(user))).build();
        } catch (Exception e) { return error(e); }
    }

    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") long id, String json) {
        try {
            User user = gson.fromJson(json, User.class);
            Response validation = validate(user, false);
            if (validation != null) return validation;
            User updated = new ControllerUser().update(id, user);
            return updated == null ? notFound("Usuario no encontrado") : Response.ok(gson.toJson(updated)).build();
        } catch (Exception e) { return error(e); }
    }

    @DELETE @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            return new ControllerUser().delete(id)
                    ? Response.ok(gson.toJson(new ApiMessage("Usuario eliminado"))).build()
                    : notFound("Usuario no encontrado");
        } catch (Exception e) { return error(e); }
    }

    private Response validate(User user, boolean requirePassword) {
        if (user == null || blank(user.getName())) return badRequest("El nombre es obligatorio");
        if (blank(user.getEmail()) || !user.getEmail().contains("@")) return badRequest("El email no es valido");
        if (requirePassword && (blank(user.getPassword()) || user.getPassword().length() < 6)) return badRequest("La password debe tener al menos 6 caracteres");
        return null;
    }
}
