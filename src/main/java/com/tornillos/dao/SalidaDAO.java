package com.tornillos.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Salida;

public class SalidaDAO {

    public void registrar(Salida s) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        int stockActual;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stock_actual FROM tornillos WHERE id=?")) {
            ps.setInt(1, s.getTornilloId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Tornillo no encontrado");
                stockActual = rs.getInt("stock_actual");
            }
        }
        if (stockActual < s.getCantidad())
            throw new SQLException("Stock insuficiente. Disponible: " + stockActual + " | Solicitado: " + s.getCantidad());

        conn.setAutoCommit(false);
        try {
            String sql = "INSERT INTO salidas (folio, tornillo_id, usuario_id, cantidad, " +
                         "precio_unitario, total, motivo, cliente, observaciones) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, s.getFolio());
                ps.setInt(2, s.getTornilloId());
                ps.setInt(3, s.getUsuarioId());
                ps.setInt(4, s.getCantidad());
                ps.setBigDecimal(5, s.getPrecioUnitario());
                ps.setBigDecimal(6, s.getTotal());
                ps.setString(7, s.getMotivo());
                ps.setString(8, s.getCliente());
                ps.setString(9, s.getObservaciones());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tornillos SET stock_actual = stock_actual - ? WHERE id=?")) {
                ps.setInt(1, s.getCantidad());
                ps.setInt(2, s.getTornilloId());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public int contarHoy() throws SQLException {
        // CORRECCIÓN: Contamos únicamente egresos activos del día
        String sql = "SELECT COUNT(*) FROM salidas WHERE DATE(fecha)=CURRENT_DATE AND activo = true";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        try {
            int tornilloId = 0, cantidad = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT tornillo_id, cantidad FROM salidas WHERE id=? AND activo = true")) {
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
            
            // CORRECCIÓN TRANSACCIONAL: Cambiado el DELETE destructivo por un UPDATE lógico de auditoría
            try (PreparedStatement ps = conn.prepareStatement("UPDATE salidas SET activo = false WHERE id=?")) {
                ps.setInt(1, id); 
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback(); 
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public List<Salida> buscar(String termino, String desde, String hasta) throws SQLException {
        List<Salida> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT s.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo, " +
            "CONCAT(u.nombre,' ',u.apellido) AS usuario_nombre " +
            "FROM salidas s " +
            "JOIN tornillos t ON s.tornillo_id=t.id " +
            "JOIN usuarios u ON s.usuario_id=u.id WHERE s.activo = true "); // CORRECCIÓN: Filtro de exclusión activa
        
        List<Object> params = new ArrayList<>();
        if (termino != null && !termino.isEmpty()) {
            sb.append("AND (LOWER(s.folio) LIKE ? OR LOWER(t.nombre) LIKE ? OR LOWER(s.cliente) LIKE ? OR LOWER(t.codigo) LIKE ?) ");
            String like = "%" + termino.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (desde != null) { 
            sb.append("AND s.fecha >= (?::date)::timestamp "); 
            params.add(desde); 
        }
        if (hasta != null) { 
            sb.append("AND s.fecha < ((?::date) + 1)::timestamp "); 
            params.add(hasta); 
        }
        sb.append("ORDER BY s.fecha DESC");
        
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i+1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Salida mapear(ResultSet rs) throws SQLException {
        Salida s = new Salida();
        s.setId(rs.getInt("id"));
        s.setFolio(rs.getString("folio"));
        s.setTornilloId(rs.getInt("tornillo_id"));
        s.setTornilloNombre(rs.getString("tornillo_nombre"));
        s.setTornilloCodigo(rs.getString("tornillo_codigo"));
        s.setUsuarioId(rs.getInt("usuario_id"));
        s.setUsuarioNombre(rs.getString("usuario_nombre"));
        s.setCantidad(rs.getInt("cantidad"));
        s.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        s.setTotal(rs.getBigDecimal("total"));
        s.setMotivo(rs.getString("motivo"));
        s.setCliente(rs.getString("cliente"));
        s.setObservaciones(rs.getString("observaciones"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) {
            s.setFecha(ts.toLocalDateTime());
        }
        return s;
    }
}