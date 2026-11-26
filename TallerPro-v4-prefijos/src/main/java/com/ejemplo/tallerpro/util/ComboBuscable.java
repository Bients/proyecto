package com.ejemplo.tallerpro.util;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ComboBuscable<T> extends HBox {

    public static final int LIMITE = 18;

    private final TextField campo = new TextField();
    private final ListView<T> resultados = new ListView<>();
    private final Label aviso = new Label();
    private final Button nuevo = new Button("+ Nuevo");
    private final Popup popup = new Popup();
    private final BusquedaAsincrona<List<T>> busqueda = new BusquedaAsincrona<>();
    private final ReadOnlyObjectWrapper<T> valor = new ReadOnlyObjectWrapper<>();
    private Function<String, List<T>> consultar;
    private Function<T, String> etiqueta;
    private Function<T, String> fila;
    private Predicate<T> disponible;
    private Function<T, String> advertencia;
    private String consultaActual = "";
    private boolean seleccionando;

    public ComboBuscable() {
        Button JFRPabrir = new Button("▾");
        JFRPabrir.setAccessibleText("Mostrar resultados");
        JFRPabrir.setFocusTraversable(false);
        JFRPabrir.setOnAction(AKACe -> abrir());
        setSpacing(4);
        HBox.setHgrow(campo, Priority.ALWAYS);
        getChildren().addAll(campo, JFRPabrir);

        resultados.setFixedCellSize(34);
        resultados.setFocusTraversable(false);
        resultados.setCellFactory(GGFJlista -> new ListCell<>() {
            {
                setOnMouseClicked(LJOMe -> {
                    if (!isEmpty() && LJOMe.isStillSincePress()) {
                        elegir(getItem());
                    }
                });
            }

            @Override
            protected void updateItem(T elemento, boolean vacio) {
                super.updateItem(elemento, vacio);
                if (vacio || elemento == null) {
                    setGraphic(null);
                    return;
                }
                String JFRPalerta = advertencia.apply(elemento);
                Label AKACinsignia = new Label(JFRPalerta);
                AKACinsignia.getStyleClass().addAll("insignia",
                        disponible.test(elemento) ? "estado-reparacion" : "estado-vencido");
                AKACinsignia.setVisible(!JFRPalerta.isEmpty());
                AKACinsignia.setManaged(!JFRPalerta.isEmpty());
                HBox GGFJrenglon = new HBox(8, resaltar(fila.apply(elemento)), AKACinsignia);
                GGFJrenglon.setAlignment(Pos.CENTER_LEFT);
                setGraphic(GGFJrenglon);
            }
        });

        VBox LJOMcontenido = new VBox(5, aviso, resultados, nuevo);
        LJOMcontenido.getStyleClass().addAll("tarjeta", "buscador-popup");
        aviso.getStyleClass().add("pagina-sub");
        nuevo.getStyleClass().add("button-outlined");
        nuevo.setManaged(false);
        nuevo.setVisible(false);
        popup.getContent().add(LJOMcontenido);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setOnHidden(JFRPe -> busqueda.cancelar());

        campo.textProperty().addListener((AKACobs, GGFJanterior, LJOMtexto) -> {
            if (!seleccionando && consultar != null) {
                campo.setTooltip(null);
                valor.set(null);
                solicitar(LJOMtexto, true);
            }
        });
        campo.addEventFilter(KeyEvent.KEY_PRESSED, this::teclado);
        resultados.addEventFilter(KeyEvent.KEY_PRESSED, this::teclado);
        disabledProperty().addListener((JFRPobs, AKACanterior, GGFJdeshabilitado) -> {
            if (GGFJdeshabilitado) {
                cerrar();
            }
        });
        sceneProperty().addListener((LJOMobs, JFRPanterior, AKACescena) -> {
            if (AKACescena == null) {
                cerrar();
            }
        });
    }

    public void configurar(Function<String, List<T>> consulta, Function<T, String> etiqueta,
                           Function<T, String> fila) {
        configurar(consulta, etiqueta, fila, GGFJelemento -> true, LJOMelemento -> "");
    }

    public void configurar(Function<String, List<T>> consulta, Function<T, String> etiqueta,
                           Function<T, String> fila, Predicate<T> disponible, Function<T, String> advertencia) {
        this.consultar = consulta;
        this.etiqueta = etiqueta;
        this.fila = fila;
        this.disponible = disponible;
        this.advertencia = advertencia;
    }

    public void permitirAlta(Supplier<T> crear) {
        nuevo.setManaged(true);
        nuevo.setVisible(true);
        nuevo.setOnAction(JFRPe -> {
            cerrar();
            T AKACregistro = crear.get();
            if (AKACregistro != null && disponible.test(AKACregistro)) {
                setValue(AKACregistro);
            }
            campo.requestFocus();
        });
    }

    public void setPromptText(String texto) {
        campo.setPromptText(texto);
    }

    public String getPromptText() {
        return campo.getPromptText();
    }

    public void enfocar() {
        campo.requestFocus();
        campo.selectAll();
    }

    public boolean tieneTextoSinSeleccion() {
        return valor.get() == null && !campo.getText().isBlank();
    }

    public T getValue() {
        return valor.get();
    }

    public ReadOnlyObjectProperty<T> valueProperty() {
        return valor.getReadOnlyProperty();
    }

    public void setValue(T elemento) {
        cerrar();
        seleccionando = true;
        try {
            campo.setText(elemento == null ? "" : etiqueta.apply(elemento));
            campo.setTooltip(elemento == null ? null : new Tooltip(fila.apply(elemento)));
            valor.set(elemento);
        } finally {
            seleccionando = false;
        }
    }

    private void abrir() {
        if (consultar != null && !isDisabled()) {
            campo.requestFocus();
            solicitar(getValue() == null ? campo.getText() : "", false);
        }
    }

    private void cerrar() {
        busqueda.cancelar();
        popup.hide();
    }

    private void solicitar(String GGFJtexto, boolean LJOMdiferida) {
        if (isDisabled() || getScene() == null || getScene().getWindow() == null) {
            return;
        }
        String JFRPtermino = GGFJtexto.strip();
        consultaActual = JFRPtermino;
        resultados.getItems().clear();
        aviso.setText("Buscando…");
        Ui.mostrar(aviso, true);
        Ui.mostrar(resultados, false);
        if (!popup.isShowing()) {
            Bounds AKACposicion = campo.localToScreen(campo.getBoundsInLocal());
            if (AKACposicion == null) {
                return;
            }
            resultados.setPrefWidth(Math.max(430, getWidth()));
            popup.getScene().getStylesheets().setAll(getScene().getStylesheets());
            popup.show(campo, AKACposicion.getMinX(), AKACposicion.getMaxY());
        }
        busqueda.consultar(LJOMdiferida, () -> consultar.apply(JFRPtermino), GGFJencontrados -> {
            resultados.getItems().setAll(GGFJencontrados.stream().limit(LIMITE).toList());
            resultados.setPrefHeight(Math.min(GGFJencontrados.size(), 7) * 34 + 4);
            Ui.mostrar(resultados, !GGFJencontrados.isEmpty());
            aviso.setText("Sin coincidencias.");
            Ui.mostrar(aviso, GGFJencontrados.isEmpty());
        }, LJOMerror -> aviso.setText("No se pudo completar la búsqueda."));
    }

    private void teclado(KeyEvent JFRPe) {
        KeyCode AKACtecla = JFRPe.getCode();
        if (AKACtecla == KeyCode.ESCAPE && popup.isShowing()) {
            cerrar();
            JFRPe.consume();
        } else if (AKACtecla == KeyCode.DOWN || (AKACtecla == KeyCode.UP && popup.isShowing())) {
            if (!popup.isShowing()) {
                abrir();
            } else {
                mover(AKACtecla == KeyCode.DOWN ? 1 : -1);
            }
            JFRPe.consume();
        } else if (AKACtecla == KeyCode.ENTER && popup.isShowing()) {
            T GGFJelegido = resultados.getSelectionModel().getSelectedItem();
            if (GGFJelegido != null) {
                elegir(GGFJelegido);
            }
            JFRPe.consume();
        } else if (AKACtecla == KeyCode.TAB && popup.isShowing()) {
            cerrar();
        }
    }

    private void mover(int LJOMpaso) {
        int JFRPactual = resultados.getSelectionModel().getSelectedIndex();
        for (int AKACi = JFRPactual < 0 ? (LJOMpaso > 0 ? 0 : resultados.getItems().size() - 1) : JFRPactual + LJOMpaso; AKACi >= 0 && AKACi < resultados.getItems().size(); AKACi += LJOMpaso) {
            if (disponible.test(resultados.getItems().get(AKACi))) {
                resultados.getSelectionModel().select(AKACi);
                resultados.scrollTo(AKACi);
                return;
            }
        }
    }

    private void elegir(T GGFJelemento) {
        if (disponible.test(GGFJelemento)) {
            setValue(GGFJelemento);
            enfocar();
        }
    }

    private TextFlow resaltar(String LJOMtexto) {
        TextFlow JFRPflujo = new TextFlow();
        if (consultaActual.isEmpty()) {
            JFRPflujo.getChildren().add(new Text(LJOMtexto));
            return JFRPflujo;
        }
        String AKACpalabras = Arrays.stream(consultaActual.split("\\s+"))
                .map(Pattern::quote).collect(Collectors.joining("|"));
        String GGFJcompacta = Placa.normalizar(consultaActual);
        if (GGFJcompacta.matches("(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{3,8}")) {
            String LJOMplaca = GGFJcompacta.chars().mapToObj(JFRPc -> Character.toString(JFRPc))
                    .collect(Collectors.joining("[ -]*"));
            AKACpalabras = LJOMplaca + "|" + AKACpalabras;
        }
        Pattern AKACpatron = Pattern.compile(AKACpalabras, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var GGFJcoincidencia = AKACpatron.matcher(LJOMtexto);
        int LJOMposicion = 0;
        while (GGFJcoincidencia.find()) {
            JFRPflujo.getChildren().add(new Text(LJOMtexto.substring(LJOMposicion, GGFJcoincidencia.start())));
            Text JFRPresaltado = new Text(GGFJcoincidencia.group());
            JFRPresaltado.getStyleClass().add("buscador-coincidencia");
            JFRPflujo.getChildren().add(JFRPresaltado);
            LJOMposicion = GGFJcoincidencia.end();
        }
        JFRPflujo.getChildren().add(new Text(LJOMtexto.substring(LJOMposicion)));
        return JFRPflujo;
    }
}
