package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.util.Preferencias;
import com.ejemplo.tallerpro.util.Ui;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
@Scope("prototype")
public class GuiaController {
    private static final String DESTACADO = "guia-destacado";

    @FXML private VBox barraGuia;
    @FXML private Label lblPasoGuia;
    @FXML private Label lblMensajeGuia;
    @FXML private FlowPane opcionesIniciales;
    @FXML private FlowPane controlesGuia;
    @FXML private Button btnAtrasGuia;
    @FXML private Button btnSiguienteGuia;

    private final List<Paso> pasos = new ArrayList<>();
    private StackPane contenido;
    private Supplier<Button> pantallaActual;
    private Node destacado;
    private String usuarioConsultado;
    private int posicion = -1;
    private int solicitado = -1;

    private record Paso(Button pantalla, String objetivo, String titulo, String mensaje,
                        String accion, Button destino) {
    }

    void configurar(StackPane contenido, Supplier<Button> pantallaActual) {
        this.contenido = contenido;
        this.pantallaActual = pantallaActual;
    }

    void agregarPaso(Button pantalla, String objetivo, String titulo, String mensaje) {
        pasos.add(new Paso(pantalla, objetivo, titulo, mensaje, null, null));
    }

    void agregarNavegacion(Button pantalla, String objetivo, String titulo, String mensaje,
                           String accion, Button destino) {
        pasos.add(new Paso(pantalla, objetivo, titulo, mensaje, accion, destino));
    }

    void ofrecer(String usuario) {
        if (posicion >= 0 || usuarioConsultado != null || usuario == null || Preferencias.guiaPresentada(usuario)) {
            return;
        }
        usuarioConsultado = usuario;
        lblPasoGuia.setText("¿Ya has usado TallerPro?");
        lblMensajeGuia.setText("La guía te muestra dónde hacer clic en las pantallas del taller.");
        Ui.mostrar(opcionesIniciales, true);
        Ui.mostrar(controlesGuia, false);
        Ui.mostrar(barraGuia, true);
    }

    void iniciar(String usuario) {
        if (posicion >= 0) {
            btnSiguienteGuia.requestFocus();
            return;
        }
        if (usuario != null) {
            Preferencias.marcarGuiaPresentada(usuario);
        }
        usuarioConsultado = null;
        posicion = 0;
        Ui.mostrar(opcionesIniciales, false);
        Ui.mostrar(controlesGuia, true);
        Ui.mostrar(barraGuia, true);
        mostrarPaso();
    }

    void pantallaMostrada(Button pantalla) {
        if (posicion < 0) {
            return;
        }
        if (solicitado >= 0 && pasos.get(solicitado).pantalla() == pantalla) {
            posicion = solicitado;
        } else if (pasos.get(posicion).destino() == pantalla) {
            posicion++;
        }
        solicitado = -1;
        mostrarPaso();
    }

    @FXML
    private void onEmpezarGuia() {
        iniciar(usuarioConsultado);
    }

    @FXML
    private void onOmitirGuia() {
        if (usuarioConsultado != null) {
            Preferencias.marcarGuiaPresentada(usuarioConsultado);
            usuarioConsultado = null;
        }
        Ui.ocultar(barraGuia);
    }

    @FXML
    private void onSiguienteGuia() {
        Paso JFRPpaso = pasos.get(posicion);
        if (pantallaActual.get() != JFRPpaso.pantalla()) {
            irAlPaso(posicion);
        } else if (posicion == pasos.size() - 1) {
            onSalirGuia();
        } else if (JFRPpaso.destino() != null && objetivo(JFRPpaso) instanceof Button AKACboton && !AKACboton.isDisabled()) {
            AKACboton.fire();
        } else {
            irAlPaso(posicion + 1);
        }
    }

    @FXML
    private void onAtrasGuia() {
        irAlPaso(posicion - 1);
    }

    @FXML
    private void onSalirGuia() {
        posicion = -1;
        solicitado = -1;
        quitarMarca();
        Ui.ocultar(barraGuia);
    }

    private void irAlPaso(int GGFJindice) {
        Button LJOMdestino = pasos.get(GGFJindice).pantalla();
        if (pantallaActual.get() != LJOMdestino) {
            solicitado = GGFJindice;
            try {
                LJOMdestino.fire();
            } finally {
                solicitado = -1;
            }
            return;
        }
        solicitado = -1;
        posicion = GGFJindice;
        mostrarPaso();
    }

    private void mostrarPaso() {
        quitarMarca();
        Paso JFRPpaso = pasos.get(posicion);
        lblPasoGuia.setText("Paso " + (posicion + 1) + " de " + pasos.size() + " · " + JFRPpaso.titulo());
        btnAtrasGuia.setDisable(posicion == 0);
        if (pantallaActual.get() != JFRPpaso.pantalla()) {
            lblMensajeGuia.setText("Para continuar, vuelve a " + JFRPpaso.pantalla().getText() + ".");
            btnSiguienteGuia.setText("Volver al paso");
            return;
        }
        lblMensajeGuia.setText(JFRPpaso.mensaje());
        btnSiguienteGuia.setText(JFRPpaso.accion() != null ? JFRPpaso.accion()
                : posicion == pasos.size() - 1 ? "Terminar" : "Siguiente");
        destacado = objetivo(JFRPpaso);
        if (destacado != null) {
            destacado.getStyleClass().add(DESTACADO);
        }
    }

    private Node objetivo(Paso AKACpaso) {
        if (AKACpaso.destino() != null && AKACpaso.objetivo().equals("#" + AKACpaso.destino().getId())) {
            return AKACpaso.destino();
        }
        return contenido.lookup(AKACpaso.objetivo());
    }

    private void quitarMarca() {
        if (destacado != null) {
            destacado.getStyleClass().remove(DESTACADO);
            destacado = null;
        }
    }
}
