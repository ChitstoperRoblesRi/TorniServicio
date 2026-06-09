package com.tornillos.model;

import java.time.LocalDateTime;

public class Usuario {
    private int id;
    private String nombre, apellido, email, username, passwordHash;
    private String rol; // "GERENTE" | "EMPLEADO"
    private int rolId;
    private boolean activo;
    private LocalDateTime creadoEn, ultimaSesion;

    public Usuario() {}
    public Usuario(int id, String nombre, String apellido, String email,
                   String username, String rol, int rolId, boolean activo) {
        this.id=id; this.nombre=nombre; this.apellido=apellido;
        this.email=email; this.username=username; this.rol=rol;
        this.rolId=rolId; this.activo=activo;
    }

    public boolean isGerente() { return "GERENTE".equalsIgnoreCase(rol); }

    // Getters & Setters
    public int getId() { return id; } public void setId(int id) { this.id=id; }
    public String getNombre() { return nombre; } public void setNombre(String n) { nombre=n; }
    public String getApellido() { return apellido; } public void setApellido(String a) { apellido=a; }
    public String getNombreCompleto() { return nombre + " " + apellido; }
    public String getEmail() { return email; } public void setEmail(String e) { email=e; }
    public String getUsername() { return username; } public void setUsername(String u) { username=u; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String p) { passwordHash=p; }
    public String getRol() { return rol; } public void setRol(String r) { rol=r; }
    public int getRolId() { return rolId; } public void setRolId(int r) { rolId=r; }
    public boolean isActivo() { return activo; } public void setActivo(boolean a) { activo=a; }
    public LocalDateTime getCreadoEn() { return creadoEn; } public void setCreadoEn(LocalDateTime c) { creadoEn=c; }
    public LocalDateTime getUltimaSesion() { return ultimaSesion; } public void setUltimaSesion(LocalDateTime u) { ultimaSesion=u; }

    @Override public String toString() { return getNombreCompleto() + " (" + rol + ")"; }
}
