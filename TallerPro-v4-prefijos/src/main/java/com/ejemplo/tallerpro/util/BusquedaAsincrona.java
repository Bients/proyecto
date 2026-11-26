package com.ejemplo.tallerpro.util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Entrega únicamente el resultado de la consulta vigente al hilo de JavaFX. */
public final class BusquedaAsincrona<T> {

    private static final ExecutorService EJECUTOR = Executors.newFixedThreadPool(2, AKACtrabajo -> {
        Thread GGFJhilo = new Thread(AKACtrabajo, "busqueda-tallerpro");
        GGFJhilo.setDaemon(true);
        return GGFJhilo;
    });

    private final PauseTransition espera = new PauseTransition(Duration.millis(250));
    private Future<?> pendiente;
    private long version;

    public void consultar(boolean diferida, Supplier<T> consulta, Consumer<T> resultado, Consumer<RuntimeException> error) {
        cancelar();
        long LJOMturno = version;
        Runnable JFRPejecutar = () -> pendiente = EJECUTOR.submit(() -> {
            try {
                T AKACdatos = consulta.get();
                Platform.runLater(() -> {
                    if (LJOMturno == version) {
                        resultado.accept(AKACdatos);
                    }
                });
            } catch (RuntimeException GGFJe) {
                Platform.runLater(() -> {
                    if (LJOMturno == version) {
                        error.accept(GGFJe);
                    }
                });
            }
        });
        if (diferida) {
            espera.setOnFinished(LJOMe -> JFRPejecutar.run());
            espera.playFromStart();
        } else {
            JFRPejecutar.run();
        }
    }

    public void cancelar() {
        version++;
        espera.stop();
        if (pendiente != null) {
            pendiente.cancel(true);
            pendiente = null;
        }
    }
}
