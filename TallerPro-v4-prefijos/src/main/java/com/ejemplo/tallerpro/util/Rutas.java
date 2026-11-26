package com.ejemplo.tallerpro.util;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ubicaciones de los datos de la aplicacion (carpeta personal del usuario). */
public final class Rutas {

    public static final Path DIRECTORIO = Path.of(System.getProperty("user.home"), ".taller-pro");
    public static final Path BASE_DATOS = DIRECTORIO.resolve("tallerpro-v2.db");
    public static final Path RESPALDOS = DIRECTORIO.resolve("respaldos");

    @Configuration(proxyBeanMethods = false)
    public static class Configuracion {
        @Bean
        AlmacenFotos almacenFotos(@Value("${taller.datos.directorio}") String directorio) {
            return new AlmacenFotos(Path.of(directorio));
        }
    }

    private Rutas() {
    }
}
