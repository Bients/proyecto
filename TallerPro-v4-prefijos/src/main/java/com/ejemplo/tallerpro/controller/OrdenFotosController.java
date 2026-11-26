package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.FaseFoto;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.service.OrdenFotoService;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import com.ejemplo.tallerpro.util.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class OrdenFotosController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OrdenFotoService servicio;

    @FXML private VBox raiz;
    @FXML private ComboBox<FaseFoto> cmbFase;
    @FXML private Button btnAgregar;
    @FXML private MenuButton btnExportar;
    @FXML private FlowPane galeria;
    @FXML private ScrollPane desplazamiento;
    @FXML private Label aviso;
    @FXML private PaginadorBusqueda<OrdenFotoService.Tarjeta> paginador;

    private OrdenTrabajo ordenActual;

    @FXML
    public void initialize() {
        aviso.visibleProperty().bind(aviso.textProperty().isNotEmpty());
        aviso.managedProperty().bind(aviso.visibleProperty());
        cmbFase.getItems().setAll(FaseFoto.values());
        cmbFase.setValue(FaseFoto.INGRESO);
        paginador.configurar(AKACpagina -> {
            if (ordenActual == null) {
                return Page::empty;
            }
            Long GGFJid = ordenActual.getId();
            FaseFoto LJOMfase = cmbFase.getValue();
            return () -> servicio.galeria(GGFJid, LJOMfase, AKACpagina, PaginadorBusqueda.LIMITE);
        }, JFRPresultado -> {
            galeria.getChildren().setAll(JFRPresultado.getContent().stream().map(this::tarjeta).toList());
            desplazamiento.setVvalue(0);
            aviso.setText(JFRPresultado.isEmpty() ? "Sin fotos en esta fase." : "");
        }, aviso::setText);
        cmbFase.valueProperty().addListener((AKACobs, GGFJanterior, LJOMnueva) -> recargar());
        raiz.setOnDragOver(JFRPe -> {
            if (editable() && JFRPe.getDragboard().hasFiles()) {
                JFRPe.acceptTransferModes(TransferMode.COPY);
                JFRPe.consume();
            }
        });
        raiz.setOnDragDropped(AKACe -> {
            boolean GGFJaceptado = editable() && AKACe.getDragboard().hasFiles();
            if (GGFJaceptado) {
                subir(AKACe.getDragboard().getFiles());
            }
            AKACe.setDropCompleted(GGFJaceptado);
            AKACe.consume();
        });
        mostrarOrden(null);
    }

    public void mostrarOrden(OrdenTrabajo orden) {
        boolean LJOMcambio = ordenActual == null || orden == null || !ordenActual.getId().equals(orden.getId())
                || ordenActual.getEstado() != orden.getEstado();
        ordenActual = orden;
        btnAgregar.setDisable(!editable());
        btnExportar.setDisable(orden == null);
        cmbFase.setDisable(orden == null);
        if (LJOMcambio) {
            recargar();
        }
    }

    public void recargar() {
        paginador.vaciar();
        galeria.getChildren().clear();
        if (ordenActual == null) {
            aviso.setText("Guarda o selecciona una orden para consultar sus fotos.");
        } else {
            paginador.reiniciar();
        }
    }

    @FXML
    private void onAgregar() {
        FileChooser JFRPselector = new FileChooser();
        JFRPselector.setTitle("Agregar fotos a la orden");
        JFRPselector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes JPG, PNG o WebP", "*.jpg", "*.jpeg", "*.png", "*.webp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        List<File> AKACelegidas = JFRPselector.showOpenMultipleDialog(raiz.getScene().getWindow());
        if (AKACelegidas != null && !AKACelegidas.isEmpty()) {
            subir(AKACelegidas);
        }
    }

    private void subir(List<File> GGFJarchivos) {
        Long LJOMid = ordenActual.getId();
        FaseFoto JFRPfase = cmbFase.getValue();
        var AKACrutas = GGFJarchivos.stream().map(File::toPath).toList();
        Ui.ejecutar(raiz, () -> servicio.agregar(LJOMid, JFRPfase, AKACrutas), GGFJtotal -> {
            if (ordenActual != null && LJOMid.equals(ordenActual.getId())) {
                recargar();
            }
        }, aviso::setText);
    }

    private HBox tarjeta(OrdenFotoService.Tarjeta LJOMtarjeta) {
        var JFRPfoto = LJOMtarjeta.foto();
        Long AKACid = ordenActual.getId();
        ImageView GGFJimagen = imagen(LJOMtarjeta.miniatura(), 92, 60);
        Button LJOMampliar = new Button();
        LJOMampliar.setPadding(javafx.geometry.Insets.EMPTY);
        LJOMampliar.setGraphic(GGFJimagen);
        LJOMampliar.setAccessibleText("Ampliar foto de " + JFRPfoto.getFase());
        LJOMampliar.setOnAction(JFRPe -> Ui.ejecutar(raiz, () -> servicio.original(AKACid, JFRPfoto.getId()),
                AKACbytes -> ampliar(AKACbytes, LJOMtarjeta), aviso::setText));
        Label GGFJnota = new Label(JFRPfoto.getNota().isBlank() ? "Sin nota" : JFRPfoto.getNota());
        GGFJnota.setWrapText(true);
        GGFJnota.setPrefWidth(177);
        GGFJnota.setPrefHeight(42);
        GGFJnota.setMinHeight(Region.USE_PREF_SIZE);
        Tooltip LJOMdetalle = new Tooltip(FECHA.format(Instant.parse(JFRPfoto.getFecha())) + "\n" + JFRPfoto.getNota());
        GGFJnota.setTooltip(LJOMdetalle);

        VBox JFRPdatos = new VBox(3, GGFJnota);
        HBox AKACtarjetaVista = new HBox(8, LJOMampliar, JFRPdatos);
        if (LJOMtarjeta.editable()) {
            Button GGFJeditar = new Button("Nota");
            Ui.accion(GGFJeditar, "accion-secundaria");
            GGFJeditar.setOnAction(LJOMe -> editarNota(AKACid, LJOMtarjeta));
            Button JFRPretirar = new Button("Eliminar");
            Ui.accion(JFRPretirar, "danger", "accion-secundaria");
            JFRPretirar.setOnAction(AKACe -> {
                if (Ui.confirmar("Eliminar foto", "¿Quieres retirar esta foto de la orden?")) {
                    Ui.ejecutar(raiz, () -> {
                        servicio.eliminar(AKACid, JFRPfoto.getId());
                        return null;
                    }, GGFJterminado -> recargar(), aviso::setText);
                }
            });
            JFRPdatos.getChildren().add(new HBox(5, GGFJeditar, JFRPretirar));
        }
        AKACtarjetaVista.getStyleClass().addAll("tarjeta", "ficha-foto");
        return AKACtarjetaVista;
    }

    private void editarNota(Long LJOMid, OrdenFotoService.Tarjeta JFRPtarjeta) {
        TextInputDialog AKACdialogo = new TextInputDialog(JFRPtarjeta.foto().getNota());
        AKACdialogo.initOwner(raiz.getScene().getWindow());
        AKACdialogo.setTitle("Nota de la foto");
        AKACdialogo.setHeaderText(JFRPtarjeta.foto().getFase().toString());
        AKACdialogo.setContentText("Nota");
        AKACdialogo.showAndWait().ifPresent(GGFJtexto -> Ui.ejecutar(raiz, () -> {
            servicio.guardarNota(LJOMid, JFRPtarjeta.foto().getId(), GGFJtexto);
            return null;
        }, LJOMguardada -> recargar(), aviso::setText));
    }

    private void ampliar(byte[] JFRPdatos, OrdenFotoService.Tarjeta AKACtarjeta) {
        Dialog<Void> GGFJdialogo = new Dialog<>();
        GGFJdialogo.initOwner(raiz.getScene().getWindow());
        GGFJdialogo.setTitle("Foto de " + AKACtarjeta.foto().getFase());
        GGFJdialogo.getDialogPane().getStylesheets().setAll(raiz.getScene().getStylesheets());
        Label LJOMnota = new Label(AKACtarjeta.foto().getNota());
        LJOMnota.setWrapText(true);
        GGFJdialogo.getDialogPane().setContent(new VBox(8, imagen(JFRPdatos, 850, 520), LJOMnota));
        GGFJdialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        GGFJdialogo.setResultConverter(JFRPboton -> null);
        GGFJdialogo.show();
    }

    @FXML
    private void onZip() {
        Long AKACid = ordenActual.getId();
        File GGFJarchivo = Ui.guardarArchivo(raiz.getScene().getWindow(), "Exportar fotos",
                ordenActual.getNumeroOrden() + "-fotos.zip", "Archivo ZIP", "*.zip");
        if (GGFJarchivo != null) {
            Ui.ejecutar(raiz, () -> servicio.exportarZip(AKACid, GGFJarchivo.toPath()),
                    LJOMdestino -> aviso.setText("Fotos exportadas a " + LJOMdestino), aviso::setText);
        }
    }

    @FXML
    private void onCarpeta() {
        Long JFRPid = ordenActual.getId();
        DirectoryChooser AKACselector = new DirectoryChooser();
        AKACselector.setTitle("Elegir carpeta para las fotos");
        File GGFJdirectorio = AKACselector.showDialog(raiz.getScene().getWindow());
        if (GGFJdirectorio != null) {
            Ui.ejecutar(raiz, () -> servicio.exportarCarpeta(JFRPid, GGFJdirectorio.toPath()),
                    LJOMdestino -> aviso.setText("Fotos exportadas a " + LJOMdestino), aviso::setText);
        }
    }

    private ImageView imagen(byte[] JFRPdatos, double AKACancho, double GGFJalto) {
        ImageView LJOMvista = new ImageView(new Image(new ByteArrayInputStream(JFRPdatos)));
        LJOMvista.setFitWidth(AKACancho);
        LJOMvista.setFitHeight(GGFJalto);
        LJOMvista.setPreserveRatio(true);
        return LJOMvista;
    }

    private boolean editable() {
        return ordenActual != null && ordenActual.getEstado() != EstadoOrden.ENTREGADO
                && ordenActual.getEstado() != EstadoOrden.ANULADO && !raiz.isDisabled();
    }
}
