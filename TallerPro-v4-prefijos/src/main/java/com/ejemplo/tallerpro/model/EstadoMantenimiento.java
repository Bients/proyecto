package com.ejemplo.tallerpro.model;

public enum EstadoMantenimiento {
    AL_DIA("Al día"),
    PROXIMO("Próximo"),
    VENCIDO("Vencido");

    private final String etiqueta;

    EstadoMantenimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
