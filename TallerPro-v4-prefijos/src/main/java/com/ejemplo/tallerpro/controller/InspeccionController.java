package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.*;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.*;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.*;
import com.ejemplo.tallerpro.service.*;
import com.ejemplo.tallerpro.service.HallazgoService.*;
import com.ejemplo.tallerpro.util.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class InspeccionController {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private enum FiltroEstado {
        PENDIENTES("Abiertos y en reparación", Set.of(Estado.ABIERTO, Estado.EN_REPARACION)),
        TODOS("Todos los estados", Set.of()),
        ABIERTOS("Abiertos", Set.of(Estado.ABIERTO)),
        REPARACION("En reparación", Set.of(Estado.EN_REPARACION)),
        REPARADOS("Reparados", Set.of(Estado.REPARADO)),
        DESCARTADOS("Descartados", Set.of(Estado.DESCARTADO));

        final String etiqueta;
        final Set<Estado> estados;
        FiltroEstado(String etiqueta, Set<Estado> estados) {
            this.etiqueta = etiqueta;
            this.estados = estados;
        }
        @Override public String toString() { return etiqueta; }
    }

    private final HallazgoService servicio;
    private final OrdenService ordenes;
    private final OrdenFotoService fotos;
    private final RepuestoService repuestos;
    private final SesionActual sesion;
    private final BusquedaAsincrona<List<Vehiculo>> consultaVehiculos = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<Ficha> consultaFicha = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<List<HallazgoVehiculo>> consultaHallazgos = new BusquedaAsincrona<>();
    private final Map<Capa, CheckBox> capas = new EnumMap<>(Capa.class);

    @FXML private VBox raiz;
    @FXML private ComboBuscable<Cliente> txtBuscar;
    @FXML private FlowPane vehiculosCliente;
    @FXML private Label lblVehiculo;
    @FXML private Label lblAviso;
    @FXML private Label lblSeleccion;
    @FXML private ComboBox<OrdenOpcion> ordenActual;
    @FXML private ComboBox<OrdenOpcion> filtroOrden;
    @FXML private ComboBox<Vista> cmbVista;
    @FXML private ComboBox<FiltroEstado> filtroEstado;
    @FXML private ComboBox<Capa> filtroCapa;
    @FXML private ComboBox<Gravedad> filtroGravedad;
    @FXML private DatePicker filtroDesde;
    @FXML private DatePicker filtroHasta;
    @FXML private FlowPane interruptores;
    @FXML private DiagramaInspeccion diagrama;
    @FXML private ListView<PlantillasInspeccion.Marca> lista;
    @FXML private Button btnRegistrar;
    @FXML private Button btnEditar;
    @FXML private Button btnDescartar;
    @FXML private Button btnConvertir;
    @FXML private Button btnHistorial;
    @FXML private Button btnFoto;

    private Ficha ficha;
    private Long ordenDirecta;
    private DiagramaInspeccion.Punto punto;
    private boolean cargando;
    private List<PlantillasInspeccion.Marca> marcas = List.of();

    @FXML
    void initialize() {
        txtBuscar.configurar(GGFJt -> servicio.buscarClientes(GGFJt, ComboBuscable.LIMITE),
                Ui::clienteEnBusqueda, Ui::clienteEnBusqueda);
        Ui.ocultar(vehiculosCliente);
        txtBuscar.valueProperty().addListener((JFRPobs, AKACantes, GGFJcliente) -> {
            if (!cargando) {
                mostrarCliente(GGFJcliente);
            }
        });

        cmbVista.getItems().setAll(Vista.values());
        cmbVista.setValue(Vista.SUPERIOR);
        filtroEstado.getItems().setAll(FiltroEstado.values());
        filtroEstado.setValue(FiltroEstado.PENDIENTES);
        valores(filtroCapa, Capa.values(), "Todas las capas");
        valores(filtroGravedad, Gravedad.values(), "Toda gravedad");
        filtroOrden.setPromptText("Todas las órdenes");
        ordenActual.setPromptText(sesion.esAdmin() ? "Sin orden activa" : "Selecciona una orden asignada");
        cmbVista.valueProperty().addListener((LJOMobs, JFRPa, AKACb) -> dibujar());
        filtroEstado.valueProperty().addListener((GGFJobs, LJOMa, JFRPb) -> recargar());
        filtroCapa.valueProperty().addListener((AKACobs, GGFJa, LJOMb) -> recargar());
        filtroGravedad.valueProperty().addListener((JFRPobs, AKACa, GGFJb) -> recargar());
        filtroOrden.valueProperty().addListener((LJOMobs, JFRPa, AKACb) -> recargar());
        filtroDesde.valueProperty().addListener((GGFJobs, LJOMa, JFRPb) -> recargar());
        filtroHasta.valueProperty().addListener((AKACobs, GGFJa, LJOMb) -> recargar());
        ordenActual.valueProperty().addListener((JFRPobs, AKACa, GGFJb) -> acciones());
        for (Capa LJOMcapa : Capa.values()) {
            CheckBox JFRPcontrol = new CheckBox(LJOMcapa + " (0)");
            JFRPcontrol.setSelected(true);
            JFRPcontrol.selectedProperty().addListener((AKACobs, GGFJa, LJOMb) -> dibujar());
            interruptores.getChildren().add(JFRPcontrol);
            capas.put(LJOMcapa, JFRPcontrol);
        }

        lista.setPlaceholder(new Label("Selecciona un vehículo para ver sus hallazgos."));
        lista.setCellFactory(JFRPv -> new ListCell<>() {
            @Override protected void updateItem(PlantillasInspeccion.Marca valor, boolean vacia) {
                super.updateItem(valor, vacia);
                if (vacia || valor == null) {
                    setGraphic(null);
                    return;
                }
                var AKACh = valor.hallazgo();
                Label GGFJtitulo = new Label(valor.numero() + ". " + AKACh.getZona() + " · " + AKACh.getTipo());
                GGFJtitulo.setWrapText(true);
                Label LJOMestado = new Label(AKACh.getGravedad() + " · " + AKACh.getEstado());
                LJOMestado.getStyleClass().add("leyenda");
                Label JFRPnota = new Label(AKACh.getNota());
                JFRPnota.setWrapText(true);
                Ui.mostrar(JFRPnota, !AKACh.getNota().isBlank());
                setGraphic(new VBox(3, GGFJtitulo, LJOMestado, JFRPnota));
            }
        });
        lista.getSelectionModel().selectedItemProperty().addListener((AKACobs, GGFJanterior, LJOMactual) -> {
            if (LJOMactual != null) {
                punto = null;
                capas.get(LJOMactual.hallazgo().getCapa()).setSelected(true);
                cmbVista.setValue(LJOMactual.hallazgo().getVista());
                lblSeleccion.setText("Hallazgo " + LJOMactual.numero() + " · " + LJOMactual.hallazgo().getZona());
            } else if (punto == null) {
                lblSeleccion.setText("Selecciona una zona o un hallazgo.");
            }
            acciones();
            dibujar();
        });
        acciones();
    }

    private <T> void valores(ComboBox<T> JFRPcombo, T[] AKACopciones, String GGFJtodos) {
        JFRPcombo.getItems().setAll(AKACopciones);
        JFRPcombo.setPromptText(GGFJtodos);
    }

    public void abrirOrden(Long id) {
        consultaVehiculos.cancelar();
        limpiarFicha();
        ordenDirecta = id;
        lblAviso.setText("Cargando inspección de la orden…");
        consultaFicha.consultar(false, () -> servicio.fichaDeOrden(id), LJOMresultado -> {
            cargando = true;
            txtBuscar.setValue(LJOMresultado.vehiculo().getCliente());
            cargando = false;
            mostrarFicha(LJOMresultado, id);
        }, this::error);
    }

    private void mostrarCliente(Cliente JFRPcliente) {
        consultaVehiculos.cancelar();
        limpiarFicha();
        ordenDirecta = null;
        if (JFRPcliente == null) {
            return;
        }
        lblAviso.setText("Buscando vehículos…");
        consultaVehiculos.consultar(false, () -> servicio.vehiculosDeCliente(JFRPcliente.getId()), AKACvehiculos -> {
            if (AKACvehiculos.size() == 1) {
                seleccionarVehiculo(AKACvehiculos.get(0).getId());
            } else if (AKACvehiculos.isEmpty()) {
                lblAviso.setText("Este cliente no tiene vehículos disponibles para inspección.");
            } else {
                lblAviso.setText("Elige un vehículo del cliente.");
                Ui.mostrar(vehiculosCliente, true);
                for (Vehiculo GGFJauto : AKACvehiculos) {
                    Button LJOMtarjeta = new Button(GGFJauto.getPlaca() + " · " + GGFJauto.getDescripcion() + "\n" + GGFJauto.getTipoCarroceria());
                    LJOMtarjeta.getStyleClass().add("button-outlined");
                    LJOMtarjeta.setOnAction(JFRPe -> seleccionarVehiculo(GGFJauto.getId()));
                    vehiculosCliente.getChildren().add(LJOMtarjeta);
                }
            }
        }, this::error);
    }

    private void limpiarFicha() {
        consultaFicha.cancelar();
        consultaHallazgos.cancelar();
        ficha = null;
        marcas = List.of();
        punto = null;
        cargando = true;
        ordenActual.getItems().clear();
        filtroOrden.getItems().clear();
        lista.getItems().clear();
        vehiculosCliente.getChildren().clear();
        Ui.ocultar(vehiculosCliente);
        cargando = false;
        diagrama.limpiar();
        lblAviso.setText("Busca un cliente para comenzar.");
        lblVehiculo.setText("Selecciona un vehículo");
        lista.setPlaceholder(new Label("Selecciona un vehículo para ver sus hallazgos."));
        contarCapas();
        acciones();
    }

    private void seleccionarVehiculo(Long AKACid) {
        lblAviso.setText("Cargando inspección…");
        consultaFicha.consultar(false, () -> servicio.ficha(AKACid), GGFJresultado -> mostrarFicha(GGFJresultado, null), this::error);
    }

    private void mostrarFicha(Ficha LJOMresultado, Long JFRPorden) {
        consultaHallazgos.cancelar();
        ficha = LJOMresultado;
        punto = null;
        marcas = List.of();
        cargando = true;
        lista.getItems().clear();
        filtroOrden.getItems().setAll(LJOMresultado.ordenes());
        filtroOrden.setValue(null);
        ordenActual.getItems().setAll(LJOMresultado.ordenes());
        ordenActual.setValue(LJOMresultado.ordenes().stream().filter(AKACo -> AKACo.id().equals(JFRPorden)).findFirst().orElse(null));
        filtroEstado.setValue(FiltroEstado.PENDIENTES);
        filtroCapa.setValue(null);
        filtroGravedad.setValue(null);
        filtroDesde.setValue(null);
        filtroHasta.setValue(null);
        cmbVista.setValue(Vista.SUPERIOR);
        lblVehiculo.setText(LJOMresultado.vehiculo().getPlaca() + " · " + LJOMresultado.vehiculo().getDescripcion()
                + " · " + LJOMresultado.vehiculo().getTipoCarroceria());
        cargando = false;
        diagrama.ajustarVista();
        recargar();
    }

    @FXML
    public void recargar() {
        if (cargando || ficha == null) {
            return;
        }
        Long GGFJvehiculo = ficha.vehiculo().getId();
        Long LJOMseleccionado = seleccionado() == null ? null : seleccionado().getId();
        var JFRPestados = filtroEstado.getValue();
        var AKACordenFiltro = filtroOrden.getValue();
        Filtros GGFJfiltros = new Filtros(JFRPestados == null ? Set.of() : JFRPestados.estados, filtroCapa.getValue(),
                filtroGravedad.getValue(), AKACordenFiltro == null ? null : AKACordenFiltro.id(),
                filtroDesde.getValue(), filtroHasta.getValue());
        lblAviso.setText("Cargando hallazgos…");
        consultaHallazgos.consultar(false, () -> servicio.consultar(GGFJvehiculo, GGFJfiltros), LJOMresultado -> {
            marcas = PlantillasInspeccion.numerar(LJOMresultado);
            lista.getItems().setAll(marcas);
            lista.setPlaceholder(new Label("No hay hallazgos con estos filtros."));
            lblAviso.setText(marcas.size() + (marcas.size() == 1 ? " hallazgo" : " hallazgos"));
            contarCapas();
            seleccionarMarca(LJOMseleccionado);
            dibujar();
            acciones();
        }, this::error);
    }

    @FXML
    void limpiarFiltros() {
        cargando = true;
        filtroEstado.setValue(FiltroEstado.PENDIENTES);
        filtroCapa.setValue(null);
        filtroGravedad.setValue(null);
        filtroOrden.setValue(null);
        filtroDesde.setValue(null);
        filtroHasta.setValue(null);
        cargando = false;
        recargar();
    }

    private void contarCapas() {
        Map<Capa, Integer> JFRPcantidades = new EnumMap<>(Capa.class);
        for (var AKACmarca : marcas) {
            JFRPcantidades.merge(AKACmarca.hallazgo().getCapa(), 1, Integer::sum);
        }
        capas.forEach((GGFJcapa, LJOMcontrol) -> LJOMcontrol.setText(GGFJcapa + " (" + JFRPcantidades.getOrDefault(GGFJcapa, 0) + ")"));
    }

    private void seleccionarMarca(Long JFRPid) {
        if (JFRPid == null) {
            return;
        }
        marcas.stream().filter(AKACm -> AKACm.hallazgo().getId().equals(JFRPid)).findFirst().ifPresent(GGFJm -> {
            lista.getSelectionModel().select(GGFJm);
            lista.scrollTo(GGFJm);
        });
    }

    private void dibujar() {
        if (ficha == null || cmbVista.getValue() == null) {
            return;
        }
        Set<Capa> LJOMvisibles = EnumSet.noneOf(Capa.class);
        capas.forEach((JFRPcapa, AKACcontrol) -> {
            if (AKACcontrol.isSelected()) {
                LJOMvisibles.add(JFRPcapa);
            }
        });
        var GGFJactual = seleccionado();
        diagrama.mostrar(ficha.vehiculo().getTipoCarroceria(), cmbVista.getValue(), LJOMvisibles, marcas,
                GGFJactual == null ? null : GGFJactual.getId(), punto,
                LJOMzona -> {
                    punto = LJOMzona;
                    lista.getSelectionModel().clearSelection();
                    lblSeleccion.setText("Zona seleccionada · " + LJOMzona.zona());
                    dibujar();
                }, this::seleccionarMarca);
    }

    private HallazgoVehiculo seleccionado() {
        var JFRPvalor = lista.getSelectionModel().getSelectedItem();
        return JFRPvalor == null ? null : JFRPvalor.hallazgo();
    }

    private void acciones() {
        var AKACelegida = ordenActual.getValue();
        boolean GGFJdesdeOrden = ordenDirecta != null;
        boolean LJOMabierta = AKACelegida == null ? sesion.esAdmin() && !GGFJdesdeOrden
                : AKACelegida.estado() != EstadoOrden.ENTREGADO && AKACelegida.estado() != EstadoOrden.ANULADO;
        var JFRPh = seleccionado();
        boolean AKACactivo = JFRPh != null && (JFRPh.getEstado() == Estado.ABIERTO || JFRPh.getEstado() == Estado.EN_REPARACION);
        btnRegistrar.setDisable(ficha == null || !LJOMabierta);
        btnEditar.setDisable(!LJOMabierta || !AKACactivo);
        btnDescartar.setDisable(!LJOMabierta || !AKACactivo || JFRPh.getDetalleOrdenId() != null);
        btnConvertir.setDisable(!LJOMabierta || !AKACactivo || AKACelegida == null || JFRPh.getDetalleOrdenId() != null);
        btnHistorial.setDisable(JFRPh == null);
        btnFoto.setDisable(JFRPh == null || JFRPh.getFotoId() == null);
    }

    private Long ordenId() {
        return ordenActual.getValue() == null ? null : ordenActual.getValue().id();
    }

    @FXML void acercar() { diagrama.zoom(1.25); }
    @FXML void alejar() { diagrama.zoom(0.8); }
    @FXML void ajustar() { diagrama.ajustarVista(); }
    @FXML void registrar() { editarHallazgo(null); }
    @FXML void editar() { editarHallazgo(seleccionado()); }

    private void editarHallazgo(HallazgoVehiculo GGFJprevio) {
        if (ficha == null) {
            return;
        }
        Long LJOMorden = ordenId();
        Long JFRPvehiculo = ficha.vehiculo().getId();
        Runnable AKACsinFotos = () -> formulario(GGFJprevio, LJOMorden, JFRPvehiculo, List.of());
        if (LJOMorden == null) {
            AKACsinFotos.run();
        } else {
            Ui.ejecutar(raiz, () -> servicio.fotosDeOrden(LJOMorden), GGFJopciones -> formulario(GGFJprevio, LJOMorden, JFRPvehiculo, GGFJopciones),
                    lblAviso::setText);
        }
    }

    private void formulario(HallazgoVehiculo LJOMprevio, Long JFRPorden, Long AKACvehiculo, List<FotoOpcion> GGFJopciones) {
        Vista LJOMvista = LJOMprevio == null ? cmbVista.getValue() : LJOMprevio.getVista();
        var JFRPcarroceria = ficha.vehiculo().getTipoCarroceria();
        ComboBox<Zona> AKACzona = new ComboBox<>();
        AKACzona.getItems().setAll(Arrays.stream(Zona.values()).filter(GGFJz -> GGFJz.aplica(JFRPcarroceria) && GGFJz.enVista(LJOMvista) != null).toList());
        Zona LJOMinicial = LJOMprevio == null ? punto == null || punto.vista() != LJOMvista ? null : punto.zona() : LJOMprevio.getZona();
        AKACzona.setValue(LJOMinicial);
        ComboBox<Tipo> JFRPtipo = new ComboBox<>();
        JFRPtipo.getItems().setAll(Tipo.values());
        JFRPtipo.setValue(LJOMprevio == null ? Tipo.RAYON : LJOMprevio.getTipo());
        ComboBox<Gravedad> AKACgravedad = new ComboBox<>();
        AKACgravedad.getItems().setAll(Gravedad.values());
        AKACgravedad.setValue(LJOMprevio == null ? Gravedad.LEVE : LJOMprevio.getGravedad());
        ComboBox<Origen> GGFJorigen = new ComboBox<>();
        GGFJorigen.getItems().setAll(Origen.values());
        boolean LJOMrecepcion = ordenActual.getValue() == null || ordenActual.getValue().estado() == EstadoOrden.RECIBIDO;
        GGFJorigen.setValue(LJOMprevio != null ? LJOMprevio.getOrigen() : LJOMrecepcion ? Origen.PREEXISTENTE : Origen.DETECTADO);
        GGFJorigen.setDisable(LJOMprevio != null);
        TextArea JFRPnota = new TextArea(LJOMprevio == null ? "" : LJOMprevio.getNota());
        JFRPnota.setPrefRowCount(3);
        JFRPnota.setWrapText(true);
        ComboBox<FotoOpcion> AKACfoto = new ComboBox<>();
        AKACfoto.getItems().setAll(GGFJopciones);
        AKACfoto.setPromptText("Sin foto");
        AKACfoto.setConverter(new StringConverter<>() {
            @Override public String toString(FotoOpcion opcion) { return opcion == null ? "" : opcion.toString(); }
            @Override public FotoOpcion fromString(String texto) { return null; }
        });
        if (LJOMprevio != null && LJOMprevio.getFotoId() != null) {
            GGFJopciones.stream().filter(GGFJf -> GGFJf.id().equals(LJOMprevio.getFotoId())).findFirst().ifPresent(AKACfoto::setValue);
        }
        Button LJOMquitarFoto = new Button("Quitar vínculo");
        LJOMquitarFoto.setDisable(GGFJopciones.isEmpty());
        LJOMquitarFoto.setOnAction(JFRPe -> AKACfoto.setValue(null));
        GridPane AKACformulario = formulario();
        fila(AKACformulario, 0, "Zona · " + LJOMvista, AKACzona);
        fila(AKACformulario, 1, "Tipo", JFRPtipo);
        fila(AKACformulario, 2, "Gravedad", AKACgravedad);
        fila(AKACformulario, 3, "Origen", GGFJorigen);
        fila(AKACformulario, 4, "Nota", JFRPnota);
        if (!GGFJopciones.isEmpty()) {
            fila(AKACformulario, 5, "Foto", new javafx.scene.layout.HBox(6, AKACfoto, LJOMquitarFoto));
        }
        Dialog<ButtonType> GGFJdialogo = dialogo(LJOMprevio == null ? "Registrar hallazgo" : "Editar evidencia", AKACformulario);
        var LJOMdp = GGFJdialogo.getDialogPane();
        LJOMdp.lookupButton(ButtonType.OK).disableProperty().bind(AKACzona.valueProperty().isNull());
        var JFRPmarca = punto;
        GGFJdialogo.showAndWait().filter(ButtonType.OK::equals).ifPresent(AKACb -> {
            Double GGFJx = null;
            Double LJOMy = null;
            if (AKACzona.getValue() == LJOMinicial) {
                if (LJOMprevio != null) {
                    GGFJx = LJOMprevio.getX();
                    LJOMy = LJOMprevio.getY();
                } else if (JFRPmarca != null && JFRPmarca.vista() == LJOMvista) {
                    GGFJx = JFRPmarca.x();
                    LJOMy = JFRPmarca.y();
                }
            }
            var JFRPdatos = new Datos(AKACzona.getValue(), LJOMvista, GGFJx, LJOMy, JFRPtipo.getValue(), AKACgravedad.getValue(),
                    GGFJorigen.getValue(), JFRPnota.getText(), AKACfoto.getValue() == null ? null : AKACfoto.getValue().id());
            Ui.ejecutar(raiz, () -> LJOMprevio == null ? servicio.registrar(AKACvehiculo, JFRPorden, JFRPdatos)
                    : servicio.actualizar(LJOMprevio.getId(), JFRPorden, JFRPdatos), AKACguardado -> recargar(), lblAviso::setText);
        });
    }

    @FXML
    void descartar() {
        var GGFJh = seleccionado();
        Long LJOMorden = ordenId();
        TextInputDialog JFRPdialogo = new TextInputDialog();
        JFRPdialogo.initOwner(raiz.getScene().getWindow());
        JFRPdialogo.setTitle("Descartar hallazgo");
        JFRPdialogo.setHeaderText("Descartar " + GGFJh.getZona() + " del historial activo");
        JFRPdialogo.setContentText("Motivo");
        JFRPdialogo.showAndWait().ifPresent(AKACmotivo -> Ui.ejecutar(raiz, () -> {
            servicio.descartar(GGFJh.getId(), LJOMorden, AKACmotivo);
            return null;
        }, GGFJignorado -> recargar(), lblAviso::setText));
    }

    @FXML
    void convertir() {
        HallazgoVehiculo LJOMh = seleccionado();
        Long JFRPorden = ordenId();
        if (LJOMh == null || JFRPorden == null) {
            return;
        }
        ComboBox<TipoDetalle> AKACtipo = new ComboBox<>();
        AKACtipo.getItems().setAll(TipoDetalle.SERVICIO, TipoDetalle.REPUESTO);
        AKACtipo.setValue(TipoDetalle.SERVICIO);
        TextField GGFJdescripcion = new TextField(LJOMh.getTipo() + " · " + LJOMh.getZona());
        TextField LJOMprecio = new TextField("0.00");
        TextField JFRPcantidad = new TextField("1");
        ComboBuscable<Repuesto> AKACrepuesto = new ComboBuscable<>();
        AKACrepuesto.setPromptText("Busca por nombre o código");
        AKACrepuesto.configurar(GGFJt -> repuestos.buscar(GGFJt, false, 0, ComboBuscable.LIMITE).getContent(),
                Repuesto::getNombre, Ui::repuestoEnBusqueda, LJOMr -> LJOMr.getStock() > 0,
                JFRPr -> JFRPr.getStock() <= 0 ? "Agotado" : JFRPr.getStock() <= JFRPr.getStockMinimo() ? "Stock bajo" : "");
        GridPane AKACformulario = formulario();
        fila(AKACformulario, 0, "Tipo de partida", AKACtipo);
        fila(AKACformulario, 1, "Descripción", GGFJdescripcion);
        fila(AKACformulario, 2, "Precio del servicio (S/)", LJOMprecio);
        fila(AKACformulario, 3, "Repuesto", AKACrepuesto);
        fila(AKACformulario, 4, "Cantidad", JFRPcantidad);
        Runnable GGFJajustarCampos = () -> {
            boolean LJOMesRepuesto = AKACtipo.getValue() == TipoDetalle.REPUESTO;
            AKACformulario.getChildren().forEach(JFRPn -> {
                Integer AKACfila = GridPane.getRowIndex(JFRPn);
                if (AKACfila != null && AKACfila >= 2) {
                    Ui.mostrar(JFRPn, AKACfila == 2 ? !LJOMesRepuesto : LJOMesRepuesto);
                }
            });
        };
        AKACtipo.valueProperty().addListener((GGFJobs, LJOMa, JFRPb) -> GGFJajustarCampos.run());
        AKACrepuesto.valueProperty().addListener((AKACobs, GGFJa, LJOMnuevo) -> {
            if (LJOMnuevo != null) {
                GGFJdescripcion.setText(LJOMnuevo.getNombre());
            }
        });
        GGFJajustarCampos.run();
        Dialog<ButtonType> JFRPdialogo = dialogo("Convertir en partida de la orden", AKACformulario);
        Label AKACerror = new Label();
        AKACerror.getStyleClass().add("error-resumen");
        AKACformulario.add(AKACerror, 0, 5, 2, 1);
        var GGFJaceptar = JFRPdialogo.getDialogPane().lookupButton(ButtonType.OK);
        DetalleOrden LJOMsolicitud = new DetalleOrden();
        GGFJaceptar.addEventFilter(javafx.event.ActionEvent.ACTION, JFRPe -> {
            try {
                boolean AKACesRepuesto = AKACtipo.getValue() == TipoDetalle.REPUESTO;
                LJOMsolicitud.setTipo(AKACtipo.getValue());
                LJOMsolicitud.setDescripcion(GGFJdescripcion.getText());
                LJOMsolicitud.setCantidad(AKACesRepuesto ? Integer.parseInt(JFRPcantidad.getText().strip()) : 1);
                LJOMsolicitud.setRepuesto(AKACesRepuesto ? AKACrepuesto.getValue() : null);
                if (AKACesRepuesto && LJOMsolicitud.getRepuesto() == null) {
                    throw new NegocioException("Selecciona un repuesto.");
                }
                if (!AKACesRepuesto) {
                    LJOMsolicitud.setPrecioUnitario(new BigDecimal(LJOMprecio.getText().strip().replace(',', '.')));
                }
            } catch (NumberFormatException GGFJex) {
                AKACerror.setText("Revisa la cantidad o el precio.");
                JFRPe.consume();
            } catch (NegocioException LJOMex) {
                AKACerror.setText(LJOMex.getMessage());
                JFRPe.consume();
            }
        });
        JFRPdialogo.showAndWait().filter(ButtonType.OK::equals).ifPresent(JFRPb ->
                Ui.ejecutar(raiz, () -> ordenes.convertirHallazgo(JFRPorden, LJOMh.getId(), LJOMsolicitud),
                        AKACguardada -> recargar(), lblAviso::setText));
    }

    @FXML
    void historial() {
        var GGFJh = seleccionado();
        Ui.ejecutar(raiz, () -> servicio.historial(GGFJh.getId()), LJOMeventos -> {
            ListView<String> JFRPcambios = new ListView<>();
            JFRPcambios.setPrefSize(570, 300);
            JFRPcambios.getItems().setAll(LJOMeventos.stream().map(AKACe -> FECHA.format(Instant.parse(AKACe.getFecha()))
                    + " · " + AKACe.getUsuario() + " · " + AKACe.getAccion()
                    + (AKACe.getNota().isBlank() ? "" : " · " + AKACe.getNota())).toList());
            dialogo("Historial · " + GGFJh.getZona(), JFRPcambios).showAndWait();
        }, lblAviso::setText);
    }

    @FXML
    void verFoto() {
        var GGFJh = seleccionado();
        Ui.ejecutar(raiz, () -> servicio.fotoDeHallazgo(GGFJh.getId()), LJOMreferencia ->
                Ui.ejecutar(raiz, () -> fotos.original(LJOMreferencia.orden(), LJOMreferencia.foto()), JFRPbytes -> {
                    ImageView AKACimagen = new ImageView(new Image(new ByteArrayInputStream(JFRPbytes)));
                    AKACimagen.setPreserveRatio(true);
                    AKACimagen.setFitWidth(790);
                    AKACimagen.setFitHeight(510);
                    dialogo("Foto de inspección", AKACimagen).showAndWait();
                }, lblAviso::setText), lblAviso::setText);
    }

    private GridPane formulario() {
        GridPane GGFJformulario = new GridPane();
        GGFJformulario.setHgap(10);
        GGFJformulario.setVgap(8);
        GGFJformulario.setPadding(new Insets(4));
        GGFJformulario.setMinWidth(410);
        return GGFJformulario;
    }

    private void fila(GridPane LJOMrejilla, int JFRPindice, String AKACtitulo, Node GGFJcontrol) {
        LJOMrejilla.addRow(JFRPindice, new Label(AKACtitulo), GGFJcontrol);
    }

    private Dialog<ButtonType> dialogo(String LJOMtitulo, Node JFRPcontenido) {
        Dialog<ButtonType> AKACdialogo = new Dialog<>();
        AKACdialogo.initOwner(raiz.getScene().getWindow());
        AKACdialogo.setTitle(LJOMtitulo);
        AKACdialogo.getDialogPane().getStylesheets().setAll(raiz.getScene().getStylesheets());
        AKACdialogo.getDialogPane().setContent(JFRPcontenido);
        AKACdialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return AKACdialogo;
    }

    private void error(RuntimeException GGFJerror) {
        lblAviso.setText(GGFJerror instanceof NegocioException ? GGFJerror.getMessage() : "No se pudo cargar la inspección.");
    }
}
