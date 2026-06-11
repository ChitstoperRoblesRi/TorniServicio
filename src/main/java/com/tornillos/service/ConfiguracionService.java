package com.tornillos.service;

import com.tornillos.dao.ConfiguracionDAO;

import java.sql.SQLException;
import java.util.Map;

public class ConfiguracionService {

    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

    public Map<String, String> obtenerTodas() throws SQLException {
        return configuracionDAO.obtenerTodas();
    }

    public boolean guardar(String clave, String valor) throws SQLException {
        return configuracionDAO.guardar(clave, valor);
    }

    public void guardarTodas(Map<String, String> config) throws SQLException {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            configuracionDAO.guardar(entry.getKey(), entry.getValue());
        }
    }
}
