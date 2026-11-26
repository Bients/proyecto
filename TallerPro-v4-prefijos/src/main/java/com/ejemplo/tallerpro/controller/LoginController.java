package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * Pantalla de acceso. Si todavia no hay usuarios, se comporta como asistente de primer uso
 * y pide crear la cuenta de administrador (no existen contraseñas por defecto).
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final SesionActual sesion;
    private final Navegador navegador;

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblError;
    @FXML private VBox boxNombre;
    @FXML private VBox boxClave2;
    @FXML private TextField txtNombre;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwdClave;
    @FXML private PasswordField pwdClave2;
    @FXML private Button btnAccion;

    private boolean modoRegistro;

    @FXML
    public void initialize() {
        modoRegistro = usuarioService.contar() == 0;
        lblError.setText("");
        if (modoRegistro) {
            lblTitulo.setText("Bienvenido a TallerPro");
            lblSubtitulo.setText("Primer uso: crea la cuenta del administrador");
            pwdClave.setPromptText("Mínimo " + UsuarioService.CLAVE_MINIMA + " caracteres");
            btnAccion.setText("Crear cuenta y entrar");
        } else {
            lblTitulo.setText("Iniciar sesión");
            lblSubtitulo.setText("Ingresa con tu usuario y contraseña");
            btnAccion.setText("Entrar");
            Ui.ocultar(boxNombre);
            Ui.ocultar(boxClave2);
        }
    }

    @FXML
    private void onAccion() {
        lblError.setText("");
        String AKACclave = pwdClave.getText();
        if (modoRegistro && !AKACclave.equals(pwdClave2.getText())) {
            lblError.setText("Las contraseñas no coinciden.");
            return;
        }
        String GGFJnombre = txtNombre.getText();
        String LJOMlogin = txtUsuario.getText();
        Ui.ejecutar(btnAccion.getParent(), () -> modoRegistro
                ? usuarioService.registrarAdministrador(GGFJnombre, LJOMlogin, AKACclave)
                : usuarioService.autenticar(LJOMlogin, AKACclave).orElseThrow(
                        () -> new NegocioException("Usuario o contraseña incorrectos.")), this::entrar, JFRPerror -> {
            pwdClave.clear();
            lblError.setText(JFRPerror);
        });
    }

    private void entrar(Usuario AKACusuario) {
        sesion.iniciar(AKACusuario);
        navegador.mostrarPrincipal();
    }
}
