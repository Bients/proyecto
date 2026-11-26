package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.service.ClienteService;
import com.ejemplo.tallerpro.service.ClienteService.FichaDni;
import com.ejemplo.tallerpro.service.ConsultaDniService;
import com.ejemplo.tallerpro.service.ConsultaDniService.Persona;
import com.ejemplo.tallerpro.service.ConsultaDniService.Resultado;
import com.ejemplo.tallerpro.util.BusquedaAsincrona;
import com.ejemplo.tallerpro.util.FichaListado;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ClienteController implements VistaConBorrador {

    private final ClienteService service;
    private final ConsultaDniService dniService;

    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtDni;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBuscar;
    @FXML private PaginadorBusqueda<Cliente> paginador;
    @FXML private Node listado;
    @FXML private Node ficha;
    @FXML private Label lblFicha;
    @FXML private Button btnOtroDni;
    @FXML private Button btnGuardar;
    @FXML private Button btnCerrar;
    @FXML private Node btnEliminar;
    @FXML private Label lblDni;
    @FXML private Label lblVehiculos;

    private final BusquedaAsincrona<Resultado> consultaDni = new BusquedaAsincrona<>();
    private Persona sugerenciaDni;
    private Consumer<Cliente> alCrear;
    private FichaListado<Cliente> fichas;

    @FXML private TableView<Cliente> tabla;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colApellido;
    @FXML private TableColumn<Cliente, String> colDni;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEmail;

    @FXML private Label lblEstado;

    @FXML
    public void initialize() {
        fichas = new FichaListado<>(tabla, ficha, Cliente::getId, this::mostrarEnFormulario,
                this::limpiarFormulario, this::valoresFormulario, this::guardarFormulario, true);
        btnEliminar.disableProperty().bind(fichas.registroProperty().isNull());
        txtDni.setTextFormatter(new TextFormatter<>(AKACcambio ->
                AKACcambio.getControlNewText().matches("[0-9]{0,8}") ? AKACcambio : null));
        txtDni.textProperty().addListener((GGFJobs, LJOManterior, JFRPnuevo) -> buscarDni(JFRPnuevo));

        colNombre.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getNombre()));
        colApellido.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getApellido()));
        colDni.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().getDni()));
        colTelefono.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(Ui.texto(JFRPc.getValue().getTelefono())));
        colEmail.setCellValueFactory(AKACc ->
                new SimpleStringProperty(Ui.texto(AKACc.getValue().getEmail())));
        colNombre.setCellFactory(Ui::celdaTextoAjustado);
        colApellido.setCellFactory(Ui::celdaTextoAjustado);
        colEmail.setCellFactory(Ui::celdaTextoAjustado);
        tabla.setPlaceholder(new Label("No hay clientes para esta búsqueda."));

        paginador.configurar(txtBuscar, GGFJpagina -> {
            String LJOMfiltro = txtBuscar.getText();
            return () -> service.buscar(LJOMfiltro, GGFJpagina, PaginadorBusqueda.LIMITE);
        }, JFRPpagina -> {
            fichas.actualizarLista(JFRPpagina.getContent());
            lblEstado.setText(JFRPpagina.getTotalElements() + " cliente(s).");
        }, lblEstado::setText);
    }

    public void recargar() {
        paginador.recargar();
    }

    public void enAltaModal(Consumer<Cliente> entregar) {
        alCrear = entregar;
        Ui.ocultar(listado);
        Ui.ocultar(btnCerrar);
        Ui.ocultar(btnEliminar);
        fichas.abrir(null);
    }

    @FXML
    private void onNuevo() {
        if (fichas.puedeSalir()) {
            fichas.abrir(null);
            txtDni.requestFocus();
            lblEstado.setText("Formulario limpio: listo para registrar.");
        }
    }

    @FXML
    private void onCerrar() {
        if (fichas.puedeSalir()) {
            fichas.cerrar();
            tabla.requestFocus();
        }
    }

    @Override
    public void autorizarSalida(Runnable continuar) {
        fichas.autorizarSalida(continuar);
    }

    @Override
    public void alSalir() {
        fichas.alSalir();
    }

    private List<String> valoresFormulario() {
        return List.of(txtDni.getText().strip(), txtNombre.getText().strip(), txtApellido.getText().strip(),
                txtTelefono.getText().strip(), txtEmail.getText().strip());
    }

    private void buscarDni(String AKACdni) {
        if (fichas.registro() != null) {
            return;
        }
        consultaDni.cancelar();
        limpiarSugerencia();
        Ui.ocultar(lblVehiculos);
        if (AKACdni.length() != 8) {
            mostrarEstadoDni("");
            return;
        }
        mostrarEstadoDni("Consultando DNI…");
        consultaDni.consultar(true, () -> dniService.consultar(AKACdni), GGFJresultado -> {
            switch (GGFJresultado.estado()) {
                case CLIENTE_EXISTENTE -> abrirFichaDni(GGFJresultado.cliente());
                case DATOS_ENCONTRADOS -> rellenarNombre(GGFJresultado.persona());
                case SIN_TOKEN -> mostrarEstadoDni("DNI nuevo. Puedes completar la ficha o configurar la consulta en Ajustes.");
                case SIN_RESULTADO -> mostrarEstadoDni("Decolecta no encontró el DNI. Puedes completar la ficha manualmente.");
            }
        }, LJOMerror -> mostrarEstadoDni(LJOMerror instanceof NegocioException
                ? LJOMerror.getMessage() + " Puedes completar la ficha manualmente."
                : "No se pudo consultar el DNI. Puedes completar la ficha manualmente."));

    }

    private void rellenarNombre(Persona JFRPpersona) {
        if (!txtNombre.getText().isBlank() || !txtApellido.getText().isBlank()) {
            mostrarEstadoDni("Decolecta encontró el DNI. Se conservó el nombre que estabas escribiendo.");
            return;
        }
        sugerenciaDni = JFRPpersona;
        txtNombre.setText(JFRPpersona.nombres());
        txtApellido.setText(JFRPpersona.apellidos());
        mostrarEstadoDni("Nombres y apellidos obtenidos de Decolecta. Revísalos antes de guardar.");
    }

    private void limpiarSugerencia() {
        if (sugerenciaDni == null) {
            return;
        }
        if (sugerenciaDni.nombres().equals(txtNombre.getText())) {
            txtNombre.clear();
        }
        if (sugerenciaDni.apellidos().equals(txtApellido.getText())) {
            txtApellido.clear();
        }
        sugerenciaDni = null;
    }

    private void abrirFichaDni(FichaDni AKACficha) {
        if (valoresFormulario().subList(1, 5).stream().anyMatch(GGFJvalor -> !GGFJvalor.isEmpty()) && !fichas.puedeSalir()) {
            mostrarEstadoDni("El DNI ya está registrado. Conservamos los datos que estás editando.");
            return;
        }
        fichas.abrir(AKACficha.cliente());
        if (AKACficha.vehiculos().isEmpty()) {
            lblVehiculos.setText("Este cliente todavía no tiene vehículos registrados.");
        } else {
            String LJOMlista = AKACficha.vehiculos().stream()
                    .map(JFRPv -> JFRPv.getPlaca() + " · " + JFRPv.getMarca() + " " + JFRPv.getModelo())
                    .collect(Collectors.joining("  |  "));
            long AKACadicionales = AKACficha.totalVehiculos() - AKACficha.vehiculos().size();
            lblVehiculos.setText("Vehículos: " + LJOMlista + (AKACadicionales > 0 ? "  |  +" + AKACadicionales : ""));
        }
        Ui.mostrar(lblVehiculos, true);
    }

    private void mostrarEstadoDni(String GGFJtexto) {
        lblDni.setText(GGFJtexto);
        Ui.mostrar(lblDni, !GGFJtexto.isBlank());
    }

    @FXML
    private void onGuardar() {
        if (guardarFormulario() && alCrear == null) {
            tabla.requestFocus();
        }
    }

    private boolean guardarFormulario() {
        consultaDni.cancelar();
        Cliente LJOMclienteActual = fichas.registro();
        if (alCrear != null && LJOMclienteActual != null) {
            alCrear.accept(LJOMclienteActual);
            return true;
        }
        Cliente JFRPcliente = Cliente.builder()
                .id(LJOMclienteActual == null ? null : LJOMclienteActual.getId())
                .nombre(txtNombre.getText().trim())
                .apellido(txtApellido.getText().trim())
                .dni(txtDni.getText().trim())
                .telefono(txtTelefono.getText().trim())
                .email(txtEmail.getText().trim())
                .build();
        try {
            boolean AKACesNuevo = JFRPcliente.getId() == null;
            Cliente GGFJguardado = service.guardar(JFRPcliente);
            if (alCrear != null) {
                alCrear.accept(GGFJguardado);
                return true;
            }
            fichas.cerrar();
            recargar();
            lblEstado.setText(AKACesNuevo
                    ? "Cliente registrado." : "Cliente actualizado.");
            return true;
        } catch (ConstraintViolationException LJOMe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(LJOMe));
        } catch (NegocioException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo guardar", JFRPe.getMessage());
        } catch (Exception AKACe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(AKACe.getMessage()));
        }
        return false;
    }

    @FXML
    private void onEliminar() {
        Cliente GGFJseleccionado = fichas.registro();
        if (GGFJseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Abre un cliente de la tabla o búscalo por DNI para eliminarlo.");
            return;
        }
        if (!Ui.confirmar("Eliminar cliente", "Se eliminará a " + GGFJseleccionado.getNombreCompleto() + ".")) {
            return;
        }
        try {
            service.eliminar(GGFJseleccionado.getId());
            fichas.cerrarSinSeleccion();
            recargar();
            lblEstado.setText("Cliente eliminado.");
        } catch (NegocioException LJOMe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo eliminar", LJOMe.getMessage());
        }
    }

    private void mostrarEnFormulario(Cliente JFRPc) {
        consultaDni.cancelar();
        sugerenciaDni = null;
        lblFicha.setText("Datos del cliente");
        txtNombre.setText(JFRPc.getNombre());
        txtApellido.setText(JFRPc.getApellido());
        txtDni.setText(JFRPc.getDni());
        txtTelefono.setText(Ui.texto(JFRPc.getTelefono()));
        txtEmail.setText(Ui.texto(JFRPc.getEmail()));
        Ui.ocultar(lblVehiculos);
        if (alCrear != null) {
            editarCampos(false);
            btnGuardar.setText("Usar este cliente");
            Ui.mostrar(btnOtroDni, true);
        }
        mostrarEstadoDni(alCrear == null ? ""
                : "Este cliente ya está registrado. Puedes seleccionarlo para el vehículo.");
    }

    private void editarCampos(boolean AKACeditable) {
        txtDni.setEditable(AKACeditable);
        txtNombre.setEditable(AKACeditable);
        txtApellido.setEditable(AKACeditable);
        txtTelefono.setEditable(AKACeditable);
        txtEmail.setEditable(AKACeditable);
    }

    private void limpiarFormulario() {
        consultaDni.cancelar();
        sugerenciaDni = null;
        lblFicha.setText("Nuevo cliente");
        editarCampos(true);
        btnGuardar.setText("Guardar");
        Ui.ocultar(btnOtroDni);
        mostrarEstadoDni("");
        Ui.ocultar(lblVehiculos);
        txtNombre.clear();
        txtApellido.clear();
        txtDni.clear();
        txtTelefono.clear();
        txtEmail.clear();
    }
}
