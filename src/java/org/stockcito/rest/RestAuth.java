package org.stockcito.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerUser;
import org.stockcito.controller.JwtService;
import org.stockcito.model.AuthResponse;
import org.stockcito.model.User;
import org.stockcito.model.UserRole;

@Path("v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestAuth extends RestSupport {

    @POST @Path("login")
    public Response login(String json) {
        try {
            User credentials = gson.fromJson(json, User.class);
            if (credentials == null || blank(credentials.getEmail()) || blank(credentials.getPassword())) return badRequest("Email y password son obligatorios");
            User user = new ControllerUser().authenticate(credentials.getEmail(), credentials.getPassword());
            if (user == null) return Response.status(Response.Status.UNAUTHORIZED).entity(gson.toJson(new org.stockcito.model.ApiMessage("Credenciales invalidas"))).build();
            return Response.ok(gson.toJson(new AuthResponse(new JwtService().create(user), user))).build();
        } catch (Exception e) { return error(e); }
    }

    @POST @Path("register")
    public Response register(String json) {
        try {
            User user = gson.fromJson(json, User.class);
            if (user == null || blank(user.getName()) || blank(user.getEmail()) || blank(user.getPassword())) return badRequest("Nombre, email y password son obligatorios");
            if (!user.getEmail().contains("@") || user.getPassword().length() < 6) return badRequest("Email invalido o password menor a 6 caracteres");
            user.setRole(UserRole.OPERADOR);
            User saved = new ControllerUser().save(user);
            return Response.status(Response.Status.CREATED).entity(gson.toJson(new AuthResponse(new JwtService().create(saved), saved))).build();
        } catch (Exception e) { return error(e); }
    }
}
