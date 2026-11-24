package com.devst.chungungo;

public class Producto {

    private int id;
    private String nombre;
    private String categoria;
    private double cantidad;
    private String unidad;
    private double stockMinimo;

    public Producto(int id, String nombre, String categoria,
                    double cantidad, String unidad, double stockMinimo) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.stockMinimo = stockMinimo;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public double getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}