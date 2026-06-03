package org.stockcito.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("swagger-ui")
public class RestSwaggerUi {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response ui() {
        String html = """
            <!doctype html>
            <html>
              <head>
                <meta charset="utf-8">
                <title>Stockcito API</title>
                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
              </head>
              <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                <script>
                  SwaggerUIBundle({
                    url: '../v3/api-docs',
                    dom_id: '#swagger-ui',
                    persistAuthorization: true,
                    displayRequestDuration: true
                  });
                </script>
              </body>
            </html>
            """;
        return Response.ok(html).build();
    }
}
