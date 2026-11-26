package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.TipoDetalle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ventana modal para editar un servicio o repuesto de una orden (doble clic en la tabla).
 * Servicios con precio editable y piezas con cantidad editable y precio histórico.
 */
public final class DialogoDetalle {

    /** Valores ya validados que devuelve la ventana. */
    public record Datos(String descripcion, BigDecimal precio, int cantidad, BigDecimal costoPendiente) {
    }

    private DialogoDetalle() {
    }

    public static Optional<Datos> pedir(Window dueno, DetalleOrden detalle, boolean admin) {
        boolean AKACpermiteCosto = admin && detalle.getTipo() == TipoDetalle.REPUESTO_EXTERNO
                && detalle.getCostoUnitario() == null;
        boolean GGFJtieneCantidad = detalle.getTipo() != TipoDetalle.SERVICIO;
        long LJOMdisponible = detalle.getRepuesto() == null || detalle.getRepuesto().getStock() == null
                ? -1 : (long) detalle.getRepuesto().getStock() + detalle.getCantidad();

        Dialog<Datos> JFRPdialogo = new Dialog<>();
        if (dueno != null) {
            JFRPdialogo.initOwner(dueno);
        }
        JFRPdialogo.setTitle("Editar " + detalle.getTipo().toString().toLowerCase(java.util.Locale.ROOT));
        JFRPdialogo.setHeaderText(detalle.getDescripcion());

        ButtonType AKACguardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        JFRPdialogo.getDialogPane().getButtonTypes().addAll(AKACguardar, ButtonType.CANCEL);

        TextField GGFJtxtDescripcion = new TextField(detalle.getDescripcion());
        TextField LJOMtxtPrecio = new TextField(detalle.getPrecioUnitario().toPlainString());
        LJOMtxtPrecio.setEditable(!GGFJtieneCantidad);
        TextField JFRPtxtCantidad = new TextField(String.valueOf(detalle.getCantidad()));
        TextField AKACtxtCosto = new TextField();
        AKACtxtCosto.setPromptText("Costo de compra");
        GGFJtxtDescripcion.setPrefColumnCount(24);

        GridPane GGFJcampos = new GridPane();
        GGFJcampos.setHgap(12);
        GGFJcampos.setVgap(10);
        GGFJcampos.add(new Label("Descripción"), 0, 0);
        GGFJcampos.add(GGFJtxtDescripcion, 1, 0);
        GGFJcampos.add(new Label("Precio (S/)"), 0, 1);
        GGFJcampos.add(LJOMtxtPrecio, 1, 1);
        if (GGFJtieneCantidad) {
            GGFJcampos.add(new Label("Cantidad"), 0, 2);
            GGFJcampos.add(JFRPtxtCantidad, 1, 2);
        }

        if (AKACpermiteCosto) {
            GGFJcampos.add(new Label("Costo pendiente (S/)"), 0, 3);
            GGFJcampos.add(AKACtxtCosto, 1, 3);
        }

        Label LJOMlblAyuda = new Label(LJOMdisponible >= 0
                ? "Unidades disponibles para esta orden: " + LJOMdisponible + " (el stock se ajusta solo)."
                : "");
        LJOMlblAyuda.setWrapText(true);
        Label JFRPlblError = new Label();
        JFRPlblError.setWrapText(true);
        JFRPlblError.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        VBox AKACcontenido = new VBox(10, GGFJcampos);
        if (GGFJtieneCantidad && LJOMdisponible >= 0) {
            AKACcontenido.getChildren().add(LJOMlblAyuda);
        }
        AKACcontenido.getChildren().add(JFRPlblError);
        AKACcontenido.setPadding(new Insets(10));
        JFRPdialogo.getDialogPane().setContent(AKACcontenido);

        Datos[] GGFJvalido = new Datos[1];
        Button LJOMbotonGuardar = (Button) JFRPdialogo.getDialogPane().lookupButton(AKACguardar);
        LJOMbotonGuardar.addEventFilter(ActionEvent.ACTION, JFRPevento -> {
            GGFJvalido[0] = null;
            String AKACdescripcion = GGFJtxtDescripcion.getText() == null ? "" : GGFJtxtDescripcion.getText().trim();
            if (AKACdescripcion.isEmpty()) {
                JFRPlblError.setText("La descripción es obligatoria.");
                JFRPevento.consume();
                return;
            }
            if (AKACdescripcion.length() > 120) {
                JFRPlblError.setText("La descripción no puede superar 120 caracteres.");
                JFRPevento.consume();
                return;
            }
            BigDecimal GGFJprecio;
            try {
                GGFJprecio = new BigDecimal(LJOMtxtPrecio.getText().trim().replace(',', '.'));
            } catch (NumberFormatException LJOMe) {
                JFRPlblError.setText("El precio debe ser un número (ejemplo: 45.50).");
                JFRPevento.consume();
                return;
            }
            if (GGFJprecio.signum() < 0) {
                JFRPlblError.setText("El precio no puede ser negativo.");
                JFRPevento.consume();
                return;
            }
            int JFRPcantidad = 1;
            if (GGFJtieneCantidad) {
                try {
                    JFRPcantidad = Integer.parseInt(JFRPtxtCantidad.getText().trim());
                } catch (NumberFormatException AKACe) {
                    JFRPlblError.setText("La cantidad debe ser un número entero.");
                    JFRPevento.consume();
                    return;
                }
                if (JFRPcantidad < 1) {
                    JFRPlblError.setText("La cantidad debe ser al menos 1.");
                    JFRPevento.consume();
                    return;
                }
            }
            BigDecimal GGFJcosto = null;
            if (AKACpermiteCosto && !AKACtxtCosto.getText().isBlank()) {
                try {
                    GGFJcosto = new BigDecimal(AKACtxtCosto.getText().strip().replace(',', '.'));
                } catch (NumberFormatException LJOMe) {
                    JFRPlblError.setText("Escribe un costo válido.");
                    JFRPevento.consume();
                    return;
                }
            }
            GGFJvalido[0] = new Datos(AKACdescripcion, GGFJprecio, JFRPcantidad, GGFJcosto);
        });
        JFRPdialogo.setResultConverter(JFRPboton -> JFRPboton == AKACguardar ? GGFJvalido[0] : null);

        Platform.runLater(GGFJtxtDescripcion::requestFocus);
        return JFRPdialogo.showAndWait();
    }
}
