package com.tornillos.model;

import java.time.LocalDateTime;

public class MovimientoInventario {
    private LocalDateTime fecha;
    private String tornilloNombre;
    private String tornilloCodigo;
    private String tipoMovimiento;
    private int cantidad;
    private int stockResultante;
    private String usuarioNombre;

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime f) { fecha = f; }
    public String getTornilloNombre() { return tornilloNombre; }
    public void setTornilloNombre(String n) { tornilloNombre = n; }
    public String getTornilloCodigo() { return tornilloCodigo; }
    public void setTornilloCodigo(String c) { tornilloCodigo = c; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String t) { tipoMovimiento = t; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int c) { cantidad = c; }
    public int getStockResultante() { return stockResultante; }
    public void setStockResultante(int s) { stockResultante = s; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String u) { usuarioNombre = u; }
}
