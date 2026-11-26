package com.ejemplo.tallerpro.model;

public enum EstadoCompra {
    BORRADOR("Borrador"),
    CONFIRMADA("Confirmada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoCompra(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
