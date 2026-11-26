package com.ejemplo.tallerpro.model;

public enum Rol {
    ADMIN("Administrador"),
    MECANICO("Mecánico");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
