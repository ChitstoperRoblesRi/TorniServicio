package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Usuario;

import java.sql.*;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario autenticar(String username, String password) throws SQLException {
        String sql = "SELECT u.*, r.nombre AS rol FROM usuarios u " +
                "JOIN roles r ON u.rol_id = r.id " +
                "WHERE u.username = ? AND u.activo = true " +
                "AND u.password_hash = crypt(?, u.password_hash)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = mapear(rs);
                    actualizarUltimaSesion(u.getId());
                    return u;
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.*, r.nombre AS rol FROM usuarios u JOIN roles r ON u.rol_id=r.id ORDER BY u.nombre";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public void crear(Usuario u, String password) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, apellido, email, username, password_hash, rol_id, activo) " +
                "VALUES (?, ?, ?, ?, crypt(?, gen_salt('bf')), ?, ?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getUsername());
            ps.setString(5, password);
            ps.setInt(6, u.getRolId());
            ps.setBoolean(7, true);
            ps.executeUpdate();
        }
    }

    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuarios SET nombre=?, apellido=?, email=?, username=?, rol_id=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getUsername()); // Añadido por si también editas el username
            ps.setInt(5, u.getRolId());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        }
    }

    public void cambiarPassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = crypt(?, gen_salt('bf')) WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<Usuario> buscar(String termino) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.*, r.nombre AS rol FROM usuarios u JOIN roles r ON u.rol_id=r.id " +
                "WHERE LOWER(u.nombre) LIKE ? OR LOWER(u.apellido) LIKE ? " +
                "OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? " +
                "ORDER BY u.nombre";
        String like = "%" + termino.toLowerCase() + "%";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public void inhabilitar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void habilitar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=true WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void actualizarUltimaSesion(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET ultima_sesion=NOW() WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setEmail(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setRol(rs.getString("rol"));
        u.setRolId(rs.getInt("rol_id"));
        u.setActivo(rs.getBoolean("activo"));
        java.sql.Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null)
            u.setCreadoEn(ts.toLocalDateTime());
        java.sql.Timestamp ul = rs.getTimestamp("ultima_sesion");
        if (ul != null)
            u.setUltimaSesion(ul.toLocalDateTime());
        return u;
    }

    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

}
