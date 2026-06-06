package org.stockcito.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnvConfig {

    private static final String ENV_FILE_NAME = ".env";
    private static volatile Map<String, String> dotenv;

    private EnvConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (isPresent(value)) return value;

        value = System.getenv(key);
        if (isPresent(value)) return value;

        value = loadDotenv().get(key);
        return isPresent(value) ? value : defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key, null);
        if (!isPresent(value)) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> current = dotenv;
        if (current != null) return current;

        synchronized (EnvConfig.class) {
            current = dotenv;
            if (current == null) {
                current = readDotenv();
                dotenv = current;
            }
            return current;
        }
    }

    private static Map<String, String> readDotenv() {
        Path envPath = findDotenv();
        if (envPath == null) return Map.of();

        Map<String, String> values = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line, values);
            }
        } catch (IOException e) {
            return Map.of();
        }
        return Map.copyOf(values);
    }

    private static Path findDotenv() {
        String explicitPath = System.getProperty("STOCKCITO_ENV_FILE");
        if (!isPresent(explicitPath)) {
            explicitPath = System.getenv("STOCKCITO_ENV_FILE");
        }
        if (isPresent(explicitPath)) {
            Path path = Path.of(explicitPath);
            if (Files.isRegularFile(path)) return path;
        }

        for (Path candidate : candidateBaseDirs()) {
            Path found = findInParents(candidate);
            if (found != null) return found;
        }
        return null;
    }

    private static List<Path> candidateBaseDirs() {
        String userDir = System.getProperty("user.dir", ".");
        String catalinaBase = System.getProperty("catalina.base");
        String classPath = EnvConfig.class.getProtectionDomain().getCodeSource() == null
                ? null
                : EnvConfig.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        return List.of(
                Path.of(userDir),
                isPresent(catalinaBase) ? Path.of(catalinaBase) : Path.of(userDir),
                isPresent(classPath) ? Path.of(classPath) : Path.of(userDir)
        );
    }

    private static Path findInParents(Path start) {
        Path current = start.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }

        while (current != null) {
            Path envPath = current.resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(envPath)) return envPath;
            current = current.getParent();
        }
        return null;
    }

    private static void parseLine(String line, Map<String, String> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return;
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) return;

        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (key.isEmpty()) return;

        values.put(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) return value;

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
