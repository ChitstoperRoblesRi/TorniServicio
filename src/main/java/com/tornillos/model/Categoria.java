package com.tornillos.model;

public class Categoria {
    private int id;
    private String nombre;
    private String descripcion;

    public Categoria() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // Al sobreescribir toString, el JComboBox de Swing mostrará el nombre automáticamente
    @Override
    public String toString() {
        return nombre;
    }
}