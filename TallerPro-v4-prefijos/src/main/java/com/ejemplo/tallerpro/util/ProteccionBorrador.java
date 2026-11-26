package com.ejemplo.tallerpro.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ProteccionBorrador<T> {
    private final Supplier<T> leerFormulario;
    private T original;
    private boolean preguntando;

    public ProteccionBorrador(Supplier<T> leerFormulario) {
        this.leerFormulario = leerFormulario;
        marcarGuardado();
    }

    public void marcarGuardado() {
        original = leerFormulario.get();
    }

    public boolean tieneCambios() {
        return !Objects.equals(original, leerFormulario.get());
    }

    public boolean autorizarSalida(Node origen, BooleanSupplier guardar) {
        return switch (preguntar(origen)) {
            case SALIR -> true;
            case GUARDAR -> guardar.getAsBoolean();
            case SEGUIR -> false;
        };
    }

    public void autorizarSalida(Node origen, Consumer<Runnable> guardar, Runnable continuar) {
        switch (preguntar(origen)) {
            case SALIR -> continuar.run();
            case GUARDAR -> guardar.accept(continuar);
            case SEGUIR -> { }
        }
    }

    private enum Decision { SALIR, GUARDAR, SEGUIR }

    private Decision preguntar(Node LJOMorigen) {
        if (preguntando) {
            return Decision.SEGUIR;
        }
        if (!tieneCambios()) {
            return Decision.SALIR;
        }
        ButtonType JFRPguardarCambios = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
        ButtonType AKACdescartar = new ButtonType("Descartar", ButtonBar.ButtonData.NO);
        ButtonType GGFJseguir = new ButtonType("Seguir editando", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert LJOMdialogo = new Alert(Alert.AlertType.CONFIRMATION,
                "Hay cambios sin guardar en esta ficha.", JFRPguardarCambios, AKACdescartar, GGFJseguir);
        LJOMdialogo.setTitle("Cambios sin guardar");
        LJOMdialogo.setHeaderText("¿Qué quieres hacer con los cambios?");
        LJOMdialogo.initOwner(LJOMorigen.getScene().getWindow());
        LJOMdialogo.getDialogPane().getStylesheets().setAll(LJOMorigen.getScene().getStylesheets());
        preguntando = true;
        try {
            ButtonType JFRPrespuesta = LJOMdialogo.showAndWait().orElse(GGFJseguir);
            if (JFRPrespuesta == JFRPguardarCambios) {
                return Decision.GUARDAR;
            }
            return JFRPrespuesta == AKACdescartar ? Decision.SALIR : Decision.SEGUIR;
        } finally {
            preguntando = false;
        }
    }
}
