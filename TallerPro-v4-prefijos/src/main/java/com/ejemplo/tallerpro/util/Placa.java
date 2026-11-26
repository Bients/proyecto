package com.ejemplo.tallerpro.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Placa {

    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\p{Z}-]+");

    private Placa() {
    }

    public static String normalizar(String valor) {
        return valor == null ? "" : SEPARADORES.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
    }
}
