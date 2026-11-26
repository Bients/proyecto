package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.service.ClienteService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.FichaListado;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class VehiculoController implements VistaConBorrador {

    private final VehiculoService service;
    private final ClienteService clienteService;
    private final Navegador navegador;
    private final SesionActual sesion;

    @FXML private Node ficha;
    private FichaListado<Vehiculo> fichas;
    @FXML private TextField txtPlaca;
    @FXML private TextField txtMarca;
    @FXML private TextField txtModelo;
    @FXML private TextField txtAnio;
    @FXML private TextField txtKm;
    @FXML private ComboBuscable<Cliente> cmbCliente;
    @FXML private javafx.scene.control.ComboBox<com.ejemplo.tallerpro.model.TipoCarroceria> cmbCarroceria;
    @FXML private TextField txtBuscar;
    @FXML private PaginadorBusqueda<Vehiculo> paginador;
    @FXML private Node listado;
    @FXML private Node btnNuevo;
    @FXML private Node btnEliminar;

    private Consumer<Vehiculo> alCrear;

    @FXML private TableView<Vehiculo> tabla;
    @FXML private TableColumn<Vehiculo, String> colPlaca;
    @FXML private TableColumn<Vehiculo, String> colMarca;
    @FXML private TableColumn<Vehiculo, String> colModelo;
    @FXML private TableColumn<Vehiculo, String> colAnio;
    @FXML private TableColumn<Vehiculo, String> colKm;
    @FXML private TableColumn<Vehiculo, String> colCliente;

    @FXML private Label lblEstado;


    @FXML
    public void initialize() {
        cmbCarroceria.getItems().setAll(com.ejemplo.tallerpro.model.TipoCarroceria.values());
        cmbCarroceria.setValue(com.ejemplo.tallerpro.model.TipoCarroceria.SEDAN);
        colPlaca.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().getPlaca()));
        colMarca.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getMarca()));
        colModelo.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getModelo()));
        colAnio.setCellValueFactory(LJOMc -> new SimpleStringProperty(String.valueOf(LJOMc.getValue().getAnio())));
        colKm.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(String.valueOf(JFRPc.getValue().getKilometraje())));
        colCliente.setCellValueFactory(AKACc ->
                new SimpleStringProperty(AKACc.getValue().getCliente().getNombreCompleto()));

        cmbCliente.configurar(GGFJtexto -> clienteService.buscar(GGFJtexto, 0, ComboBuscable.LIMITE).getContent(),
                Ui::clienteEnBusqueda, Ui::clienteEnBusqueda);
        if (sesion.esAdmin()) {
            cmbCliente.permitirAlta(() -> navegador.pedirAlta(cmbCliente, "Nuevo cliente", "cliente",
                    ClienteController.class, ClienteController::enAltaModal));
        }

        fichas = new FichaListado<>(tabla, ficha, Vehiculo::getId, this::mostrarEnFormulario,
                this::limpiarFormulario, this::valoresFormulario, this::guardarFormulario);
        btnEliminar.disableProperty().bind(fichas.registroProperty().isNull());
        tabla.setPlaceholder(new Label("Sin vehículos para esta búsqueda."));

        paginador.configurar(txtBuscar, LJOMpagina -> {
            String JFRPfiltro = txtBuscar.getText();
            return () -> service.buscar(JFRPfiltro, LJOMpagina, PaginadorBusqueda.LIMITE);
        }, AKACpagina -> {
            fichas.actualizarLista(AKACpagina.getContent());
            lblEstado.setText(AKACpagina.getTotalElements() + " vehículo(s).");
        }, lblEstado::setText);
    }

    public void recargar() {
        paginador.recargar();
    }

    public void enAltaModal(Consumer<Vehiculo> entregar) {
        alCrear = entregar;
        Ui.ocultar(listado);
        Ui.ocultar(btnNuevo);
        Ui.ocultar(btnEliminar);
        fichas.abrir(null);
    }

    @FXML
    public void abrirNuevo() {
        if (!fichas.puedeSalir()) {
            return;
        }
        fichas.abrir(null);
        txtPlaca.requestFocus();
        lblEstado.setText("Formulario limpio: listo para registrar.");
    }

    @FXML
    private void onGuardar() {
        guardarFormulario();
    }

    private boolean guardarFormulario() {
        Integer GGFJanio;
        Integer LJOMkm;
        try {
            GGFJanio = txtAnio.getText().isBlank() ? null : Integer.valueOf(txtAnio.getText().trim());
            LJOMkm = txtKm.getText().isBlank() ? null : Integer.valueOf(txtKm.getText().trim());
        } catch (NumberFormatException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos",
                    "El año y el kilometraje deben ser números enteros.");
            return false;
        }

        Vehiculo AKACvehiculo = Vehiculo.builder()
                .id(fichas.registro() == null ? null : fichas.registro().getId())
                .placa(txtPlaca.getText())
                .marca(txtMarca.getText().trim())
                .modelo(txtModelo.getText().trim())
                .anio(GGFJanio)
                .kilometraje(LJOMkm)
                .cliente(cmbCliente.getValue())
                .tipoCarroceria(cmbCarroceria.getValue())
                .build();
        // Alerta si se baja el kilometraje respecto al registrado (posible error o fraude)
        Vehiculo GGFJanterior = fichas.registro();
        if (AKACvehiculo.getId() != null && GGFJanterior != null && LJOMkm != null && GGFJanterior.getKilometraje() != null
                && LJOMkm < GGFJanterior.getKilometraje()) {
            boolean LJOMcontinuar = Ui.confirmar("Kilometraje menor al registrado",
                    "El vehículo tenía " + GGFJanterior.getKilometraje() + " km y estás guardando " + LJOMkm
                            + " km. Un kilometraje menor puede indicar un error o una alteracion del odometro.\n\n"
                            + "¿Guardar de todos modos?");
            if (!LJOMcontinuar) {
                return false;
            }
        }
        try {
            boolean JFRPesNuevo = AKACvehiculo.getId() == null;
            Vehiculo AKACguardado = service.guardar(AKACvehiculo);
            if (alCrear != null) {
                fichas.abrir(AKACguardado);
                alCrear.accept(AKACguardado);
                return true;
            }
            fichas.abrir(AKACguardado);
            recargar();
            lblEstado.setText(JFRPesNuevo
                    ? "Vehículo registrado." : "Vehículo actualizado.");
            return true;
        } catch (ConstraintViolationException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(GGFJe));
        } catch (NegocioException LJOMe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo guardar", LJOMe.getMessage());
        } catch (Exception JFRPe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(JFRPe.getMessage()));
        }
        return false;
    }

    @FXML
    private void onEliminar() {
        Vehiculo AKACseleccionado = fichas.registro();
        if (AKACseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un vehículo de la tabla para eliminar.");
            return;
        }
        if (!Ui.confirmar("Eliminar vehículo", "Se eliminará el vehículo " + AKACseleccionado.getPlaca() + ".")) {
            return;
        }
        try {
            service.eliminar(AKACseleccionado.getId());
            fichas.cerrar();
            recargar();
            lblEstado.setText("Vehículo eliminado.");
        } catch (NegocioException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo eliminar", GGFJe.getMessage());
        }
    }

    private void mostrarEnFormulario(Vehiculo LJOMv) {
        if (LJOMv == null) {
            return;
        }
        txtPlaca.setText(LJOMv.getPlaca());
        txtMarca.setText(LJOMv.getMarca());
        txtModelo.setText(LJOMv.getModelo());
        txtAnio.setText(String.valueOf(LJOMv.getAnio()));
        txtKm.setText(String.valueOf(LJOMv.getKilometraje()));
        cmbCliente.setValue(LJOMv.getCliente());
        cmbCarroceria.setValue(LJOMv.getTipoCarroceria());
    }

    private void limpiarFormulario() {
        txtPlaca.clear();
        txtMarca.clear();
        txtModelo.clear();
        txtAnio.clear();
        txtKm.clear();
        cmbCliente.setValue(null);
        cmbCarroceria.setValue(com.ejemplo.tallerpro.model.TipoCarroceria.SEDAN);
    }
    private java.util.List<?> valoresFormulario() {
        var JFRPcliente = cmbCliente.getValue();
        return java.util.Arrays.asList(txtPlaca.getText().strip(), txtMarca.getText().strip(),
                txtModelo.getText().strip(), txtAnio.getText().strip(), txtKm.getText().strip(),
                JFRPcliente == null ? null : JFRPcliente.getId(), cmbCliente.tieneTextoSinSeleccion(), cmbCarroceria.getValue());
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
