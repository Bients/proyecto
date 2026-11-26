package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Zona;
import com.ejemplo.tallerpro.model.TipoCarroceria;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.ejemplo.tallerpro.util.PlantillasInspeccion.ANCHO;
import static com.ejemplo.tallerpro.util.PlantillasInspeccion.ALTO;

public final class DiagramaInspeccion extends ScrollPane {
    public record Punto(Zona zona, Vista vista, double x, double y) {
    }

    private final Pane lienzo = new Pane();
    private final Group dibujo = new Group();
    private final Group controles = new Group();
    private final Scale escala = new Scale(1, 1);
    private double zoom = 1;

    public DiagramaInspeccion() {
        lienzo.setPrefSize(ANCHO, ALTO);
        lienzo.setMinSize(ANCHO, ALTO);
        lienzo.setMaxSize(ANCHO, ALTO);
        lienzo.getChildren().addAll(dibujo, controles);
        lienzo.getTransforms().add(escala);
        StackPane AKACcentrado = new StackPane(new Group(lienzo));
        AKACcentrado.setAlignment(Pos.CENTER);
        setContent(AKACcentrado);
        setFitToHeight(true);
        setFitToWidth(true);
        setPannable(true);
        setMinHeight(255);
        setMinWidth(240);
        getStyleClass().add("inspeccion-lienzo");
        viewportBoundsProperty().addListener((GGFJobs, LJOMantes, JFRPahora) -> ajustarEscala());
        setOnScroll(AKACe -> {
            if (AKACe.isControlDown() && AKACe.getDeltaY() != 0) {
                zoom(AKACe.getDeltaY() > 0 ? 1.2 : 1 / 1.2);
                AKACe.consume();
            }
        });
    }

    public void zoom(double factor) {
        zoom = Math.max(1, Math.min(3.4, zoom * factor));
        ajustarEscala();
    }

    public void ajustarVista() {
        zoom = 1;
        ajustarEscala();
        setHvalue(0.5);
        setVvalue(0.5);
    }

    private void ajustarEscala() {
        double GGFJbase = Math.min(getViewportBounds().getWidth() / ANCHO, getViewportBounds().getHeight() / ALTO);
        double LJOMaumento = Math.max(0.4, GGFJbase * zoom);
        escala.setX(LJOMaumento);
        escala.setY(LJOMaumento);
    }

    public void limpiar() {
        dibujo.getChildren().clear();
        controles.getChildren().clear();
    }

    public void mostrar(TipoCarroceria tipo, Vista vista, Set<Capa> capas, List<PlantillasInspeccion.Marca> marcas,
                       Long seleccion, Punto punto, Consumer<Punto> seleccionarZona, Consumer<Long> seleccionarMarca) {
        limpiar();
        var JFRPplantilla = PlantillasInspeccion.obtener(tipo, vista);
        if (JFRPplantilla.espejo()) {
            dibujo.getTransforms().setAll(new javafx.scene.transform.Translate(ANCHO, 0), new Scale(-1, 1));
        } else {
            dibujo.getTransforms().clear();
        }
        for (var AKACpieza : JFRPplantilla.trazos()) {
            SVGPath GGFJpath = new SVGPath();
            GGFJpath.setContent(AKACpieza.svg());
            GGFJpath.getStyleClass().addAll("inspeccion-pieza", "inspeccion-" + AKACpieza.clase());
            dibujo.getChildren().add(GGFJpath);
        }
        dibujo.setMouseTransparent(true);
        for (Zona LJOMzona : Zona.values()) {
            var JFRPregion = LJOMzona.enVista(vista);
            if (!LJOMzona.aplica(tipo) || !capas.contains(LJOMzona.capa()) || JFRPregion == null) {
                continue;
            }
            Rectangle AKACforma = new Rectangle((JFRPregion.x() - JFRPregion.ancho() / 2) * ANCHO,
                    (JFRPregion.y() - JFRPregion.alto() / 2) * ALTO, JFRPregion.ancho() * ANCHO, JFRPregion.alto() * ALTO);
            AKACforma.setArcWidth(16);
            AKACforma.setArcHeight(16);
            AKACforma.getStyleClass().add("inspeccion-zona");
            if (punto != null && punto.zona() == LJOMzona && punto.vista() == vista) {
                AKACforma.getStyleClass().add("inspeccion-zona-activa");
            }
            AKACforma.setCursor(Cursor.HAND);
            Tooltip.install(AKACforma, new Tooltip(LJOMzona.toString()));
            AKACforma.setOnMouseClicked(GGFJe -> {
                if (GGFJe.isStillSincePress() && GGFJe.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    var LJOMlocal = lienzo.sceneToLocal(GGFJe.getSceneX(), GGFJe.getSceneY());
                    seleccionarZona.accept(new Punto(LJOMzona, vista, limitar(LJOMlocal.getX() / ANCHO),
                            limitar(LJOMlocal.getY() / ALTO)));
                    GGFJe.consume();
                }
            });
            controles.getChildren().add(AKACforma);
        }
        var JFRPenCapa = marcas.stream().filter(AKACm -> capas.contains(AKACm.hallazgo().getCapa())).toList();
        for (var GGFJubicacion : PlantillasInspeccion.ubicar(vista, JFRPenCapa)) {
            var LJOMmarca = GGFJubicacion.marca();
            var JFRPhallazgo = LJOMmarca.hallazgo();
            var AKACguia = new Line(GGFJubicacion.anclaX(), GGFJubicacion.anclaY(), GGFJubicacion.x(), GGFJubicacion.y());
            AKACguia.setStyle("-fx-stroke: #d46b2b; -fx-stroke-width: 1.5;");
            AKACguia.setMouseTransparent(true);
            controles.getChildren().add(AKACguia);
            Label GGFJnumero = new Label(Integer.toString(LJOMmarca.numero()));
            GGFJnumero.getStyleClass().add("inspeccion-marca");
            if (JFRPhallazgo.getId().equals(seleccion)) {
                GGFJnumero.getStyleClass().add("inspeccion-marca-activa");
            }
            GGFJnumero.setMinSize(28, 28);
            GGFJnumero.setAlignment(Pos.CENTER);
            GGFJnumero.setLayoutX(GGFJubicacion.x() - 14);
            GGFJnumero.setLayoutY(GGFJubicacion.y() - 14);
            GGFJnumero.setCursor(Cursor.HAND);
            GGFJnumero.setAccessibleText("Hallazgo " + LJOMmarca.numero() + " en " + JFRPhallazgo.getZona());
            Tooltip.install(GGFJnumero, new Tooltip(GGFJnumero.getAccessibleText()));
            GGFJnumero.setOnMouseClicked(LJOMe -> {
                seleccionarMarca.accept(JFRPhallazgo.getId());
                LJOMe.consume();
            });
            controles.getChildren().add(GGFJnumero);
        }
    }

    private static double limitar(double JFRPvalor) {
        return Math.max(0, Math.min(1, JFRPvalor));
    }
}
