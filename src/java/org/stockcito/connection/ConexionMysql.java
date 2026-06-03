package org.stockcito.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionMysql {

    private Connection conn;

    public Connection open() throws SQLException {
        String host = getConfig("DB_HOST", "127.0.0.1");
        String port = getConfig("DB_PORT", "3306");
        String dbName = getConfig("DB_NAME", "stockcito_db");
        String user = getConfig("DB_USER", "stockcito");
        String password = getConfig("DB_PASSWORD", "stockcito123");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontro el driver de MySQL.", e);
        }

        try {
            conn = connect(host, port, dbName, user, password);
        } catch (SQLException e) {
            if ("stockcito-db".equals(host)) {
                conn = connect("127.0.0.1", port, dbName, user, password);
            } else {
                throw new SQLException(
                        "No se pudo conectar a MySQL con host=" + host
                        + ", puerto=" + port
                        + ", base=" + dbName
                        + ", usuario=" + user
                        + ". Verifica que el usuario exista y la password sea correcta.",
                        e
                );
            }
        }

        return conn;
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Connection connect(String host, String port, String dbName, String user, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        String parametros = "?useSSL=false&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url + parametros, user, password);
    }

    private String getConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return getEnv(key, defaultValue);
    }

    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
