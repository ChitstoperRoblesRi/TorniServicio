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

    /**
     * Intermedia el filtrado y la consulta asíncrona de salidas de inventario.
     */
    public List<Salida> buscarSalidas(String termino, String desde, String hasta) throws SQLException {
        String t = (termino != null) ? termino.trim() : "";
        String d = (desde != null && !desde.trim().isEmpty()) ? desde.trim() : null;
        String h = (hasta != null && !hasta.trim().isEmpty()) ? hasta.trim() : null;
        return salidaDAO.buscar(t, d, h);
    }

    /**
     * Valida las reglas de negocio de stock, registra el egreso en la base de datos
     * y dispara de forma inmediata el subproceso síncrono del motor de alertas.
     */
    public void registrarSalida(Salida salida) throws SQLException {
        // Ejecuta las validaciones internas y la transacción empaquetada en el DAO
        salidaDAO.registrar(salida);
        
        // Ejecuta el motor de alertas de forma inmediata tras alterar el stock
        alertaService.verificarAlertas();
    }

    /**
     * Revierte transaccionalmente un egreso de inventario erróneo y reevalúa
     * el estado de criticidad de las alertas del sistema.
     */
    public void eliminarSalida(int id) throws SQLException {
        // Ejecuta la reversión física del stock en la BD
        salidaDAO.eliminar(id);
        
        // Reevalúa el estado del inventario para limpiar o generar alertas
        alertaService.verificarAlertas();
    }

    /**
     * Recupera la lista filtrada de tornillos que cuentan con existencias para su egreso.
     */
    public List<Tornillo> obtenerTornillosConStock() throws SQLException {
        return tornilloDAO.listarConStockDisponible();
    }
}