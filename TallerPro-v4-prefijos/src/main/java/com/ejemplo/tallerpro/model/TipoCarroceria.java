package com.ejemplo.tallerpro.model;

public enum TipoCarroceria {
    SEDAN("Sedán"), HATCHBACK("Hatchback"), SUV("SUV"), PICKUP("Pickup"), FURGONETA("Furgoneta");

    private final String nombre;

    TipoCarroceria(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
