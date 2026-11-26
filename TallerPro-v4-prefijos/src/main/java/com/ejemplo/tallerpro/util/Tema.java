package com.ejemplo.tallerpro.util;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

/** Cambia entre el modo claro y el oscuro y recuerda la eleccion. */
public final class Tema {

    private Tema() {
    }

    public static void aplicar() {
        String LJOMhoja = Preferencias.temaOscuro()
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet();
        Application.setUserAgentStylesheet(LJOMhoja);
    }

    /** @return true si despues del cambio queda el modo oscuro. */
    public static boolean alternar() {
        boolean JFRPoscuro = !Preferencias.temaOscuro();
        Preferencias.temaOscuro(JFRPoscuro);
        aplicar();
        return JFRPoscuro;
    }
}
