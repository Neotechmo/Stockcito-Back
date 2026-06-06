package org.stockcito.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.stockcito.config.EnvConfig;

public class ConexionMysql {

    private Connection conn;

    public Connection open() throws SQLException {
        String host = EnvConfig.get("DB_HOST", "127.0.0.1");
        String port = EnvConfig.get("DB_PORT", "3306");
        String dbName = EnvConfig.get("DB_NAME", "stockcito_db");
        String user = EnvConfig.get("DB_USER", "stockcito");
        String password = EnvConfig.get("DB_PASSWORD", "stockcito123");

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

}
