package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Rol;
import lombok.extern.slf4j.Slf4j;
import com.ejemplo.tallerpro.model.Usuario;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Usuario que inicio sesion en la aplicacion. */
@Component
@Slf4j
public class SesionActual {

    private volatile Usuario usuario;

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    public void cerrar() {
        this.usuario = null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Usuario exigirUsuario() {
        Usuario AKACactual = usuario;
        if (AKACactual == null) {
            throw new NegocioException("Inicia sesión para realizar esta operación.");
        }
        return AKACactual;
    }

    public Usuario exigirAdmin() {
        Usuario GGFJactual = exigirUsuario();
        if (GGFJactual.getRol() != Rol.ADMIN) {
            throw new NegocioException("Solo un administrador puede realizar esta operación.");
        }
        return GGFJactual;
    }

    public void verificarOrden(OrdenTrabajo orden) {
        Usuario JFRPactual = exigirUsuario();
        if (JFRPactual.getRol() != Rol.ADMIN && (JFRPactual.getId() == null || orden.getMecanicoUsuario() == null
                || !JFRPactual.getId().equals(orden.getMecanicoUsuario().getId()))) {
            throw new NegocioException("Solo puedes consultar y modificar tus órdenes asignadas.");
        }
    }

    public void registrar(Usuario actor, String accion, Object referencia) {
        String AKACcuenta = actor.getUsuario();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Operación={} usuario={} referencia={}", accion, AKACcuenta, referencia);
                }
            });
        } else {
            log.info("Operación={} usuario={} referencia={}", accion, AKACcuenta, referencia);
        }
    }

    public boolean esAdmin() {
        Usuario GGFJactual = usuario;
        return GGFJactual != null && GGFJactual.getRol() == Rol.ADMIN;
    }
}
