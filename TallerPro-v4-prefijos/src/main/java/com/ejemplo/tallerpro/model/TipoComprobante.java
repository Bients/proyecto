package com.ejemplo.tallerpro.model;

public enum TipoComprobante {
    FACTURA("Factura"),
    BOLETA("Boleta"),
    OTRO("Otro");

    private final String etiqueta;

    TipoComprobante(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
