package com.ejemplo.tallerpro.util;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FichaListado<T> implements VistaConBorrador {
    private final TableView<T> tabla;
    private final Node ficha;
    private final Function<T, Long> id;
    private final Consumer<T> llenar;
    private final Runnable limpiar;
    private final BooleanSupplier guardar;
    private final boolean conservarSeleccion;
    private final ProteccionBorrador<?> borrador;
    private final ReadOnlyObjectWrapper<T> registro = new ReadOnlyObjectWrapper<>();
    private boolean sincronizando;
    private Long seleccionConservada;

    public FichaListado(TableView<T> tabla, Node ficha, Function<T, Long> id, Consumer<T> llenar,
                       Runnable limpiar, Supplier<?> valores, BooleanSupplier guardar) {
        this(tabla, ficha, id, llenar, limpiar, valores, guardar, false);
    }

    public FichaListado(TableView<T> tabla, Node ficha, Function<T, Long> id, Consumer<T> llenar,
                       Runnable limpiar, Supplier<?> valores, BooleanSupplier guardar, boolean conservarSeleccion) {
        this.tabla = tabla;
        this.ficha = ficha;
        this.id = id;
        this.llenar = llenar;
        this.limpiar = limpiar;
        this.guardar = guardar;
        this.conservarSeleccion = conservarSeleccion;
        borrador = new ProteccionBorrador<>(valores);
        Ui.ocultar(ficha);
        tabla.getSelectionModel().selectedItemProperty().addListener((GGFJobs, LJOMantes, JFRPnuevo) -> {
            if (!sincronizando && JFRPnuevo != null && !esRegistro(JFRPnuevo)) {
                Platform.runLater(() -> {
                    if (tabla.getSelectionModel().getSelectedItem() != JFRPnuevo || esRegistro(JFRPnuevo)) {
                        return;
                    }
                    if (puedeSalir()) {
                        abrir(JFRPnuevo);
                    } else {
                        actualizarSeleccion();
                    }
                });
            }
        });
        if (conservarSeleccion) {
            tabla.setRowFactory(AKACvista -> {
                TableRow<T> GGFJfila = new TableRow<>();
                GGFJfila.setOnMouseClicked(LJOMe -> {
                    if (!ficha.isVisible() && !GGFJfila.isEmpty() && LJOMe.getButton() == MouseButton.PRIMARY) {
                        abrir(GGFJfila.getItem());
                    }
                });
                return GGFJfila;
            });
            tabla.addEventHandler(KeyEvent.KEY_PRESSED, JFRPe -> {
                T AKACelegido = tabla.getSelectionModel().getSelectedItem();
                if (JFRPe.getCode() == KeyCode.ENTER && !ficha.isVisible() && AKACelegido != null) {
                    abrir(AKACelegido);
                    JFRPe.consume();
                }
            });
        }
    }

    public T registro() {
        return registro.get();
    }

    public ReadOnlyObjectProperty<T> registroProperty() {
        return registro.getReadOnlyProperty();
    }

    public void abrir(T nuevo) {
        registro.set(nuevo);
        if (nuevo == null) {
            limpiar.run();
        } else {
            llenar.accept(nuevo);
        }
        borrador.marcarGuardado();
        Ui.mostrar(ficha, true);
        actualizarSeleccion();
    }

    public void cerrar() {
        registro.set(null);
        limpiar.run();
        borrador.marcarGuardado();
        Ui.ocultar(ficha);
        actualizarSeleccion();
    }

    public void cerrarSinSeleccion() {
        seleccionConservada = null;
        cerrar();
    }

    public void actualizarLista(List<T> datos) {
        sincronizando = true;
        try {
            tabla.getItems().setAll(datos);
            actualizarSeleccion();
        } finally {
            sincronizando = false;
        }
    }

    public boolean puedeSalir() {
        return !ficha.isVisible() || borrador.autorizarSalida(ficha, guardar);
    }

    @Override
    public void autorizarSalida(Runnable continuar) {
        if (puedeSalir()) {
            continuar.run();
        }
    }

    @Override
    public void alSalir() {
        cerrar();
    }

    private boolean esRegistro(T GGFJcandidato) {
        return registro() != null && Objects.equals(id.apply(registro()), id.apply(GGFJcandidato));
    }

    private void actualizarSeleccion() {
        boolean LJOManterior = sincronizando;
        sincronizando = true;
        try {
            var JFRPseleccion = tabla.getSelectionModel();
            Long AKACobjetivo = registro() == null
                    ? (ficha.isVisible() ? null : seleccionConservada)
                    : id.apply(registro());
            tabla.getItems().stream().filter(GGFJfila -> AKACobjetivo != null && AKACobjetivo.equals(id.apply(GGFJfila)))
                    .findFirst().ifPresentOrElse(LJOMfila -> {
                        if (conservarSeleccion) {
                            seleccionConservada = id.apply(LJOMfila);
                        }
                        JFRPseleccion.select(LJOMfila);
                    }, JFRPseleccion::clearSelection);
        } finally {
            sincronizando = LJOManterior;
        }
    }
}
