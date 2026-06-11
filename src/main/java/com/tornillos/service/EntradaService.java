package com.tornillos.service;

import com.tornillos.dao.EntradaDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Entrada;
import com.tornillos.model.Tornillo;

import java.sql.SQLException;
import java.util.List;

public class EntradaService {

    private final EntradaDAO entradaDAO = new EntradaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    public void registrar(Entrada entrada) throws SQLException {
        entradaDAO.registrar(entrada);
        alertaService.verificarAlertas();
    }

    public void eliminar(int id) throws SQLException {
        entradaDAO.eliminar(id);
        alertaService.verificarAlertas();
    }

    public List<Entrada> buscar(String termino, String desde, String hasta) throws SQLException {
        return entradaDAO.buscar(termino, desde, hasta);
    }

    public int contarHoy() throws SQLException {
        return entradaDAO.contarHoy();
    }

    public List<Tornillo> listarTornillosActivos() throws SQLException {
        return tornilloDAO.listarTodos();
    }
}
