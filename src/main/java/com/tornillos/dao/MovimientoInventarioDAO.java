package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.MovimientoInventario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    public List<MovimientoInventario> listar(String tipo, String desde, String hasta) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        String sql =
            "SELECT m.fecha, m.tornillo_nombre, m.tornillo_codigo, m.tipo_movimiento, " +
            "m.cantidad, m.usuario_nombre, " +
            "SUM(m.ajuste) OVER (PARTITION BY m.tornillo_id ORDER BY m.fecha ROWS UNBOUNDED PRECEDING) AS stock_resultante " +
            "FROM ( " +
                "SELECT t.creado_en AS fecha, t.id AS tornillo_id, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo, " +
                    "'Creaci\u00f3n' AS tipo_movimiento, " +
                    "(t.stock_actual - COALESCE(ent.neto,0) + COALESCE(sal.neto,0)) AS cantidad, " +
                    "(t.stock_actual - COALESCE(ent.neto,0) + COALESCE(sal.neto,0)) AS ajuste, " +
                    "'Sistema' AS usuario_nombre " +
                "FROM tornillos t " +
                "LEFT JOIN (SELECT tornillo_id, SUM(cantidad) AS neto FROM entradas GROUP BY tornillo_id) ent ON ent.tornillo_id=t.id " +
                "LEFT JOIN (SELECT tornillo_id, SUM(cantidad) AS neto FROM salidas GROUP BY tornillo_id) sal ON sal.tornillo_id=t.id " +
                "WHERE t.activo=true AND (t.stock_actual - COALESCE(ent.neto,0) + COALESCE(sal.neto,0)) > 0 " +
                "UNION ALL " +
                "SELECT e.fecha, e.tornillo_id, t.nombre, t.codigo, " +
                    "'Entrada' AS tipo_movimiento, e.cantidad, e.cantidad AS ajuste, " +
                    "CONCAT(u.nombre,' ',u.apellido) AS usuario_nombre " +
                "FROM entradas e " +
                "JOIN tornillos t ON e.tornillo_id=t.id " +
                "JOIN usuarios u ON e.usuario_id=u.id " +
                "UNION ALL " +
                "SELECT s.fecha, s.tornillo_id, t.nombre, t.codigo, " +
                    "'Salida' AS tipo_movimiento, s.cantidad, -s.cantidad AS ajuste, " +
                    "CONCAT(u.nombre,' ',u.apellido) AS usuario_nombre " +
                "FROM salidas s " +
                "JOIN tornillos t ON s.tornillo_id=t.id " +
                "JOIN usuarios u ON s.usuario_id=u.id " +
            ") m WHERE 1=1 ";

        List<Object> params = new ArrayList<>();
        if (tipo != null && !tipo.isEmpty()) {
            sql += "AND m.tipo_movimiento = ? ";
            params.add(tipo);
        }
        if (desde != null && !desde.isEmpty()) {
            sql += "AND m.fecha >= (?::date)::timestamp ";
            params.add(desde);
        }
        if (hasta != null && !hasta.isEmpty()) {
            sql += "AND m.fecha < ((?::date) + 1)::timestamp ";
            params.add(hasta);
        }
        sql += "ORDER BY m.fecha DESC";

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        MovimientoInventario m = new MovimientoInventario();
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) m.setFecha(ts.toLocalDateTime());
        m.setTornilloNombre(rs.getString("tornillo_nombre"));
        m.setTornilloCodigo(rs.getString("tornillo_codigo"));
        m.setTipoMovimiento(rs.getString("tipo_movimiento"));
        m.setCantidad(rs.getInt("cantidad"));
        m.setStockResultante(rs.getInt("stock_resultante"));
        m.setUsuarioNombre(rs.getString("usuario_nombre"));
        return m;
    }
}
