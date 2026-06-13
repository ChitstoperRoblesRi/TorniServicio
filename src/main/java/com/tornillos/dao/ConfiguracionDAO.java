package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionDAO {

    public Map<String, String> obtainAll() throws SQLException {
        Map<String, String> mapa = new HashMap<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT clave, valor FROM configuracion")) {
            while (rs.next()) {
                mapa.put(rs.getString("clave"), rs.getString("valor"));
            }
        }
        return mapa;
    }

    public String obtener(String clave) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("SELECT valor FROM configuracion WHERE clave=?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("valor") : null;
            }
        }
    }

    public void guardar(String clave, String valor) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
                "INSERT INTO configuracion (clave, valor) VALUES (?,?) " +
                "ON CONFLICT (clave) DO UPDATE SET valor=EXCLUDED.valor")) {
            ps.setString(1, clave); 
            ps.setString(2, valor);
            ps.executeUpdate();
        }
    }
}