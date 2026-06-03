package org.stockcito.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerImportJob;
import org.stockcito.model.ImportJob;

@Path("v1/imports")
@Produces(MediaType.APPLICATION_JSON)
public class RestImportJob extends RestSupport {

    @GET public Response getAll() {
        try { return Response.ok(gson.toJson(new ControllerImportJob().getAll())).build(); } catch (Exception e) { return error(e); }
    }
    @GET @Path("{id}") public Response getById(@PathParam("id") long id) {
        try {
            ImportJob job = new ControllerImportJob().getById(id);
            return job == null ? notFound("Importacion no encontrada") : Response.ok(gson.toJson(job)).build();
        } catch (Exception e) { return error(e); }
    }
    @POST @Path("preview") @Consumes(MediaType.APPLICATION_JSON) public Response preview(String json) {
        try {
            ImportJob job = gson.fromJson(json, ImportJob.class);
            if (job == null || job.getUserId() <= 0) return badRequest("El usuario es obligatorio");
            return Response.status(Response.Status.CREATED).entity(gson.toJson(new ControllerImportJob().preview(job))).build();
        } catch (Exception e) { return error(e); }
    }
    @POST @Path("{id}/confirm") public Response confirm(@PathParam("id") long id) {
        try {
            ImportJob job = new ControllerImportJob().confirm(id);
            return job == null ? notFound("Importacion no encontrada") : Response.ok(gson.toJson(job)).build();
        } catch (Exception e) { return error(e); }
    }
    @POST @Path("{id}/cancel") public Response cancel(@PathParam("id") long id) {
        try {
            ImportJob job = new ControllerImportJob().cancel(id);
            return job == null ? notFound("Importacion no encontrada") : Response.ok(gson.toJson(job)).build();
        } catch (Exception e) { return error(e); }
    }
}
