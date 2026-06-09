package com.tornillos.service;

import com.tornillos.model.Usuario;

public class SessionManager {
    private static SessionManager instance;
    private Usuario usuarioActual;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void iniciarSesion(Usuario u) { this.usuarioActual = u; }
    public void cerrarSesion() { this.usuarioActual = null; }
    public Usuario getUsuarioActual() { return usuarioActual; }
    public boolean isLoggedIn() { return usuarioActual != null; }
    public boolean isGerente() { return isLoggedIn() && usuarioActual.isGerente(); }
}
