package com.ejemplo.tallerpro.model;

public enum EstadoPago {
    SIN_PAGAR("Sin pagar"), PARCIAL("Parcial"), PAGADO("Pagado");

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
