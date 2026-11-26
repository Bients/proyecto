package com.ejemplo.tallerpro.exception;

/**
 * La IA no se pudo usar (sin clave, clave invalida, sin saldo, sin internet...).
 * El mensaje explica el motivo y la aplicacion responde con el diagnostico basico local.
 */
public class IaNoDisponibleException extends NegocioException {
    public IaNoDisponibleException(String motivo) {
        super(motivo);
    }
}
