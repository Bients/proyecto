package com.ejemplo.tallerpro.model;

public enum TipoMovimiento {
    ENTRADA_COMPRA("Entrada por compra"),
    SALIDA_ORDEN("Salida por orden"),
    DEVOLUCION_ORDEN("Devolución de orden"),
    AJUSTE("Ajuste");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
