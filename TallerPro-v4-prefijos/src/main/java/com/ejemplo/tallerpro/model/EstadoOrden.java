package com.ejemplo.tallerpro.model;

public enum EstadoOrden {
    RECIBIDO("Recibido"),
    DIAGNOSTICO("En diagnóstico"),
    REPARACION("En reparación"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Se usa como texto en combos y tablas; en la base se guarda el name(). */
    @Override
    public String toString() {
        return etiqueta;
    }
}
