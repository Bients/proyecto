package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.service.ConsultaDniService;
import com.ejemplo.tallerpro.service.IaService;
import com.ejemplo.tallerpro.service.ReporteService;
import com.ejemplo.tallerpro.service.RespaldoService;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Ajustes: clave de la IA, contraseña propia, copias de seguridad y exportacion a Excel. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class AjustesController {

    private final UsuarioService usuarioService;
    private final RespaldoService respaldoService;
    private final ReporteService reporteService;
    private final IaService iaService;
    private final ConsultaDniService consultaDni;
    private final SesionActual sesion;
    private final ObjectProvider<BuildProperties> compilacion;

    @FXML private PasswordField pwdApiKey;
    @FXML private Label lblApiKey;
    @FXML private Button btnProbar;
    @FXML private PasswordField pwdActual;
    @FXML private PasswordField pwdNueva;
    @FXML private PasswordField pwdNueva2;
    @FXML private VBox boxIa;
    @FXML private VBox boxDni;
    @FXML private PasswordField pwdDniToken;
    @FXML private Label lblDniToken;
    @FXML private VBox boxRespaldo;
    @FXML private VBox boxExcel;
    @FXML private Label lblRespaldo;
    @FXML private Label lblEstado;
    @FXML private Label lblTitulo;
    @FXML private Label lblDescripcion;
    @FXML private Label lblVersion;

    @FXML
    public void initialize() {
        BuildProperties GGFJinformacion = compilacion.getIfAvailable();
        if (GGFJinformacion == null) {
            Ui.ocultar(lblVersion);
        } else {
            lblVersion.setText("TallerPro " + GGFJinformacion.getVersion());
        }
        // Copias de seguridad y Excel son solo para el administrador
        if (!sesion.esAdmin()) {
            lblTitulo.setText("Mi contraseña");
            lblDescripcion.setText("Cambia la contraseña de tu cuenta.");
            Ui.ocultar(boxIa);
            Ui.ocultar(boxDni);
            Ui.ocultar(boxRespaldo);
            Ui.ocultar(boxExcel);
        } else {
            lblRespaldo.setText("Copias automáticas (últimas 7) en: " + respaldoService.directorioCopias());
        }
        recargar();
    }

    public void recargar() {
        if (sesion.esAdmin()) {
            lblApiKey.setText(iaService.estadoCredencial());
            lblDniToken.setText(consultaDni.consultaExternaActiva()
                    ? "Consulta automática activada."
                    : "Consulta externa desactivada. Puedes activarla con un token de Decolecta.");
        }
        lblEstado.setText("Listo.");
    }

    @FXML
    private void onGuardarDniToken() {
        try {
            consultaDni.guardarToken(pwdDniToken.getText());
            pwdDniToken.clear();
            recargar();
            lblEstado.setText("Consulta automática de DNI activada.");
        } catch (NegocioException LJOMe) {
            lblEstado.setText(LJOMe.getMessage());
        }
    }

    @FXML
    private void onQuitarDniToken() {
        try {
            consultaDni.quitarToken();
            pwdDniToken.clear();
            recargar();
            lblEstado.setText("Consulta externa de DNI desactivada.");
        } catch (NegocioException JFRPe) {
            lblEstado.setText(JFRPe.getMessage());
        }
    }

    @FXML
    private void onGuardarApiKey() {
        try {
            iaService.guardarCredencial(pwdApiKey.getText());
            pwdApiKey.clear();
            recargar();
            lblEstado.setText("Clave guardada. Pulsa «Probar conexión» para comprobarla.");
        } catch (NegocioException AKACe) {
            lblEstado.setText(AKACe.getMessage());
        }
    }

    @FXML
    private void onProbarConexion() {
        Task<Void> GGFJtarea = new Task<>() {
            @Override
            protected Void call() {
                iaService.probarConexion();
                return null;
            }
        };
        GGFJtarea.setOnSucceeded(LJOMe -> {
            btnProbar.setDisable(false);
            lblEstado.setText("Conexión correcta: la IA funciona.");
            Ui.alerta(Alert.AlertType.INFORMATION, "Conexión correcta", "La clave es válida y la IA respondió.");
        });
        GGFJtarea.setOnFailed(JFRPe -> {
            btnProbar.setDisable(false);
            lblEstado.setText("La conexión con la IA falló.");
            Throwable AKACerror = GGFJtarea.getException();
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo conectar",
                    AKACerror instanceof NegocioException ? AKACerror.getMessage() : "Ocurrió un error al comprobar la conexión.");
        });
        btnProbar.setDisable(true);
        lblEstado.setText("Probando la conexión…");
        Thread GGFJhilo = new Thread(GGFJtarea, "probar-ia");
        GGFJhilo.setDaemon(true);
        GGFJhilo.start();
    }

    @FXML
    private void onQuitarApiKey() {
        try {
            iaService.quitarCredencial();
        } catch (NegocioException LJOMe) {
            lblEstado.setText(LJOMe.getMessage());
            return;
        }
        pwdApiKey.clear();
        recargar();
        lblEstado.setText("Clave de API eliminada.");
    }

    @FXML
    private void onCambiarClave() {
        if (!pwdNueva.getText().equals(pwdNueva2.getText())) {
            Ui.alerta(Alert.AlertType.WARNING, "Revisa los datos", "La nueva contraseña y su confirmación no coinciden.");
            return;
        }
        try {
            usuarioService.cambiarClave(sesion.getUsuario().getId(), pwdActual.getText(), pwdNueva.getText());
            pwdActual.clear();
            pwdNueva.clear();
            pwdNueva2.clear();
            lblEstado.setText("Contraseña actualizada.");
        } catch (NegocioException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo cambiar", JFRPe.getMessage());
        }
    }

    @FXML
    private void onCrearCopia() {
        String AKACnombre = "TallerPro-respaldo-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".zip";
        File GGFJarchivo = Ui.guardarArchivo(lblEstado.getScene().getWindow(), "Guardar base y fotos",
                AKACnombre, "Respaldo de TallerPro", "*.zip");
        if (GGFJarchivo != null) {
            Ui.ejecutar(boxRespaldo, () -> {
                try {
                    respaldoService.crearCopia(GGFJarchivo.toPath());
                    return GGFJarchivo;
                } catch (SQLException | IOException LJOMe) {
                    throw new NegocioException("No se pudo crear el respaldo: " + LJOMe.getMessage());
                }
            }, JFRPguardado -> lblEstado.setText("Copia guardada: " + JFRPguardado.getName()),
                    AKACerror -> Ui.alerta(Alert.AlertType.ERROR, "No se pudo crear la copia", AKACerror));
        }
    }

    @FXML
    private void onRestaurar() {
        FileChooser GGFJselector = new FileChooser();
        GGFJselector.setTitle("Elegir copia de seguridad");
        GGFJselector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo de TallerPro", "*.zip", "*.db"));
        File LJOMarchivo = GGFJselector.showOpenDialog(lblEstado.getScene().getWindow());
        if (LJOMarchivo == null || !Ui.confirmar("Restaurar copia",
                "Al reiniciar se restaurarán la base y las fotos de la copia. Durante el arranque se respaldarán los datos actuales.")) {
            return;
        }
        Ui.ejecutar(boxRespaldo, () -> {
            try {
                respaldoService.programarRestauracion(LJOMarchivo.toPath());
                return LJOMarchivo;
            } catch (SQLException | IOException JFRPe) {
                throw new NegocioException("No se pudo validar el respaldo: " + JFRPe.getMessage());
            }
        }, AKACguardado -> {
            lblEstado.setText("Restauración programada para el próximo arranque.");
            Ui.alerta(Alert.AlertType.INFORMATION, "Restauración programada",
                    "Cierra TallerPro y vuélvelo a abrir para aplicar la copia.");
        }, GGFJerror -> Ui.alerta(Alert.AlertType.ERROR, "No se pudo programar la restauración", GGFJerror));
    }

    @FXML
    private void onExportarExcel() {
        String LJOMnombre = "TallerPro-datos-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        File JFRParchivo = Ui.guardarArchivo(lblEstado.getScene().getWindow(), "Exportar a Excel", LJOMnombre,
                "Libro de Excel", "*.xlsx");
        if (JFRParchivo == null) {
            return;
        }
        Ui.ejecutar(boxExcel, () -> {
            try {
                reporteService.exportarExcel(JFRParchivo.toPath());
                return JFRParchivo;
            } catch (IOException AKACe) {
                throw new NegocioException("No se pudo exportar el archivo Excel.");
            }
        }, GGFJguardado -> lblEstado.setText("Datos exportados: " + GGFJguardado.getName()), lblEstado::setText);
    }
}
