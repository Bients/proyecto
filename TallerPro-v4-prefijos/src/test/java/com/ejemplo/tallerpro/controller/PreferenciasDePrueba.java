package com.ejemplo.tallerpro.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public final class PreferenciasDePrueba implements PreferencesFactory {
    private final Preferences usuario = new Nodo(null, "");
    private final Preferences sistema = new Nodo(null, "");

    @Override public Preferences userRoot() { return usuario; }
    @Override public Preferences systemRoot() { return sistema; }

    private static final class Nodo extends AbstractPreferences {
        private final Map<String, String> valores = new HashMap<>();
        private final Map<String, Nodo> hijos = new HashMap<>();

        Nodo(Nodo padre, String nombre) {
            super(padre, nombre);
        }

        @Override protected void putSpi(String clave, String valor) { valores.put(clave, valor); }
        @Override protected String getSpi(String clave) { return valores.get(clave); }
        @Override protected void removeSpi(String clave) { valores.remove(clave); }
        @Override protected void removeNodeSpi() { valores.clear(); hijos.clear(); }
        @Override protected String[] keysSpi() { return valores.keySet().toArray(String[]::new); }
        @Override protected String[] childrenNamesSpi() { return hijos.keySet().toArray(String[]::new); }
        @Override protected AbstractPreferences childSpi(String nombre) {
            return hijos.computeIfAbsent(nombre, GGFJclave -> new Nodo(this, GGFJclave));
        }
        @Override protected void syncSpi() {}
        @Override protected void flushSpi() {}
    }
}
