package com.tornillos.service;

import com.tornillos.dao.UsuarioDAO;
import com.tornillos.model.Usuario;

import java.sql.SQLException;
import java.util.List;

public class UsuarioService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * Recupera el catálogo completo de usuarios registrados para administración.
     */
    public List<Usuario> obtenerTodosLosUsuarios() throws SQLException {
        return usuarioDAO.listarTodos();
    }

    /**
     * Intermedia el filtrado parametrizado del personal activo e inactivo.
     */
    public List<Usuario> buscarUsuarios(String termino) throws SQLException {
        if (termino == null || termino.trim().isEmpty()) {
            return obtenerTodosLosUsuarios();
        }
        return usuarioDAO.buscar(termino.trim());
    }

    /**
     * Da de alta un nuevo usuario en la base de datos delegando la encriptación hash nativa.
     */
    public void registrarUsuario(Usuario usuario, String password) throws SQLException {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para nuevos registros.");
        }
        usuarioDAO.crear(usuario, password);
    }

    /**
     * Actualiza la información de perfil relacional de un usuario.
     */
    public void actualizarUsuario(Usuario usuario) throws SQLException {
        usuarioDAO.actualizar(usuario);
    }

    /**
     * Modifica de manera segura las credenciales de acceso de un usuario.
     */
    public void redefinirPassword(int userId, String newPassword) throws SQLException {
        usuarioDAO.cambiarPassword(userId, newPassword);
    }

    /**
     * Cambia el estado operativo de un usuario en el sistema.
     */
    public void cambiarEstadoUsuario(int id, boolean habilitar) throws SQLException {
        if (habilitar) {
            usuarioDAO.habilitar(id);
        } else {
            usuarioDAO.inhabilitar(id);
        }
    }
}