package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.controller.MainController;
import com.ejemplo.tallerpro.util.HostServicesHolder;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.Restauracion;
import com.ejemplo.tallerpro.util.CopiaTaller;
import com.ejemplo.tallerpro.util.Tema;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

public class JavaFxApplication extends Application {

    public record CierreAplicacion() {
    }

    private static final long SPLASH_MINIMO_MS = 1500;

    private ConfigurableApplicationContext context;
    private Stage stage;
    private Image icono;
    private String advertenciaRestauracion;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Tema.aplicar();
        this.icono = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icono.png")));
        stage.getIcons().add(icono);
        stage.setTitle("TallerPro | Gestión de Taller Automotriz");

        Stage LJOMsplash = crearSplash();
        LJOMsplash.show();
        long JFRPinicio = System.currentTimeMillis();

        // Spring arranca en un hilo aparte para que el splash se vea y no se congele
        Task<ConfigurableApplicationContext> AKACarranque = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() throws Exception {
                return new SpringApplicationBuilder(TallerProApplication.class)
                        .web(WebApplicationType.NONE)
                        .initializers(GGFJcontexto -> {
                            Path LJOMdatos = Path.of(GGFJcontexto.getEnvironment()
                                    .getRequiredProperty("taller.datos.directorio"));
                            try {
                                Files.createDirectories(LJOMdatos);
                                Restauracion.aplicarPendiente(LJOMdatos);
                            } catch (IOException | SQLException JFRPe) {
                                if (JFRPe.getSuppressed().length != 0 || !CopiaTaller.esBaseSqlite(LJOMdatos.resolve(CopiaTaller.BASE))) {
                                    throw new IllegalStateException("No se pudo restaurar el respaldo. " + JFRPe.getMessage(), JFRPe);
                                }
                                advertenciaRestauracion = "La restauración sigue pendiente y se conserva la base actual. "
                                        + JFRPe.getMessage();
                            }
                        })
                        .run();
            }
        };
        AKACarranque.setOnSucceeded(AKACe -> {
            context = AKACarranque.getValue();
            context.getBean(HostServicesHolder.class).setHostServices(getHostServices());
            context.getBean(Navegador.class).configurar(this::mostrarLogin, this::mostrarPrincipal, context::getBean);

            long GGFJrestante = Math.max(0, SPLASH_MINIMO_MS - (System.currentTimeMillis() - JFRPinicio));
            PauseTransition LJOMespera = new PauseTransition(Duration.millis(GGFJrestante));
            LJOMespera.setOnFinished(JFRPev -> {
                LJOMsplash.close();
                mostrarLogin();
                stage.show();
                if (advertenciaRestauracion != null) {
                    Alert AKACalerta = new Alert(Alert.AlertType.WARNING, advertenciaRestauracion);
                    AKACalerta.initOwner(stage);
                    AKACalerta.setHeaderText("Restauración pendiente");
                    AKACalerta.show();
                }
            });
            LJOMespera.play();
        });
        AKACarranque.setOnFailed(GGFJe -> {
            LJOMsplash.close();
            Throwable LJOMerror = AKACarranque.getException();
            Alert JFRPalerta = new Alert(Alert.AlertType.ERROR,
                    "No se pudo iniciar la aplicación:\n" + (LJOMerror == null ? "" : LJOMerror.getMessage()));
            JFRPalerta.setHeaderText("Error de arranque");
            JFRPalerta.showAndWait();
            Platform.exit();
        });
        Thread AKAChilo = new Thread(AKACarranque, "arranque-spring");
        AKAChilo.setDaemon(true);
        AKAChilo.start();
    }

    private Stage crearSplash() {
        ImageView GGFJlogo = new ImageView(icono);
        GGFJlogo.setFitWidth(110);
        GGFJlogo.setFitHeight(110);

        Label LJOMnombre = new Label("TallerPro");
        LJOMnombre.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label JFRPlema = new Label("Gestión de taller automotriz");
        JFRPlema.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Label AKACcargando = new Label("Iniciando…");
        AKACcargando.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        ProgressBar GGFJbarra = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        GGFJbarra.setPrefWidth(240);
        GGFJbarra.setStyle("-fx-accent: #f97316;");

        VBox LJOMcaja = new VBox(10, GGFJlogo, LJOMnombre, JFRPlema, GGFJbarra, AKACcargando);
        LJOMcaja.setAlignment(Pos.CENTER);
        LJOMcaja.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #1e293b);"
                + "-fx-border-color: #f97316; -fx-border-width: 0 0 4 0;");

        Stage JFRPsplash = new Stage(StageStyle.UNDECORATED);
        JFRPsplash.getIcons().add(icono);
        JFRPsplash.setScene(new Scene(LJOMcaja, 460, 320));
        JFRPsplash.centerOnScreen();
        return JFRPsplash;
    }

    private void mostrarLogin() {
        stage.setScene(crearEscena(cargarVista("/view/login-view.fxml").getRoot(), 460, 640));
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private void mostrarPrincipal() {
        var AKACvista = cargarVista("/view/main-view.fxml");
        stage.setScene(crearEscena(AKACvista.getRoot(), 1320, 800));
        stage.setResizable(true);
        stage.setMinWidth(1142);
        stage.setMinHeight(742);
        stage.sizeToScene();
        stage.centerOnScreen();
        var GGFJcontrolador = (MainController) AKACvista.getController();
        Platform.runLater(GGFJcontrolador::ofrecerGuia);
    }

    private FXMLLoader cargarVista(String LJOMfxml) {
        try {
            FXMLLoader JFRPloader = new FXMLLoader(getClass().getResource(LJOMfxml));
            // LINEA CLAVE: Spring construye los controladores (tambien los de fx:include)
            JFRPloader.setControllerFactory(context::getBean);
            JFRPloader.load();
            return JFRPloader;
        } catch (IOException AKACe) {
            throw new IllegalStateException("No se pudo cargar la vista " + LJOMfxml, AKACe);
        }
    }

    private Scene crearEscena(Parent GGFJraiz, double LJOMancho, double JFRPalto) {
        Scene AKACescena = new Scene(GGFJraiz, LJOMancho, JFRPalto);
        AKACescena.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        return AKACescena;
    }

    @Override
    public void stop() {
        if (context != null) {
            context.publishEvent(new CierreAplicacion());
            context.close();
        }
        Platform.exit();
    }
}
