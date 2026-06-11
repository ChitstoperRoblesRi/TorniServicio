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

    public void crear(Usuario u, String password) throws SQLException {
        usuarioDAO.crear(u, password);
    }

    public void actualizar(Usuario u) throws SQLException {
        usuarioDAO.actualizar(u);
    }

    public void cambiarPassword(int userId, String newPassword) throws SQLException {
        usuarioDAO.cambiarPassword(userId, newPassword);
    }

    public void habilitar(int id) throws SQLException {
        usuarioDAO.habilitar(id);
    }

    public void inhabilitar(int id) throws SQLException {
        usuarioDAO.inhabilitar(id);
    }

    public List<Usuario> buscar(String termino) throws SQLException {
        return usuarioDAO.buscar(termino);
    }
}
