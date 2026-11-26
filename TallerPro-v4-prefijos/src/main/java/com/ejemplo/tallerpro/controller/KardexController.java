package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.service.InventarioService.MovimientoKardex;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoMovimiento;
import com.ejemplo.tallerpro.service.InventarioService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.Ui;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class KardexController {

    private static final String TODOS = "Todos los tipos";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final InventarioService inventario;
    private final RepuestoService repuestos;

    @FXML private ComboBuscable<Repuesto> txtBuscar;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Object> cmbTipo;
    @FXML private TableView<MovimientoKardex> tabla;
    @FXML private TableColumn<MovimientoKardex, String> colFecha;
    @FXML private TableColumn<MovimientoKardex, String> colTipo;
    @FXML private TableColumn<MovimientoKardex, String> colCantidad;
    @FXML private TableColumn<MovimientoKardex, String> colSaldo;
    @FXML private TableColumn<MovimientoKardex, String> colCosto;
    @FXML private TableColumn<MovimientoKardex, String> colOrigen;
    @FXML private TableColumn<MovimientoKardex, String> colUsuario;
    @FXML private TableColumn<MovimientoKardex, String> colMotivo;
    @FXML private PaginadorBusqueda<MovimientoKardex> paginador;
    @FXML private Label lblEstado;

    @FXML
    public void initialize() {
        txtBuscar.configurar(LJOMtexto -> repuestos.buscar(LJOMtexto, false, 0, ComboBuscable.LIMITE).getContent(),
                Repuesto::getNombre, Ui::repuestoEnBusqueda);
        cmbTipo.getItems().add(TODOS);
        cmbTipo.getItems().addAll(List.of(TipoMovimiento.values()));
        cmbTipo.setValue(TODOS);

        colFecha.setCellValueFactory(JFRPc -> new SimpleStringProperty(FECHA.format(Instant.parse(JFRPc.getValue().movimiento().getFecha()))));
        colTipo.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().movimiento().getTipo().toString()));
        colCantidad.setCellValueFactory(GGFJc -> new SimpleStringProperty(String.format("%+d", GGFJc.getValue().movimiento().getCantidad())));
        colSaldo.setCellValueFactory(LJOMc -> new SimpleStringProperty(String.valueOf(LJOMc.getValue().movimiento().getSaldoResultante())));
        colCosto.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().movimiento().getCostoUnitario().toPlainString()));
        colOrigen.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().referencia()));
        colOrigen.setCellFactory(GGFJc -> Ui.celdaTextoAjustado());
        colUsuario.setCellValueFactory(LJOMc -> new SimpleStringProperty(LJOMc.getValue().movimiento().getUsuario()));
        colMotivo.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().movimiento().getMotivo()));
        paginador.configurar(AKACpagina -> {
            Repuesto GGFJrepuesto = txtBuscar.getValue();
            if (GGFJrepuesto == null) {
                return Page::empty;
            }
            TipoMovimiento LJOMtipo = cmbTipo.getValue() instanceof TipoMovimiento JFRPvalor ? JFRPvalor : null;
            LocalDate AKACdesde = dpDesde.getValue();
            LocalDate GGFJhasta = dpHasta.getValue();
            return () -> inventario.buscarKardex(GGFJrepuesto.getId(), LJOMtipo, AKACdesde, GGFJhasta, AKACpagina, PaginadorBusqueda.LIMITE);
        }, LJOMpagina -> {
            tabla.getItems().setAll(LJOMpagina.getContent());
            lblEstado.setText("");
        }, lblEstado::setText);
        Label JFRPinstrucciones = new Label("Selecciona un repuesto para ver sus movimientos.");
        tabla.setPlaceholder(JFRPinstrucciones);
        txtBuscar.valueProperty().addListener((AKACobs, GGFJantes, LJOMahora) -> {
            tabla.getItems().clear();
            JFRPinstrucciones.setText(LJOMahora == null ? "Selecciona un repuesto para ver sus movimientos."
                    : "Sin movimientos para los filtros seleccionados.");
            paginador.reiniciar();
        });
        cmbTipo.valueProperty().addListener((JFRPobs, AKACantes, GGFJahora) -> paginador.reiniciar());
        dpDesde.valueProperty().addListener((LJOMobs, JFRPantes, AKACahora) -> paginador.reiniciar());
        dpHasta.valueProperty().addListener((GGFJobs, LJOMantes, JFRPahora) -> paginador.reiniciar());
    }

    public void abrir(Repuesto repuesto) {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cmbTipo.setValue(TODOS);
        txtBuscar.setValue(repuesto);
        recargar();
    }

    public void recargar() {
        paginador.recargar();
    }
}
