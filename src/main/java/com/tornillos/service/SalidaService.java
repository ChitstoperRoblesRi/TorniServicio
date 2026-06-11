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

    public void registrar(Salida salida) throws SQLException {
        Tornillo t = tornilloDAO.obtenerPorId(salida.getTornilloId());
        if (t == null) {
            throw new SQLException("Tornillo no encontrado");
        }
        int stockActual = t.getStockActual();
        if (stockActual < salida.getCantidad()) {
            throw new SQLException("Stock insuficiente. Disponible: " + stockActual
                    + " | Solicitado: " + salida.getCantidad());
        }
        salidaDAO.registrar(salida);
        alertaService.verificarAlertas();
    }

    public void eliminar(int id) throws SQLException {
        salidaDAO.eliminar(id);
        alertaService.verificarAlertas();
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
