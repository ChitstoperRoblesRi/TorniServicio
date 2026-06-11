package com.tornillos.service;

import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Tornillo;

import java.sql.SQLException;
import java.util.List;

public class TornilloService {

    private final TornilloDAO tornilloDAO = new TornilloDAO();

    public List<Tornillo> listarTodos() throws SQLException {
        return tornilloDAO.listarTodos();
    }

    public List<Tornillo> listarConFiltro(String termino, String estadoStock) throws SQLException {
        return tornilloDAO.listarConFiltro(termino, estadoStock);
    }

    public Tornillo obtenerPorId(int id) throws SQLException {
        return tornilloDAO.obtenerPorId(id);
    }

    public boolean crear(Tornillo tornillo) throws SQLException {
        return tornilloDAO.crear(tornillo);
    }

    public boolean actualizar(Tornillo tornillo) throws SQLException {
        return tornilloDAO.actualizar(tornillo);
    }

    public boolean darDeBaja(int id) throws SQLException {
        return tornilloDAO.eliminar(id);
    }

    public boolean eliminar(int id) throws SQLException {
        return tornilloDAO.eliminar(id);
    }
}
