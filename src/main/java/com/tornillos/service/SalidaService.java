package com.tornillos.service;

import com.tornillos.dao.SalidaDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Salida;
import com.tornillos.model.Tornillo;

import java.sql.SQLException;
import java.util.List;

public class SalidaService {

    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    public boolean registrar(Salida salida) throws SQLException {
        Tornillo tornillo = tornilloDAO.obtenerPorId(salida.getTornilloId());
        if (tornillo == null) {
            throw new SQLException("Tornillo no encontrado");
        }
        int stockActual = tornillo.getStockActual();
        if (stockActual < salida.getCantidad()) {
            throw new SQLException("Stock insuficiente. Disponible: " + stockActual
                    + " | Solicitado: " + salida.getCantidad());
        }
        boolean resultado = salidaDAO.registrar(salida);
        alertaService.verificarAlertas();
        return resultado;
    }

    public boolean eliminar(int id) throws SQLException {
        boolean resultado = salidaDAO.eliminar(id);
        alertaService.verificarAlertas();
        return resultado;
    }

    public List<Salida> buscar(String termino, String desde, String hasta) throws SQLException {
        return salidaDAO.buscar(termino, desde, hasta);
    }

    public int contarHoy() throws SQLException {
        return salidaDAO.contarHoy();
    }

    public List<Tornillo> listarTornillosActivos() throws SQLException {
        return tornilloDAO.listarTodos();
    }
}
