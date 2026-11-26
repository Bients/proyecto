package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.MetodoPago;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Pago;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.PagoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public final class DialogoPagos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private DialogoPagos() {
    }

    private record Datos(OrdenTrabajo orden, List<Pago> abonos) {
    }

    public static void mostrar(Window ventana, OrdenTrabajo orden, OrdenService ordenes, PagoService pagos,
                               Runnable alCerrar) {
        Dialog<Void> GGFJdialogo = new Dialog<>();
        GGFJdialogo.initOwner(ventana);
        GGFJdialogo.setTitle("Pagos de " + orden.getNumeroOrden());
        GGFJdialogo.setResizable(true);
        GGFJdialogo.getDialogPane().setPrefWidth(735);
        GGFJdialogo.getDialogPane().setMinWidth(580);
        GGFJdialogo.getDialogPane().getStylesheets().setAll(ventana.getScene().getStylesheets());
        GGFJdialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Ui.accion((Button) GGFJdialogo.getDialogPane().lookupButton(ButtonType.CLOSE), "accion-secundaria");
        GGFJdialogo.setResultConverter(AKACboton -> null);

        Label GGFJtotal = importe();
        Label LJOMcobrado = importe();
        Label JFRPsaldo = importe();
        JFRPsaldo.getStyleClass().add("total");
        Label AKACaFavor = importe();
        FlowPane GGFJresumen = new FlowPane(16, 5, JFRPsaldo, GGFJtotal, LJOMcobrado, AKACaFavor);
        GGFJresumen.setAlignment(Pos.CENTER_LEFT);
        Label LJOMaviso = new Label();
        LJOMaviso.getStyleClass().add("error-resumen");
        LJOMaviso.setWrapText(true);
        TableView<Pago> JFRPtabla = new TableView<>();
        JFRPtabla.setPrefHeight(275);
        JFRPtabla.setPlaceholder(new Label("Sin pagos registrados."));
        JFRPtabla.getColumns().add(columna("Fecha", 130, AKACp -> FECHA.format(Instant.parse(AKACp.getFecha()))));
        JFRPtabla.getColumns().add(columna("Método", 98, GGFJp -> GGFJp.getMetodo().toString()));
        TableColumn<Pago, String> LJOMcolMonto = columna("Monto", 132, JFRPp -> Ui.moneda(JFRPp.getMonto()));
        LJOMcolMonto.setMinWidth(125);
        LJOMcolMonto.setCellFactory(AKACc -> Ui.celdaImporte());
        JFRPtabla.getColumns().add(LJOMcolMonto);
        JFRPtabla.getColumns().add(columna("Usuario", 90, Pago::getUsuario));
        JFRPtabla.getColumns().add(columna("Nota", 120, GGFJp -> Ui.texto(GGFJp.getNota())));
        JFRPtabla.getColumns().add(columna("Estado", 210, LJOMp -> LJOMp.isAnulado() ? "Anulado: " + LJOMp.getMotivoAnulacion() : "Vigente"));

        TextField JFRPmonto = new TextField();
        JFRPmonto.setPromptText("Monto (S/)");
        JFRPmonto.setPrefColumnCount(12);
        ComboBox<MetodoPago> AKACmetodo = new ComboBox<>();
        AKACmetodo.getItems().setAll(MetodoPago.values());
        AKACmetodo.setPromptText("Método");
        AKACmetodo.setAccessibleText("Método de pago");
        AKACmetodo.setPrefWidth(150);
        TextField GGFJnota = new TextField();
        GGFJnota.setPromptText("Nota (opcional)");
        GGFJnota.setPrefColumnCount(15);
        Button LJOMregistrar = new Button("Registrar pago");
        Ui.accion(LJOMregistrar, "accent", "accion-principal");
        Button JFRPanular = new Button("Anular pago");
        Ui.accion(JFRPanular, "danger", "accion-secundaria");
        JFRPanular.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> JFRPtabla.getSelectionModel().getSelectedItem() == null || JFRPtabla.getSelectionModel().getSelectedItem().isAnulado(),
                JFRPtabla.getSelectionModel().selectedItemProperty()));
        FlowPane AKACcampos = new FlowPane(8, 6, JFRPmonto, AKACmetodo, GGFJnota, LJOMregistrar);
        AKACcampos.setAlignment(Pos.CENTER_LEFT);
        Ui.mostrar(AKACcampos, orden.getEstado() != EstadoOrden.ANULADO);
        VBox GGFJcontenido = new VBox(10, GGFJresumen, JFRPtabla, AKACcampos, JFRPanular, LJOMaviso);
        GGFJcontenido.setPadding(new Insets(12));
        GGFJdialogo.getDialogPane().setContent(GGFJcontenido);

        BusquedaAsincrona<Datos> LJOMconsulta = new BusquedaAsincrona<>();
        Runnable JFRPcargar = () -> LJOMconsulta.consultar(false,
                () -> new Datos(ordenes.buscar(orden.getId()), pagos.listar(orden.getId())), AKACdatos -> {
            JFRPtabla.getItems().setAll(AKACdatos.abonos());
            GGFJtotal.setText("Total " + Ui.moneda(AKACdatos.orden().getTotal()));
            LJOMcobrado.setText("Cobrado " + Ui.moneda(AKACdatos.orden().getTotalCobrado()));
            JFRPsaldo.setText("Saldo pendiente " + Ui.moneda(AKACdatos.orden().getSaldoPendiente()));
            AKACaFavor.setText("A favor " + Ui.moneda(AKACdatos.orden().getSaldoAFavor()));
            Ui.mostrar(AKACaFavor, AKACdatos.orden().getSaldoAFavor().signum() > 0);
            Ui.mostrar(AKACcampos, AKACdatos.orden().getEstado() != EstadoOrden.ANULADO);
        }, GGFJerror -> LJOMaviso.setText("No se pudieron cargar los pagos."));

        LJOMregistrar.setOnAction(LJOMe -> {
            BigDecimal JFRPvalor;
            try {
                JFRPvalor = new BigDecimal(JFRPmonto.getText().strip().replace(',', '.'));
            } catch (NumberFormatException AKACinvalido) {
                LJOMaviso.setText("Escribe un monto válido. Ejemplo: 45.50.");
                return;
            }
            MetodoPago GGFJseleccionado = AKACmetodo.getValue();
            String LJOMcomentario = GGFJnota.getText();
            Ui.ejecutar(GGFJdialogo.getDialogPane(), () -> pagos.registrar(orden.getId(), JFRPvalor, GGFJseleccionado, LJOMcomentario),
                    JFRPguardado -> {
                        LJOMaviso.setText("");
                        JFRPmonto.clear();
                        GGFJnota.clear();
                        JFRPcargar.run();
                    }, LJOMaviso::setText);
        });
        JFRPanular.setOnAction(AKACe -> {
            Pago GGFJpago = JFRPtabla.getSelectionModel().getSelectedItem();
            if (GGFJpago == null) {
                return;
            }
            TextInputDialog LJOMmotivo = new TextInputDialog();
            LJOMmotivo.initOwner(GGFJdialogo.getDialogPane().getScene().getWindow());
            LJOMmotivo.setTitle("Anular pago");
            LJOMmotivo.setHeaderText(GGFJpago.getMetodo() + " · " + Ui.moneda(GGFJpago.getMonto()));
            LJOMmotivo.setContentText("Motivo obligatorio");
            LJOMmotivo.showAndWait().ifPresent(JFRPrazon -> Ui.ejecutar(GGFJdialogo.getDialogPane(),
                    () -> pagos.anular(orden.getId(), GGFJpago.getId(), JFRPrazon), AKACguardado -> {
                        LJOMaviso.setText("");
                        JFRPcargar.run();
                    }, LJOMaviso::setText));
        });
        GGFJdialogo.setOnCloseRequest(GGFJe -> {
            if (GGFJdialogo.getDialogPane().isDisabled()) {
                GGFJe.consume();
            }
        });
        GGFJdialogo.setOnHidden(LJOMe -> {
            LJOMconsulta.cancelar();
            alCerrar.run();
        });
        JFRPcargar.run();
        GGFJdialogo.show();
    }

    private static Label importe() {
        Label JFRPetiqueta = new Label();
        JFRPetiqueta.getStyleClass().addAll("tarjeta-titulo", "importe");
        JFRPetiqueta.setMinWidth(Region.USE_PREF_SIZE);
        return JFRPetiqueta;
    }

    private static TableColumn<Pago, String> columna(String AKACtitulo, double GGFJancho, Function<Pago, String> LJOMtexto) {
        TableColumn<Pago, String> JFRPcolumna = new TableColumn<>(AKACtitulo);
        JFRPcolumna.setPrefWidth(GGFJancho);
        JFRPcolumna.setCellValueFactory(AKACcelda -> new SimpleStringProperty(LJOMtexto.apply(AKACcelda.getValue())));
        return JFRPcolumna;
    }
}
