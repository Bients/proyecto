package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.MovimientoInventario;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.service.OrdenService;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public final class DialogoTrazabilidad {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private DialogoTrazabilidad() {
    }

    public static void mostrar(Window ventana, OrdenTrabajo orden, DetalleOrden detalle, OrdenService servicio) {
        Dialog<Void> LJOMdialogo = new Dialog<>();
        LJOMdialogo.initOwner(ventana);
        LJOMdialogo.setTitle("Trazabilidad de " + orden.getNumeroOrden());
        LJOMdialogo.getDialogPane().getStylesheets().setAll(ventana.getScene().getStylesheets());
        LJOMdialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        LJOMdialogo.setResultConverter(JFRPboton -> null);
        CheckBox AKACfiltrar = new CheckBox("Solo la partida seleccionada");
        AKACfiltrar.setDisable(detalle == null);
        AKACfiltrar.setSelected(detalle != null);
        TableView<MovimientoInventario> GGFJtabla = new TableView<>();
        GGFJtabla.getColumns().add(columna("Partida", 205, LJOMm -> {
            DetalleOrden JFRPorigen = LJOMm.getDetalleOrden();
            return JFRPorigen == null ? "Histórico sin partida · " + LJOMm.getRepuesto().getNombre()
                    : "#" + JFRPorigen.getId() + " " + JFRPorigen.getDescripcion() + (JFRPorigen.isRetirado() ? " (retirada)" : "");
        }));
        GGFJtabla.getColumns().add(columna("Movimiento", 125, AKACm -> AKACm.getTipo().toString()));
        GGFJtabla.getColumns().add(columna("Cantidad", 68, GGFJm -> String.format("%+d", GGFJm.getCantidad())));
        GGFJtabla.getColumns().add(columna("Saldo", 58, LJOMm -> Integer.toString(LJOMm.getSaldoResultante())));
        GGFJtabla.getColumns().add(columna("Fecha", 115, JFRPm -> FECHA.format(Instant.parse(JFRPm.getFecha()))));
        GGFJtabla.getColumns().add(columna("Usuario", 85, MovimientoInventario::getUsuario));
        GGFJtabla.getColumns().add(columna("Motivo", 210, MovimientoInventario::getMotivo));
        GGFJtabla.setPlaceholder(new Label("Sin movimientos para esta selección."));
        VBox.setVgrow(GGFJtabla, Priority.ALWAYS);
        Label AKACaviso = new Label();
        PaginadorBusqueda<MovimientoInventario> GGFJpaginador = new PaginadorBusqueda<>();
        GGFJpaginador.configurar(LJOMpagina -> {
            Long JFRPpartida = AKACfiltrar.isSelected() ? detalle.getId() : null;
            return () -> servicio.trazabilidad(orden.getId(), JFRPpartida, LJOMpagina, PaginadorBusqueda.LIMITE);
        }, AKACpagina -> {
            GGFJtabla.getItems().setAll(AKACpagina.getContent());
            AKACaviso.setText("");
        }, AKACaviso::setText);
        AKACfiltrar.selectedProperty().addListener((GGFJobs, LJOMantes, JFRPahora) -> GGFJpaginador.reiniciar());
        LJOMdialogo.getDialogPane().setContent(new VBox(8, AKACfiltrar, GGFJtabla, GGFJpaginador, AKACaviso));
        LJOMdialogo.getDialogPane().setPrefSize(900, 450);
        GGFJpaginador.reiniciar();
        LJOMdialogo.showAndWait();
    }

    private static TableColumn<MovimientoInventario, String> columna(String AKACtitulo, double GGFJancho,
                                                                     Function<MovimientoInventario, String> LJOMtexto) {
        TableColumn<MovimientoInventario, String> JFRPcolumna = new TableColumn<>(AKACtitulo);
        JFRPcolumna.setPrefWidth(GGFJancho);
        JFRPcolumna.setCellValueFactory(AKACcelda -> new SimpleStringProperty(LJOMtexto.apply(AKACcelda.getValue())));
        return JFRPcolumna;
    }
}
