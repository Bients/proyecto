package com.ejemplo.tallerpro.util;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

/**
 * Guarda el HostServices de JavaFX (que solo existe cuando la ventana ya arranco)
 * para que los controladores puedan abrir enlaces en el navegador.
 */
@Component
public class HostServicesHolder {

    private HostServices hostServices;

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void abrir(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
        }
    }
}
