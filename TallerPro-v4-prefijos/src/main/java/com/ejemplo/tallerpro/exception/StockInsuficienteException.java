package com.ejemplo.tallerpro.exception;

public class StockInsuficienteException extends NegocioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
