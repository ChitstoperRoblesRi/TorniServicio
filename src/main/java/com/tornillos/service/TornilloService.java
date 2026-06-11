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

    public void crear(Tornillo t) throws SQLException {
        tornilloDAO.crear(t);
    }

    public void actualizar(Tornillo t) throws SQLException {
        tornilloDAO.actualizar(t);
    }

    public void darDeBaja(int id) throws SQLException {
        tornilloDAO.darDeBaja(id);
    }

    public void eliminar(int id) throws SQLException {
        tornilloDAO.eliminar(id);
    }
}
