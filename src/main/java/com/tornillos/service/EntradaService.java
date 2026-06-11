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

    public boolean registrar(Entrada entrada) throws SQLException {
        boolean resultado = entradaDAO.registrar(entrada);
        alertaService.verificarAlertas();
        return resultado;
    }

    public boolean eliminar(int id) throws SQLException {
        boolean resultado = entradaDAO.eliminar(id);
        alertaService.verificarAlertas();
        return resultado;
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
