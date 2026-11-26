package com.ejemplo.tallerpro.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Preferencias locales del usuario (tema, clave de la IA, restauracion pendiente). */
public final class Preferencias {

    private static final Preferences PREFS = Preferences.userRoot().node("tallerpro");

    private Preferencias() {
    }

    public static boolean temaOscuro() {
        return PREFS.getBoolean("tema.oscuro", false);
    }

    public static void temaOscuro(boolean oscuro) {
        PREFS.putBoolean("tema.oscuro", oscuro);
    }

    public static boolean guiaPresentada(String usuario) {
        return PREFS.getBoolean("guia.presentada." + usuario, false);
    }

    public static void marcarGuiaPresentada(String usuario) {
        PREFS.putBoolean("guia.presentada." + usuario, true);
    }

    public static String apiKey() {
        return PREFS.get("ia.apikey", "");
    }

    public static void apiKey(String clave) {
        if (clave == null || clave.isBlank()) {
            PREFS.remove("ia.apikey");
        } else {
            PREFS.put("ia.apikey", clave.trim());
        }
    }

    public static String tokenDni() {
        return PREFS.get("dni.decolecta.token", "");
    }

    public static void tokenDni(String token) throws BackingStoreException {
        if (token.isBlank()) {
            PREFS.remove("dni.decolecta.token");
        } else {
            PREFS.put("dni.decolecta.token", token);
        }
        PREFS.flush();
    }

    public static String restaurarPendiente() {
        return PREFS.get("restaurar.pendiente", "");
    }

    public static void restaurarPendiente(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            PREFS.remove("restaurar.pendiente");
        } else {
            PREFS.put("restaurar.pendiente", ruta);
        }
    }
}
