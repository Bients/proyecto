package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.service.InventarioService;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.FichaListado;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import java.util.function.Consumer;

/** Inventario de repuestos con alerta de stock bajo. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class RepuestoController implements VistaConBorrador {

    private final RepuestoService service;
    private final InventarioService inventario;
    private final SesionActual sesion;

    @FXML private Node ficha;
    private FichaListado<Repuesto> fichas;
    @FXML private TextField txtNombre;
    @FXML private TextField txtStock;
    @FXML private TextField txtSku;
    @FXML private TextField txtCosto;
    @FXML private Node btnAjuste;
    @FXML private Node btnKardex;
    private Consumer<Repuesto> abrirKardex;
    @FXML private TextField txtMinimo;
    @FXML private TextField txtPrecio;
    @FXML private CheckBox chkBajo;
    @FXML private TextField txtBuscar;
    @FXML private PaginadorBusqueda<Repuesto> paginador;
    @FXML private Node listado;
    @FXML private Node btnNuevo;
    @FXML private Node btnEliminar;

    private Consumer<Repuesto> alCrear;

    @FXML private TableView<Repuesto> tabla;
    @FXML private TableColumn<Repuesto, String> colId;
    @FXML private TableColumn<Repuesto, String> colNombre;
    @FXML private TableColumn<Repuesto, String> colStock;
    @FXML private TableColumn<Repuesto, String> colMinimo;
    @FXML private TableColumn<Repuesto, String> colPrecio;
    @FXML private TableColumn<Repuesto, String> colEstadoStock;

    @FXML private Label lblEstado;


    @FXML
    public void initialize() {
        Ui.mostrar(btnAjuste, sesion.esAdmin());
        limpiarFormulario();
        colId.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getCodigo()));
        colNombre.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getNombre()));
        colStock.setCellValueFactory(JFRPc -> new SimpleStringProperty(String.valueOf(JFRPc.getValue().getStock())));
        colMinimo.setCellValueFactory(AKACc -> new SimpleStringProperty(String.valueOf(AKACc.getValue().getStockMinimo())));
        colPrecio.setCellValueFactory(GGFJc -> new SimpleStringProperty(Ui.moneda(GGFJc.getValue().getPrecio())));
        colEstadoStock.setCellValueFactory(LJOMc ->
                new SimpleStringProperty(LJOMc.getValue().esStockBajo() ? "Stock bajo" : "Stock OK"));
        colEstadoStock.setCellFactory(Ui.celdaInsignia());

        fichas = new FichaListado<>(tabla, ficha, Repuesto::getId, this::mostrarEnFormulario,
                this::limpiarFormulario, this::valoresFormulario, this::guardarFormulario);
        btnEliminar.disableProperty().bind(fichas.registroProperty().isNull());
        btnAjuste.disableProperty().bind(fichas.registroProperty().isNull());
        btnKardex.disableProperty().bind(fichas.registroProperty().isNull());
        colPrecio.setCellFactory(JFRPc -> Ui.celdaImporte());
        tabla.setPlaceholder(new Label("Sin repuestos para esta búsqueda."));

        paginador.configurar(txtBuscar, AKACpagina -> {
            String GGFJfiltro = txtBuscar.getText();
            boolean LJOMbajo = chkBajo.isSelected();
            return () -> service.buscar(GGFJfiltro, LJOMbajo, AKACpagina, PaginadorBusqueda.LIMITE);
        }, JFRPpagina -> {
            fichas.actualizarLista(JFRPpagina.getContent());
            lblEstado.setText(JFRPpagina.getTotalElements() + " repuesto(s).");
        }, lblEstado::setText);
        chkBajo.selectedProperty().addListener((AKACobs, GGFJantes, LJOMahora) -> paginador.reiniciar());
    }

    public void recargar() {
        paginador.recargar();
    }

    public void enAltaModal(Consumer<Repuesto> entregar) {
        alCrear = entregar;
        Ui.ocultar(listado);
        Ui.ocultar(btnNuevo);
        Ui.ocultar(btnEliminar);
        fichas.abrir(null);
        Ui.ocultar(btnAjuste);
        Ui.ocultar(btnKardex);
    }

    @FXML
    private void onNuevo() {
        if (!fichas.puedeSalir()) {
            return;
        }
        fichas.abrir(null);
        txtNombre.requestFocus();
        lblEstado.setText("Formulario limpio: listo para registrar.");
    }

    @FXML
    private void onGuardar() {
        guardarFormulario();
    }

    private boolean guardarFormulario() {
        Integer JFRPminimo;
        BigDecimal AKACprecio;
        try {
            JFRPminimo = txtMinimo.getText().isBlank() ? null : Integer.valueOf(txtMinimo.getText().trim());
            AKACprecio = txtPrecio.getText().isBlank()
                    ? null : new BigDecimal(txtPrecio.getText().trim().replace(',', '.'));
        } catch (NumberFormatException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos",
                    "El mínimo debe ser entero y el precio un número (ejemplo: 45.50).");
            return false;
        }
        Repuesto LJOMrepuesto = Repuesto.builder()
                .id(fichas.registro() == null ? null : fichas.registro().getId())
                .nombre(txtNombre.getText().trim())
                .sku(txtSku.getText().strip())
                .stockMinimo(JFRPminimo)
                .precio(AKACprecio)
                .build();
        try {
            boolean JFRPesNuevo = LJOMrepuesto.getId() == null;
            Repuesto AKACguardado = service.guardar(LJOMrepuesto);
            if (alCrear != null) {
                fichas.abrir(AKACguardado);
                alCrear.accept(AKACguardado);
                return true;
            }
            fichas.abrir(AKACguardado);
            recargar();
            lblEstado.setText(JFRPesNuevo
                    ? "Repuesto registrado." : "Repuesto actualizado.");
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
    private void onAjustar() {
        if (fichas.registro() == null || !fichas.puedeSalir()) {
            return;
        }
        long AKACid = fichas.registro().getId();
        Dialog<Void> GGFJdialogo = new Dialog<>();
        GGFJdialogo.initOwner(ficha.getScene().getWindow());
        GGFJdialogo.setTitle("Ajuste de inventario");
        GGFJdialogo.setHeaderText(txtNombre.getText() + " · stock " + txtStock.getText());
        GGFJdialogo.getDialogPane().getStylesheets().addAll(ficha.getScene().getStylesheets());
        ButtonType LJOMaplicar = new ButtonType("Aplicar ajuste", ButtonBar.ButtonData.OK_DONE);
        GGFJdialogo.getDialogPane().getButtonTypes().addAll(LJOMaplicar, ButtonType.CANCEL);
        GGFJdialogo.setResultConverter(JFRPboton -> null);
        TextField AKACcantidad = new TextField();
        AKACcantidad.setPromptText("Ejemplo: 5 o -2");
        TextArea GGFJmotivo = new TextArea();
        GGFJmotivo.setPrefRowCount(3);
        GGFJmotivo.setWrapText(true);
        Label LJOMerror = new Label();
        LJOMerror.getStyleClass().add("error-resumen");
        LJOMerror.setWrapText(true);
        GGFJdialogo.getDialogPane().setContent(new VBox(8, new Label("Cantidad con signo"), AKACcantidad,
                new Label("Motivo obligatorio"), GGFJmotivo, LJOMerror));
        GGFJdialogo.getDialogPane().lookupButton(LJOMaplicar).addEventFilter(ActionEvent.ACTION, JFRPe -> {
            JFRPe.consume();
            int AKACunidades;
            try {
                AKACunidades = Integer.parseInt(AKACcantidad.getText().strip());
            } catch (NumberFormatException GGFJinvalida) {
                LJOMerror.setText("Escribe una cantidad entera. Usa un signo menos para descontar.");
                return;
            }
            String LJOMrazon = GGFJmotivo.getText();
            Ui.ejecutar(GGFJdialogo.getDialogPane(), () -> inventario.ajustar(AKACid, AKACunidades, LJOMrazon), JFRPguardado -> {
                fichas.abrir(JFRPguardado);
                recargar();
                GGFJdialogo.close();
            }, LJOMerror::setText);
        });
        GGFJdialogo.showAndWait();
    }

    @FXML
    private void onEliminar() {
        Repuesto AKACseleccionado = fichas.registro();
        if (AKACseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un repuesto de la tabla para eliminar.");
            return;
        }
        if (!Ui.confirmar("Eliminar repuesto", "Se eliminará \"" + AKACseleccionado.getNombre() + "\" del inventario.")) {
            return;
        }
        try {
            service.eliminar(AKACseleccionado.getId());
            fichas.cerrar();
            recargar();
            lblEstado.setText("Repuesto eliminado.");
        } catch (NegocioException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo eliminar", GGFJe.getMessage());
        }
    }

    private void mostrarEnFormulario(Repuesto LJOMr) {
        if (LJOMr == null) {
            return;
        }
        txtNombre.setText(LJOMr.getNombre());
        txtStock.setText(String.valueOf(LJOMr.getStock()));
        txtSku.setText(Ui.texto(LJOMr.getSku()));
        txtCosto.setText(LJOMr.getCostoPromedio().toPlainString());
        txtMinimo.setText(String.valueOf(LJOMr.getStockMinimo()));
        txtPrecio.setText(LJOMr.getPrecio().toPlainString());
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtStock.setText("0");
        txtCosto.setText("0");
        txtSku.clear();
        txtMinimo.clear();
        txtPrecio.clear();
    }
    public void configurarKardex(Consumer<Repuesto> accion) {
        abrirKardex = accion;
    }

    @FXML
    private void onKardex() {
        if (abrirKardex != null && fichas.registro() != null) {
            abrirKardex.accept(fichas.registro());
        }
    }

    private java.util.List<String> valoresFormulario() {
        return java.util.List.of(txtNombre.getText().strip(), txtSku.getText().strip(),
                txtMinimo.getText().strip(), txtPrecio.getText().strip());
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
