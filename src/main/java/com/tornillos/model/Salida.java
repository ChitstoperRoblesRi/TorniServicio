package com.tornillos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Salida {
    private int id;
    private String folio;
    private int tornilloId, usuarioId;
    private String tornilloNombre, tornilloCodigo, usuarioNombre;
    private int cantidad;
    private BigDecimal precioUnitario, total;
    private String motivo, cliente, observaciones;
    private LocalDateTime fecha;

    public Salida() {}

    public int getId() { return id; } public void setId(int id) { this.id=id; }
    public String getFolio() { return folio; } public void setFolio(String f) { folio=f; }
    public int getTornilloId() { return tornilloId; } public void setTornilloId(int t) { tornilloId=t; }
    public int getUsuarioId() { return usuarioId; } public void setUsuarioId(int u) { usuarioId=u; }
    public String getTornilloNombre() { return tornilloNombre; } public void setTornilloNombre(String t) { tornilloNombre=t; }
    public String getTornilloCodigo() { return tornilloCodigo; } public void setTornilloCodigo(String t) { tornilloCodigo=t; }
    public String getUsuarioNombre() { return usuarioNombre; } public void setUsuarioNombre(String u) { usuarioNombre=u; }
    public int getCantidad() { return cantidad; } public void setCantidad(int c) { cantidad=c; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; } public void setPrecioUnitario(BigDecimal p) { precioUnitario=p; }
    public BigDecimal getTotal() { return total; } public void setTotal(BigDecimal t) { total=t; }
    public String getMotivo() { return motivo; } public void setMotivo(String m) { motivo=m; }
    public String getCliente() { return cliente; } public void setCliente(String c) { cliente=c; }
    public String getObservaciones() { return observaciones; } public void setObservaciones(String o) { observaciones=o; }
    public LocalDateTime getFecha() { return fecha; } public void setFecha(LocalDateTime f) { fecha=f; }
}
