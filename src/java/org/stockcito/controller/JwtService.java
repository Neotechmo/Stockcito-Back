package org.stockcito.controller;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.stockcito.config.EnvConfig;
import org.stockcito.model.User;

public class JwtService {

    private static final Gson GSON = new Gson();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public String create(User user) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", String.valueOf(user.getId()));
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole().name());
        payload.put("iat", now);
        payload.put("exp", now + getExpirationSeconds());
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String body = encode(GSON.toJson(payload));
        String unsigned = header + "." + body;
        return unsigned + "." + sign(unsigned);
    }

    public boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !constantTimeEquals(parts[2], sign(parts[0] + "." + parts[1]))) return false;
            Map<?, ?> payload = GSON.fromJson(new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8), Map.class);
            Number exp = (Number) payload.get("exp");
            return exp != null && exp.longValue() > Instant.now().getEpochSecond();
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String getSecret() {
        return EnvConfig.get("JWT_SECRET", "stockcito-cambiar-esta-clave-en-produccion-2026");
    }

    private long getExpirationSeconds() {
        return EnvConfig.getLong("JWT_EXPIRATION_SECONDS", 86400L);
    }
}
