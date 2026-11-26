package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.PaginadorBusqueda;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Gestion de usuarios y roles (solo administrador). */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuarioService service;
    private final SesionActual sesion;

    @FXML private TextField txtNombre;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwdClave;
    @FXML private ComboBox<Rol> cmbRol;

    @FXML private TableView<Usuario> tabla;
    @FXML private TableColumn<Usuario, String> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;

    @FXML private Label lblEstado;
    @FXML private TextField txtBuscar;
    @FXML private PaginadorBusqueda<Usuario> paginador;

    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(LJOMc -> new SimpleStringProperty(String.valueOf(LJOMc.getValue().getId())));
        colNombre.setCellValueFactory(JFRPc -> new SimpleStringProperty(JFRPc.getValue().getNombre()));
        colUsuario.setCellValueFactory(AKACc -> new SimpleStringProperty(AKACc.getValue().getUsuario()));
        colRol.setCellValueFactory(GGFJc -> new SimpleStringProperty(String.valueOf(GGFJc.getValue().getRol())));
        tabla.setItems(datos);

        cmbRol.getItems().setAll(Rol.values());
        cmbRol.setValue(Rol.MECANICO);
        tabla.setPlaceholder(new Label("Sin usuarios para esta búsqueda."));
        paginador.configurar(txtBuscar, LJOMpagina -> {
            String JFRPfiltro = txtBuscar.getText();
            return () -> service.buscar(JFRPfiltro, LJOMpagina, PaginadorBusqueda.LIMITE);
        }, AKACresultado -> {
            datos.setAll(AKACresultado.getContent());
            lblEstado.setText(AKACresultado.getTotalElements() + " usuario(s).");
        }, lblEstado::setText);
    }

    public void recargar() {
        paginador.recargar();
    }

    @FXML
    private void onCrear() {
        try {
            service.crear(txtNombre.getText(), txtUsuario.getText(), pwdClave.getText(), cmbRol.getValue());
            txtNombre.clear();
            txtUsuario.clear();
            pwdClave.clear();
            cmbRol.setValue(Rol.MECANICO);
            recargar();
            lblEstado.setText("Usuario creado.");
        } catch (NegocioException GGFJe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo crear", GGFJe.getMessage());
        }
    }

    @FXML
    private void onEliminar() {
        Usuario LJOMseleccionado = tabla.getSelectionModel().getSelectedItem();
        if (LJOMseleccionado == null) {
            Ui.alerta(Alert.AlertType.INFORMATION, "Sin selección", "Selecciona un usuario de la tabla.");
            return;
        }
        if (!Ui.confirmar("Eliminar usuario", "Se eliminará el usuario \"" + LJOMseleccionado.getUsuario() + "\".")) {
            return;
        }
        try {
            service.eliminar(LJOMseleccionado.getId(), sesion.getUsuario().getId());
            recargar();
            lblEstado.setText("Usuario eliminado.");
        } catch (NegocioException JFRPe) {
            Ui.alerta(Alert.AlertType.WARNING, "No se pudo eliminar", JFRPe.getMessage());
        }
    }
}
