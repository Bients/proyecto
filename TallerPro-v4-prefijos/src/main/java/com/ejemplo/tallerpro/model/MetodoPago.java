package com.ejemplo.tallerpro.model;

public enum MetodoPago {
    EFECTIVO("Efectivo"), YAPE("Yape"), PLIN("Plin"), TRANSFERENCIA("Transferencia"), TARJETA("Tarjeta");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
