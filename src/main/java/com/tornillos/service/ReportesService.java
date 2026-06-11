package com.tornillos.service;

import com.tornillos.dao.MovimientoInventarioDAO;
import com.tornillos.model.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

public class ReportesService {

    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    public List<MovimientoInventario> listarMovimientos(String tipo, String desde, String hasta) throws SQLException {
        return movimientoDAO.listar(tipo, desde, hasta);
    }
}
