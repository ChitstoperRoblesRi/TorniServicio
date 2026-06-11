package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Alerta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertaDAO {

    public boolean crear(Alerta alerta) throws SQLException {
        String check = "SELECT id FROM alertas WHERE tornillo_id=? AND tipo=? LIMIT 1";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(check)) {
            ps.setInt(1, alerta.getTornilloId());
            ps.setString(2, alerta.getTipo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id");
                    String update = "UPDATE alertas SET mensaje=?, creada_en=NOW() WHERE id=?";
                    try (PreparedStatement ps2 = DatabaseConfig.getConnection().prepareStatement(update)) {
                        ps2.setString(1, alerta.getMensaje());
                        ps2.setInt(2, existingId);
                        ps2.executeUpdate();
                    }
                    alerta.setId(existingId);
                    return true;
                }
            }
        }
        String sql = "INSERT INTO alertas (tornillo_id, tipo, mensaje) VALUES (?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, alerta.getTornilloId());
            ps.setString(2, alerta.getTipo());
            ps.setString(3, alerta.getMensaje());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) alerta.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public List<Alerta> listarActivas() throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT ON (a.tornillo_id) a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo "
                +
                "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                "WHERE t.activo=true AND t.stock_actual <= t.stock_minimo " +
                "ORDER BY a.tornillo_id, a.creada_en DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public int contarActivas() throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(
                    "SELECT COUNT(DISTINCT a.tornillo_id) FROM alertas a " +
                    "JOIN tornillos t ON a.tornillo_id=t.id " +
                    "WHERE t.activo=true AND t.stock_actual <= t.stock_minimo")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("DELETE FROM alertas WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminarTodas() throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM alertas");
            return true;
        }
    }

    public List<Alerta> listarHistorial() throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                "ORDER BY a.creada_en DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Alerta> buscar(String termino) throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT ON (a.tornillo_id) a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo "
                +
                "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                "WHERE t.activo=true AND t.stock_actual <= t.stock_minimo " +
                "AND (LOWER(t.nombre) LIKE ? OR LOWER(a.tipo) LIKE ? OR LOWER(a.mensaje) LIKE ?) " +
                "ORDER BY a.tornillo_id, a.creada_en DESC";
        String like = "%" + termino.toLowerCase() + "%";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean marcarEnviadaEmail(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE alertas SET enviada_email=true WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Alerta> listarNoEnviadas() throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                "WHERE a.enviada_email=false ORDER BY a.creada_en DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public Alerta buscarNoEnviada(int tornilloId, String tipo) throws SQLException {
        String sql = "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                "WHERE a.tornillo_id=? AND a.tipo=? AND a.enviada_email=false " +
                "LIMIT 1";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tornilloId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapear(rs);
            }
        }
        return null;
    }

    private Alerta mapear(ResultSet rs) throws SQLException {
        Alerta alerta = new Alerta();
        alerta.setId(rs.getInt("id"));
        alerta.setTornilloId(rs.getInt("tornillo_id"));
        alerta.setTornilloNombre(rs.getString("tornillo_nombre"));
        alerta.setTornilloCodigo(rs.getString("tornillo_codigo"));
        alerta.setTipo(rs.getString("tipo"));
        alerta.setMensaje(rs.getString("mensaje"));
        alerta.setEnviadaEmail(rs.getBoolean("enviada_email"));
        Timestamp creadaEnTs = rs.getTimestamp("creada_en");
        if (creadaEnTs != null)
            alerta.setCreadaEn(creadaEnTs.toLocalDateTime());
        return alerta;
    }

}
