package com.tornillos.model;

import java.time.LocalDateTime;

public class Alerta {
    public enum Tipo { STOCK_BAJO, STOCK_CRITICO, SIN_STOCK }

    private int id, tornilloId;
    private String tornilloNombre, tornilloCodigo, tipo, mensaje;
    private boolean enviadaEmail;
    private LocalDateTime creadaEn;

    public Alerta() {}
    public Alerta(int tornilloId, String tornilloNombre, String tipo, String mensaje) {
        this.tornilloId=tornilloId; this.tornilloNombre=tornilloNombre;
        this.tipo=tipo; this.mensaje=mensaje;
    }

    public int getId() { return id; } public void setId(int id) { this.id=id; }
    public int getTornilloId() { return tornilloId; } public void setTornilloId(int t) { tornilloId=t; }
    public String getTornilloNombre() { return tornilloNombre; } public void setTornilloNombre(String t) { tornilloNombre=t; }
    public String getTornilloCodigo() { return tornilloCodigo; } public void setTornilloCodigo(String t) { tornilloCodigo=t; }
    public String getTipo() { return tipo; } public void setTipo(String t) { tipo=t; }
    public String getMensaje() { return mensaje; } public void setMensaje(String m) { mensaje=m; }
    public boolean isEnviadaEmail() { return enviadaEmail; } public void setEnviadaEmail(boolean e) { enviadaEmail=e; }
    public LocalDateTime getCreadaEn() { return creadaEn; } public void setCreadaEn(LocalDateTime c) { creadaEn=c; }
}
