package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.EstadoPago;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.repository.OrdenRepository.OrdenTablero;
import com.ejemplo.tallerpro.service.MantenimientoService;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.PagoService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.util.BusquedaAsincrona;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongConsumer;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ResumenController {

    private static final int MESES_GRAFICO = 6;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdenService ordenService;
    private final SesionActual sesion;
    private final PagoService pagos;
    private final VehiculoService vehiculoService;
    private final RepuestoService repuestoService;
    private final MantenimientoService mantenimientoService;

    @FXML private Label lblDia;
    @FXML private TextField txtBuscar;
    @FXML private RadioButton rbIngresosHoy;
    @FXML private TableView<OrdenTablero> tabla;
    @FXML private TableColumn<OrdenTablero, String> colNumero;
    @FXML private TableColumn<OrdenTablero, String> colPlaca;
    @FXML private TableColumn<OrdenTablero, String> colClienteMotivo;
    @FXML private TableColumn<OrdenTablero, String> colEstado;
    @FXML private TableColumn<OrdenTablero, String> colDias;
    @FXML private TableColumn<OrdenTablero, String> colPago;
    @FXML private TableColumn<OrdenTablero, OrdenTablero> colAbrir;
    @FXML private PaginadorBusqueda<OrdenTablero> paginador;
    @FXML private VBox estadoVacio;
    @FXML private Label lblEstado;
    @FXML private Button btnRegistrarVehiculo;
    @FXML private Label lblAlertas;
    @FXML private TitledPane panelResumen;
    @FXML private VBox bloqueCaja;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblCobrado;
    @FXML private DatePicker dpCaja;
    @FXML private VBox cajaMetodos;
    @FXML private Label lblCaja;
    @FXML private VBox tarjetaIngresos;
    @FXML private BarChart<String, Number> graficoIngresos;
    @FXML private PieChart graficoServicios;

    private record Alertas(List<Repuesto> stockBajo, long mantenimientos, boolean sinVehiculos) {
    }

    private final BusquedaAsincrona<Alertas> alertas = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<OrdenService.ResumenTablero> resumen = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<List<PagoService.CajaMetodo>> caja = new BusquedaAsincrona<>();
    private final BusquedaAsincrona<Map<YearMonth, BigDecimal>> mensual = new BusquedaAsincrona<>();
    private Runnable nuevaOrden;
    private LongConsumer abrirOrden;
    private Runnable registrarVehiculo;
    private String errorLista;
    private boolean listaCargada;
    private boolean sinVehiculos;

    @FXML
    public void initialize() {
        boolean JFRPadmin = sesion.esAdmin();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        texto(colNumero, OrdenTablero::getNumero);
        texto(colPlaca, OrdenTablero::getPlaca);
        texto(colClienteMotivo, AKACorden -> Ui.texto(AKACorden.getCliente()) + " · " + Ui.texto(AKACorden.getProblema()));
        colClienteMotivo.setCellFactory(Ui::celdaTextoAjustado);
        texto(colEstado, GGFJorden -> EstadoOrden.valueOf(GGFJorden.getEstado()).toString());
        colEstado.setCellFactory(Ui.celdaInsignia());
        texto(colDias, LJOMorden -> LJOMorden.getDias() == null ? "—" : LJOMorden.getDias().toString());
        if (JFRPadmin) {
            texto(colPago, this::estadoPago);
            colPago.setCellFactory(Ui.celdaInsignia());
        }
        colPago.setVisible(JFRPadmin);
        colAbrir.setCellValueFactory(JFRPcelda -> new ReadOnlyObjectWrapper<>(JFRPcelda.getValue()));
        colAbrir.setCellFactory(AKACcolumna -> new TableCell<>() {
            private final Button boton = new Button("Abrir");
            {
                Ui.accion(boton, "button-outlined", "accion-secundaria");
                boton.setOnAction(GGFJe -> abrir(getItem()));
            }

            @Override
            protected void updateItem(OrdenTablero orden, boolean vacia) {
                super.updateItem(orden, vacia);
                setGraphic(vacia || orden == null ? null : boton);
                if (orden != null) {
                    boton.setAccessibleText("Abrir orden " + orden.getNumero());
                }
            }
        });
        tabla.setOnKeyPressed(LJOMe -> {
            if (LJOMe.getCode() == KeyCode.ENTER) {
                abrir(tabla.getSelectionModel().getSelectedItem());
                LJOMe.consume();
            }
        });
        paginador.configurar(txtBuscar, JFRPpagina -> {
            LocalDate AKAChoy = LocalDate.now();
            boolean GGFJsoloHoy = rbIngresosHoy.isSelected();
            String LJOMplaca = txtBuscar.getText();
            return () -> ordenService.ordenesTablero(AKAChoy, GGFJsoloHoy, LJOMplaca, JFRPpagina, PaginadorBusqueda.LIMITE);
        }, JFRPpagina -> {
            errorLista = null;
            listaCargada = true;
            tabla.getItems().setAll(JFRPpagina.getContent());
            mostrarLista();
        }, AKACerror -> {
            errorLista = "No se pudieron cargar las órdenes. Pulsa Actualizar.";
            tabla.getItems().clear();
            mostrarLista();
        });

        Ui.mostrar(bloqueCaja, JFRPadmin);
        Ui.mostrar(tarjetaIngresos, JFRPadmin);
        if (!JFRPadmin) {
            panelResumen.setText("Servicios más pedidos");
        }
        dpCaja.setValue(LocalDate.now());
        dpCaja.valueProperty().addListener((GGFJobs, LJOManterior, JFRPnueva) -> recargarCaja());
        panelResumen.expandedProperty().addListener((AKACobs, GGFJanterior, LJOMabierto) -> {
            if (LJOMabierto) {
                recargarResumen();
            }
        });
        mostrarLista();
    }

    private void texto(TableColumn<OrdenTablero, String> JFRPcolumna, Function<OrdenTablero, String> AKACvalor) {
        JFRPcolumna.setCellValueFactory(GGFJcelda -> new ReadOnlyStringWrapper(AKACvalor.apply(GGFJcelda.getValue())));
    }

    private String estadoPago(OrdenTablero LJOMorden) {
        if (LJOMorden.getCobrado() == 0) {
            return EstadoPago.SIN_PAGAR.toString();
        }
        return (LJOMorden.getCobrado() >= LJOMorden.getTotal() ? EstadoPago.PAGADO : EstadoPago.PARCIAL).toString();
    }

    public void configurarAcciones(Runnable nuevaOrden, LongConsumer abrirOrden, Runnable registrarVehiculo) {
        this.nuevaOrden = nuevaOrden;
        this.abrirOrden = abrirOrden;
        this.registrarVehiculo = registrarVehiculo;
    }

    @FXML
    private void onNuevaOrden() {
        if (nuevaOrden != null) {
            nuevaOrden.run();
        }
    }

    @FXML
    private void onRegistrarVehiculo() {
        if (sesion.esAdmin() && registrarVehiculo != null) {
            registrarVehiculo.run();
        }
    }

    @FXML
    private void onCambiarVista() {
        listaCargada = false;
        errorLista = null;
        tabla.getItems().clear();
        mostrarLista();
        paginador.reiniciar();
    }

    private void abrir(OrdenTablero JFRPorden) {
        if (JFRPorden != null && abrirOrden != null) {
            abrirOrden.accept(JFRPorden.getId());
        }
    }

    @FXML
    public void recargar() {
        lblDia.setText(LocalDate.now().format(FECHA));
        paginador.recargar();
        boolean AKACadmin = sesion.esAdmin();
        alertas.consultar(false, () -> new Alertas(repuestoService.alertasStock(5),
                AKACadmin ? mantenimientoService.contarPendientes() : 0,
                AKACadmin && vehiculoService.contar() == 0), this::mostrarAlertas, GGFJerror -> {
            sinVehiculos = false;
            Ui.ocultar(lblAlertas);
            mostrarLista();
        });
        if (panelResumen.isExpanded()) {
            recargarResumen();
        }
    }

    private void mostrarLista() {
        boolean LJOMvacia = tabla.getItems().isEmpty();
        Ui.mostrar(tabla, !LJOMvacia);
        Ui.mostrar(paginador, !LJOMvacia);
        Ui.mostrar(estadoVacio, LJOMvacia);
        if (!LJOMvacia) {
            return;
        }
        boolean JFRPfiltrada = !txtBuscar.getText().isBlank();
        boolean AKACprimerVehiculo = listaCargada && errorLista == null && !JFRPfiltrada
                && !rbIngresosHoy.isSelected() && sinVehiculos;
        Ui.mostrar(btnRegistrarVehiculo, AKACprimerVehiculo);
        if (errorLista != null) {
            lblEstado.setText(errorLista);
        } else if (!listaCargada) {
            lblEstado.setText("Buscando órdenes…");
        } else if (JFRPfiltrada) {
            lblEstado.setText("No hay órdenes en esta vista que coincidan con la placa.");
        } else if (AKACprimerVehiculo) {
            lblEstado.setText("Registra un vehículo para crear su primera orden.");
        } else {
            lblEstado.setText(rbIngresosHoy.isSelected()
                    ? "Todavía no ingresaron órdenes hoy." : "No hay órdenes pendientes.");
        }
    }

    private void mostrarAlertas(Alertas GGFJdatos) {
        sinVehiculos = GGFJdatos.sinVehiculos();
        List<String> LJOMavisos = new ArrayList<>();
        for (Repuesto JFRPrepuesto : GGFJdatos.stockBajo()) {
            LJOMavisos.add(JFRPrepuesto.getNombre() + ": " + JFRPrepuesto.getStock() + " disponibles (mín. "
                    + JFRPrepuesto.getStockMinimo() + ")");
        }
        if (GGFJdatos.mantenimientos() > 0) {
            LJOMavisos.add("Mantenimientos pendientes: " + GGFJdatos.mantenimientos());
        }
        lblAlertas.setText(String.join(" · ", LJOMavisos));
        Ui.mostrar(lblAlertas, !LJOMavisos.isEmpty());
        mostrarLista();
    }

    private void recargarResumen() {
        resumen.consultar(false, ordenService::resumenTablero, AKACdatos -> {
            if (AKACdatos.cobranza() != null) {
                lblPorCobrar.setText("Saldo por cobrar " + Ui.moneda(AKACdatos.cobranza().saldo()));
                lblCobrado.setText("Cobrado acumulado " + Ui.moneda(AKACdatos.cobranza().cobrado()));
            }
            graficoServicios.setData(FXCollections.observableArrayList(AKACdatos.servicios().stream()
                    .map(GGFJfila -> new PieChart.Data(GGFJfila.getNombre() + " (" + GGFJfila.getCantidad() + ")", GGFJfila.getCantidad()))
                    .toList()));
        }, LJOMerror -> {
            lblPorCobrar.setText("No se pudo cargar el resumen.");
            lblCobrado.setText("");
            graficoServicios.getData().clear();
        });
        if (sesion.esAdmin()) {
            recargarCaja();
            mensual.consultar(false, this::cobrosMensuales, this::mostrarCobrosMensuales,
                    JFRPerror -> graficoIngresos.getData().clear());
        }
    }

    private void recargarCaja() {
        LocalDate AKACfecha = dpCaja.getValue();
        cajaMetodos.getChildren().clear();
        if (AKACfecha == null) {
            caja.cancelar();
            lblCaja.setText("Selecciona una fecha.");
            return;
        }
        lblCaja.setText("Consultando caja…");
        caja.consultar(false, () -> pagos.caja(AKACfecha, AKACfecha), GGFJfilas -> {
            cajaMetodos.getChildren().setAll(GGFJfilas.stream().map(LJOMfila -> {
                Label JFRPrenglon = new Label(LJOMfila.metodo() + "   Cobros " + Ui.moneda(LJOMfila.cobros()) + "   ·   Anulaciones "
                        + Ui.moneda(LJOMfila.anulaciones()) + "   ·   Neto " + Ui.moneda(LJOMfila.neto()));
                JFRPrenglon.setWrapText(true);
                JFRPrenglon.getStyleClass().add("importe");
                return JFRPrenglon;
            }).toList());
            lblCaja.setText("Neto del día " + Ui.moneda(GGFJfilas.stream().map(PagoService.CajaMetodo::neto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }, AKACerror -> lblCaja.setText("No se pudo consultar el cierre de caja."));
    }

    private Map<YearMonth, BigDecimal> cobrosMensuales() {
        YearMonth GGFJactual = YearMonth.now();
        Map<YearMonth, BigDecimal> LJOMporMes = new LinkedHashMap<>();
        for (int JFRPi = MESES_GRAFICO - 1; JFRPi >= 0; JFRPi--) {
            YearMonth AKACmes = GGFJactual.minusMonths(JFRPi);
            LJOMporMes.put(AKACmes, pagos.caja(AKACmes.atDay(1), AKACmes.atEndOfMonth()).stream()
                    .map(PagoService.CajaMetodo::neto).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return LJOMporMes;
    }

    private void mostrarCobrosMensuales(Map<YearMonth, BigDecimal> GGFJporMes) {
        DateTimeFormatter LJOMformato = DateTimeFormatter.ofPattern("MMM yy", Locale.forLanguageTag("es"));
        XYChart.Series<String, Number> JFRPserie = new XYChart.Series<>();
        JFRPserie.setName("Cobros netos");
        GGFJporMes.forEach((AKACmes, GGFJimporte) -> JFRPserie.getData().add(new XYChart.Data<>(AKACmes.format(LJOMformato), GGFJimporte)));
        graficoIngresos.getData().setAll(List.of(JFRPserie));
    }
}
