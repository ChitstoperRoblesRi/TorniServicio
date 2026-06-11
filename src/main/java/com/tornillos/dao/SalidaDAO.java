package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Salida;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalidaDAO {

    public boolean registrar(Salida salida) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        try {
            String sql = "INSERT INTO salidas (folio, tornillo_id, usuario_id, cantidad, " +
                         "precio_unitario, total, motivo, cliente, observaciones) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, salida.getFolio());
                ps.setInt(2, salida.getTornilloId());
                ps.setInt(3, salida.getUsuarioId());
                ps.setInt(4, salida.getCantidad());
                ps.setBigDecimal(5, salida.getPrecioUnitario());
                ps.setBigDecimal(6, salida.getTotal());
                ps.setString(7, salida.getMotivo());
                ps.setString(8, salida.getCliente());
                ps.setString(9, salida.getObservaciones());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tornillos SET stock_actual = stock_actual - ? WHERE id=?")) {
                ps.setInt(1, salida.getCantidad());
                ps.setInt(2, salida.getTornilloId());
                ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public int contarHoy() throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM salidas WHERE DATE(fecha)=CURRENT_DATE")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Salida mapear(ResultSet rs) throws SQLException {
        Salida salida = new Salida();
        salida.setId(rs.getInt("id"));
        salida.setFolio(rs.getString("folio"));
        salida.setTornilloId(rs.getInt("tornillo_id"));
        salida.setTornilloNombre(rs.getString("tornillo_nombre"));
        salida.setTornilloCodigo(rs.getString("tornillo_codigo"));
        salida.setUsuarioId(rs.getInt("usuario_id"));
        salida.setUsuarioNombre(rs.getString("usuario_nombre"));
        salida.setCantidad(rs.getInt("cantidad"));
        salida.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        salida.setTotal(rs.getBigDecimal("total"));
        salida.setMotivo(rs.getString("motivo"));
        salida.setCliente(rs.getString("cliente"));
        salida.setObservaciones(rs.getString("observaciones"));
        Timestamp fechaTs = rs.getTimestamp("fecha");
        if (fechaTs != null) salida.setFecha(fechaTs.toLocalDateTime());
        return salida;
    }



    public boolean eliminar(int id) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        try {
            int tornilloId = 0, cantidad = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT tornillo_id, cantidad FROM salidas WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tornilloId = rs.getInt("tornillo_id");
                        cantidad   = rs.getInt("cantidad");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tornillos SET stock_actual = stock_actual + ? WHERE id=?")) {
                ps.setInt(1, cantidad); ps.setInt(2, tornilloId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM salidas WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException ex) {
            conn.rollback(); throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Salida> buscar(String termino, String desde, String hasta) throws SQLException {
        List<Salida> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT s.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo, " +
            "CONCAT(u.nombre,' ',u.apellido) AS usuario_nombre " +
            "FROM salidas s JOIN tornillos t ON s.tornillo_id=t.id " +
            "JOIN usuarios u ON s.usuario_id=u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (termino != null && !termino.isEmpty()) {
            sb.append("AND (LOWER(s.folio) LIKE ? OR LOWER(t.nombre) LIKE ? OR LOWER(s.cliente) LIKE ? OR LOWER(t.codigo) LIKE ?) ");
            String like = "%" + termino.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (desde != null) { sb.append("AND s.fecha >= (?::date)::timestamp "); params.add(desde); }
        if (hasta != null) { sb.append("AND s.fecha < ((?::date) + 1)::timestamp "); params.add(hasta); }
        sb.append("ORDER BY s.fecha DESC");
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }
}
