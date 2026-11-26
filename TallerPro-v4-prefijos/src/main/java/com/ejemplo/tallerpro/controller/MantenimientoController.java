package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.ProgramaMantenimiento;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.service.MantenimientoService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.util.HostServicesHolder;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.FichaListado;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import javafx.scene.Node;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

/** Recordatorios de mantenimiento: avisa que clientes deben volver al taller. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MantenimientoController implements VistaConBorrador {

    private final MantenimientoService service;
    private final VehiculoService vehiculoService;
    private final HostServicesHolder host;

    @FXML private ComboBuscable<Vehiculo> cmbVehiculo;
    @FXML private TextField txtServicio;
    @FXML private TextField txtCadaKm;
    @FXML private TextField txtCadaMeses;
    @FXML private TextField txtUltimoKm;
    @FXML private DatePicker dpUltimaFecha;
    @FXML private CheckBox chkPendientes;
    @FXML private TextField txtBuscar;

    @FXML private TableView<ProgramaMantenimiento> tabla;
    @FXML private TableColumn<ProgramaMantenimiento, String> colPlaca;
    @FXML private TableColumn<ProgramaMantenimiento, String> colCliente;
    @FXML private TableColumn<ProgramaMantenimiento, String> colServicio;
    @FXML private TableColumn<ProgramaMantenimiento, String> colPlan;
    @FXML private TableColumn<ProgramaMantenimiento, String> colUltimo;
    @FXML private TableColumn<ProgramaMantenimiento, String> colRestante;
    @FXML private TableColumn<ProgramaMantenimiento, String> colEstadoMant;

    @FXML private Label lblEstado;

    @FXML private Node ficha;
    @FXML private Node btnEliminar;
    @FXML private Node btnRealizado;
    @FXML private Node btnRecordar;
    private FichaListado<ProgramaMantenimiento> fichas;
    private Long idEdicion;

    @FXML private PaginadorBusqueda<ProgramaMantenimiento> paginador;

    @FXML
    public void initialize() {
        colPlaca.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().getVehiculo().getPlaca()));
        colCliente.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(JFRPc.getValue().getVehiculo().getCliente().getNombreCompleto()));
        colServicio.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getServicio()));
        colPlan.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().descripcionPlan()));
        colUltimo.setCellValueFactory(LJOMc -> new SimpleStringProperty(
                LJOMc.getValue().getUltimoKm() + " km · " + LJOMc.getValue().getUltimaFecha()));
        colRestante.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(JFRPc.getValue().resumenRestante(LocalDate.now())));
        colCliente.setCellFactory(AKACc -> Ui.celdaTextoAjustado());
        colServicio.setCellFactory(GGFJc -> Ui.celdaTextoAjustado());
        colPlan.setCellFactory(LJOMc -> Ui.celdaTextoAjustado());
        colUltimo.setCellFactory(JFRPc -> Ui.celdaTextoAjustado());
        colRestante.setCellFactory(AKACc -> Ui.celdaTextoAjustado());
        colEstadoMant.setCellValueFactory(GGFJc ->
                new SimpleStringProperty(String.valueOf(GGFJc.getValue().calcularEstado())));
        colEstadoMant.setCellFactory(Ui.celdaInsignia());

        cmbVehiculo.configurar(LJOMtexto -> vehiculoService.buscar(LJOMtexto, 0, ComboBuscable.LIMITE).getContent(),
                Ui::vehiculoEnBusqueda, Ui::vehiculoEnBusqueda);
        // En un registro nuevo, el kilometraje del ultimo servicio se sugiere con el actual del vehiculo
        cmbVehiculo.valueProperty().addListener((JFRPobs, AKACanterior, GGFJv) -> {
            if (idEdicion == null && GGFJv != null && txtUltimoKm.getText().isBlank()) {
                txtUltimoKm.setText(String.valueOf(GGFJv.getKilometraje()));
            }
        });
        dpUltimaFecha.setValue(LocalDate.now());

        fichas = new FichaListado<>(tabla, ficha, ProgramaMantenimiento::getId, this::mostrarEnFormulario,
                this::limpiarFormulario, this::valoresFormulario, this::guardarFormulario);
        btnEliminar.disableProperty().bind(fichas.registroProperty().isNull());
        btnRealizado.disableProperty().bind(fichas.registroProperty().isNull());
        btnRecordar.disableProperty().bind(fichas.registroProperty().isNull());
        tabla.setPlaceholder(new Label("Sin mantenimientos para esta búsqueda."));
        paginador.configurar(txtBuscar, LJOMpagina -> {
            String JFRPfiltro = txtBuscar.getText();
            boolean AKACpendientes = chkPendientes.isSelected();
            return () -> service.buscar(JFRPfiltro, AKACpendientes, LJOMpagina, PaginadorBusqueda.LIMITE);
        }, GGFJresultados -> {
            fichas.actualizarLista(GGFJresultados.getContent());
            lblEstado.setText(GGFJresultados.getTotalElements() + " mantenimiento(s) programado(s).");
        }, lblEstado::setText);
        chkPendientes.selectedProperty().addListener((LJOMobs, JFRPa, AKACb) -> paginador.reiniciar());
    }

    public void recargar() {
        paginador.recargar();
    }

    @FXML
    private void onNuevo() {
        if (!fichas.puedeSalir()) {
            return;
        }
        fichas.abrir(null);
        lblEstado.setText("Formulario limpio: programa un nuevo mantenimiento.");
    }

    @FXML
    private void onGuardar() {
        guardarFormulario();
    }

    private boolean guardarFormulario() {
        Integer GGFJcadaKm;
        Integer LJOMcadaMeses;
        Integer JFRPultimoKm;
        try {
            GGFJcadaKm = txtCadaKm.getText().isBlank() ? 0 : Integer.valueOf(txtCadaKm.getText().trim());
            LJOMcadaMeses = txtCadaMeses.getText().isBlank() ? 0 : Integer.valueOf(txtCadaMeses.getText().trim());
            JFRPultimoKm = txtUltimoKm.getText().isBlank() ? null : Integer.valueOf(txtUltimoKm.getText().trim());
        } catch (NumberFormatException AKACe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", "Los km y los meses deben ser números enteros.");
            return false;
        }
        ProgramaMantenimiento GGFJp = new ProgramaMantenimiento();
        GGFJp.setId(idEdicion);
        GGFJp.setVehiculo(cmbVehiculo.getValue());
        GGFJp.setServicio(txtServicio.getText().trim());
        GGFJp.setCadaKm(GGFJcadaKm);
        GGFJp.setCadaMeses(LJOMcadaMeses);
        GGFJp.setUltimoKm(JFRPultimoKm);
        GGFJp.setUltimaFecha(dpUltimaFecha.getValue());
        try {
            boolean LJOMesNuevo = GGFJp.getId() == null;
            fichas.abrir(service.guardar(GGFJp));
            recargar();
            lblEstado.setText(LJOMesNuevo ? "Mantenimiento programado." : "Mantenimiento actualizado.");
            return true;
        } catch (ConstraintViolationException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(JFRPe));
        } catch (NegocioException AKACe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo guardar", AKACe.getMessage());
        } catch (Exception GGFJe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(GGFJe.getMessage()));
        }
        return false;
    }

    @FXML
    private void onRealizado() {
        ProgramaMantenimiento LJOMseleccionado = fichas.registro();
        if (LJOMseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un mantenimiento de la tabla para marcarlo como realizado.");
            return;
        }
        if (!fichas.puedeSalir()) {
            return;
        }
        try {
            service.marcarRealizado(LJOMseleccionado.getId());
            fichas.cerrar();
            recargar();
            lblEstado.setText("Marcado como realizado hoy, con el kilometraje actual del vehículo.");
        } catch (NegocioException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo actualizar", JFRPe.getMessage());
        }
    }

    @FXML
    private void onEliminar() {
        ProgramaMantenimiento AKACseleccionado = fichas.registro();
        if (AKACseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un mantenimiento de la tabla para eliminar.");
            return;
        }
        if (!Ui.confirmar("Eliminar mantenimiento", "Se eliminará \"" + AKACseleccionado.getServicio() + "\".")) {
            return;
        }
        try {
            service.eliminar(AKACseleccionado.getId());
            fichas.cerrar();
            recargar();
            lblEstado.setText("Mantenimiento eliminado.");
        } catch (NegocioException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo eliminar", GGFJe.getMessage());
        }
    }

    @FXML
    private void onWhatsApp() {
        ProgramaMantenimiento LJOMseleccionado = fichas.registro();
        if (LJOMseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un mantenimiento para recordarle al cliente.");
            return;
        }
        Cliente JFRPcliente = LJOMseleccionado.getVehiculo().getCliente();
        String AKACtelefono = Ui.texto(JFRPcliente.getTelefono());
        String GGFJmensaje = String.format(Locale.US,
                "Hola %s, le recordamos que su vehículo %s tiene pendiente el servicio \"%s\" (%s). "
                        + "Escribanos para agendar su cita. Gracias.",
                JFRPcliente.getNombre(), LJOMseleccionado.getVehiculo().getPlaca(), LJOMseleccionado.getServicio(),
                LJOMseleccionado.resumenRestante(LocalDate.now()));
        Ui.abrirWhatsApp(host, AKACtelefono, GGFJmensaje);
    }

    private void mostrarEnFormulario(ProgramaMantenimiento LJOMp) {
        if (LJOMp == null) {
            return;
        }
        idEdicion = LJOMp.getId();
        cmbVehiculo.setValue(LJOMp.getVehiculo());
        txtServicio.setText(LJOMp.getServicio());
        txtCadaKm.setText(String.valueOf(LJOMp.getCadaKm()));
        txtCadaMeses.setText(String.valueOf(LJOMp.getCadaMeses()));
        txtUltimoKm.setText(String.valueOf(LJOMp.getUltimoKm()));
        dpUltimaFecha.setValue(LJOMp.getUltimaFecha());
    }

    private void limpiarFormulario() {
        idEdicion = null;
        cmbVehiculo.setValue(null);
        txtServicio.clear();
        txtCadaKm.clear();
        txtCadaMeses.clear();
        txtUltimoKm.clear();
        dpUltimaFecha.setValue(LocalDate.now());
    }

    private java.util.List<?> valoresFormulario() {
        Vehiculo JFRPvehiculo = cmbVehiculo.getValue();
        return java.util.Arrays.asList(JFRPvehiculo == null ? null : JFRPvehiculo.getId(), cmbVehiculo.tieneTextoSinSeleccion(),
                txtServicio.getText().strip(), txtCadaKm.getText().strip(), txtCadaMeses.getText().strip(),
                txtUltimoKm.getText().strip(), dpUltimaFecha.getValue());
    }

    @Override
    public void autorizarSalida(Runnable continuar) {
        fichas.autorizarSalida(continuar);
    }

    @Override
    public void alSalir() {
        fichas.alSalir();
    }
}
