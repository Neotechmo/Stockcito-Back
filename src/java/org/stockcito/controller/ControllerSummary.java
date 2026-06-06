package org.stockcito.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import org.stockcito.connection.ConexionMysql;
import org.stockcito.config.EnvConfig;
import org.stockcito.model.SummaryRequest;
import org.stockcito.model.SummaryResponse;

public class ControllerSummary {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

    public SummaryResponse summarize(SummaryRequest request) throws Exception {
        String apiKey = EnvConfig.get("OPENAI_API_KEY", null);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY no esta configurada");
        }

        SummaryRequest options = request == null ? new SummaryRequest() : request;
        String model = EnvConfig.get("OPENAI_MODEL", "gpt-4.1-mini");
        String inventoryContext = buildInventoryContext(options);
        String body = buildRequestBody(model, options, inventoryContext);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_RESPONSES_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI respondio con status " + response.statusCode() + ": " + extractError(response.body()));
        }

        String summary = extractOutputText(response.body());
        if (summary == null || summary.isBlank()) {
            throw new IllegalStateException("OpenAI no regreso un resumen");
        }
        return new SummaryResponse(summary.trim(), model);
    }

    private String buildInventoryContext(SummaryRequest request) throws Exception {
        int daysBack = numberOrDefault(request.getDaysBack(), 30);
        int lowStockLimit = numberOrDefault(request.getLowStockLimit(), 8);
        int highExitLimit = numberOrDefault(request.getHighExitLimit(), 8);
        int slowMovementDays = numberOrDefault(request.getSlowMovementDays(), 60);
        int slowMovementLimit = numberOrDefault(request.getSlowMovementLimit(), 8);

        JsonObject context = new JsonObject();
        context.addProperty("analysisWindowDays", daysBack);
        context.addProperty("slowMovementDays", slowMovementDays);
        context.add("lowStockProducts", lowStockProducts(lowStockLimit));
        context.add("productsWithManyExits", productsWithManyExits(daysBack, highExitLimit));
        context.add("productsWithLittleOrNoRecentExits", slowMovingProducts(slowMovementDays, slowMovementLimit));
        context.add("recentMovements", recentMovements(daysBack));
        return context.toString();
    }

    private JsonArray lowStockProducts(int limit) throws Exception {
        String sql = """
            SELECT p.id, p.name, p.sku, p.current_stock, p.min_stock, p.unit_of_measure,
                   c.name category_name, s.name supplier_name
            FROM products p
            JOIN categories c ON c.id=p.category_id
            LEFT JOIN suppliers s ON s.id=p.supplier_id
            WHERE p.is_active=TRUE
              AND p.current_stock <= p.min_stock
            ORDER BY (p.min_stock - p.current_stock) DESC, p.name
            LIMIT ?
            """;
        JsonArray rows = new JsonArray();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(productStockJson(rs));
            }
        } finally {
            mysql.close();
        }
        return rows;
    }

    private JsonArray productsWithManyExits(int daysBack, int limit) throws Exception {
        String sql = """
            SELECT p.id, p.name, p.sku, p.current_stock, p.min_stock, p.unit_of_measure,
                   c.name category_name, s.name supplier_name,
                   COUNT(m.id) exit_count, COALESCE(SUM(m.quantity),0) total_exit_quantity,
                   MAX(m.movement_date) last_exit_at
            FROM products p
            JOIN categories c ON c.id=p.category_id
            LEFT JOIN suppliers s ON s.id=p.supplier_id
            JOIN inventory_movements m ON m.product_id=p.id AND m.movement_type='EXIT'
            WHERE p.is_active=TRUE
              AND m.movement_date >= DATE_SUB(NOW(), INTERVAL ? DAY)
            GROUP BY p.id, p.name, p.sku, p.current_stock, p.min_stock, p.unit_of_measure, c.name, s.name
            ORDER BY total_exit_quantity DESC, exit_count DESC, p.name
            LIMIT ?
            """;
        JsonArray rows = new JsonArray();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, daysBack);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject row = productStockJson(rs);
                    row.addProperty("exitCount", rs.getLong("exit_count"));
                    row.addProperty("totalExitQuantity", rs.getBigDecimal("total_exit_quantity"));
                    row.addProperty("lastExitAt", String.valueOf(rs.getTimestamp("last_exit_at")));
                    rows.add(row);
                }
            }
        } finally {
            mysql.close();
        }
        return rows;
    }

    private JsonArray slowMovingProducts(int slowMovementDays, int limit) throws Exception {
        String sql = """
            SELECT p.id, p.name, p.sku, p.current_stock, p.min_stock, p.unit_of_measure,
                   c.name category_name, s.name supplier_name,
                   MAX(m.movement_date) last_exit_at,
                   COALESCE(SUM(CASE WHEN m.movement_date >= DATE_SUB(NOW(), INTERVAL ? DAY) THEN m.quantity ELSE 0 END),0) recent_exit_quantity
            FROM products p
            JOIN categories c ON c.id=p.category_id
            LEFT JOIN suppliers s ON s.id=p.supplier_id
            LEFT JOIN inventory_movements m ON m.product_id=p.id AND m.movement_type='EXIT'
            WHERE p.is_active=TRUE
              AND p.current_stock > 0
            GROUP BY p.id, p.name, p.sku, p.current_stock, p.min_stock, p.unit_of_measure, c.name, s.name
            HAVING recent_exit_quantity = 0
            ORDER BY p.current_stock DESC, p.name
            LIMIT ?
            """;
        JsonArray rows = new JsonArray();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slowMovementDays);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject row = productStockJson(rs);
                    row.addProperty("lastExitAt", rs.getTimestamp("last_exit_at") == null ? null : String.valueOf(rs.getTimestamp("last_exit_at")));
                    row.addProperty("recentExitQuantity", rs.getBigDecimal("recent_exit_quantity"));
                    rows.add(row);
                }
            }
        } finally {
            mysql.close();
        }
        return rows;
    }

    private JsonArray recentMovements(int daysBack) throws Exception {
        String sql = """
            SELECT m.movement_type, COUNT(*) movement_count, COALESCE(SUM(m.quantity),0) total_quantity
            FROM inventory_movements m
            WHERE m.movement_date >= DATE_SUB(NOW(), INTERVAL ? DAY)
            GROUP BY m.movement_type
            ORDER BY m.movement_type
            """;
        JsonArray rows = new JsonArray();
        ConexionMysql mysql = new ConexionMysql();
        try (Connection conn = mysql.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, daysBack);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject row = new JsonObject();
                    row.addProperty("movementType", rs.getString("movement_type"));
                    row.addProperty("movementCount", rs.getLong("movement_count"));
                    row.addProperty("totalQuantity", rs.getBigDecimal("total_quantity"));
                    rows.add(row);
                }
            }
        } finally {
            mysql.close();
        }
        return rows;
    }

    private JsonObject productStockJson(ResultSet rs) throws Exception {
        JsonObject row = new JsonObject();
        row.addProperty("productId", rs.getLong("id"));
        row.addProperty("name", rs.getString("name"));
        row.addProperty("sku", rs.getString("sku"));
        row.addProperty("category", rs.getString("category_name"));
        row.addProperty("supplier", rs.getString("supplier_name"));
        row.addProperty("currentStock", rs.getBigDecimal("current_stock"));
        row.addProperty("minimumStock", rs.getBigDecimal("min_stock"));
        row.addProperty("unitOfMeasure", rs.getString("unit_of_measure"));
        return row;
    }

    private String buildRequestBody(String model, SummaryRequest request, String inventoryContext) {
        int maxSentences = request.getMaxSentences() == null ? 3 : request.getMaxSentences();
        String language = isBlank(request.getLanguage()) ? "español" : request.getLanguage().trim();

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("instructions",
                "Eres un analista de inventario de Stockcito. Genera alertas y recomendaciones operativas claras. "
                + "Debes detectar productos sin stock suficiente o por debajo del minimo, productos con muchas salidas "
                + "que conviene comprar, y productos con pocas o nulas salidas que conviene revisar en bodega para evitar caducidad o sobreinventario. "
                + "No inventes datos que no aparezcan en el JSON. Si no hay datos en una seccion, dilo brevemente.");
        root.addProperty("input",
                "Con base en este JSON de inventario, escribe un diagnostico en " + language
                + " en " + maxSentences + " mensajes como maximo. "
                + "Cada mensaje debe ser accionable, por ejemplo: comprar mas, revisar bodega, vigilar stock insuficiente, o verificar productos sin salida. "
                + "Incluye nombres de productos y cantidades cuando existan. JSON:\\n\\n" + inventoryContext);
        return root.toString();
    }

    private int numberOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String extractOutputText(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("output_text") && !json.get("output_text").isJsonNull()) {
            return json.get("output_text").getAsString();
        }
        if (!json.has("output") || !json.get("output").isJsonArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        JsonArray output = json.getAsJsonArray("output");
        for (JsonElement outputItem : output) {
            if (!outputItem.isJsonObject()) continue;
            JsonObject outputObject = outputItem.getAsJsonObject();
            if (!outputObject.has("content") || !outputObject.get("content").isJsonArray()) continue;
            for (JsonElement contentItem : outputObject.getAsJsonArray("content")) {
                if (!contentItem.isJsonObject()) continue;
                JsonObject contentObject = contentItem.getAsJsonObject();
                if (contentObject.has("text") && !contentObject.get("text").isJsonNull()) {
                    if (text.length() > 0) text.append('\n');
                    text.append(contentObject.get("text").getAsString());
                }
            }
        }
        return text.toString();
    }

    private String extractError(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject error = json.getAsJsonObject("error");
                if (error.has("message") && !error.get("message").isJsonNull()) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
