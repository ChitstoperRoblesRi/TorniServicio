package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Usuario;

import java.sql.*;
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

    public boolean crear(Usuario usuario, String password) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, apellido, email, username, password_hash, rol_id, activo) " +
                "VALUES (?, ?, ?, ?, crypt(?, gen_salt('bf')), ?, ?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getUsername());
            ps.setString(5, password);
            ps.setInt(6, usuario.getRolId());
            ps.setBoolean(7, true);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nombre=?, apellido=?, email=?, rol_id=?, activo=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getEmail());
            ps.setInt(4, usuario.getRolId());
            ps.setBoolean(5, usuario.isActivo());
            ps.setInt(6, usuario.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarPassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = crypt(?, gen_salt('bf')) WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
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

    public boolean inhabilitar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean habilitar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=true WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
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
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellido(rs.getString("apellido"));
        usuario.setEmail(rs.getString("email"));
        usuario.setUsername(rs.getString("username"));
        usuario.setRol(rs.getString("rol"));
        usuario.setRolId(rs.getInt("rol_id"));
        usuario.setActivo(rs.getBoolean("activo"));
        Timestamp creadoEnTs = rs.getTimestamp("creado_en");
        if (creadoEnTs != null)
            usuario.setCreadoEn(creadoEnTs.toLocalDateTime());
        Timestamp ultimaSesionTs = rs.getTimestamp("ultima_sesion");
        if (ultimaSesionTs != null)
            usuario.setUltimaSesion(ultimaSesionTs.toLocalDateTime());
        return usuario;
    }

    public boolean eliminar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE usuarios SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

}
