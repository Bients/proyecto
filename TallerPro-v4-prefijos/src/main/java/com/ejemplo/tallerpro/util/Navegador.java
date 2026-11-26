package com.ejemplo.tallerpro.util;

import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;

/** Permite a los controladores pedir el cambio entre la pantalla de acceso y la principal. */
@Component
public class Navegador {

    private Runnable alMostrarLogin = () -> { };
    private Runnable alMostrarPrincipal = () -> { };

    private Function<Class<?>, Object> fabricaControladores;

    public void configurar(Runnable login, Runnable principal, Function<Class<?>, Object> fabricaControladores) {
        this.alMostrarLogin = login;
        this.alMostrarPrincipal = principal;
        this.fabricaControladores = fabricaControladores;
    }

    public <T, C> T pedirAlta(Node origen, String titulo, String vista, Class<C> tipo,
                             BiConsumer<C, Consumer<T>> configurar) {
        Dialog<T> JFRPdialogo = new Dialog<>();
        JFRPdialogo.setTitle(titulo);
        JFRPdialogo.initOwner(origen.getScene().getWindow());
        JFRPdialogo.getDialogPane().getStylesheets().setAll(origen.getScene().getStylesheets());
        JFRPdialogo.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        JFRPdialogo.setResultConverter(AKACboton -> null);
        try {
            FXMLLoader GGFJloader = cargarVista(vista);
            JFRPdialogo.getDialogPane().setContent(GGFJloader.getRoot());
            C LJOMcontrolador = tipo.cast(GGFJloader.getController());
            configurar.accept(LJOMcontrolador, JFRPregistro -> {
                JFRPdialogo.setResult(JFRPregistro);
                JFRPdialogo.close();
            });
            if (LJOMcontrolador instanceof VistaConBorrador AKACborrador) {
                AtomicBoolean GGFJcierreAutorizado = new AtomicBoolean();
                JFRPdialogo.setOnCloseRequest(LJOMevento -> {
                    if (JFRPdialogo.getResult() != null || GGFJcierreAutorizado.get()) {
                        return;
                    }
                    LJOMevento.consume();
                    AKACborrador.autorizarSalida(() -> {
                        GGFJcierreAutorizado.set(true);
                        AKACborrador.alSalir();
                        Platform.runLater(JFRPdialogo::close);
                    });
                });
            }
            return JFRPdialogo.showAndWait().orElse(null);
        } catch (IOException JFRPe) {
            Ui.alerta(Alert.AlertType.ERROR, "No se pudo abrir el formulario", titulo);
            return null;
        }
    }

    public FXMLLoader cargarVista(String vista) throws IOException {
        URL AKACrecurso = getClass().getResource("/view/" + vista + "-view.fxml");
        if (AKACrecurso == null) {
            throw new IOException("Vista no encontrada: " + vista);
        }
        FXMLLoader GGFJloader = new FXMLLoader(AKACrecurso);
        GGFJloader.setControllerFactory(fabricaControladores::apply);
        GGFJloader.load();
        return GGFJloader;
    }

    public void mostrarLogin() {
        alMostrarLogin.run();
    }

    public void mostrarPrincipal() {
        alMostrarPrincipal.run();
    }
}
