package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.IaNoDisponibleException;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.service.DiagnosticoLocalService;
import com.ejemplo.tallerpro.service.IaService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.SesionActual;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Asistente de IA: conversacion con preguntas de seguimiento. Si la IA no esta disponible
 * (sin clave, clave invalida, sin internet o sin saldo) responde igualmente con el diagnostico
 * basico local y explica el motivo, para que la pantalla nunca se quede sin respuesta.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DiagnosticoController {

    private static final int MAXIMO_MENSAJES = 12;

    private final IaService iaService;
    private final DiagnosticoLocalService localService;
    private final VehiculoService vehiculoService;
    private final SesionActual sesion;

    @FXML private ComboBuscable<Vehiculo> cmbVehiculo;
    @FXML private TextArea txtSintomas;
    @FXML private TextArea txtRespuesta;
    @FXML private Button btnConsultar;
    @FXML private Button btnNuevaConsulta;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblEstado;

    /** Conversacion enviada a la IA (turnos user / assistant, en orden). */
    private final List<IaService.Mensaje> historial = new ArrayList<>();

    /** Resultado de una consulta: respuesta de la IA o del modo basico (local = true). */
    private record Resultado(String texto, boolean local, String aviso) {
    }

    @FXML
    public void initialize() {
        cmbVehiculo.configurar(LJOMtexto -> vehiculoService.buscar(LJOMtexto, 0, ComboBuscable.LIMITE).getContent(),
                Ui::vehiculoEnBusqueda, Ui::vehiculoEnBusqueda);
        progreso.setVisible(false);
        progreso.managedProperty().bind(progreso.visibleProperty());
        btnNuevaConsulta.disableProperty().bind(btnConsultar.disableProperty());
        actualizarAviso();
    }

    public void recargar() {
        if (!progreso.isVisible()) {
            actualizarAviso();
        }
    }

    private void actualizarAviso() {
        if (iaService.hayClave()) {
            lblEstado.setText("Listo. Puedes hacer preguntas de seguimiento.");
        } else {
            lblEstado.setText(sesion.esAdmin()
                    ? "Modo local activo. Puedes configurar la IA en Ajustes y respaldos."
                    : "Modo local activo. La configuración de la IA corresponde a un administrador.");
        }
    }

    @FXML
    private void onConsultar() {
        if (btnConsultar.isDisabled()) {
            return;
        }
        String JFRPpregunta = txtSintomas.getText() == null ? "" : txtSintomas.getText().trim();
        if (JFRPpregunta.isEmpty()) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Falta la consulta", "Escribe tu pregunta o el síntoma del vehículo.");
            return;
        }
        Vehiculo GGFJv = cmbVehiculo.getValue();
        String LJOMdescripcionVehiculo = GGFJv == null ? "" : GGFJv.getDescripcion() + " (" + GGFJv.getKilometraje() + " km)";

        historial.add(new IaService.Mensaje("user", JFRPpregunta));
        recortarHistorial();
        List<IaService.Mensaje> JFRPcopia = List.copyOf(historial);

        agregarATranscripcion("TÚ:\n" + JFRPpregunta + "\n\n");
        txtSintomas.clear();

        // La consulta a internet corre en un hilo aparte para no congelar la ventana
        Task<Resultado> AKACtarea = new Task<>() {
            @Override
            protected Resultado call() {
                try {
                    return new Resultado(iaService.conversar(LJOMdescripcionVehiculo, JFRPcopia), false, null);
                } catch (IaNoDisponibleException GGFJe) {
                    return new Resultado(localService.diagnosticar(LJOMdescripcionVehiculo, JFRPpregunta), true, GGFJe.getMessage());
                }
            }
        };
        AKACtarea.setOnSucceeded(LJOMe -> {
            Resultado JFRPr = AKACtarea.getValue();
            if (JFRPr.local()) {
                // La respuesta local no forma parte de la conversacion con la IA
                historial.remove(historial.size() - 1);
                agregarATranscripcion("⚠ La IA no está disponible: " + JFRPr.aviso()
                        + "\n\nMODO BÁSICO (sin IA):\n" + JFRPr.texto() + "\n\n");
                terminar("Respondió el modo local. El motivo aparece en la conversación.");
            } else {
                historial.add(new IaService.Mensaje("assistant", JFRPr.texto()));
                agregarATranscripcion("TALLERPRO IA:\n" + JFRPr.texto() + "\n\n");
                terminar("Respuesta lista. Es una orientación: confírmala con una inspección. Puedes seguir preguntando.");
            }
        });
        AKACtarea.setOnFailed(AKACe -> {
            Throwable GGFJerror = AKACtarea.getException();
            historial.remove(historial.size() - 1);
            terminar("No se pudo obtener la respuesta.");
            String LJOMmensaje = GGFJerror instanceof NegocioException
                    ? GGFJerror.getMessage() : "Ocurrió un error al preparar el diagnóstico.";
            Ui.alerta(Alert.AlertType.WARNING, "Asistente de IA", LJOMmensaje);
        });

        btnConsultar.setDisable(true);
        progreso.setVisible(true);
        lblEstado.setText("Consultando…");
        Thread JFRPhilo = new Thread(AKACtarea, "diagnostico-ia");
        JFRPhilo.setDaemon(true);
        JFRPhilo.start();
    }

    @FXML
    private void onNuevaConsulta() {
        if (btnConsultar.isDisabled()) {
            return;
        }
        historial.clear();
        txtRespuesta.clear();
        txtSintomas.clear();
        txtSintomas.requestFocus();
        actualizarAviso();
    }

    @FXML
    private void onCopiar() {
        if (txtRespuesta.getText() == null || txtRespuesta.getText().isBlank()) {
            return;
        }
        ClipboardContent AKACcontenido = new ClipboardContent();
        AKACcontenido.putString(txtRespuesta.getText());
        Clipboard.getSystemClipboard().setContent(AKACcontenido);
        lblEstado.setText("Conversación copiada al portapapeles.");
    }

    /** Conserva solo los ultimos turnos, empezando siempre por un mensaje del usuario. */
    private void recortarHistorial() {
        while (historial.size() > MAXIMO_MENSAJES) {
            historial.remove(0);
        }
        while (!historial.isEmpty() && !"user".equals(historial.get(0).rol())) {
            historial.remove(0);
        }
    }

    private void agregarATranscripcion(String GGFJtexto) {
        txtRespuesta.appendText(GGFJtexto);
        txtRespuesta.positionCaret(txtRespuesta.getLength());
    }

    private void terminar(String LJOMmensaje) {
        btnConsultar.setDisable(false);
        progreso.setVisible(false);
        lblEstado.setText(LJOMmensaje);
    }
}
