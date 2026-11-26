package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.BusquedaAsincrona;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.FiltrosOrdenBusqueda;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.Ui;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import com.ejemplo.tallerpro.util.SesionActual;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;

import java.util.List;
import java.math.BigDecimal;

/** Linea de tiempo de todo lo que se le ha hecho a un vehiculo. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class HistorialController {

    private final OrdenService ordenService;
    private final SesionActual sesion;
    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;

    @FXML private ComboBuscable<Vehiculo> cmbVehiculo;
    @FXML private TextField txtBuscar;
    @FXML private FiltrosOrdenBusqueda filtrosBusqueda;
    @FXML private PaginadorBusqueda<OrdenTrabajo> paginador;
    @FXML private Label lblBusqueda;

    private final BusquedaAsincrona<OrdenService.ResumenHistorial> resumen = new BusquedaAsincrona<>();
    @FXML private Label lblVisitas;
    @FXML private Label lblCobrado;
    @FXML private Label lblPendiente;
    @FXML private VBox tarjetaCobrado;
    @FXML private VBox tarjetaPendiente;
    @FXML private Label lblKm;
    @FXML private Label lblPropietario;

    @FXML private TableView<OrdenTrabajo> tabla;
    @FXML private TableColumn<OrdenTrabajo, String> colId;
    @FXML private TableColumn<OrdenTrabajo, String> colFecha;
    @FXML private TableColumn<OrdenTrabajo, String> colKm;
    @FXML private TableColumn<OrdenTrabajo, String> colEstado;
    @FXML private TableColumn<OrdenTrabajo, String> colProblema;
    @FXML private TableColumn<OrdenTrabajo, String> colClienteHistorico;
    @FXML private TableColumn<OrdenTrabajo, String> colMecanico;
    @FXML private TableColumn<OrdenTrabajo, String> colTotal;
    @FXML private TableColumn<OrdenTrabajo, String> colCobrado;
    @FXML private TableColumn<OrdenTrabajo, String> colPendiente;
    @FXML private TableColumn<OrdenTrabajo, String> colPago;

    @FXML private TableView<DetalleOrden> tablaDetalles;
    @FXML private javafx.scene.layout.VBox fichaVisita;
    @FXML private Label lblVisita;
    @FXML private TableColumn<DetalleOrden, String> colDTipo;
    @FXML private TableColumn<DetalleOrden, String> colDDescripcion;
    @FXML private TableColumn<DetalleOrden, String> colDCantidad;
    @FXML private TableColumn<DetalleOrden, String> colDSubtotal;

    private final ObservableList<OrdenTrabajo> datos = FXCollections.observableArrayList();
    private final BusquedaAsincrona<OrdenTrabajo> ordenSeleccionada = new BusquedaAsincrona<>();

    private final ObservableList<DetalleOrden> datosDetalle = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(GGFJc -> new SimpleStringProperty(GGFJc.getValue().getNumeroOrden()));
        colFecha.setCellValueFactory(LJOMc -> new SimpleStringProperty(String.valueOf(LJOMc.getValue().getFechaIngreso())));
        colKm.setCellValueFactory(JFRPc -> new SimpleStringProperty(
                JFRPc.getValue().getKilometraje() == null ? "-" : JFRPc.getValue().getKilometraje() + " km"));
        colEstado.setCellValueFactory(AKACc -> new SimpleStringProperty(String.valueOf(AKACc.getValue().getEstado())));
        colEstado.setCellFactory(Ui.celdaInsignia());
        colClienteHistorico.setCellValueFactory(GGFJc -> new SimpleStringProperty(
                GGFJc.getValue().getDatosHistoricos().getClienteNombre()));
        colProblema.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().getProblema()));
        colMecanico.setCellValueFactory(JFRPc -> new SimpleStringProperty(Ui.texto(JFRPc.getValue().getMecanico())));
        colTotal.setCellValueFactory(AKACc -> new SimpleStringProperty(Ui.moneda(AKACc.getValue().getTotal())));
        colCobrado.setCellValueFactory(GGFJc -> new SimpleStringProperty(Ui.moneda(GGFJc.getValue().getTotalCobrado())));
        colPendiente.setCellValueFactory(LJOMc -> new SimpleStringProperty(Ui.moneda(LJOMc.getValue().getSaldoPendiente())));
        colPago.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().getEstadoPago().toString()));
        colPago.setCellFactory(Ui.celdaInsignia());
        boolean AKACadmin = sesion.esAdmin();
        Ui.mostrar(tarjetaCobrado, AKACadmin);
        Ui.mostrar(tarjetaPendiente, AKACadmin);
        colCobrado.setVisible(AKACadmin);
        colPendiente.setVisible(AKACadmin);
        colPago.setVisible(AKACadmin);
        tabla.setItems(datos);
        tabla.setPlaceholder(new Label("Selecciona un vehículo o ajusta los filtros del historial."));
        Ui.ocultar(fichaVisita);
        lblBusqueda.visibleProperty().bind(lblBusqueda.textProperty().isNotEmpty());
        lblBusqueda.managedProperty().bind(lblBusqueda.visibleProperty());
        tablaDetalles.setPlaceholder(new Label("Sin partidas en esta visita."));
        tabla.getSelectionModel().selectedItemProperty().addListener((GGFJobs, LJOManterior, JFRPactual) -> {
            ordenSeleccionada.cancelar();
            datosDetalle.clear();
            Ui.mostrar(fichaVisita, JFRPactual != null);
            if (JFRPactual != null) {
                lblVisita.setText("Partidas de " + JFRPactual.getNumeroOrden());
                ordenSeleccionada.consultar(false, () -> ordenService.buscar(JFRPactual.getId()),
                        AKACorden -> datosDetalle.setAll(AKACorden.getDetalles()), GGFJerror -> lblBusqueda.setText(GGFJerror.getMessage()));
            }
        });

        colDTipo.setCellValueFactory(LJOMc -> new SimpleStringProperty(String.valueOf(LJOMc.getValue().getTipo())));
        colDDescripcion.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().getDescripcion()));
        colDDescripcion.setCellFactory(AKACc -> Ui.celdaTextoAjustado());
        colDCantidad.setCellValueFactory(GGFJc -> new SimpleStringProperty(String.valueOf(GGFJc.getValue().getCantidad())));
        colDSubtotal.setCellValueFactory(LJOMc -> new SimpleStringProperty(Ui.moneda(LJOMc.getValue().getSubtotal())));
        colDSubtotal.setCellFactory(JFRPc -> Ui.celdaImporte());
        tablaDetalles.setItems(datosDetalle);

        cmbVehiculo.configurar(AKACtexto -> vehiculoService.buscar(AKACtexto, 0, ComboBuscable.LIMITE).getContent(),
                Ui::vehiculoEnBusqueda, Ui::vehiculoEnBusqueda);
        paginador.configurar(txtBuscar, GGFJpagina -> {
            Vehiculo LJOMvehiculo = cmbVehiculo.getValue();
            if (LJOMvehiculo == null) {
                return Page::empty;
            }
            OrdenService.Filtros JFRPfiltros = filtrosBusqueda.valores(txtBuscar.getText(), LJOMvehiculo.getId());
            return () -> ordenService.buscar(JFRPfiltros, GGFJpagina, PaginadorBusqueda.LIMITE);
        }, AKACpagina -> {
            tabla.getSelectionModel().clearSelection();
            datos.setAll(AKACpagina.getContent());
            datosDetalle.clear();
            lblBusqueda.setText("");
        }, lblBusqueda::setText);
        filtrosBusqueda.configurar(usuarioService, sesion.esAdmin(), paginador::reiniciar);
        cmbVehiculo.valueProperty().addListener((GGFJobs, LJOManterior, JFRPactual) -> cargarHistorial());
        cargarHistorial();
    }

    public void recargar() {
        cargarHistorial();
    }

    private void cargarHistorial() {
        Vehiculo AKACvehiculo = cmbVehiculo.getValue();
        resumen.cancelar();
        paginador.vaciar();
        datos.clear();
        datosDetalle.clear();
        lblBusqueda.setText("");
        lblVisitas.setText("0");
        lblCobrado.setText(Ui.moneda(BigDecimal.ZERO));
        lblPendiente.setText(Ui.moneda(BigDecimal.ZERO));
        if (AKACvehiculo == null) {
            lblKm.setText("-");
            lblPropietario.setText("Selecciona un vehículo para ver su historial");
            return;
        }
        lblKm.setText(AKACvehiculo.getKilometraje() == null ? "-" : AKACvehiculo.getKilometraje() + " km");
        lblPropietario.setText(AKACvehiculo.getPlaca() + " · " + AKACvehiculo.getDescripcion()
                + " · Propietario: " + AKACvehiculo.getCliente().getNombreCompleto());
        resumen.consultar(false, () -> ordenService.resumenHistorial(AKACvehiculo.getId()), GGFJtotales -> {
            lblVisitas.setText(String.valueOf(GGFJtotales.visitas()));
            lblCobrado.setText(Ui.moneda(GGFJtotales.cobrado()));
            lblPendiente.setText(Ui.moneda(GGFJtotales.saldo()));
        }, LJOMerror -> {
            lblVisitas.setText("—");
            lblCobrado.setText("—");
            lblPendiente.setText("—");
        });
        paginador.reiniciar();
    }
}
