package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.exception.StockInsuficienteException;
import com.ejemplo.tallerpro.exception.SaldoPendienteException;
import com.ejemplo.tallerpro.model.CentimosConverter;
import com.ejemplo.tallerpro.service.PagoService;
import com.ejemplo.tallerpro.util.DialogoPagos;
import javafx.scene.control.TextInputDialog;
import com.ejemplo.tallerpro.model.DatosOrden;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.OrdenFoto;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.OrdenFotoService;
import com.ejemplo.tallerpro.service.PdfService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.FiltrosOrdenBusqueda;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.DialogoDetalle;
import com.ejemplo.tallerpro.util.DialogoTrazabilidad;
import com.ejemplo.tallerpro.util.BusquedaAsincrona;
import com.ejemplo.tallerpro.util.HostServicesHolder;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.SesionActual;
import jakarta.validation.ConstraintViolationException;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class OrdenController {

    private static final String CLASE_ERROR = "campo-error";
    private static final String CLASE_DIAS_ALERTA = "dias-alerta";
    /** Pasados estos dias en el taller, la orden se marca en rojo en la columna "Días". */
    private static final int DIAS_ALERTA = 7;
    private static final List<String> SERVICIOS_FRECUENTES = List.of(
            "Cambio de aceite y filtro",
            "Alineación y balanceo",
            "Cambio de pastillas de freno",
            "Diagnóstico con escáner",
            "Cambio de batería",
            "Rotación de llantas",
            "Mantenimiento de suspensión",
            "Recarga de aire acondicionado");

    private final OrdenService service;
    private final PagoService pagos;
    private final com.ejemplo.tallerpro.service.HallazgoService hallazgos;
    private java.util.function.LongConsumer abrirInspeccion;
    private final VehiculoService vehiculoService;
    private final RepuestoService repuestoService;
    private final PdfService pdfService;
    private final OrdenFotoService fotos;
    private final HostServicesHolder host;
    private final SesionActual sesion;
    private final UsuarioService usuarioService;
    private final Navegador navegador;

    // ---- Cabecera de la orden ----
    @FXML private VBox vistaOrdenes;
    @FXML private Button btnGuardar;
    @FXML private Button btnAnular;
    @FXML private Label lblNumeroOrden;
    @FXML private TitledPane masDetalles;
    @FXML private Button btnPdf;
    @FXML private ComboBuscable<Vehiculo> cmbVehiculo;
    @FXML private ComboBox<EstadoOrden> cmbEstado;
    @FXML private ComboBuscable<Usuario> cmbMecanico;
    @FXML private TextField txtKm;
    @FXML private TextArea txtProblema;
    @FXML private TextField txtDescuento;
    @FXML private TabPane tabsOrden;
    @FXML private Tab tabFotos;
    @FXML private Node barraFiltros;
    @FXML private OrdenFotosController fotosViewController;
    @FXML private Label lblErrores;
    @FXML private TextField txtBuscar;
    @FXML private FiltrosOrdenBusqueda filtrosBusqueda;
    @FXML private PaginadorBusqueda<OrdenTrabajo> paginador;

    // ---- Tabla de ordenes ----
    @FXML private TableView<OrdenTrabajo> tabla;
    @FXML private TableColumn<OrdenTrabajo, String> colId;
    @FXML private TableColumn<OrdenTrabajo, String> colFecha;
    @FXML private TableColumn<OrdenTrabajo, String> colPlaca;
    @FXML private TableColumn<OrdenTrabajo, String> colCliente;
    @FXML private TableColumn<OrdenTrabajo, String> colEstadoOrden;
    @FXML private TableColumn<OrdenTrabajo, String> colMecanico;
    @FXML private TableColumn<OrdenTrabajo, String> colDias;
    @FXML private TableColumn<OrdenTrabajo, String> colTotal;
    @FXML private TableColumn<OrdenTrabajo, String> colPago;

    // ---- Detalle (servicios y repuestos) ----
    @FXML private Label lblOrdenActual;
    @FXML private Label lblAviso;
    @FXML private VBox panelEntrada;
    @FXML private ComboBox<TipoDetalle> cmbTipo;
    @FXML private HBox filaServicio;
    @FXML private ComboBox<String> cmbServicio;
    @FXML private TextField txtPrecio;
    @FXML private HBox filaRepuesto;
    @FXML private VBox filaExterno;
    @FXML private TextField txtExterno;
    @FXML private TextField txtExternoCantidad;
    @FXML private TextField txtExternoCosto;
    @FXML private TextField txtExternoPrecio;
    @FXML private Label lblDatosHistoricos;
    @FXML private Label lblGanancia;
    @FXML private Button btnTrazabilidad;

    private final BusquedaAsincrona<Optional<Repuesto>> consultaAlertaStock = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<Optional<BigDecimal>> consultaGanancia = new BusquedaAsincrona<>();
    @FXML private ComboBuscable<Repuesto> cmbRepuesto;
    @FXML private TextField txtCantidad;
    @FXML private Label lblStockInfo;
    @FXML private Label lblAlertaStock;
    @FXML private TableView<DetalleOrden> tablaDetalles;
    @FXML private TableColumn<DetalleOrden, String> colDTipo;
    @FXML private TableColumn<DetalleOrden, String> colDDescripcion;
    @FXML private TableColumn<DetalleOrden, String> colDCantidad;
    @FXML private TableColumn<DetalleOrden, String> colDPrecio;
    @FXML private TableColumn<DetalleOrden, String> colDSubtotal;
    @FXML private Button btnQuitar;
    @FXML private Label lblTotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblCobrado;
    @FXML private Label lblSaldo;
    @FXML private Label lblAFavor;
    @FXML private Button btnPagos;
    @FXML private Button btnInspeccion;

    @FXML private Label lblEstado;

    private final ObservableList<OrdenTrabajo> datos = FXCollections.observableArrayList();
    private final ObservableList<DetalleOrden> datosDetalle = FXCollections.observableArrayList();
    private final Label lblTablaVacia = new Label("Sin órdenes. Cambia los filtros o pulsa Nueva orden.");
    private OrdenTrabajo ordenActual;

    /** Evita que recargar la tabla vuelva a llenar el formulario por la seleccion automatica. */
    private boolean recargando;

    @FXML
    public void initialize() {
        configurarTablaOrdenes();
        configurarTablaDetalle();
        configurarCabecera();
        configurarEntradaDetalle();

        paginador.configurar(txtBuscar, LJOMpagina -> {
            OrdenService.Filtros JFRPfiltros = filtrosBusqueda.valores(txtBuscar.getText(), null);
            return () -> service.buscar(JFRPfiltros, LJOMpagina, OrdenService.TAMANIO_PAGINA);
        }, AKACpagina -> {
            recargando = true;
            try {
                tabla.getSelectionModel().clearSelection();
                datos.setAll(AKACpagina.getContent());
            } finally {
                recargando = false;
            }
            lblEstado.setText(AKACpagina.getTotalElements() + " orden(es) en total.");
        }, lblEstado::setText);
        filtrosBusqueda.configurar(usuarioService, sesion.esAdmin(), paginador::reiniciar);
        tabsOrden.getSelectionModel().selectedItemProperty().addListener((GGFJobs, LJOMantes, JFRPtab) -> {
            Ui.mostrar(barraFiltros, JFRPtab != tabFotos);
            if (JFRPtab == tabFotos) {
                masDetalles.setExpanded(false);
                fotosViewController.recargar();
            }
        });
        txtDescuento.textProperty().addListener((AKACobs, GGFJantes, LJOMahora) -> actualizarTotales());
        boolean JFRPadmin = sesion.esAdmin();
        Ui.mostrar(btnPdf, JFRPadmin);
        Ui.mostrar(btnPagos, JFRPadmin);
        Ui.mostrar(lblCobrado, JFRPadmin);
        Ui.mostrar(lblSaldo, JFRPadmin);
        Ui.mostrar(txtExternoCosto, JFRPadmin);
        colPago.setVisible(JFRPadmin);
        prepararNuevo();
    }

    public void configurarInspeccion(java.util.function.LongConsumer abrir) {
        abrirInspeccion = abrir;
    }

    @FXML
    private void onInspeccion() {
        if (ordenActual != null && abrirInspeccion != null) {
            abrirInspeccion.accept(ordenActual.getId());
        }
    }

    public void abrirOrden(long id) {
        Ui.ejecutar(vistaOrdenes, () -> service.buscar(id), this::mostrarEnFormulario, lblEstado::setText);
    }

    public void recargar() {
        paginador.recargar();
    }

    public void enfocarBusqueda() {
        if (tabsOrden.getSelectionModel().getSelectedItem() == tabFotos) {
            tabsOrden.getSelectionModel().select(0);
        }
        txtBuscar.requestFocus();
        txtBuscar.selectAll();
    }

    // ------------------------------------------------------------------
    // Configuracion inicial
    // ------------------------------------------------------------------

    private void configurarTablaOrdenes() {
        colId.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getNumeroOrden()));
        colFecha.setCellValueFactory(GGFJc ->
                new SimpleStringProperty(String.valueOf(GGFJc.getValue().getFechaIngreso())));
        colPlaca.setCellValueFactory(LJOMc ->
                new SimpleStringProperty(LJOMc.getValue().getDatosHistoricos().getPlaca()));
        colCliente.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(JFRPc.getValue().getDatosHistoricos().getClienteNombre()));
        colEstadoOrden.setCellValueFactory(AKACc ->
                new SimpleStringProperty(String.valueOf(AKACc.getValue().getEstado())));
        colEstadoOrden.setCellFactory(Ui.celdaInsignia());
        colMecanico.setCellValueFactory(GGFJc -> new SimpleStringProperty(Ui.texto(GGFJc.getValue().getMecanico())));
        colDias.setCellValueFactory(LJOMc -> new SimpleStringProperty(diasEnTaller(LJOMc.getValue())));
        colDias.setCellFactory(JFRPcolumna -> new TableCell<OrdenTrabajo, String>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                getStyleClass().remove(CLASE_DIAS_ALERTA);
                if (vacio || item == null || item.isEmpty()) {
                    setText(null);
                    return;
                }
                setText(item);
                if (superaLimiteDias(item)) {
                    getStyleClass().add(CLASE_DIAS_ALERTA);
                }
            }
        });
        colDias.setStyle("-fx-alignment: CENTER;");
        colPago.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getEstadoPago().toString()));
        colPago.setCellFactory(Ui.celdaInsignia());
        colTotal.setCellValueFactory(GGFJc -> new SimpleStringProperty(Ui.moneda(GGFJc.getValue().getTotal())));
        colTotal.setCellFactory(LJOMc -> Ui.celdaImporte());

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(lblTablaVacia);
        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty()
                .addListener((JFRPobs, AKACanterior, GGFJactual) -> {
                    if (!recargando && GGFJactual != null) {
                        abrirOrden(GGFJactual.getId());
                    }
                });
        // Menu contextual (clic derecho) sobre cada fila; en filas vacias no aparece
        tabla.setRowFactory(LJOMtv -> {
            TableRow<OrdenTrabajo> JFRPfila = new TableRow<>();
            JFRPfila.contextMenuProperty().bind(Bindings.when(JFRPfila.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(crearMenuContextual(JFRPfila)));
            return JFRPfila;
        });
    }

    private void configurarTablaDetalle() {
        colDTipo.setCellValueFactory(AKACc -> new SimpleStringProperty(String.valueOf(AKACc.getValue().getTipo())));
        colDDescripcion.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getDescripcion()
                + (GGFJc.getValue().isDevueltoEnAnulacion() ? " (devuelto)" : "")));
        // Un servicio no tiene cantidad: se muestra "-"
        colDCantidad.setCellValueFactory(LJOMc -> new SimpleStringProperty(
                LJOMc.getValue().getTipo() == TipoDetalle.SERVICIO ? "-" : String.valueOf(LJOMc.getValue().getCantidad())));
        colDCantidad.setStyle("-fx-alignment: CENTER;");
        colDPrecio.setCellValueFactory(JFRPc ->
                new SimpleStringProperty(Ui.moneda(JFRPc.getValue().getPrecioUnitario())));
        colDSubtotal.setCellValueFactory(AKACc ->
                new SimpleStringProperty(Ui.moneda(AKACc.getValue().getSubtotal())));
        colDPrecio.setCellFactory(GGFJc -> Ui.celdaImporte());
        colDSubtotal.setCellFactory(LJOMc -> Ui.celdaImporte());

        tablaDetalles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaDetalles.setPlaceholder(new Label("Aún no se agregaron servicios ni repuestos"));
        tablaDetalles.setItems(datosDetalle);
        // Doble clic: editar el servicio o repuesto en una ventana
        tablaDetalles.setRowFactory(JFRPtv -> {
            TableRow<DetalleOrden> AKACfila = new TableRow<>();
            AKACfila.setOnMouseClicked(GGFJevento -> {
                if (GGFJevento.getButton() == MouseButton.PRIMARY && GGFJevento.getClickCount() == 2 && !AKACfila.isEmpty()) {
                    editarDetalle(AKACfila.getItem());
                }
            });
            return AKACfila;
        });
    }

    private void configurarCabecera() {
        cmbVehiculo.configurar(LJOMtexto -> vehiculoService.buscar(LJOMtexto, 0, ComboBuscable.LIMITE).getContent(),
                Ui::vehiculoEnBusqueda, Ui::vehiculoEnBusqueda);
        if (sesion.esAdmin()) {
            cmbVehiculo.permitirAlta(() -> navegador.pedirAlta(cmbVehiculo, "Nuevo vehículo", "vehiculo",
                    VehiculoController.class, VehiculoController::enAltaModal));
        }
        cmbMecanico.configurar(JFRPtexto -> usuarioService.buscar(JFRPtexto, 0, ComboBuscable.LIMITE).getContent(),
                Usuario::getNombre, Ui::usuarioEnBusqueda);

        // Validacion visual: el borde rojo se quita en cuanto el usuario corrige ese campo
        lblErrores.managedProperty().bind(lblErrores.visibleProperty());
        lblErrores.setVisible(false);
        cmbVehiculo.valueProperty().addListener((AKACobs, GGFJa, LJOMb) -> {
            quitarError(cmbVehiculo);
            if (LJOMb != null && ordenActual == null) {
                Platform.runLater(txtProblema::requestFocus);
            }
        });
        txtProblema.textProperty().addListener((JFRPobs, AKACa, GGFJb) -> quitarError(txtProblema));
        txtKm.textProperty().addListener((LJOMobs, JFRPa, AKACb) -> quitarError(txtKm));
    }

    private void configurarEntradaDetalle() {
        cmbTipo.getItems().setAll(TipoDetalle.values());
        cmbTipo.valueProperty().addListener((GGFJobs, LJOMa, JFRPb) -> actualizarModoEntrada());
        cmbTipo.setValue(TipoDetalle.SERVICIO);

        // Servicio: la descripcion es un combo editable con sugerencias; se puede escribir cualquier otra
        cmbServicio.getItems().setAll(SERVICIOS_FRECUENTES);
        cmbServicio.setEditable(true);

        // Repuesto: un solo combo del inventario; se elige de la lista o se busca escribiendo el nombre
        cmbRepuesto.configurar(AKACtexto -> repuestoService.buscar(AKACtexto, false, 0, ComboBuscable.LIMITE).getContent(),
                Repuesto::getNombre, Ui::repuestoEnBusqueda,
                GGFJr -> true, LJOMr -> LJOMr.getStock() == 0 ? "Agotado" : LJOMr.esStockBajo() ? "Stock bajo" : "");
        if (sesion.esAdmin()) {
            cmbRepuesto.permitirAlta(() -> navegador.pedirAlta(cmbRepuesto, "Nuevo repuesto", "repuesto",
                    RepuestoController.class, RepuestoController::enAltaModal));
        }
        cmbRepuesto.valueProperty().addListener((JFRPobs, AKACa, GGFJr) -> actualizarInfoRepuesto(GGFJr));
        txtCantidad.setText("1");

        actualizarModoEntrada();
        actualizarInfoRepuesto(null);
    }

    private ContextMenu crearMenuContextual(TableRow<OrdenTrabajo> LJOMfila) {
        Menu JFRPmarcar = new Menu("Marcar como");
        MenuItem AKACpdf = new MenuItem("Generar PDF");
        AKACpdf.setVisible(sesion.esAdmin());
        AKACpdf.setOnAction(GGFJe -> generarPdf(LJOMfila.getItem()));
        MenuItem LJOMwhatsApp = new MenuItem("Avisar por WhatsApp");
        LJOMwhatsApp.setOnAction(JFRPe -> avisarWhatsApp(LJOMfila.getItem()));
        MenuItem AKACanular = new MenuItem("Anular");
        AKACanular.setOnAction(GGFJe -> anularOrden(LJOMfila.getItem()));
        MenuItem LJOMreabrir = new MenuItem("Reabrir");
        LJOMreabrir.setOnAction(JFRPe -> reabrirOrden(LJOMfila.getItem()));
        ContextMenu AKACmenu = new ContextMenu(JFRPmarcar, new SeparatorMenuItem(), AKACpdf, LJOMwhatsApp,
                new SeparatorMenuItem(), AKACanular, LJOMreabrir);
        AKACmenu.setOnShowing(GGFJe -> {
            EstadoOrden LJOMactual = LJOMfila.getItem().getEstado();
            JFRPmarcar.getItems().setAll(OrdenService.transicionesDesde(LJOMactual).stream()
                    .filter(JFRPestado -> JFRPestado != EstadoOrden.ANULADO)
                    .map(AKACestado -> {
                        MenuItem GGFJitem = new MenuItem(AKACestado.toString());
                        GGFJitem.setOnAction(LJOMevento -> cambiarEstado(LJOMfila.getItem(), AKACestado));
                        return GGFJitem;
                    }).toList());
            JFRPmarcar.setDisable(JFRPmarcar.getItems().isEmpty());
            AKACanular.setVisible(OrdenService.transicionesDesde(LJOMactual).contains(EstadoOrden.ANULADO));
            LJOMreabrir.setVisible(LJOMactual == EstadoOrden.ENTREGADO && sesion.esAdmin());
        });
        return AKACmenu;
    }

    // ------------------------------------------------------------------
    // Cabecera de la orden
    // ------------------------------------------------------------------

    @FXML
    private void onNuevo() {
        abrirNueva();
    }

    public void abrirNueva() {
        tabla.getSelectionModel().clearSelection();
        prepararNuevo();
        lblEstado.setText("Busca una placa, selecciona el vehículo y escribe el motivo.");
        Platform.runLater(cmbVehiculo::enfocar);
    }

    private void prepararNuevo() {
        ordenActual = null;
        fotosViewController.mostrarOrden(null);
        limpiarErrores();
        lblNumeroOrden.setText("");
        masDetalles.setExpanded(false);
        cmbVehiculo.setValue(null);
        mostrarOpcionesEstado(EstadoOrden.RECIBIDO, false);
        cmbMecanico.setValue(sesion.esAdmin() ? null : sesion.getUsuario());
        cmbMecanico.setPromptText("Buscar mecánico (opcional)");
        txtKm.clear();
        txtProblema.clear();
        txtDescuento.clear();
        datosDetalle.clear();
        limpiarEntrada();
        actualizarPanelDetalle();
    }

    @FXML
    private void onGuardar() {
        limpiarErrores();
        List<String> JFRPfaltantes = new ArrayList<>();
        if (cmbVehiculo.getValue() == null) {
            marcarError(cmbVehiculo);
            JFRPfaltantes.add("Selecciona un vehículo");
        }
        if (txtProblema.getText() == null || txtProblema.getText().isBlank()) {
            marcarError(txtProblema);
            JFRPfaltantes.add("Describe el problema reportado");
        }
        if (cmbMecanico.tieneTextoSinSeleccion()) {
            JFRPfaltantes.add("Selecciona un mecánico de los resultados o limpia el campo");
        }
        Integer AKACkm = null;
        if (txtKm.getText() != null && !txtKm.getText().isBlank()) {
            try {
                AKACkm = Integer.valueOf(txtKm.getText().trim());
            } catch (NumberFormatException GGFJe) {
                marcarError(txtKm);
                JFRPfaltantes.add("El kilometraje debe ser un número entero");
            }
        }
        if (!JFRPfaltantes.isEmpty()) {
            lblErrores.setText(String.join(" · ", JFRPfaltantes));
            lblErrores.setVisible(true);
            return;
        }

        OrdenTrabajo LJOMorden = new OrdenTrabajo();
        LJOMorden.setId(ordenActual == null ? null : ordenActual.getId());
        LJOMorden.setVehiculo(cmbVehiculo.getValue());
        LJOMorden.setEstado(cmbEstado.getValue());
        LJOMorden.setMecanicoUsuario(cmbMecanico.getValue());
        LJOMorden.setKilometraje(AKACkm);
        LJOMorden.setProblema(txtProblema.getText().trim());
        try {
            LJOMorden.setDescuento(leerDescuento());
            guardarCabecera(LJOMorden, null);
        } catch (ConstraintViolationException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(JFRPe));
        } catch (NegocioException AKACe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo guardar", AKACe.getMessage());
        } catch (Exception GGFJe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(GGFJe.getMessage()));
        }
    }

    private void guardarCabecera(OrdenTrabajo LJOMorden, String JFRPautorizacion) {
        Ui.ejecutar(vistaOrdenes, () -> {
            try {
                return new ResultadoGuardado(JFRPautorizacion == null ? service.guardar(LJOMorden)
                        : service.guardarConEntregaAutorizada(LJOMorden, JFRPautorizacion), null);
            } catch (SaldoPendienteException AKACsaldo) {
                return new ResultadoGuardado(null, AKACsaldo);
            }
        }, GGFJresultado -> {
            if (GGFJresultado.saldo() != null) {
                pedirAutorizacion(GGFJresultado.saldo()).ifPresent(LJOMmotivo -> guardarCabecera(LJOMorden, LJOMmotivo));
                return;
            }
            mostrarEnFormulario(GGFJresultado.orden());
            paginador.recargar();
            lblEstado.setText("Orden " + GGFJresultado.orden().getNumeroOrden() + " guardada.");
            if (LJOMorden.getId() == null) {
                ofrecerHallazgos(GGFJresultado.orden().getId());
            }
        }, JFRPerror -> {
            lblErrores.setText(JFRPerror);
            lblErrores.setVisible(true);
        });
    }

    private void ofrecerHallazgos(Long AKACorden) {
        Ui.ejecutar(vistaOrdenes, () -> hallazgos.abiertosParaIncluir(AKACorden), GGFJpendientes -> {
            if (GGFJpendientes.isEmpty() || ordenActual == null || !AKACorden.equals(ordenActual.getId())) {
                return;
            }
            Dialog<ButtonType> LJOMdialogo = new Dialog<>();
            LJOMdialogo.initOwner(vistaOrdenes.getScene().getWindow());
            LJOMdialogo.setTitle("Hallazgos abiertos del vehículo");
            LJOMdialogo.setHeaderText("Selecciona cuáles se atenderán en esta orden.");
            VBox JFRPopciones = new VBox(7);
            var AKACcasillas = GGFJpendientes.stream().map(GGFJh -> {
                var LJOMcasilla = new javafx.scene.control.CheckBox(GGFJh.getZona() + " · " + GGFJh.getTipo());
                LJOMcasilla.setWrapText(true);
                return LJOMcasilla;
            }).toList();
            JFRPopciones.getChildren().setAll(AKACcasillas);
            var JFRPpanel = new javafx.scene.control.ScrollPane(JFRPopciones);
            JFRPpanel.setFitToWidth(true);
            JFRPpanel.setPrefViewportHeight(Math.min(260, 38 * GGFJpendientes.size()));
            JFRPpanel.setPrefViewportWidth(390);
            LJOMdialogo.getDialogPane().getStylesheets().setAll(vistaOrdenes.getScene().getStylesheets());
            LJOMdialogo.getDialogPane().setContent(JFRPpanel);
            ButtonType AKACincluir = new ButtonType("Incluir", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            LJOMdialogo.getDialogPane().getButtonTypes().addAll(AKACincluir, ButtonType.CANCEL);
            LJOMdialogo.showAndWait().filter(AKACincluir::equals).ifPresent(GGFJconfirmado -> {
                var LJOMseleccion = java.util.stream.IntStream.range(0, GGFJpendientes.size())
                        .filter(JFRPi -> AKACcasillas.get(JFRPi).isSelected()).mapToObj(AKACi -> GGFJpendientes.get(AKACi).getId()).toList();
                if (!LJOMseleccion.isEmpty()) {
                    Ui.ejecutar(vistaOrdenes, () -> {
                        hallazgos.incluir(AKACorden, LJOMseleccion);
                        return LJOMseleccion.size();
                    }, GGFJn -> lblEstado.setText(GGFJn + " hallazgo(s) incluidos en la orden."), lblEstado::setText);
                }
            });
        }, lblEstado::setText);
    }

    private record ResultadoGuardado(OrdenTrabajo orden, SaldoPendienteException saldo) {
    }

    private Optional<String> pedirAutorizacion(SaldoPendienteException LJOMpendiente) {
        if (!sesion.esAdmin()) {
            Ui.alerta(Alert.AlertType.WARNING, "Entrega bloqueada", LJOMpendiente.getMessage());
            return Optional.empty();
        }
        TextInputDialog JFRPdialogo = new TextInputDialog();
        JFRPdialogo.initOwner(vistaOrdenes.getScene().getWindow());
        JFRPdialogo.setTitle("Autorizar entrega con saldo");
        JFRPdialogo.setHeaderText("Saldo pendiente: " + Ui.moneda(LJOMpendiente.getSaldo()));
        JFRPdialogo.setContentText("Motivo obligatorio");
        JFRPdialogo.getDialogPane().getStylesheets().addAll(vistaOrdenes.getScene().getStylesheets());
        return JFRPdialogo.showAndWait();
    }

    @FXML
    private void onPagos() {
        if (ordenActual == null) {
            return;
        }
        try {
            if (leerDescuento().compareTo(ordenActual.getDescuento()) != 0) {
                throw new NegocioException("Guarda el descuento de la orden antes de abrir Pagos.");
            }
        } catch (NegocioException AKACe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa el descuento", AKACe.getMessage());
            return;
        }
        Long GGFJid = ordenActual.getId();
        DialogoPagos.mostrar(vistaOrdenes.getScene().getWindow(), ordenActual, service, pagos,
                () -> Ui.ejecutar(vistaOrdenes, () -> service.buscar(GGFJid), this::actualizarOrdenAbierta, lblEstado::setText));
    }

    @FXML
    private void onAnular() {
        anularOrden(ordenActual);
    }

    private void anularOrden(OrdenTrabajo LJOMorden) {
        if (LJOMorden == null) {
            return;
        }
        Ui.ejecutar(vistaOrdenes, () -> service.buscar(LJOMorden.getId()),
                JFRPcompleta -> pedirMotivo(JFRPcompleta, true).ifPresent(AKACdatos -> ejecutarAccion(
                        () -> service.anular(JFRPcompleta.getId(), AKACdatos.motivo(), AKACdatos.noInstalados()),
                        "Orden anulada.")), lblEstado::setText);
    }

    private void reabrirOrden(OrdenTrabajo GGFJorden) {
        if (GGFJorden == null) {
            return;
        }
        pedirMotivo(GGFJorden, false).ifPresent(LJOMdatos -> ejecutarAccion(
                () -> service.reabrir(GGFJorden.getId(), LJOMdatos.motivo()),
                "Orden reabierta en Listo."));
    }

    private record DatosAccion(String motivo, Set<Long> noInstalados) {
    }

    private Optional<DatosAccion> pedirMotivo(OrdenTrabajo JFRPorden, boolean AKACanulacion) {
        String GGFJaccion = AKACanulacion ? "Anular" : "Reabrir";
        Dialog<DatosAccion> LJOMdialogo = new Dialog<>();
        LJOMdialogo.initOwner(tabla.getScene().getWindow());
        LJOMdialogo.setTitle(GGFJaccion + " orden");
        LJOMdialogo.setHeaderText(GGFJaccion + " " + JFRPorden.getNumeroOrden());
        LJOMdialogo.getDialogPane().getStylesheets().addAll(tabla.getScene().getStylesheets());
        ButtonType JFRPconfirmar = new ButtonType(GGFJaccion, ButtonBar.ButtonData.OK_DONE);
        LJOMdialogo.getDialogPane().getButtonTypes().addAll(JFRPconfirmar, ButtonType.CANCEL);
        TextArea AKACmotivo = new TextArea();
        AKACmotivo.setPrefRowCount(3);
        AKACmotivo.setWrapText(true);
        VBox GGFJcontenido = new VBox(10, new Label("Motivo obligatorio"), AKACmotivo);
        Map<Long, CheckBox> LJOMrepuestos = new HashMap<>();
        if (AKACanulacion) {
            VBox JFRPopciones = new VBox(8);
            for (DetalleOrden AKACdetalle : JFRPorden.getDetalles()) {
                if (AKACdetalle.getTipo() == TipoDetalle.REPUESTO && AKACdetalle.getRepuesto() != null) {
                    CheckBox GGFJopcion = new CheckBox(AKACdetalle.getDescripcion() + " (" + AKACdetalle.getCantidad() + ")");
                    LJOMrepuestos.put(AKACdetalle.getId(), GGFJopcion);
                    JFRPopciones.getChildren().add(GGFJopcion);
                }
            }
            if (!LJOMrepuestos.isEmpty()) {
                ScrollPane LJOMlista = new ScrollPane(JFRPopciones);
                LJOMlista.setFitToWidth(true);
                LJOMlista.setPrefViewportHeight(140);
                GGFJcontenido.getChildren().addAll(new Label("Marca solo los repuestos no instalados que vuelven al stock."), LJOMlista);
            }
        }
        LJOMdialogo.getDialogPane().setContent(GGFJcontenido);
        LJOMdialogo.getDialogPane().lookupButton(JFRPconfirmar).disableProperty().bind(Bindings.createBooleanBinding(
                () -> AKACmotivo.getText().isBlank() || AKACmotivo.getText().strip().length() > 500, AKACmotivo.textProperty()));
        LJOMdialogo.setResultConverter(JFRPboton -> JFRPboton == JFRPconfirmar ? new DatosAccion(AKACmotivo.getText(),
                LJOMrepuestos.entrySet().stream().filter(AKACe -> AKACe.getValue().isSelected())
                        .map(Map.Entry::getKey).collect(Collectors.toSet())) : null);
        return LJOMdialogo.showAndWait();
    }

    private void ejecutarAccion(Supplier<OrdenTrabajo> GGFJaccion, String LJOMmensaje) {
        vistaOrdenes.setDisable(true);
        Task<OrdenTrabajo> JFRPtarea = new Task<>() {
            @Override
            protected OrdenTrabajo call() {
                return GGFJaccion.get();
            }
        };
        JFRPtarea.setOnSucceeded(AKACe -> {
            vistaOrdenes.setDisable(false);
            actualizarOrdenAbierta(JFRPtarea.getValue());
            lblEstado.setText(LJOMmensaje);
        });
        JFRPtarea.setOnFailed(GGFJe -> {
            vistaOrdenes.setDisable(false);
            Throwable LJOMerror = JFRPtarea.getException();
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo actualizar la orden",
                    LJOMerror instanceof NegocioException ? LJOMerror.getMessage() : "Ocurrió un error al guardar los cambios.");
        });
        Thread JFRPhilo = new Thread(JFRPtarea, "actualizar-orden");
        JFRPhilo.setDaemon(true);
        JFRPhilo.start();
    }

    private void cambiarEstado(OrdenTrabajo AKACorden, EstadoOrden GGFJestado) {
        if (AKACorden == null) {
            return;
        }
        if (AKACorden.getEstado() == GGFJestado) {
            lblEstado.setText("La orden " + AKACorden.getNumeroOrden() + " ya está en estado " + GGFJestado + ".");
            return;
        }
        try {
            actualizarOrdenAbierta(service.cambiarEstado(AKACorden.getId(), GGFJestado));
            lblEstado.setText("Orden " + AKACorden.getNumeroOrden() + " marcada como " + GGFJestado + ".");
        } catch (SaldoPendienteException LJOMpendiente) {
            pedirAutorizacion(LJOMpendiente).ifPresent(JFRPmotivo -> {
                try {
                    actualizarOrdenAbierta(service.entregarConSaldo(AKACorden.getId(), JFRPmotivo));
                    lblEstado.setText("Entrega autorizada y motivo registrado.");
                } catch (NegocioException AKACe) {
                    Ui.alerta(Alert.AlertType.WARNING, "No se pudo entregar", AKACe.getMessage());
                }
            });
        } catch (NegocioException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo cambiar el estado", GGFJe.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Servicios y repuestos de la orden
    // ------------------------------------------------------------------

    @FXML
    private void onAgregarDetalle() {
        if (ordenActual == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin orden",
                    "Primero guarda o selecciona una orden para agregarle servicios y repuestos.");
            return;
        }
        if (ordenBloqueada()) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Orden cerrada", lblAviso.getText());
            return;
        }
        DetalleOrden LJOMsolicitud = null;
        try {
            LJOMsolicitud = leerDetalleNuevo();
            OrdenTrabajo JFRPactualizada = service.agregarDetalle(ordenActual.getId(), LJOMsolicitud);
            actualizarOrdenAbierta(JFRPactualizada);
            informarConsumo(LJOMsolicitud.getRepuesto(), "Partida agregada.");
        } catch (StockInsuficienteException AKACe) {
            if (LJOMsolicitud != null) {
                ofrecerExterno(AKACe.getMessage(), LJOMsolicitud.getDescripcion(), LJOMsolicitud.getCantidad(),
                        LJOMsolicitud.getRepuesto().getPrecio());
            }
        } catch (ConstraintViolationException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(GGFJe));
        } catch (NegocioException LJOMe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo agregar", LJOMe.getMessage());
        } catch (Exception JFRPe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(JFRPe.getMessage()));
        }
    }

    private DetalleOrden leerDetalleNuevo() {
        DetalleOrden AKACdetalle = new DetalleOrden();
        AKACdetalle.setTipo(cmbTipo.getValue());
        switch (AKACdetalle.getTipo()) {
            case REPUESTO -> {
                Repuesto GGFJrepuesto = cmbRepuesto.getValue();
                if (GGFJrepuesto == null) {
                    throw new NegocioException("Selecciona un repuesto del inventario.");
                }
                AKACdetalle.setDescripcion(GGFJrepuesto.getNombre());
                AKACdetalle.setCantidad(cantidad(txtCantidad.getText()));
                AKACdetalle.setRepuesto(GGFJrepuesto);
            }
            case REPUESTO_EXTERNO -> {
                AKACdetalle.setDescripcion(txtExterno.getText().strip());
                AKACdetalle.setCantidad(cantidad(txtExternoCantidad.getText()));
                if (sesion.esAdmin()) {
                    AKACdetalle.setCostoUnitario(importe(txtExternoCosto.getText(), "costo"));
                }
                AKACdetalle.registrarPrecio(importe(txtExternoPrecio.getText(), "precio"));
            }
            case SERVICIO -> {
                AKACdetalle.setDescripcion(cmbServicio.getEditor().getText().strip());
                AKACdetalle.setCantidad(1);
                AKACdetalle.registrarPrecio(importe(txtPrecio.getText(), "precio"));
            }
        }
        if (AKACdetalle.getDescripcion().isBlank()) {
            throw new NegocioException("Escribe la descripción de la partida.");
        }
        return AKACdetalle;
    }

    private int cantidad(String LJOMtexto) {
        try {
            int JFRPcantidad = Integer.parseInt(LJOMtexto.strip());
            if (JFRPcantidad > 0) {
                return JFRPcantidad;
            }
        } catch (NumberFormatException AKACe) {
            throw new NegocioException("La cantidad debe ser un entero positivo.");
        }
        throw new NegocioException("La cantidad debe ser un entero positivo.");
    }

    private BigDecimal importe(String GGFJtexto, String LJOMcampo) {
        try {
            BigDecimal JFRPvalor = new BigDecimal(GGFJtexto.strip().replace(',', '.'));
            if (JFRPvalor.signum() >= 0) {
                return JFRPvalor;
            }
        } catch (NumberFormatException AKACe) {
            throw new NegocioException("Escribe un " + LJOMcampo + " válido. Ejemplo: 45.50.");
        }
        throw new NegocioException("El " + LJOMcampo + " no puede ser negativo.");
    }

    private void ofrecerExterno(String GGFJmotivo, String LJOMdescripcion, int JFRPcantidad, BigDecimal AKACprecio) {
        if (Ui.confirmar("Stock insuficiente", GGFJmotivo + "\n¿Quieres preparar " + JFRPcantidad
                + " unidad(es) como repuesto externo? No se descontará inventario.")) {
            cmbTipo.setValue(TipoDetalle.REPUESTO_EXTERNO);
            txtExterno.setText(LJOMdescripcion);
            txtExternoCantidad.setText(Integer.toString(JFRPcantidad));
            txtExternoCosto.clear();
            txtExternoPrecio.setText(AKACprecio.toPlainString());
            (sesion.esAdmin() ? txtExternoCosto : txtExternoPrecio).requestFocus();
        }
    }

    private void informarConsumo(Repuesto GGFJrepuesto, String LJOMmensaje) {
        lblEstado.setText(LJOMmensaje);
        if (GGFJrepuesto != null) {
            Long JFRPid = GGFJrepuesto.getId();
            consultaAlertaStock.consultar(false, () -> repuestoService.alertaStock(JFRPid), AKACresultado ->
                    AKACresultado.ifPresent(GGFJr -> {
                        lblAlertaStock.setText("Stock bajo de " + GGFJr.getNombre() + ": quedan " + GGFJr.getStock()
                                + " (mínimo " + GGFJr.getStockMinimo() + ").");
                        Ui.mostrar(lblAlertaStock, true);
                    }), LJOMerror -> Ui.ocultar(lblAlertaStock));
        }
    }

    @FXML
    private void onTrazabilidad() {
        if (ordenActual != null) {
            DialogoTrazabilidad.mostrar(tabla.getScene().getWindow(), ordenActual,
                    tablaDetalles.getSelectionModel().getSelectedItem(), service);
        }
    }

    @FXML
    private void onQuitarDetalle() {
        DetalleOrden JFRPseleccionado = tablaDetalles.getSelectionModel().getSelectedItem();
        if (ordenActual == null || JFRPseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección",
                    "Selecciona un servicio o repuesto de la lista para quitarlo.");
            return;
        }
        if (ordenBloqueada()) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Orden cerrada", lblAviso.getText());
            return;
        }
        try {
            actualizarOrdenAbierta(service.quitarDetalle(ordenActual.getId(), JFRPseleccionado.getId()));
            lblEstado.setText("Quitado de la orden " + ordenActual.getNumeroOrden() + ".");
        } catch (NegocioException AKACe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo quitar", AKACe.getMessage());
        }
    }

    private void editarDetalle(DetalleOrden GGFJdetalle) {
        if (ordenActual == null || GGFJdetalle == null) {
            return;
        }
        if (ordenBloqueada()) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Orden cerrada", lblAviso.getText());
            return;
        }
        Optional<DialogoDetalle.Datos> LJOMdatos = DialogoDetalle.pedir(tablaDetalles.getScene().getWindow(), GGFJdetalle, sesion.esAdmin());
        if (LJOMdatos.isEmpty()) {
            return;
        }
        try {
            var JFRPedicion = LJOMdatos.get();
            OrdenTrabajo AKACactualizada = service.actualizarDetalleConPrecio(ordenActual.getId(), GGFJdetalle.getId(),
                    JFRPedicion.descripcion(), JFRPedicion.precio(), JFRPedicion.cantidad(), JFRPedicion.costoPendiente());
            actualizarOrdenAbierta(AKACactualizada);
            informarConsumo(JFRPedicion.cantidad() > GGFJdetalle.getCantidad() ? GGFJdetalle.getRepuesto() : null,
                    "Partida actualizada.");
        } catch (StockInsuficienteException GGFJe) {
            ofrecerExterno(GGFJe.getMessage(), LJOMdatos.get().descripcion(),
                    LJOMdatos.get().cantidad() - GGFJdetalle.getCantidad(), GGFJdetalle.getPrecioUnitario());
        } catch (ConstraintViolationException LJOMe) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", Ui.violaciones(LJOMe));
        } catch (NegocioException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo actualizar", JFRPe.getMessage());
        } catch (Exception AKACe) {
            Ui.alerta(Alert.AlertType.ERROR, "Error inesperado", String.valueOf(AKACe.getMessage()));
        }
    }

    /** Muestra solo los campos que corresponden al tipo elegido (servicio o repuesto). */
    private void actualizarModoEntrada() {
        boolean GGFJrepuesto = cmbTipo.getValue() == TipoDetalle.REPUESTO;
        Ui.mostrar(filaServicio, cmbTipo.getValue() == TipoDetalle.SERVICIO);
        Ui.mostrar(filaExterno, cmbTipo.getValue() == TipoDetalle.REPUESTO_EXTERNO);
        Ui.mostrar(filaRepuesto, GGFJrepuesto);
        Ui.mostrar(lblStockInfo, GGFJrepuesto);
    }

    private void actualizarInfoRepuesto(Repuesto LJOMrepuesto) {
        lblStockInfo.setText(LJOMrepuesto == null
                ? "Elige un repuesto de la lista o escribe su nombre para buscarlo en el inventario."
                : "Stock disponible: " + LJOMrepuesto.getStock() + " · Precio: " + Ui.moneda(LJOMrepuesto.getPrecio()));
    }

    private void limpiarEntrada() {
        consultaAlertaStock.cancelar();
        Ui.ocultar(lblAlertaStock);
        cmbServicio.setValue(null);
        cmbServicio.getEditor().clear();
        txtPrecio.clear();
        txtExterno.clear();
        txtExternoCantidad.setText("1");
        txtExternoCosto.clear();
        txtExternoPrecio.clear();
        cmbRepuesto.setValue(null);
        txtCantidad.setText("1");
    }

    private boolean ordenBloqueada() {
        return ordenActual != null && OrdenService.transicionesDesde(ordenActual.getEstado()).isEmpty();
    }

    // ------------------------------------------------------------------
    // PDF y aviso al cliente
    // ------------------------------------------------------------------

    @FXML
    private void onPdf() {
        if (ordenActual == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin orden", "Selecciona o guarda una orden para generar su PDF.");
            return;
        }
        generarPdf(ordenActual);
    }

    private void generarPdf(OrdenTrabajo JFRPorden) {
        if (JFRPorden == null || !sesion.esAdmin()) {
            return;
        }
        Ui.ejecutar(vistaOrdenes, () -> fotos.listar(JFRPorden.getId()),
                AKACadjuntos -> vistaPreviaPdf(JFRPorden, AKACadjuntos, "").ifPresent(GGFJincluir -> guardarPdf(JFRPorden, GGFJincluir)),
                LJOMerror -> vistaPreviaPdf(JFRPorden, List.of(), "No se pudieron consultar las fotos. Puedes generar el PDF sin adjuntos.")
                        .ifPresent(JFRPincluir -> guardarPdf(JFRPorden, JFRPincluir)));
    }

    private Optional<Boolean> vistaPreviaPdf(OrdenTrabajo AKACorden, List<OrdenFoto> GGFJadjuntos, String LJOMerror) {
        Dialog<Boolean> JFRPdialogo = new Dialog<>();
        JFRPdialogo.initOwner(tabla.getScene().getWindow());
        JFRPdialogo.setTitle("Generar PDF");
        JFRPdialogo.setHeaderText(AKACorden.getNumeroOrden() + " · " + AKACorden.getDatosHistoricos().getPlaca());
        var AKACpanel = JFRPdialogo.getDialogPane();
        AKACpanel.getStylesheets().setAll(tabla.getScene().getStylesheets());
        AKACpanel.setPrefWidth(575);
        ButtonType GGFJgenerar = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        AKACpanel.getButtonTypes().addAll(ButtonType.CANCEL, GGFJgenerar);
        Ui.accion((Button) AKACpanel.lookupButton(GGFJgenerar), "accent", "accion-principal");
        Ui.accion((Button) AKACpanel.lookupButton(ButtonType.CANCEL), "button-outlined", "accion-secundaria");

        Label LJOMcontenido = new Label("Datos del cliente y del vehículo, motivo, partidas, abonos e inspección, si corresponde.");
        LJOMcontenido.setWrapText(true);
        Label JFRPresumen = new Label("Total " + Ui.moneda(AKACorden.getTotal()) + "   ·   Cobrado "
                + Ui.moneda(AKACorden.getTotalCobrado()) + "   ·   Saldo " + Ui.moneda(AKACorden.getSaldoPendiente()));
        JFRPresumen.setWrapText(true);
        JFRPresumen.getStyleClass().add("importe");
        CheckBox AKACincluir = new CheckBox("Incluir fotos de la orden (" + GGFJadjuntos.size() + ")");
        AKACincluir.setDisable(GGFJadjuntos.isEmpty());
        ListView<String> GGFJlista = new ListView<>(FXCollections.observableArrayList(GGFJadjuntos.stream()
                .map(LJOMfoto -> LJOMfoto.getFase() + (Ui.texto(LJOMfoto.getNota()).isBlank() ? "" : " · " + LJOMfoto.getNota())).toList()));
        GGFJlista.setAccessibleText("Fotos disponibles para el PDF");
        GGFJlista.setPrefHeight(110);
        GGFJlista.setMinHeight(Region.USE_PREF_SIZE);
        Ui.mostrar(GGFJlista, !GGFJadjuntos.isEmpty());
        VBox JFRPopciones = new VBox(10, LJOMcontenido, JFRPresumen, AKACincluir, GGFJlista);
        if (!LJOMerror.isBlank()) {
            Label AKACaviso = new Label(LJOMerror);
            AKACaviso.setWrapText(true);
            JFRPopciones.getChildren().add(AKACaviso);
        }
        AKACpanel.setContent(JFRPopciones);
        JFRPdialogo.setResultConverter(GGFJboton -> GGFJboton == GGFJgenerar ? AKACincluir.isSelected() : null);
        return JFRPdialogo.showAndWait();
    }

    private void guardarPdf(OrdenTrabajo LJOMorden, boolean JFRPincluir) {
        File AKACarchivo = Ui.guardarArchivo(tabla.getScene().getWindow(), "Guardar orden en PDF",
                LJOMorden.getNumeroOrden() + "-" + LJOMorden.getDatosHistoricos().getPlaca() + ".pdf",
                "Documento PDF", "*.pdf");
        if (AKACarchivo == null) {
            return;
        }
        Ui.ejecutar(vistaOrdenes, () -> {
            try {
                pdfService.generarOrden(LJOMorden.getId(), AKACarchivo.toPath(), JFRPincluir);
                return AKACarchivo;
            } catch (IOException GGFJe) {
                throw new NegocioException("No se pudo generar el PDF: " + GGFJe.getMessage());
            }
        }, LJOMguardado -> {
            lblEstado.setText("PDF guardado: " + LJOMguardado.getName());
            host.abrir(LJOMguardado.toURI().toString());
        }, JFRPmensaje -> Ui.alerta(Alert.AlertType.ERROR, "No se pudo generar el PDF", JFRPmensaje));
    }

    @FXML
    private void onWhatsApp() {
        if (ordenActual == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin orden",
                    "Selecciona una orden para avisar al cliente.");
            return;
        }
        avisarWhatsApp(ordenActual);
    }

    private void avisarWhatsApp(OrdenTrabajo AKACorden) {
        if (AKACorden == null) {
            return;
        }
        DatosOrden GGFJcliente = AKACorden.getDatosHistoricos();
        String LJOMtelefono = Ui.texto(GGFJcliente.getClienteTelefono());
        String JFRPmensaje = String.format(Locale.US,
                "Hola %s, le informamos que su vehículo %s (%s) se encuentra en estado: %s. "
                        + "Total actual: %s. Gracias por confiar en nosotros.",
                GGFJcliente.getClienteNombre(),
                AKACorden.getDatosHistoricos().getPlaca(),
                AKACorden.getDatosHistoricos().getDescripcionVehiculo(),
                AKACorden.getEstado(),
                Ui.moneda(AKACorden.getTotal()));
        Ui.abrirWhatsApp(host, LJOMtelefono, JFRPmensaje);
    }

    /** Dias desde el ingreso; vacio para las ordenes ya entregadas. */
    private String diasEnTaller(OrdenTrabajo AKACorden) {
        if (OrdenService.transicionesDesde(AKACorden.getEstado()).isEmpty() || AKACorden.getFechaIngreso() == null) {
            return "";
        }
        long GGFJdias = ChronoUnit.DAYS.between(AKACorden.getFechaIngreso(), LocalDate.now());
        return String.valueOf(Math.max(GGFJdias, 0));
    }

    private static boolean superaLimiteDias(String LJOMtexto) {
        try {
            return Integer.parseInt(LJOMtexto) > DIAS_ALERTA;
        } catch (NumberFormatException JFRPe) {
            return false;
        }
    }

    private void actualizarOrdenAbierta(OrdenTrabajo AKACorden) {
        if (ordenActual != null && ordenActual.getId().equals(AKACorden.getId())) {
            mostrarEnFormulario(AKACorden);
        }
        paginador.recargar();
    }

    private void mostrarEnFormulario(OrdenTrabajo GGFJo) {
        if (GGFJo == null) {
            return;
        }
        ordenActual = GGFJo;
        fotosViewController.mostrarOrden(GGFJo);
        limpiarErrores();
        lblNumeroOrden.setText(GGFJo.getNumeroOrden());
        cmbVehiculo.setValue(GGFJo.getVehiculo());
        mostrarOpcionesEstado(GGFJo.getEstado(), true);
        cmbMecanico.setValue(GGFJo.getMecanicoUsuario());
        cmbMecanico.setPromptText(GGFJo.getMecanicoUsuario() == null && GGFJo.getMecanico() != null && !GGFJo.getMecanico().isBlank()
                ? GGFJo.getMecanico() + " (histórico)" : "Buscar mecánico (opcional)");
        txtKm.setText(GGFJo.getKilometraje() == null ? "" : String.valueOf(GGFJo.getKilometraje()));
        txtProblema.setText(GGFJo.getProblema());
        txtDescuento.setText(GGFJo.getDescuento().toPlainString());
        datosDetalle.setAll(GGFJo.getDetalles());
        limpiarEntrada();
        actualizarPanelDetalle();
    }

    private void actualizarPanelDetalle() {
        if (ordenActual == null) {
            lblOrdenActual.setText("Servicios y repuestos (sin orden seleccionada)");
        } else {
            lblOrdenActual.setText("Servicios y repuestos de la orden " + ordenActual.getNumeroOrden()
                    + " - " + ordenActual.getDatosHistoricos().getPlaca());
        }
        boolean LJOMregistrada = ordenActual != null;
        btnTrazabilidad.setDisable(!LJOMregistrada);
        btnPagos.setDisable(!LJOMregistrada);
        btnInspeccion.setDisable(!LJOMregistrada);
        actualizarTotales();
        Ui.mostrar(lblDatosHistoricos, LJOMregistrada);
        if (LJOMregistrada) {
            DatosOrden JFRPdatos = ordenActual.getDatosHistoricos();
            lblDatosHistoricos.setText(JFRPdatos.getClienteNombre() + " · DNI " + JFRPdatos.getClienteDni()
                    + " · " + JFRPdatos.getDescripcionVehiculo());
        }
        actualizarGanancia();
        boolean AKACbloqueada = ordenBloqueada();
        btnGuardar.setDisable(AKACbloqueada);
        btnAnular.setDisable(ordenActual == null || AKACbloqueada);
        cmbVehiculo.setDisable(AKACbloqueada || LJOMregistrada && ordenActual.getDetalles().stream()
                .anyMatch(GGFJd -> GGFJd.getTipo() == TipoDetalle.REPUESTO));
        cmbEstado.setDisable(AKACbloqueada || !LJOMregistrada);
        cmbMecanico.setDisable(AKACbloqueada || !sesion.esAdmin());
        txtKm.setEditable(!AKACbloqueada);
        txtProblema.setEditable(!AKACbloqueada);
        txtDescuento.setEditable(!AKACbloqueada && sesion.esAdmin());
        panelEntrada.setDisable(AKACbloqueada);
        btnQuitar.setDisable(AKACbloqueada);
        Ui.mostrar(lblAviso, AKACbloqueada);
        if (AKACbloqueada) {
            lblAviso.setText(ordenActual.getEstado() == EstadoOrden.ANULADO
                    ? "Anulada por " + ordenActual.getUsuarioAnulacion() + ". Motivo: " + ordenActual.getMotivoAnulacion()
                    : OrdenService.MSG_ORDEN_ENTREGADA);
        }
    }

    private BigDecimal leerDescuento() {
        BigDecimal LJOMdescuento = txtDescuento.getText().isBlank() ? CentimosConverter.CERO
                : importe(txtDescuento.getText(), "descuento");
        if (LJOMdescuento.stripTrailingZeros().scale() > 2) {
            throw new NegocioException("El descuento admite hasta dos decimales.");
        }
        return LJOMdescuento;
    }

    private void actualizarTotales() {
        OrdenTrabajo JFRPorden = ordenActual == null ? new OrdenTrabajo() : ordenActual;
        BigDecimal AKACdescuento = JFRPorden.getDescuento();
        try {
            BigDecimal GGFJescrito = leerDescuento();
            if (GGFJescrito.compareTo(JFRPorden.getSubtotal()) <= 0) {
                AKACdescuento = GGFJescrito;
            }
        } catch (NegocioException LJOMe) {
            // La entrada en edición se valida antes de guardar; la vista mantiene el último importe válido.
        }
        OrdenTrabajo.Importes JFRPimportes = JFRPorden.importes(AKACdescuento);
        lblSubtotal.setText("Subtotal " + Ui.moneda(JFRPimportes.subtotal()));
        lblTotal.setText("Total " + Ui.moneda(JFRPimportes.total()));
        lblCobrado.setText("Cobrado " + Ui.moneda(JFRPimportes.cobrado()));
        lblSaldo.setText("Saldo pendiente " + Ui.moneda(JFRPimportes.pendiente()));
        lblAFavor.setText("A favor " + Ui.moneda(JFRPimportes.aFavor()));
        Ui.mostrar(lblAFavor, sesion.esAdmin() && JFRPimportes.aFavor().signum() > 0);
    }

    private void actualizarGanancia() {
        consultaGanancia.cancelar();
        boolean AKACvisible = ordenActual != null && sesion.esAdmin() && ordenActual.getEstado() != EstadoOrden.ANULADO;
        Ui.mostrar(lblGanancia, AKACvisible);
        if (AKACvisible) {
            Long GGFJid = ordenActual.getId();
            lblGanancia.setText("");
            consultaGanancia.consultar(false, () -> service.ganancia(GGFJid), LJOMresultado ->
                    lblGanancia.setText(LJOMresultado.map(JFRPmargen -> "Ganancia estimada: " + Ui.moneda(JFRPmargen))
                            .orElse("Ganancia no disponible: faltan costos históricos.")),
                    AKACerror -> lblGanancia.setText("No se pudo consultar la ganancia."));
        }
    }

    private void mostrarOpcionesEstado(EstadoOrden GGFJactual, boolean LJOMregistrada) {
        List<EstadoOrden> JFRPopciones = new ArrayList<>();
        JFRPopciones.add(GGFJactual);
        if (LJOMregistrada) {
            JFRPopciones.addAll(OrdenService.transicionesDesde(GGFJactual).stream()
                    .filter(AKACestado -> AKACestado != EstadoOrden.ANULADO).toList());
        }
        cmbEstado.getItems().setAll(JFRPopciones);
        cmbEstado.setValue(GGFJactual);
    }

    // ------------------------------------------------------------------
    // Validacion visual (borde rojo + resumen de errores)
    // ------------------------------------------------------------------

    private void marcarError(Node GGFJcampo) {
        if (!GGFJcampo.getStyleClass().contains(CLASE_ERROR)) {
            GGFJcampo.getStyleClass().add(CLASE_ERROR);
        }
    }

    private void quitarError(Node LJOMcampo) {
        LJOMcampo.getStyleClass().remove(CLASE_ERROR);
        boolean JFRPquedan = cmbVehiculo.getStyleClass().contains(CLASE_ERROR)
                || txtProblema.getStyleClass().contains(CLASE_ERROR)
                || txtKm.getStyleClass().contains(CLASE_ERROR);
        if (!JFRPquedan) {
            lblErrores.setVisible(false);
        }
    }

    private void limpiarErrores() {
        cmbVehiculo.getStyleClass().remove(CLASE_ERROR);
        txtProblema.getStyleClass().remove(CLASE_ERROR);
        txtKm.getStyleClass().remove(CLASE_ERROR);
        lblErrores.setVisible(false);
    }
}
