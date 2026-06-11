package com.tornillos.service;

import com.tornillos.dao.UsuarioDAO;
import com.tornillos.model.Usuario;

import java.sql.SQLException;
import java.util.List;

public class UsuarioService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Usuario autenticar(String username, String password) throws SQLException {
        return usuarioDAO.autenticar(username, password);
    }

    public List<Usuario> listarTodos() throws SQLException {
        return usuarioDAO.listarTodos();
    }

    public boolean crear(Usuario usuario, String password) throws SQLException {
        return usuarioDAO.crear(usuario, password);
    }

    public boolean actualizar(Usuario usuario) throws SQLException {
        return usuarioDAO.actualizar(usuario);
    }

    public boolean cambiarPassword(int userId, String newPassword) throws SQLException {
        return usuarioDAO.cambiarPassword(userId, newPassword);
    }

    public boolean habilitar(int id) throws SQLException {
        return usuarioDAO.habilitar(id);
    }

    public boolean inhabilitar(int id) throws SQLException {
        return usuarioDAO.inhabilitar(id);
    }

    public List<Usuario> buscar(String termino) throws SQLException {
        return usuarioDAO.buscar(termino);
    }
}
