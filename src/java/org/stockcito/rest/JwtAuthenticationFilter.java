package org.stockcito.rest;

import com.google.gson.Gson;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.stockcito.controller.JwtService;
import org.stockcito.model.ApiMessage;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        String path = context.getUriInfo().getPath();
        if (path.startsWith("v1/auth/") || path.equals("v1/health")
                || path.equals("swagger-ui") || path.startsWith("swagger-ui/")
                || path.equals("v3/api-docs") || path.startsWith("v3/api-docs/")) {
            return;
        }
        String authorization = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !new JwtService().isValid(authorization.substring(7))) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new Gson().toJson(new ApiMessage("Token JWT ausente o invalido"))).build());
        }
    }
}
