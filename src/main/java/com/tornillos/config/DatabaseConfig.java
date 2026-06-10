package com.tornillos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConfig {
    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class.getName());

    private static String HOST = "localhost";
    private static String PORT = "5432";
    private static String DATABASE = "tornillos_db";
    private static String USER = "postgres";
    private static String PASSWORD = "123";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL Driver no encontrado", e);
            }
            String url = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DATABASE);
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("ApplicationName", "SistemaInventarioTornillos");
            connection = DriverManager.getConnection(url, props);
            connection.setAutoCommit(true);
            LOG.info("Conexión establecida con PostgreSQL");
        }
        return connection;
    }

    public static void setCredentials(String host, String port, String db, String user, String pass) {
        HOST = host;
        PORT = port;
        DATABASE = db;
        USER = user;
        PASSWORD = pass;
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    public static boolean testConnection(String host, String port, String db, String user, String pass) {
        try {
            Class.forName("org.postgresql.Driver");
            String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("connectTimeout", "5");
            Connection c = DriverManager.getConnection(url, props);
            c.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Conexión cerrada.");
            }
        } catch (SQLException e) {
            LOG.warning("Error cerrando conexión: " + e.getMessage());
        }
    }
}
