package com.ejemplo.tallerpro.exception;

import java.math.BigDecimal;

public class SaldoPendienteException extends NegocioException {

    private final BigDecimal saldo;

    public SaldoPendienteException(BigDecimal saldo) {
        super("La orden tiene un saldo pendiente de S/ " + saldo.toPlainString()
                + ". Registra el pago o solicita la autorización de un administrador.");
        this.saldo = saldo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
