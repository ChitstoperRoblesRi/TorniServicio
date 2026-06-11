package com.tornillos.service;

import com.tornillos.dao.AlertaDAO;
import com.tornillos.dao.EntradaDAO;
import com.tornillos.dao.SalidaDAO;
import com.tornillos.dao.TornilloDAO;

import java.sql.SQLException;

public class DashboardService {

    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final EntradaDAO entradaDAO = new EntradaDAO();
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final AlertaDAO alertaDAO = new AlertaDAO();

    public int[] obtenerResumen() throws SQLException {
        return new int[] {
            tornilloDAO.contarTotal(),
            tornilloDAO.contarStockBajo(),
            entradaDAO.contarHoy(),
            salidaDAO.contarHoy(),
            alertaDAO.contarActivas()
        };
    }
}
