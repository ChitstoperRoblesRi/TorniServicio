package com.tornillos.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DatabaseConfig {
    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class.getName());

    private static String HOST = "localhost";
    private static String PORT = "5432";
    private static String DATABASE = "tornillos_db";
    private static String USER = "postgres";
    private static String PASSWORD = "Josuemysql22*"; // <--- Usa tu contraseña real aquí

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            inicializarBaseDeDatos();

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
            LOG.info("Conexion establecida con PostgreSQL con exito.");
        }
        return connection;
    }

    /**
     * Revisa si la BD existe; si no, la crea y ejecuta el script SQL embebido.
     */
    private static void inicializarBaseDeDatos() {
        // 1. Conectar a la BD por defecto 'postgres' para poder crear la nuestra
        String urlServidor = String.format("jdbc:postgresql://%s:%s/postgres", HOST, PORT);
        
        try (Connection connServer = DriverManager.getConnection(urlServidor, USER, PASSWORD);
             Statement st = connServer.createStatement()) {
            
            // Verificar si nuestra BD ya existe en el servidor
            var rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + DATABASE + "'");
            if (!rs.next()) {
                LOG.info("La base de datos '" + DATABASE + "' no existe. Creando automaticamente...");
                st.executeUpdate("CREATE DATABASE " + DATABASE);
                LOG.info("Base de datos '" + DATABASE + "' creada de forma exitosa.");
                
                // 2. Como es nueva, le cargamos el script con las tablas e inserts
                ejecutarScriptEstructura();
            }
        } catch (SQLException e) {
            LOG.severe("Error critico inicializando el servidor de base de datos: " + e.getMessage());
        }
    }

    /**
     * Lee el archivo SQL desde los recursos y lo ejecuta en la nueva base de datos.
     */
    private static void ejecutarScriptEstructura() {
        String urlNuevaBD = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DATABASE);
        
        // Buscamos el archivo SQL en la carpeta de recursos del proyecto
        try (InputStream is = DatabaseConfig.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                LOG.warning("No se encontro el archivo 'schema.sql' en las rutas de recursos del proyecto.");
                return;
            }
            
            String scriptSql = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            try (Connection connNueva = DriverManager.getConnection(urlNuevaBD, USER, PASSWORD);
                 Statement stScript = connNueva.createStatement()) {
                
                LOG.info("Montando tablas, llaves primarias, funciones e inserciones iniciales...");
                stScript.execute(scriptSql);
                LOG.info("¡Estructura y datos de muestra cargados perfectamente de forma automatica!");
            }
            
        } catch (Exception e) {
            LOG.severe("Error ejecutando el script automatico de inicializacion: " + e.getMessage());
        }
    }

    public static void setCredentials(String host, String port, String db, String user, String pass) {
        HOST = host; PORT = port; DATABASE = db; USER = user; PASSWORD = pass;
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
        connection = null;
    }

    public static boolean testConnection(String host, String port, String db, String user, String pass) {
        try {
            Class.forName("org.postgresql.Driver");
            String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
            Properties props = new Properties();
            props.setProperty("user", user); props.setProperty("password", pass);
            props.setProperty("connectTimeout", "5");
            Connection c = DriverManager.getConnection(url, props);
            c.close();
            return true;
        } catch (Exception e) { return false; }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Conexion cerrada.");
            }
        } catch (SQLException e) {
            LOG.warning("Error cerrando conexion: " + e.getMessage());
        }
    }
}