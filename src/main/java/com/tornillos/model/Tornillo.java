package com.tornillos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tornillo {
    private int id;
    private String codigo, nombre, descripcion;
    private String material, cabezaTipo, unidadMedida, ubicacion;
    private BigDecimal diametroMm, longitudMm, pasoRosca;
    private BigDecimal precioCosto, precioVenta;
    private int stockActual, stockMinimo, stockMaximo;
    private boolean activo;
    private LocalDateTime creadoEn, actualizadoEn;

    public Tornillo() {
    }

    public Tornillo(String codigo, String nombre, String descripcion, String material,
                    String cabezaTipo, String unidadMedida, String ubicacion,
                    BigDecimal diametroMm, BigDecimal longitudMm, BigDecimal pasoRosca,
                    BigDecimal precioCosto, BigDecimal precioVenta,
                    int stockActual, int stockMinimo, int stockMaximo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.material = material;
        this.cabezaTipo = cabezaTipo;
        this.unidadMedida = unidadMedida;
        this.ubicacion = ubicacion;
        this.diametroMm = diametroMm;
        this.longitudMm = longitudMm;
        this.pasoRosca = pasoRosca;
        this.precioCosto = precioCosto;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.stockMaximo = stockMaximo;
    }

    public String getEstadoStock() {
        if (stockActual == 0)
            return "SIN_STOCK";
        else if (stockActual <= stockMinimo / 2)
            return "CRÍTICO";
        else if (stockActual <= stockMinimo)
            return "BAJO";
        else
            return "NORMAL";
    }

    public boolean necesitaAlerta() {
        return stockActual <= stockMinimo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String c) {
        codigo = c;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String n) {
        nombre = n;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String d) {
        descripcion = d;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String m) {
        material = m;
    }

    public String getCabezaTipo() {
        return cabezaTipo;
    }

    public void setCabezaTipo(String c) {
        cabezaTipo = c;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String u) {
        unidadMedida = u;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String u) {
        ubicacion = u;
    }

    public BigDecimal getDiametroMm() {
        return diametroMm;
    }

    public void setDiametroMm(BigDecimal d) {
        diametroMm = d;
    }

    public BigDecimal getLongitudMm() {
        return longitudMm;
    }

    public void setLongitudMm(BigDecimal l) {
        longitudMm = l;
    }

    public BigDecimal getPasoRosca() {
        return pasoRosca;
    }

    public void setPasoRosca(BigDecimal p) {
        pasoRosca = p;
    }

    public BigDecimal getPrecioCosto() {
        return precioCosto;
    }

    public void setPrecioCosto(BigDecimal p) {
        precioCosto = p;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal p) {
        precioVenta = p;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int s) {
        stockActual = s;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int s) {
        stockMinimo = s;
    }

    public int getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(int s) {
        stockMaximo = s;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean a) {
        activo = a;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime c) {
        creadoEn = c;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime a) {
        actualizadoEn = a;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
