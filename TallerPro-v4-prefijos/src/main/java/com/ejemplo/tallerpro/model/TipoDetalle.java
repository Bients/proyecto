package com.ejemplo.tallerpro.model;

public enum TipoDetalle {
    SERVICIO("Servicio"),
    REPUESTO("Repuesto"),
    REPUESTO_EXTERNO("Repuesto externo");

    private final String etiqueta;

    TipoDetalle(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
