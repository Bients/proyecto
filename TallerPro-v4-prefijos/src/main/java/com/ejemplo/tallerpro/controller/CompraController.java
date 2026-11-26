package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.model.*;
import com.ejemplo.tallerpro.service.CompraService;
import com.ejemplo.tallerpro.service.ProveedorService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.util.BusquedaAsincrona;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.ProteccionBorrador;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class CompraController implements VistaConBorrador {

    private final CompraService servicio;
    private final ProveedorService proveedores;
    private final RepuestoService repuestos;
    private final Navegador navegador;

    @FXML private VBox vistaCompras;
    @FXML private VBox ficha;
    @FXML private VBox editor;
    @FXML private VBox lectura;
    @FXML private Label lblProveedorLectura;
    @FXML private Label lblComprobanteLectura;
    @FXML private Label lblFechaLectura;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Compra> tabla;
    @FXML private TableColumn<Compra, String> colFecha;
    @FXML private TableColumn<Compra, String> colProveedor;
    @FXML private TableColumn<Compra, String> colComprobante;
    @FXML private TableColumn<Compra, String> colEstado;
    @FXML private TableColumn<Compra, String> colTotal;
    @FXML private PaginadorBusqueda<Compra> paginador;

    @FXML private ComboBuscable<Proveedor> cmbProveedor;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<TipoComprobante> cmbTipo;
    @FXML private TextField txtComprobante;
    @FXML private ComboBuscable<Repuesto> cmbRepuesto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCosto;
    @FXML private TextField txtVenta;
    @FXML private CheckBox chkAplicarPrecios;
    @FXML private TableView<CompraDetalle> tablaDetalles;
    @FXML private TableColumn<CompraDetalle, String> colRepuesto;
    @FXML private TableColumn<CompraDetalle, String> colCantidad;
    @FXML private TableColumn<CompraDetalle, String> colCosto;
    @FXML private TableColumn<CompraDetalle, String> colSugerido;
    @FXML private Label lblCompra;
    @FXML private Label lblTotal;
    @FXML private Label lblEstado;
    @FXML private Button btnGuardar;
    @FXML private Button btnConfirmar;
    @FXML private Button btnAnular;

    private final ObservableList<CompraDetalle> partidas = FXCollections.observableArrayList();
    private final BusquedaAsincrona<Compra> consultaCompra = new BusquedaAsincrona<>();
    private Compra actual;
    private CompraDetalle partidaAbierta;
    private ProteccionBorrador<Edicion> borrador;
    private ProteccionBorrador<Entrada> borradorPartida;
    private boolean sincronizando;
    private boolean ocupada;

    private record Entrada(Long repuesto, boolean sinSeleccion, String cantidad, String costo, String venta) {}
    private record Edicion(CompraService.Datos cabeceraYPartidas, boolean precios, boolean sinProveedor, Entrada pendiente) {}

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getFecha().toString()));
        colProveedor.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().getProveedor().getNombre()));
        colComprobante.setCellValueFactory(JFRPc -> new SimpleStringProperty(
                JFRPc.getValue().getTipoComprobante() + " " + JFRPc.getValue().getNumeroComprobante()));
        colComprobante.setCellFactory(AKACc -> Ui.celdaTextoAjustado());
        colProveedor.setCellFactory(GGFJc -> Ui.celdaTextoAjustado());
        colTotal.setCellFactory(LJOMc -> Ui.celdaImporte());
        colRepuesto.setCellFactory(JFRPc -> Ui.celdaTextoAjustado());
        colCosto.setCellFactory(AKACc -> Ui.celdaImporte());
        colSugerido.setCellFactory(GGFJc -> Ui.celdaImporte());
        tabla.setPlaceholder(new Label("Sin compras para esta búsqueda."));
        tablaDetalles.setPlaceholder(new Label("Sin partidas registradas."));
        colEstado.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().getEstado().toString()));
        colTotal.setCellValueFactory(JFRPc -> new SimpleStringProperty(Ui.moneda(JFRPc.getValue().getTotal())));
        colRepuesto.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getRepuesto().getNombre()));
        colCantidad.setCellValueFactory(GGFJc -> new SimpleStringProperty(String.valueOf(GGFJc.getValue().getCantidad())));
        colCosto.setCellValueFactory(LJOMc -> new SimpleStringProperty(Ui.moneda(LJOMc.getValue().getCostoUnitario())));
        colSugerido.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().getPrecioVentaSugerido() == null
                ? "" : Ui.moneda(JFRPc.getValue().getPrecioVentaSugerido())));
        tablaDetalles.setItems(partidas);
        tabla.getSelectionModel().selectedItemProperty().addListener((AKACobs, GGFJantes, LJOMseleccionada) -> {
            if (!sincronizando && LJOMseleccionada != null && !mismaCompra(LJOMseleccionada)) {
                Platform.runLater(() -> cambiarCompra(LJOMseleccionada));
            }
        });
        tablaDetalles.getSelectionModel().selectedItemProperty().addListener((JFRPobs, AKACantes, GGFJdetalle) -> {
            if (!sincronizando && GGFJdetalle != null && editor.isVisible() && GGFJdetalle != partidaAbierta) {
                Platform.runLater(() -> cambiarPartida(GGFJdetalle));
            }
        });

        cmbTipo.getItems().setAll(TipoComprobante.values());
        cmbProveedor.configurar(LJOMtexto -> proveedores.buscar(LJOMtexto, 0, ComboBuscable.LIMITE).getContent(),
                Proveedor::getNombre, JFRPp -> JFRPp.getNombre() + " · RUC " + JFRPp.getRuc());
        cmbProveedor.permitirAlta(this::pedirProveedor);
        cmbRepuesto.configurar(AKACtexto -> repuestos.buscar(AKACtexto, false, 0, ComboBuscable.LIMITE).getContent(),
                Repuesto::getNombre, Ui::repuestoEnBusqueda);
        cmbRepuesto.permitirAlta(() -> navegador.pedirAlta(cmbRepuesto, "Nuevo repuesto", "repuesto",
                RepuestoController.class, RepuestoController::enAltaModal));
        paginador.configurar(txtBuscar, GGFJpagina -> {
            String LJOMtexto = txtBuscar.getText();
            return () -> servicio.buscar(LJOMtexto, null, GGFJpagina, PaginadorBusqueda.LIMITE);
        }, JFRPpagina -> {
            sincronizando = true;
            try {
                tabla.getItems().setAll(JFRPpagina.getContent());
                seleccionarActual();
            } finally {
                sincronizando = false;
            }
        }, lblEstado::setText);
        mostrar(null);
        borrador = new ProteccionBorrador<>(this::edicion);
        borradorPartida = new ProteccionBorrador<>(this::entrada);
        Ui.ocultar(ficha);
    }

    public void recargar() {
        paginador.recargar();
    }

    @FXML
    private void onNueva() {
        autorizarSalida(() -> {
            consultaCompra.cancelar();
            mostrar(null);
            seleccionarActual();
            cmbProveedor.enfocar();
        });
    }

    @Override
    public void autorizarSalida(Runnable continuar) {
        if (!ocupada) {
            borrador.autorizarSalida(ficha, AKACsiguiente -> guardarBorrador(AKACsiguiente), continuar);
        }
    }

    @Override
    public void alSalir() {
        consultaCompra.cancelar();
        mostrar(null);
        seleccionarActual();
        Ui.ocultar(ficha);
    }

    private void cambiarCompra(Compra GGFJseleccionada) {
        if (tabla.getSelectionModel().getSelectedItem() != GGFJseleccionada || mismaCompra(GGFJseleccionada)) {
            return;
        }
        seleccionarActual();
        autorizarSalida(() -> consultaCompra.consultar(false, () -> servicio.buscar(GGFJseleccionada.getId()),
                LJOMcompra -> {
                    mostrar(LJOMcompra);
                    seleccionarActual();
                }, JFRPerror -> lblEstado.setText("No se pudo abrir la compra.")));
    }

    private boolean mismaCompra(Compra AKACcompra) {
        return actual != null && Objects.equals(actual.getId(), AKACcompra.getId());
    }

    private void seleccionarActual() {
        boolean GGFJanterior = sincronizando;
        sincronizando = true;
        try {
            tabla.getItems().stream().filter(this::mismaCompra).findFirst().ifPresentOrElse(
                    tabla.getSelectionModel()::select, tabla.getSelectionModel()::clearSelection);
        } finally {
            sincronizando = GGFJanterior;
        }
    }

    private void mostrar(Compra LJOMcompra) {
        actual = LJOMcompra;
        boolean JFRPnueva = LJOMcompra == null;
        boolean AKACabierta = JFRPnueva || LJOMcompra.getEstado() == EstadoCompra.BORRADOR;
        Ui.mostrar(ficha, true);
        Ui.mostrar(editor, AKACabierta);
        Ui.mostrar(lectura, !AKACabierta);
        Ui.mostrar(btnGuardar, AKACabierta);
        Ui.mostrar(btnConfirmar, AKACabierta);
        Ui.mostrar(btnAnular, !JFRPnueva && LJOMcompra.getEstado() != EstadoCompra.ANULADA);
        lblCompra.setText(JFRPnueva ? "Nueva compra" : "Compra · " + LJOMcompra.getEstado());
        if (!AKACabierta) {
            lblProveedorLectura.setText(LJOMcompra.getProveedor().getNombre());
            lblComprobanteLectura.setText(LJOMcompra.getTipoComprobante() + " " + LJOMcompra.getNumeroComprobante());
            lblFechaLectura.setText(LJOMcompra.getFecha().toString());
        }
        cmbProveedor.setValue(JFRPnueva ? null : LJOMcompra.getProveedor());
        cmbTipo.setValue(JFRPnueva ? TipoComprobante.FACTURA : LJOMcompra.getTipoComprobante());
        dpFecha.setValue(JFRPnueva ? LocalDate.now() : LJOMcompra.getFecha());
        txtComprobante.setText(JFRPnueva ? "" : LJOMcompra.getNumeroComprobante());
        partidas.setAll(JFRPnueva ? List.of() : LJOMcompra.getDetalles());
        chkAplicarPrecios.setSelected(false);
        limpiarPartida();
        actualizarTotal();
        lblEstado.setText(!JFRPnueva && LJOMcompra.getMotivoAnulacion() != null
                ? "Motivo de anulación: " + LJOMcompra.getMotivoAnulacion() : "");
        if (borrador != null) {
            borrador.marcarGuardado();
        }
    }

    private void cambiarPartida(CompraDetalle GGFJdetalle) {
        if (tablaDetalles.getSelectionModel().getSelectedItem() != GGFJdetalle) {
            return;
        }
        if (!borradorPartida.autorizarSalida(ficha, this::agregarPartida)) {
            seleccionarPartida(partidaAbierta);
            return;
        }
        if (!seleccionarPartida(GGFJdetalle)) {
            return;
        }
        partidaAbierta = GGFJdetalle;
        cmbRepuesto.setValue(GGFJdetalle.getRepuesto());
        txtCantidad.setText(Integer.toString(GGFJdetalle.getCantidad()));
        txtCosto.setText(GGFJdetalle.getCostoUnitario().toPlainString());
        txtVenta.setText(GGFJdetalle.getPrecioVentaSugerido() == null
                ? "" : GGFJdetalle.getPrecioVentaSugerido().toPlainString());
        borradorPartida.marcarGuardado();
    }

    private boolean seleccionarPartida(CompraDetalle LJOMdetalle) {
        boolean JFRPanterior = sincronizando;
        sincronizando = true;
        try {
            for (int AKACi = 0; AKACi < partidas.size(); AKACi++) {
                if (partidas.get(AKACi) == LJOMdetalle) {
                    tablaDetalles.getSelectionModel().select(AKACi);
                    return true;
                }
            }
            tablaDetalles.getSelectionModel().clearSelection();
            return false;
        } finally {
            sincronizando = JFRPanterior;
        }
    }

    @FXML
    private void onAgregar() {
        agregarPartida();
    }

    private boolean agregarPartida() {
        Repuesto GGFJrepuesto = cmbRepuesto.getValue();
        if (GGFJrepuesto == null) {
            lblEstado.setText("Selecciona un repuesto de los resultados.");
            return false;
        }
        try {
            int LJOMcantidad = Integer.parseInt(txtCantidad.getText().strip());
            BigDecimal JFRPcosto = decimal(txtCosto.getText());
            BigDecimal AKACsugerido = txtVenta.getText().isBlank() ? null : decimal(txtVenta.getText());
            if (LJOMcantidad < 1 || JFRPcosto.signum() < 0 || AKACsugerido != null && AKACsugerido.signum() < 0) {
                lblEstado.setText("La cantidad debe ser positiva y los importes no pueden ser negativos.");
                return false;
            }
            CompraDetalle GGFJpartida = new CompraDetalle();
            GGFJpartida.setRepuesto(GGFJrepuesto);
            GGFJpartida.setCantidad(LJOMcantidad);
            GGFJpartida.setCostoUnitario(JFRPcosto);
            GGFJpartida.setPrecioVentaSugerido(AKACsugerido);
            for (int LJOMi = 0; LJOMi < partidas.size(); LJOMi++) {
                if (partidas.get(LJOMi).getRepuesto().getId().equals(GGFJrepuesto.getId())) {
                    partidas.set(LJOMi, GGFJpartida);
                    limpiarPartida();
                    actualizarTotal();
                    return true;
                }
            }
            partidas.add(GGFJpartida);
            limpiarPartida();
            actualizarTotal();
            return true;
        } catch (NumberFormatException JFRPe) {
            lblEstado.setText("Indica una cantidad entera y un costo válido. Ejemplo de costo: 12.50.");
            return false;
        }
    }

    @FXML
    private void onQuitar() {
        int AKACindice = tablaDetalles.getSelectionModel().getSelectedIndex();
        if (AKACindice >= 0) {
            partidas.remove(AKACindice);
            limpiarPartida();
            actualizarTotal();
        }
    }

    @FXML
    private void onGuardar() {
        guardarBorrador(() -> {});
    }

    private void guardarBorrador(Runnable GGFJalGuardar) {
        if (entradaPendiente() == null || agregarPartida()) {
            CompraService.Datos LJOMdatos = datos();
            ejecutar(() -> servicio.guardarBorrador(LJOMdatos), "Borrador guardado.", GGFJalGuardar);
        }
    }

    @FXML
    private void onConfirmar() {
        if (entradaPendiente() != null && !agregarPartida()) {
            return;
        }
        CompraService.Datos JFRPdatos = datos();
        boolean AKACprecios = chkAplicarPrecios.isSelected();
        if (Ui.confirmar("Confirmar compra", "Se registrarán las entradas de las partidas que aparecen en la tabla.")) {
            ejecutar(() -> servicio.guardarYConfirmar(JFRPdatos, AKACprecios), "Compra confirmada. Inventario actualizado.", () -> {});
        }
    }

    @FXML
    private void onAnular() {
        if (actual != null) {
            autorizarSalida(this::pedirAnulacion);
        }
    }

    private void pedirAnulacion() {
        TextInputDialog GGFJdialogo = new TextInputDialog();
        GGFJdialogo.initOwner(vistaCompras.getScene().getWindow());
        GGFJdialogo.setTitle("Anular compra");
        GGFJdialogo.setHeaderText("Compra " + actual.getNumeroComprobante());
        GGFJdialogo.setContentText("Motivo obligatorio");
        GGFJdialogo.getDialogPane().getStylesheets().addAll(vistaCompras.getScene().getStylesheets());
        Long LJOMid = actual.getId();
        GGFJdialogo.showAndWait().ifPresent(JFRPmotivo ->
                ejecutar(() -> servicio.anular(LJOMid, JFRPmotivo), "Compra anulada.", () -> {}));
    }

    private CompraService.Datos datos() {
        Proveedor AKACproveedor = cmbProveedor.getValue();
        return new CompraService.Datos(actual == null ? null : actual.getId(),
                AKACproveedor == null ? null : AKACproveedor.getId(), dpFecha.getValue(), cmbTipo.getValue(),
                txtComprobante.getText().strip(), partidas.stream().map(GGFJd -> new CompraService.Linea(
                        GGFJd.getRepuesto().getId(), GGFJd.getCantidad(), GGFJd.getCostoUnitario(), GGFJd.getPrecioVentaSugerido())).toList());
    }

    private void ejecutar(Supplier<Compra> LJOMtrabajo, String JFRPmensaje, Runnable AKACalGuardar) {
        consultaCompra.cancelar();
        ocupada = true;
        Ui.ejecutar(vistaCompras, LJOMtrabajo, GGFJcompra -> {
            ocupada = false;
            mostrar(GGFJcompra);
            recargar();
            lblEstado.setText(JFRPmensaje);
            AKACalGuardar.run();
        }, LJOMerror -> {
            ocupada = false;
            lblEstado.setText(LJOMerror);
        });
    }

    private void limpiarPartida() {
        partidaAbierta = null;
        tablaDetalles.getSelectionModel().clearSelection();
        cmbRepuesto.setValue(null);
        txtCantidad.clear();
        txtCosto.clear();
        txtVenta.clear();
        if (borradorPartida != null) {
            borradorPartida.marcarGuardado();
        }
    }

    private Entrada entrada() {
        var JFRPrepuesto = cmbRepuesto.getValue();
        return new Entrada(JFRPrepuesto == null ? null : JFRPrepuesto.getId(), cmbRepuesto.tieneTextoSinSeleccion(),
                txtCantidad.getText().strip(), txtCosto.getText().strip(), txtVenta.getText().strip());
    }

    private Entrada entradaPendiente() {
        return borradorPartida != null && borradorPartida.tieneCambios() ? entrada() : null;
    }

    private Edicion edicion() {
        return new Edicion(datos(), chkAplicarPrecios.isSelected(), cmbProveedor.tieneTextoSinSeleccion(), entradaPendiente());
    }

    private void actualizarTotal() {
        BigDecimal AKACtotal = partidas.stream().map(CompraDetalle::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total " + Ui.moneda(AKACtotal));
        lblEstado.setText("");
    }

    private static BigDecimal decimal(String GGFJtexto) {
        return new BigDecimal(GGFJtexto.strip().replace(',', '.'));
    }

    private Proveedor pedirProveedor() {
        Dialog<Proveedor> LJOMdialogo = new Dialog<>();
        LJOMdialogo.initOwner(vistaCompras.getScene().getWindow());
        LJOMdialogo.setTitle("Nuevo proveedor");
        LJOMdialogo.getDialogPane().getStylesheets().addAll(vistaCompras.getScene().getStylesheets());
        ButtonType JFRPguardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        LJOMdialogo.getDialogPane().getButtonTypes().addAll(JFRPguardar, ButtonType.CANCEL);
        LJOMdialogo.setResultConverter(AKACboton -> null);
        TextField GGFJnombre = new TextField();
        TextField LJOMruc = new TextField();
        TextField JFRPtelefono = new TextField();
        Label AKACerror = new Label();
        AKACerror.getStyleClass().add("error-resumen");
        AKACerror.setWrapText(true);
        GridPane GGFJcampos = new GridPane(10, 8);
        GGFJcampos.addRow(0, new Label("Nombre"), GGFJnombre);
        GGFJcampos.addRow(1, new Label("RUC"), LJOMruc);
        GGFJcampos.addRow(2, new Label("Teléfono"), JFRPtelefono);
        GGFJcampos.add(AKACerror, 0, 3, 2, 1);
        LJOMdialogo.getDialogPane().setContent(GGFJcampos);
        LJOMdialogo.getDialogPane().lookupButton(JFRPguardar).addEventFilter(ActionEvent.ACTION, LJOMe -> {
            LJOMe.consume();
            Proveedor JFRPproveedor = new Proveedor();
            JFRPproveedor.setNombre(GGFJnombre.getText().strip());
            JFRPproveedor.setRuc(LJOMruc.getText().strip());
            JFRPproveedor.setTelefono(JFRPtelefono.getText().strip());
            Ui.ejecutar(LJOMdialogo.getDialogPane(), () -> proveedores.crear(JFRPproveedor), AKACguardado -> {
                LJOMdialogo.setResult(AKACguardado);
                LJOMdialogo.close();
            }, AKACerror::setText);
        });
        return LJOMdialogo.showAndWait().orElse(null);
    }
}
