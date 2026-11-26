package com.ejemplo.tallerpro.model;

public enum OrigenMovimiento {
    SALDO_INICIAL("Saldo inicial"),
    COMPRA("Compra"),
    ORDEN("Orden"),
    AJUSTE_MANUAL("Ajuste manual");

    private final String etiqueta;

    OrigenMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
