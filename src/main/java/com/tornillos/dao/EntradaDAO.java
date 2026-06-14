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
import com.tornillos.model.Entrada;

public class EntradaDAO {

    public void registrar(Entrada e) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        try {
            String sql = "INSERT INTO entradas (folio, tornillo_id, usuario_id, cantidad, " +
                         "precio_unitario, total, numero_factura, observaciones) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, e.getFolio());
                ps.setInt(2, e.getTornilloId());
                ps.setInt(3, e.getUsuarioId());
                ps.setInt(4, e.getCantidad());
                ps.setBigDecimal(5, e.getPrecioUnitario());
                ps.setBigDecimal(6, e.getTotal());
                ps.setString(7, e.getNumeroFactura());
                ps.setString(8, e.getObservaciones());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tornillos SET stock_actual = stock_actual + ? WHERE id=?")) {
                ps.setInt(1, e.getCantidad());
                ps.setInt(2, e.getTornilloId());
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
        String sql = "SELECT COUNT(*) FROM entradas WHERE DATE(fecha)=CURRENT_DATE";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        try {
            String getQ = "SELECT tornillo_id, cantidad FROM entradas WHERE id=?";
            int tornilloId = 0, cantidad = 0;
            try (PreparedStatement ps = conn.prepareStatement(getQ)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tornilloId = rs.getInt("tornillo_id");
                        cantidad   = rs.getInt("cantidad");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT stock_actual FROM tornillos WHERE id=?")) {
                ps.setInt(1, tornilloId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("stock_actual") < cantidad) {
                        throw new SQLException("Stock actual (" + rs.getInt("stock_actual")
                            + ") es menor que la cantidad a revertir (" + cantidad
                            + "). No se puede eliminar la entrada.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tornillos SET stock_actual = stock_actual - ? WHERE id=?")) {
                ps.setInt(1, cantidad); ps.setInt(2, tornilloId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM entradas WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
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

    public List<Entrada> buscar(String termino, String desde, String hasta) throws SQLException {
        List<Entrada> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT e.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo, " +
            "CONCAT(u.nombre,' ',u.apellido) AS usuario_nombre " +
            "FROM entradas e " +
            "JOIN tornillos t ON e.tornillo_id=t.id " +
            "JOIN usuarios u ON e.usuario_id=u.id WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        if (termino != null && !termino.isEmpty()) {
            sb.append("AND (LOWER(e.folio) LIKE ? OR LOWER(t.nombre) LIKE ? OR LOWER(t.codigo) LIKE ?) ");
            String like = "%" + termino.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (desde != null) { 
            sb.append("AND e.fecha >= (?::date)::timestamp "); 
            params.add(desde); 
        }
        if (hasta != null) { 
            sb.append("AND e.fecha < ((?::date) + 1)::timestamp "); 
            params.add(hasta); 
        }
        sb.append("ORDER BY e.fecha DESC");
        
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

    private Entrada mapear(ResultSet rs) throws SQLException {
        Entrada e = new Entrada();
        e.setId(rs.getInt("id"));
        e.setFolio(rs.getString("folio"));
        e.setTornilloId(rs.getInt("tornillo_id"));
        e.setTornilloNombre(rs.getString("tornillo_nombre"));
        e.setTornilloCodigo(rs.getString("tornillo_codigo"));
        e.setUsuarioId(rs.getInt("usuario_id"));
        e.setUsuarioNombre(rs.getString("usuario_nombre"));
        e.setCantidad(rs.getInt("cantidad"));
        e.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        e.setTotal(rs.getBigDecimal("total"));
        e.setNumeroFactura(rs.getString("numero_factura"));
        e.setObservaciones(rs.getString("observaciones"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) {
            e.setFecha(ts.toLocalDateTime());
        }
        return e;
    }
}