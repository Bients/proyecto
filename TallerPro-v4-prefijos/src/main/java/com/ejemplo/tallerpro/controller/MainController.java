package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.util.ComboBuscable;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.Preferencias;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Tema;
import com.ejemplo.tallerpro.util.Ui;
import com.ejemplo.tallerpro.util.VistaConBorrador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ventana principal con barra lateral. Cada boton muestra una vista y la recarga para que
 * siempre refleje lo último guardado en las demás.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final SesionActual sesion;
    private final Navegador navegador;

    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label tituloSeguimiento;
    @FXML private Label tituloAdministracion;
    @FXML private Node grupoInventario;
    @FXML private Button btnTema;

    @FXML private Button navTablero;
    @FXML private Button navOrdenes;
    @FXML private Button navHistorial;
    @FXML private Button navInspeccion;
    @FXML private Button navVehiculos;
    @FXML private Button navClientes;
    @FXML private Button navRepuestos;
    @FXML private Button navCompras;
    @FXML private Button navKardex;
    @FXML private Button navMantenimiento;
    @FXML private Button navDiagnostico;
    @FXML private Button navAjustes;
    @FXML private Button navUsuarios;

    @FXML private BorderPane raizPrincipal;
    @FXML private StackPane contenido;
    @FXML private GuiaController guiaController;
    private final Map<Button, FXMLLoader> vistas = new HashMap<>();
    private Button activo;

    @FXML
    public void initialize() {
        lblUsuario.setText(sesion.getUsuario().getNombre());
        lblRol.setText(sesion.getUsuario().getRol().toString());
        btnTema.setText(Preferencias.temaOscuro() ? "Modo claro" : "Modo oscuro");

        // El mecánico trabaja desde el tablero, sus órdenes y el historial.
        if (!sesion.esAdmin()) {
            navAjustes.setText("Mi contraseña");
            tituloSeguimiento.setText("SEGUIMIENTO");
            tituloAdministracion.setText("CUENTA");
            Ui.ocultar(grupoInventario);
            Ui.ocultar(navClientes);
            Ui.ocultar(navUsuarios);
            Ui.ocultar(navVehiculos);
            Ui.ocultar(navMantenimiento);
        }
        configurarGuia();
        raizPrincipal.addEventFilter(KeyEvent.KEY_PRESSED, this::enfocarBusqueda);
        irTablero();
    }

    private void enfocarBusqueda(KeyEvent AKACtecla) {
        if (!AKACtecla.isControlDown() || AKACtecla.getCode() != KeyCode.F) {
            return;
        }
        if (contenido.getChildren().isEmpty()) {
            return;
        }
        if (activo == navOrdenes && vistas.get(activo).getController() instanceof OrdenController GGFJordenes) {
            GGFJordenes.enfocarBusqueda();
            AKACtecla.consume();
            return;
        }
        Node LJOMvista = contenido.getChildren().get(0);
        Node JFRPcampo = LJOMvista.lookup("#txtBuscar");
        if (JFRPcampo instanceof TextField AKACtexto) {
            AKACtexto.requestFocus();
            AKACtexto.selectAll();
            AKACtecla.consume();
        } else if (JFRPcampo instanceof ComboBuscable<?> GGFJbuscador) {
            GGFJbuscador.enfocar();
            AKACtecla.consume();
        } else if (LJOMvista.lookup("#cmbVehiculo") instanceof ComboBuscable<?> LJOMbuscador) {
            LJOMbuscador.enfocar();
            AKACtecla.consume();
        }
    }

    public void ofrecerGuia() {
        var JFRPusuario = sesion.getUsuario();
        if (JFRPusuario != null && raizPrincipal.getScene() != null
                && raizPrincipal.getScene().getWindow().isShowing()) {
            guiaController.ofrecer(JFRPusuario.getUsuario());
        }
    }

    @FXML
    private void onGuia() {
        guiaController.iniciar(sesion.getUsuario().getUsuario());
    }

    private void configurarGuia() {
        guiaController.configurar(contenido, () -> activo);
        guiaController.agregarNavegacion(navTablero, "#btnNuevaOrden", "Recibir un vehículo",
                "Pulsa Nueva orden en el Tablero para abrir la ficha de ingreso.", "Abrir nueva orden", navOrdenes);
        guiaController.agregarPaso(navOrdenes, "#cmbVehiculo", "Buscar el vehículo",
                "Escribe la placa y elige un resultado. Si solo quieres recorrer la guía, pulsa Siguiente.");
        guiaController.agregarPaso(navOrdenes, "#txtProblema", "Anotar el motivo de ingreso",
                "Aquí se describe la falla reportada o el trabajo que solicita el cliente.");
        guiaController.agregarPaso(navOrdenes, "#btnGuardar", "Registrar el ingreso",
                "Guardar registra la orden en el taller. Pulsa Siguiente para seguir con la guía.");
        guiaController.agregarPaso(navOrdenes, "#cmbEstado", "Seguir el avance",
                "En una orden registrada, usa Estado para indicar en qué etapa está el trabajo.");
        if (sesion.esAdmin()) {
            guiaController.agregarPaso(navOrdenes, "#btnPagos", "Consultar el saldo y los abonos",
                    "En una orden guardada, abre Pagos para ver los abonos y registrar un cobro.");
        }
        guiaController.agregarNavegacion(navOrdenes, "#navInspeccion", "Abrir Inspección",
                "Pulsa Inspección en el menú para revisar los hallazgos de un vehículo.",
                "Abrir Inspección", navInspeccion);
        guiaController.agregarPaso(navInspeccion, "#txtBuscar", "Consultar hallazgos",
                "Busca al cliente, elige su vehículo y revisa las zonas marcadas en el diagrama.");
        guiaController.agregarNavegacion(navInspeccion, "#navHistorial", "Abrir el historial",
                "Pulsa Historial del vehículo para consultar las visitas anteriores.",
                "Abrir Historial", navHistorial);
        guiaController.agregarPaso(navHistorial, "#cmbVehiculo", "Revisar las visitas",
                "Busca por placa y selecciona una visita para ver los trabajos que se hicieron.");
    }

    @FXML private void irTablero() {
        mostrar(navTablero, "resumen", ResumenController.class, AKACresumen -> {
            AKACresumen.configurarAcciones(() -> abrirOrdenDesdeTablero(OrdenController::abrirNueva),
                    GGFJid -> abrirOrdenDesdeTablero(LJOMordenes -> LJOMordenes.abrirOrden(GGFJid)),
                    () -> mostrar(navVehiculos, "vehiculo", VehiculoController.class, JFRPvehiculos -> {
                        JFRPvehiculos.recargar();
                        JFRPvehiculos.abrirNuevo();
                    }));
            AKACresumen.recargar();
        });
    }

    private void abrirOrdenDesdeTablero(Consumer<OrdenController> AKACaccion) {
        mostrarOrdenes(GGFJordenes -> {
            GGFJordenes.recargar();
            AKACaccion.accept(GGFJordenes);
        });
    }

    private void mostrarOrdenes(Consumer<OrdenController> LJOMaccion) {
        mostrar(navOrdenes, "orden", OrdenController.class, JFRPordenes -> {
            JFRPordenes.configurarInspeccion(AKACid -> mostrar(navInspeccion, "inspeccion", InspeccionController.class,
                    GGFJinspeccion -> GGFJinspeccion.abrirOrden(AKACid)));
            LJOMaccion.accept(JFRPordenes);
        });
    }

    @FXML private void irOrdenes() {
        mostrarOrdenes(OrdenController::recargar);
    }

    @FXML private void irHistorial() { mostrar(navHistorial, "historial", HistorialController.class, HistorialController::recargar); }

    @FXML private void irInspeccion() { mostrar(navInspeccion, "inspeccion", InspeccionController.class, InspeccionController::recargar); }

    @FXML private void irVehiculos() { mostrar(navVehiculos, "vehiculo", VehiculoController.class, VehiculoController::recargar); }

    @FXML private void irClientes() { mostrar(navClientes, "cliente", ClienteController.class, ClienteController::recargar); }

    @FXML private void irRepuestos() {
        mostrar(navRepuestos, "repuesto", RepuestoController.class, LJOMrepuestos -> {
            LJOMrepuestos.configurarKardex(JFRPrepuesto -> mostrar(navKardex, "kardex", KardexController.class,
                    AKACkardex -> AKACkardex.abrir(JFRPrepuesto)));
            LJOMrepuestos.recargar();
        });
    }

    @FXML private void irCompras() { mostrar(navCompras, "compra", CompraController.class, CompraController::recargar); }

    @FXML private void irKardex() { mostrar(navKardex, "kardex", KardexController.class, KardexController::recargar); }

    @FXML private void irMantenimiento() {
        mostrar(navMantenimiento, "mantenimiento", MantenimientoController.class, MantenimientoController::recargar);
    }

    @FXML private void irDiagnostico() {
        mostrar(navDiagnostico, "diagnostico", DiagnosticoController.class, DiagnosticoController::recargar);
    }

    @FXML private void irAjustes() { mostrar(navAjustes, "ajustes", AjustesController.class, AjustesController::recargar); }

    @FXML private void irUsuarios() { mostrar(navUsuarios, "usuarios", UsuariosController.class, UsuariosController::recargar); }

    @FXML
    private void onTema() {
        boolean GGFJoscuro = Tema.alternar();
        btnTema.setText(GGFJoscuro ? "Modo claro" : "Modo oscuro");
    }

    @FXML
    private void onCerrarSesion() {
        if (Ui.confirmar("Cerrar sesión", "¿Quieres cerrar la sesión actual?")) {
            VistaConBorrador LJOMsalida = borradorActivo();
            autorizar(LJOMsalida, () -> {
                if (LJOMsalida != null) {
                    LJOMsalida.alSalir();
                }
                sesion.cerrar();
                navegador.mostrarLogin();
            });
        }
    }

    private VistaConBorrador borradorActivo() {
        if (activo != null && vistas.get(activo).getController() instanceof VistaConBorrador JFRPborrador) {
            return JFRPborrador;
        }
        return null;
    }

    private <C> void mostrar(Button AKACboton, String GGFJrecurso, Class<C> LJOMtipo, Consumer<C> JFRPalMostrar) {
        VistaConBorrador AKACsalida = activo == AKACboton ? null : borradorActivo();
        autorizar(AKACsalida, () -> abrir(AKACboton, GGFJrecurso, LJOMtipo, JFRPalMostrar, AKACsalida));
    }

    private void autorizar(VistaConBorrador GGFJsalida, Runnable LJOMcontinuar) {
        if (GGFJsalida == null) {
            LJOMcontinuar.run();
        } else {
            GGFJsalida.autorizarSalida(LJOMcontinuar);
        }
    }

    private <C> void abrir(Button JFRPboton, String AKACrecurso, Class<C> GGFJtipo, Consumer<C> LJOMalMostrar,
                           VistaConBorrador JFRPsalida) {
        FXMLLoader AKACdestino;
        try {
            AKACdestino = vistas.get(JFRPboton);
            if (AKACdestino == null) {
                AKACdestino = navegador.cargarVista(AKACrecurso);
            }
            LJOMalMostrar.accept(GGFJtipo.cast(AKACdestino.getController()));
        } catch (IOException | RuntimeException GGFJe) {
            log.error("No se pudo abrir la pantalla {}", AKACrecurso, GGFJe);
            Ui.alerta(Alert.AlertType.ERROR, "No se pudo abrir la pantalla",
                    "No se pudo abrir «" + JFRPboton.getText() + "». Puedes volver a intentarlo o abrir otra pantalla.");
            return;
        }
        if (JFRPsalida != null) {
            JFRPsalida.alSalir();
        }
        vistas.put(JFRPboton, AKACdestino);
        if (activo != JFRPboton) {
            contenido.getChildren().setAll((Node) AKACdestino.getRoot());
            if (activo != null) {
                activo.getStyleClass().remove("nav-activo");
            }
            JFRPboton.getStyleClass().add("nav-activo");
            activo = JFRPboton;
        }
        guiaController.pantallaMostrada(JFRPboton);
    }
}
