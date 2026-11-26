package com.ejemplo.tallerpro.model;

public enum FaseFoto {
    INGRESO("Ingreso"), DIAGNOSTICO("Diagnóstico"), REPUESTOS("Repuestos"), ENTREGA("Entrega");

    private final String etiqueta;

    FaseFoto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
