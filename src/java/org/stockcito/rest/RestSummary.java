package org.stockcito.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.stockcito.controller.ControllerSummary;
import org.stockcito.model.SummaryRequest;

@Path("v1/ai/summaries")
@Produces(MediaType.APPLICATION_JSON)
public class RestSummary extends RestSupport {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response summarize(String json) {
        try {
            SummaryRequest request = blank(json) ? new SummaryRequest() : gson.fromJson(json, SummaryRequest.class);
            Response validation = validate(request);
            if (validation != null) return validation;
            return Response.ok(gson.toJson(new ControllerSummary().summarize(request))).build();
        } catch (IllegalStateException e) {
            return serviceUnavailable(e.getMessage());
        } catch (Exception e) {
            return error(e);
        }
    }

    private Response validate(SummaryRequest request) {
        if (request == null) return badRequest("El body debe ser un JSON valido");
        if (request.getDaysBack() != null && (request.getDaysBack() < 1 || request.getDaysBack() > 365)) {
            return badRequest("daysBack debe estar entre 1 y 365");
        }
        if (request.getLowStockLimit() != null && (request.getLowStockLimit() < 1 || request.getLowStockLimit() > 25)) {
            return badRequest("lowStockLimit debe estar entre 1 y 25");
        }
        if (request.getHighExitLimit() != null && (request.getHighExitLimit() < 1 || request.getHighExitLimit() > 25)) {
            return badRequest("highExitLimit debe estar entre 1 y 25");
        }
        if (request.getSlowMovementDays() != null && (request.getSlowMovementDays() < 1 || request.getSlowMovementDays() > 365)) {
            return badRequest("slowMovementDays debe estar entre 1 y 365");
        }
        if (request.getSlowMovementLimit() != null && (request.getSlowMovementLimit() < 1 || request.getSlowMovementLimit() > 25)) {
            return badRequest("slowMovementLimit debe estar entre 1 y 25");
        }
        if (request.getMaxSentences() != null && (request.getMaxSentences() < 1 || request.getMaxSentences() > 10)) {
            return badRequest("maxSentences debe estar entre 1 y 10");
        }
        return null;
    }
}
