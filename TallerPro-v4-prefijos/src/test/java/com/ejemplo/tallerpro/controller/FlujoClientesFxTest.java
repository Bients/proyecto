package com.ejemplo.tallerpro.controller;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.repository.OrdenRepository.OrdenTablero;
import com.ejemplo.tallerpro.service.ClienteService;
import com.ejemplo.tallerpro.service.ConsultaDniService;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.service.IaService;
import com.ejemplo.tallerpro.service.DiagnosticoLocalService;
import com.ejemplo.tallerpro.service.MantenimientoService;
import com.ejemplo.tallerpro.service.HallazgoService;
import com.ejemplo.tallerpro.service.OrdenFotoService;
import com.ejemplo.tallerpro.service.PdfService;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.PagoService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.service.ServidorIaPrueba;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.util.Navegador;
import com.ejemplo.tallerpro.util.Preferencias;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.HostServicesHolder;
import com.ejemplo.tallerpro.util.Tema;
import com.ejemplo.tallerpro.util.Ui;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlujoClientesFxTest {
    private final Cliente ana = cliente(1L, "Ana", "00012345");
    private final Cliente beatriz = cliente(2L, "Beatriz", "00012346");
    private final Usuario admin = new Usuario();
    private final SesionActual sesion = new SesionActual();
    private ClienteService servicio;
    private ConsultaDniService dni;
    private Navegador navegador;
    private Stage ventana;
    private IaService ia;
    private OrdenService ordenesGuia;

    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        System.setProperty("java.util.prefs.PreferencesFactory", PreferenciasDePrueba.class.getName());
        var GGFJlista = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                Tema.aplicar();
                GGFJlista.countDown();
            });
        } catch (UnsupportedOperationException LJOMe) {
            assumeTrue(!String.valueOf(LJOMe.getMessage()).contains("DISPLAY"), "JavaFX requiere una pantalla disponible");
            throw LJOMe;
        }
        assertThat(GGFJlista.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @BeforeEach
    void abrirClientes() throws Exception {
        admin.setNombre("Administradora de prueba");
        admin.setRol(Rol.ADMIN);
        sesion.iniciar(admin);
        servicio = mock(ClienteService.class);
        when(servicio.buscar(anyString(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(ana, beatriz)));
        when(servicio.guardar(any())).thenAnswer(JFRPllamada -> JFRPllamada.getArgument(0));
        dni = mock(ConsultaDniService.class);
        var AKACvehiculos = mock(VehiculoService.class);
        when(AKACvehiculos.buscar(anyString(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(
                Vehiculo.builder().id(11L).placa("QA1235").marca("Kia").modelo("Rio").anio(2020)
                        .kilometraje(1000).cliente(ana).build(),
                Vehiculo.builder().id(12L).placa("QB1236").marca("Toyota").modelo("Yaris").anio(2021)
                        .kilometraje(2000).cliente(beatriz).build())));
        navegador = spy(new Navegador());
        fx(() -> {
            ordenesGuia = mock(OrdenService.class);
            when(ordenesGuia.ordenesTablero(any(), anyBoolean(), anyString(), anyInt(), anyInt()))
                    .thenReturn(Page.empty());
            when(ordenesGuia.buscar(any(OrdenService.Filtros.class), anyInt(), anyInt())).thenReturn(Page.empty());
            var GGFJpagos = mock(PagoService.class);
            var LJOMhallazgos = mock(HallazgoService.class);
            var JFRPfotos = mock(OrdenFotoService.class);
            var AKACrepuestos = mock(RepuestoService.class);
            var GGFJusuarios = mock(UsuarioService.class);
            navegador.configurar(() -> {}, () -> {}, LJOMclase -> {
                if (LJOMclase == MainController.class) {
                    return new MainController(sesion, navegador);
                }
                if (LJOMclase == ResumenController.class) {
                    return new ResumenController(ordenesGuia, sesion, GGFJpagos, AKACvehiculos, AKACrepuestos,
                            mock(MantenimientoService.class));
                }
                if (LJOMclase == OrdenController.class) {
                    return new OrdenController(ordenesGuia, GGFJpagos, LJOMhallazgos, AKACvehiculos, AKACrepuestos,
                            mock(PdfService.class), JFRPfotos, mock(HostServicesHolder.class), sesion, GGFJusuarios, navegador);
                }
                if (LJOMclase == OrdenFotosController.class) {
                    return new OrdenFotosController(JFRPfotos);
                }
                if (LJOMclase == InspeccionController.class) {
                    return new InspeccionController(LJOMhallazgos, ordenesGuia, JFRPfotos, AKACrepuestos, sesion);
                }
                if (LJOMclase == HistorialController.class) {
                    return new HistorialController(ordenesGuia, sesion, AKACvehiculos, GGFJusuarios);
                }
                if (LJOMclase == ClienteController.class) {
                    return new ClienteController(servicio, dni);
                }
                if (LJOMclase == VehiculoController.class) {
                    return new VehiculoController(AKACvehiculos, servicio, navegador, sesion);
                }
                if (LJOMclase == DiagnosticoController.class) {
                    return new DiagnosticoController(ia, new DiagnosticoLocalService(), AKACvehiculos, sesion);
                }
                if (LJOMclase == GuiaController.class) {
                    return new GuiaController();
                }
                throw new AssertionError("Controlador inesperado: " + LJOMclase);
            });
            ventana = new Stage();
            ventana.setScene(new Scene(navegador.cargarVista("main").getRoot(), 1160, 740));
            ventana.show();
            boton("navClientes").fire();
            return null;
        });
        esperar(() -> tabla().getItems().size() == 2);
    }

    @Test
    void ofreceLaGuiaEnLaVentanaPrincipalUnaVezPorUsuario() throws Exception {
        admin.setUsuario("guia-junit");
        fx(() -> {
            var JFRPvista = navegador.cargarVista("main");
            ventana.setScene(new Scene(JFRPvista.getRoot(), 1126, 703));
            var AKACprincipal = (MainController) JFRPvista.getController();
            assertThat(barraGuia().isManaged()).isFalse();
            AKACprincipal.ofrecerGuia();
            assertThat(barraGuia().isVisible()).isTrue();
            assertThat(leyendaGuia("lblPasoGuia")).isEqualTo("¿Ya has usado TallerPro?");
            assertThat(Window.getWindows().stream().filter(Window::isShowing).toList()).containsExactly(ventana);
            boton("btnConozcoGuia").fire();
            AKACprincipal.ofrecerGuia();
            assertThat(barraGuia().isManaged()).isFalse();
            assertThat(Preferencias.guiaPresentada(admin.getUsuario())).isTrue();
            assertThat(Preferencias.guiaPresentada("guia-otra-cuenta")).isFalse();
            boton("btnGuia").fire();
            assertThat(leyendaGuia("lblPasoGuia")).contains("Recibir un vehículo");
            return null;
        });
    }

    @Test
    void laGuiaResaltaLosControlesRealesYAvanzaConLaNavegacionDelTaller() throws Exception {
        fx(() -> {
            boton("navTablero").fire();
            var GGFJnueva = boton("btnNuevaOrden");
            boton("btnGuia").fire();
            assertThat(GGFJnueva.getStyleClass()).contains("guia-destacado");
            GGFJnueva.fire();
            assertThat(GGFJnueva.getStyleClass()).doesNotContain("guia-destacado");
            assertThat(boton("navOrdenes").getStyleClass()).contains("nav-activo");
            comprobarMarca("cmbVehiculo");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("txtProblema");
            boton("btnAtrasGuia").fire();
            comprobarMarca("cmbVehiculo");
            boton("btnSiguienteGuia").fire();
            boton("btnSiguienteGuia").fire();
            comprobarMarca("btnGuardar");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("cmbEstado");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("btnPagos");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("navInspeccion");
            boton("navInspeccion").fire();
            comprobarMarca("txtBuscar");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("navHistorial");
            boton("btnSiguienteGuia").fire();
            comprobarMarca("cmbVehiculo");
            assertThat(boton("navHistorial").getStyleClass()).contains("nav-activo");
            assertThat(boton("btnSiguienteGuia").getText()).isEqualTo("Terminar");
            boton("btnSiguienteGuia").fire();
            assertThat(barraGuia().isManaged()).isFalse();
            assertThat(ventana.getScene().lookup(".guia-destacado")).isNull();
            assertThat(boton("navHistorial").getStyleClass()).contains("nav-activo");
            return null;
        });
        verify(ordenesGuia, never()).guardar(any());
    }

    @Test
    void salirDeLaGuiaConservaElBorradorYElRegresoRespetaLaConfirmacion() throws Exception {
        fx(() -> {
            boton("btnGuia").fire();
            boton("navTablero").fire();
            comprobarMarca("btnNuevaOrden");
            boton("navClientes").fire();
            boton("btnNuevo").fire();
            texto("txtNombre").setText("Borrador que debe conservarse");
            assertThat(leyendaGuia("lblMensajeGuia")).contains("vuelve a Tablero");
            return null;
        });
        pulsarConDialogo("btnSiguienteGuia", "Seguir editando");
        fx(() -> {
            assertThat(boton("navClientes").getStyleClass()).contains("nav-activo");
            assertThat(ventana.getScene().lookup(".guia-destacado")).isNull();
            boton("btnSalirGuia").fire();
            assertThat(barraGuia().isManaged()).isFalse();
            assertThat(texto("txtNombre").getText()).isEqualTo("Borrador que debe conservarse");
            return null;
        });
        verify(servicio, org.mockito.Mockito.never()).guardar(any());
    }

    private void comprobarMarca(String LJOMid) {
        assertThat(ventana.getScene().lookup("#" + LJOMid).getStyleClass()).contains("guia-destacado");
    }

    private VBox barraGuia() {
        return (VBox) ventana.getScene().lookup("#barraGuia");
    }

    private String leyendaGuia(String JFRPid) {
        return ((Label) ventana.getScene().lookup("#" + JFRPid)).getText();
    }

    @AfterEach
    void cerrarVentana() throws Exception {
        fx(() -> {
            if (ventana != null) {
                ventana.close();
            }
            return null;
        });
    }

    @Test
    void primerArranqueMuestraElMinimoDeLaClaveDefinidoPorElServicio() throws Exception {
        var AKACusuarios = mock(UsuarioService.class);
        var GGFJacceso = new Navegador();
        fx(() -> {
            GGFJacceso.configurar(() -> {}, () -> {}, LJOMclase -> new LoginController(AKACusuarios, sesion, GGFJacceso));
            ventana.setScene(new Scene(GGFJacceso.cargarVista("login").getRoot()));
            assertThat(((Label) ventana.getScene().lookup("#lblSubtitulo")).getText())
                    .contains("crea la cuenta del administrador");
            assertThat(texto("pwdClave").getPromptText())
                    .isEqualTo("Mínimo " + UsuarioService.CLAVE_MINIMA + " caracteres");
            return null;
        });
    }

    @Test
    void diagnosticoConectadoMantieneLaInterfazActivaMientrasEspera() throws Exception {
        try (var JFRPservidor = new ServidorIaPrueba(200, "Respuesta conectada de prueba.", true)) {
            ia = JFRPservidor.servicio(sesion, () -> ServidorIaPrueba.CLAVE);
            consultarDiagnostico();
            assertThat(JFRPservidor.recibida()).isTrue();
            assertThat(fx(() -> boton("btnConsultar").isDisabled())).isTrue();
            assertThat(fx(() -> boton("btnNuevaConsulta").isDisabled())).isTrue();
            assertThat(fx(() -> ((Label) ventana.getScene().lookup("#lblEstado")).getText())).contains("Consultando");
            JFRPservidor.responder();
            esperar(() -> respuestaIa().contains("Respuesta conectada de prueba."));
            assertThat(fx(this::respuestaIa)).contains("TALLERPRO IA").doesNotContain("MODO BÁSICO");
            assertThat(fx(() -> boton("btnConsultar").isDisabled())).isFalse();
        }
    }

    @Test
    void diagnosticoCaeAlModoLocalTrasPerderLaConexion() throws Exception {
        try (var AKACservidor = new ServidorIaPrueba(200, "Respuesta no disponible", false)) {
            ia = AKACservidor.servicio(sesion, () -> ServidorIaPrueba.CLAVE);
            AKACservidor.close();
            consultarDiagnostico();
            esperar(() -> respuestaIa().contains("MODO BÁSICO"));
            assertThat(fx(this::respuestaIa)).contains("No se pudo conectar").doesNotContain(ServidorIaPrueba.CLAVE);
            assertThat(fx(() -> boton("btnNuevaConsulta").isDisabled())).isFalse();
        }
    }

    @Test
    void diagnosticoSinClaveOrientaAlMecanicoYRespondeLocalmente() throws Exception {
        try (var GGFJservidor = new ServidorIaPrueba(200, "Respuesta no solicitada", false)) {
            ia = GGFJservidor.servicio(sesion, () -> "");
            fx(() -> {
                admin.setRol(Rol.MECANICO);
                ventana.setScene(new Scene(navegador.cargarVista("main").getRoot(), 1140, 710));
                assertThat(boton("navAjustes").getText()).isEqualTo("Mi contraseña");
                assertThat(((Label) ventana.getScene().lookup("#tituloSeguimiento")).getText()).isEqualTo("SEGUIMIENTO");
                assertThat(((Label) ventana.getScene().lookup("#tituloAdministracion")).getText()).isEqualTo("CUENTA");
                assertThat(ventana.getScene().lookup("#grupoInventario").isVisible()).isFalse();
                boton("navDiagnostico").fire();
                assertThat(((Label) ventana.getScene().lookup("#lblEstado")).getText())
                        .contains("administrador").doesNotContain("Ajustes");
                return null;
            });
            consultarDiagnostico();
            esperar(() -> respuestaIa().contains("MODO BÁSICO"));
            assertThat(fx(this::respuestaIa)).contains("no tiene una clave");
            assertThat(GGFJservidor.consultas()).isZero();
        }
    }

    @Test
    void elMenuDeAdminSigueElOrdenDelTrabajo() throws Exception {
        fx(() -> {
            var LJOMmenu = (VBox) boton("navTablero").getParent();
            assertThat(LJOMmenu.getChildren().stream().filter(JFRPn -> JFRPn instanceof Button)
                    .map(AKACn -> ((Button) AKACn).getText()).limit(10).toList())
                    .containsExactly("Tablero", "Órdenes de trabajo", "Inspección", "Diagnóstico",
                            "Clientes", "Vehículos", "Historial del vehículo", "Mantenimiento",
                            "Ajustes y respaldos", "Usuarios");
            var GGFJinventario = (VBox) ventana.getScene().lookup("#grupoInventario");
            assertThat(GGFJinventario.isVisible()).isTrue();
            assertThat(GGFJinventario.getChildren().stream().filter(LJOMn -> LJOMn instanceof Button)
                    .map(JFRPn -> ((Button) JFRPn).getText()).toList()).containsExactly("Repuestos", "Compras", "Kardex");
            return null;
        });
    }

    @Test
    void tableroVacioOrientaYAbreUnaOrdenConSuBoton() throws Exception {
        var AKACordenes = mock(OrdenService.class);
        var GGFJvehiculos = mock(VehiculoService.class);
        var LJOMfila = mock(OrdenTablero.class);
        when(LJOMfila.getEstado()).thenReturn("LISTO");
        when(LJOMfila.getNumero()).thenReturn("OT-0042");
        when(LJOMfila.getPlaca()).thenReturn("QA1235");
        when(LJOMfila.getDias()).thenReturn(0);
        when(LJOMfila.getId()).thenReturn(42L);
        when(AKACordenes.ordenesTablero(any(LocalDate.class), eq(false), anyString(), anyInt(), anyInt()))
                .thenReturn(Page.empty());
        when(AKACordenes.ordenesTablero(any(LocalDate.class), eq(true), eq("qa-1235"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(LJOMfila)));
        when(AKACordenes.ordenesTablero(any(LocalDate.class), eq(true), eq(""), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(LJOMfila)));
        var JFRPalta = new AtomicBoolean();
        var AKACabierta = new AtomicReference<Long>();
        var GGFJtablero = new ResumenController(AKACordenes, sesion, mock(PagoService.class), GGFJvehiculos,
                mock(RepuestoService.class), mock(MantenimientoService.class));
        fx(() -> {
            var LJOMvista = new Navegador();
            LJOMvista.configurar(() -> {}, () -> {}, JFRPclase -> GGFJtablero);
            ventana.setScene(new Scene(LJOMvista.cargarVista("resumen").getRoot(), 900, 700));
            GGFJtablero.configurarAcciones(() -> {}, AKACabierta::set, () -> JFRPalta.set(true));
            GGFJtablero.recargar();
            return null;
        });
        esperar(() -> boton("btnRegistrarVehiculo").isVisible());
        fx(() -> {
            assertThat(tabla().isManaged()).isFalse();
            assertThat(((Label) ventana.getScene().lookup("#lblEstado")).getText()).contains("Registra un vehículo");
            boton("btnRegistrarVehiculo").fire();
            ((RadioButton) ventana.getScene().lookup("#rbIngresosHoy")).fire();
            texto("txtBuscar").setText("qa-1235");
            return null;
        });
        esperar(() -> tabla().isVisible());
        fx(() -> {
            ventana.getScene().getRoot().applyCss();
            ventana.getScene().getRoot().layout();
            tabla().lookupAll(".button").stream().filter(AKACn -> "Abrir orden OT-0042".equals(AKACn.getAccessibleText()))
                    .map(Button.class::cast).findFirst().orElseThrow().fire();
            return null;
        });
        assertThat(JFRPalta.get()).isTrue();
        assertThat(AKACabierta.get()).isEqualTo(42L);
    }

    @Test
    void whatsappSinTelefonoMuestraAvisoYNoAbreUnEnlace() throws Exception {
        HostServicesHolder GGFJhost = mock(HostServicesHolder.class);
        var LJOMcerrada = fx(() -> {
            var JFRPrespuesta = responder("No se pudo preparar WhatsApp", ButtonType.OK);
            Ui.abrirWhatsApp(GGFJhost, "", "Aviso de orden");
            return JFRPrespuesta;
        });
        LJOMcerrada.get(5, TimeUnit.SECONDS);
        verifyNoInteractions(GGFJhost);
    }

    private void consultarDiagnostico() throws Exception {
        fx(() -> {
            boton("navDiagnostico").fire();
            ((TextArea) ventana.getScene().lookup("#txtSintomas")).setText("El auto hace un ruido al frenar.");
            boton("btnConsultar").fire();
            return null;
        });
    }

    private String respuestaIa() {
        return ((TextArea) ventana.getScene().lookup("#txtRespuesta")).getText();
    }

    @Test
    void vehiculosMuestraPrimeroLaListaYProtegeLaFichaAlCambiarDeFilaOSalir() throws Exception {
        fx(() -> {
            boton("navVehiculos").fire();
            return null;
        });
        esperar(() -> tabla().getItems().size() == 2);
        assertThat(fx(() -> ventana.getScene().lookup("#ficha").isVisible())).isFalse();
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
        fx(() -> {
            tabla().getSelectionModel().select(0);
            return null;
        });
        esperar(() -> texto("txtPlaca").getText().equals("QA1235"));
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isFalse();

        var AKACcancelar = fx(() -> {
            texto("txtMarca").setText("Marca en borrador");
            tabla().getSelectionModel().select(1);
            return responder("Seguir editando");
        });
        AKACcancelar.get(5, TimeUnit.SECONDS);
        assertThat(fx(() -> tabla().getSelectionModel().getSelectedIndex())).isZero();
        assertThat(fx(() -> texto("txtMarca").getText())).isEqualTo("Marca en borrador");
        pulsarConDialogo("navTablero", "Seguir editando");
        assertThat(fx(() -> texto("txtPlaca").getText())).isEqualTo("QA1235");
        pulsarConDialogo("btnNuevo", "Descartar");
        assertThat(fx(() -> texto("txtPlaca").getText())).isEmpty();
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
        assertThat(fx(() -> ventana.getScene().lookup("#txtId"))).isNull();
    }

    @Test
    void cambiarFilaPermiteSeguirEditandoODescartar() throws Exception {
        seleccionar(ana);
        var GGFJcancelar = fx(() -> {
            texto("txtNombre").setText("Nombre editado");
            tabla().getSelectionModel().select(beatriz);
            return responder("Seguir editando");
        });
        GGFJcancelar.get(5, TimeUnit.SECONDS);
        assertThat(fx(() -> tabla().getSelectionModel().getSelectedItem())).isSameAs(ana);
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo("Nombre editado");

        var LJOMdescartar = fx(() -> {
            tabla().getSelectionModel().select(beatriz);
            return responder("Descartar");
        });
        LJOMdescartar.get(5, TimeUnit.SECONDS);
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo("Beatriz");
    }

    @Test
    void nuevoGuardaAntesDeLimpiarYSoloPreguntaPorCambiosReales() throws Exception {
        seleccionar(ana);
        var JFRPguardar = fx(() -> {
            texto("txtNombre").setText("Nombre guardado");
            var AKACrespuesta = responder("Guardar");
            boton("btnNuevo").fire();
            return AKACrespuesta;
        });
        JFRPguardar.get(5, TimeUnit.SECONDS);
        var GGFJdatos = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(servicio).guardar(GGFJdatos.capture());
        assertThat(GGFJdatos.getValue().getNombre()).isEqualTo("Nombre guardado");
        assertThat(fx(() -> texto("txtNombre").getText())).isEmpty();

        seleccionar(ana);
        fx(() -> {
            texto("txtNombre").setText(" Ana ");
            boton("btnNuevo").fire();
            return null;
        });
        assertThat(fx(() -> texto("txtNombre").getText())).isEmpty();
    }

    @Test
    void guardarAlCambiarDeFilaAbreElClienteElegido() throws Exception {
        seleccionar(ana);
        var LJOMguardar = fx(() -> {
            texto("txtNombre").setText("Ana actualizada");
            tabla().getSelectionModel().select(beatriz);
            return responder("Guardar");
        });
        LJOMguardar.get(5, TimeUnit.SECONDS);
        esperar(() -> texto("txtNombre").getText().equals("Beatriz"));
        verify(servicio).guardar(org.mockito.ArgumentMatchers.argThat(JFRPc -> JFRPc.getId().equals(ana.getId())
                && JFRPc.getNombre().equals("Ana actualizada")));
        assertThat(fx(() -> tabla().getSelectionModel().getSelectedItem())).isSameAs(beatriz);
    }

    @Test
    void navegarRespetaLaDecisionYDescartaElBorradorAlSalir() throws Exception {
        fx(() -> {
            boton("btnNuevo").fire();
            texto("txtNombre").setText("Alta pendiente");
            return null;
        });
        pulsarConDialogo("navTablero", "Seguir editando");
        assertThat(fx(() -> boton("navClientes").getStyleClass().contains("nav-activo"))).isTrue();
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo("Alta pendiente");

        pulsarConDialogo("navTablero", "Descartar");
        assertThat(fx(() -> boton("navTablero").getStyleClass().contains("nav-activo"))).isTrue();
        fx(() -> {
            boton("navClientes").fire();
            return null;
        });
        assertThat(fx(() -> texto("txtNombre").getText())).isEmpty();
        assertThat(fx(this::fichaVisible)).isFalse();
    }

    @Test
    void eliminarSoloSeHabilitaConUnaFichaGuardada() throws Exception {
        assertThat(fx(this::fichaVisible)).isFalse();
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
        seleccionar(ana);
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isFalse();
        fx(() -> {
            boton("btnNuevo").fire();
            return null;
        });
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
    }

    @Test
    void cerrarFichaProtegeElBorradorYPermiteReabrirLaMismaFila() throws Exception {
        assertThat(fx(() -> ventana.getScene().lookup("#ficha").isManaged())).isFalse();
        assertThat(fx(() -> ventana.getScene().lookup("#txtId"))).isNull();
        when(servicio.buscar("Ana", 0, 15)).thenReturn(new PageImpl<>(List.of(ana)));
        fx(() -> {
            texto("txtBuscar").setText("Ana");
            return null;
        });
        esperar(() -> tabla().getItems().size() == 1);
        seleccionar(ana);
        fx(() -> {
            texto("txtNombre").setText("Borrador de Ana");
            return null;
        });
        pulsarConDialogo("btnCerrar", "Seguir editando");
        assertThat(fx(this::fichaVisible)).isTrue();
        pulsarConDialogo("btnCerrar", "Descartar");
        assertThat(fx(this::fichaVisible)).isFalse();
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
        assertThat(fx(() -> texto("txtBuscar").getText())).isEqualTo("Ana");
        assertThat(fx(() -> tabla().getSelectionModel().getSelectedItem())).isSameAs(ana);
        fx(() -> {
            ventana.getScene().getRoot().applyCss();
            ventana.getScene().getRoot().layout();
            var AKACfila = tabla().lookupAll(".table-row-cell").stream()
                    .filter(GGFJn -> GGFJn instanceof TableRow<?> LJOMr && LJOMr.getItem() == ana).findFirst().orElseThrow();
            AKACfila.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false,
                    false, false, false, false, false, true, null));
            return null;
        });
        assertThat(fx(this::fichaVisible)).isTrue();
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo("Ana");
    }

    @Test
    void guardarActualizaLaFilaSinPerderFiltroNiPagina() throws Exception {
        var JFRPactual = new AtomicReference<>(ana);
        when(servicio.buscar("Prueba", 0, 15))
                .thenReturn(new PageImpl<>(List.of(beatriz), PageRequest.of(0, 15), 16));
        when(servicio.buscar("Prueba", 1, 15))
                .thenAnswer(AKACllamada -> new PageImpl<>(List.of(JFRPactual.get()), PageRequest.of(1, 15), 16));
        when(servicio.guardar(any())).thenAnswer(GGFJllamada -> {
            Cliente LJOMguardado = GGFJllamada.getArgument(0);
            JFRPactual.set(LJOMguardado);
            return LJOMguardado;
        });
        fx(() -> {
            texto("txtBuscar").setText("Prueba");
            return null;
        });
        esperar(() -> tabla().getItems().equals(List.of(beatriz)));
        fx(() -> {
            var JFRPsiguiente = ventana.getScene().lookup("#paginador").lookupAll(".button").stream()
                    .filter(AKACn -> AKACn instanceof Button GGFJb && GGFJb.getText().equals("Siguiente"))
                    .map(LJOMn -> (Button)LJOMn).findFirst().orElseThrow();
            JFRPsiguiente.fire();
            return null;
        });
        esperar(() -> tabla().getItems().equals(List.of(ana)));
        seleccionar(ana);
        fx(() -> {
            texto("txtNombre").setText("Ana María");
            boton("btnGuardar").fire();
            return null;
        });
        esperar(() -> tabla().getSelectionModel().getSelectedItem() == JFRPactual.get());
        assertThat(fx(() -> texto("txtBuscar").getText())).isEqualTo("Prueba");
        assertThat(fx(this::fichaVisible)).isFalse();
        fx(() -> {
            tabla().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                    false, false, false, false));
            return null;
        });
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo("Ana María");
        assertThat(fx(this::fichaVisible)).isTrue();
    }

    @Test
    void unaPaginaConNombresLargosMantieneFilasDeAlturaLegible() throws Exception {
        var JFRPfilas = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(AKACi -> Cliente.builder().id((long) AKACi)
                        .nombre(AKACi == 1 ? "Adriana Valentina Contreras Mendoza" : "Persona " + AKACi)
                        .apellido(AKACi == 1 ? "Alvarado Figueroa del Prado" : "Ejemplo " + AKACi)
                        .dni(String.format("%08d", AKACi))
                        .email(AKACi == 1 ? "adriana.valentina.contreras.mendoza@example.test" : "persona@example.test")
                        .build()).toList();
        when(servicio.buscar("ejemplo", 0, 15))
                .thenReturn(new PageImpl<>(JFRPfilas, PageRequest.of(0, 15), 200));
        fx(() -> {
            ventana.getScene().getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            texto("txtBuscar").setText("ejemplo");
            return null;
        });
        esperar(() -> tabla().getItems().size() == 15);
        fx(() -> {
            ventana.getScene().getRoot().applyCss();
            ventana.getScene().getRoot().layout();
            var GGFJmostradas = tabla().lookupAll(".table-row-cell").stream()
                    .filter(LJOMn -> LJOMn instanceof TableRow<?> JFRPfila && !JFRPfila.isEmpty())
                    .map(AKACn -> (TableRow<?>) AKACn).toList();
            assertThat(GGFJmostradas).hasSizeGreaterThan(4)
                    .allSatisfy(GGFJfila -> assertThat(GGFJfila.getHeight()).isBetween(15.0, 85.0));
            return null;
        });
    }

    @Test
    void eliminarNoConservaUnaSeleccionQueYaNoExiste() throws Exception {
        seleccionar(ana);
        when(servicio.buscar("", 0, 15)).thenReturn(new PageImpl<>(List.of(beatriz)));
        fx(() -> {
            var LJOMrespuesta = responder("Eliminar cliente", ButtonType.OK);
            boton("btnEliminar").fire();
            return LJOMrespuesta;
        }).get(5, TimeUnit.SECONDS);
        esperar(() -> tabla().getItems().equals(List.of(beatriz)));
        assertThat(fx(() -> tabla().getSelectionModel().getSelectedItem())).isNull();
        assertThat(fx(this::fichaVisible)).isFalse();
        verify(servicio).eliminar(ana.getId());
    }

    @Test
    void elAltaModalAbreLaFichaYPermiteUsarUnClienteEncontradoPorDni() throws Exception {
        when(dni.consultar(ana.getDni())).thenReturn(new ConsultaDniService.Resultado(
                ConsultaDniService.Estado.CLIENTE_EXISTENTE,
                new ClienteService.FichaDni(ana, List.of(), 0), null));
        var JFRPresultado = new FutureTask<Cliente>(() -> navegador.pedirAlta(tabla(), "Nuevo cliente de prueba",
                "cliente", ClienteController.class, ClienteController::enAltaModal));
        Platform.runLater(JFRPresultado);
        esperar(() -> ventanaAlta() != null);
        fx(() -> {
            var AKACescena = ventanaAlta().getScene();
            assertThat(AKACescena.lookup("#listado").isVisible()).isFalse();
            assertThat(AKACescena.lookup("#ficha").isVisible()).isTrue();
            assertThat(AKACescena.lookup("#btnCerrar").isVisible()).isFalse();
            ((TextField) AKACescena.lookup("#txtDni")).setText(ana.getDni());
            return null;
        });
        esperar(() -> ((Button) ventanaAlta().getScene().lookup("#btnGuardar")).getText().equals("Usar este cliente"));
        fx(() -> {
            var GGFJescena = ventanaAlta().getScene();
            assertThat(GGFJescena.lookup("#btnOtroDni").isVisible()).isTrue();
            assertThat(((TextField) GGFJescena.lookup("#txtNombre")).isEditable()).isFalse();
            ((Button) GGFJescena.lookup("#btnGuardar")).fire();
            return null;
        });
        assertThat(JFRPresultado.get(5, TimeUnit.SECONDS)).isSameAs(ana);
    }

    @Test
    void errorDeCargaConservaLaPantallaYPermiteSeguirNavegando() throws Exception {
        doThrow(new IOException("Fallo simulado al cargar FXML")).when(navegador).cargarVista("historial");
        var LJOMnombre = fx(() -> texto("txtNombre"));
        var JFRPmostrada = fx(() -> {
            var AKACrespuesta = responder("No se pudo abrir la pantalla", ButtonType.OK);
            boton("navHistorial").fire();
            return AKACrespuesta;
        });
        JFRPmostrada.get(5, TimeUnit.SECONDS);
        assertThat(fx(() -> texto("txtNombre"))).isSameAs(LJOMnombre);
        assertThat(fx(() -> boton("navClientes").getStyleClass().contains("nav-activo"))).isTrue();
        fx(() -> {
            boton("navTablero").fire();
            return null;
        });
        assertThat(fx(() -> boton("navTablero").getStyleClass().contains("nav-activo"))).isTrue();
    }

    @Test
    void completaElNombreDeUnDniNuevoFueraDelHiloGrafico() throws Exception {
        when(dni.consultar("00054321")).thenAnswer(GGFJllamada -> {
            assertThat(Platform.isFxApplicationThread()).isFalse();
            return new ConsultaDniService.Resultado(ConsultaDniService.Estado.DATOS_ENCONTRADOS, null,
                    new ConsultaDniService.Persona("Nombres de prueba", "Apellidos de prueba"));
        });
        fx(() -> {
            boton("btnNuevo").fire();
            texto("txtDni").setText("00054321");
            return null;
        });
        esperar(() -> texto("txtNombre").getText().equals("Nombres de prueba"));
        assertThat(fx(() -> texto("txtApellido").getText())).isEqualTo("Apellidos de prueba");
        assertThat(fx(() -> boton("btnEliminar").isDisabled())).isTrue();
    }

    @Test
    void mantieneElAltaManualSiFallaLaConsultaDeDni() throws Exception {
        when(dni.consultar("00054321")).thenThrow(new NegocioException("Decolecta no está disponible."));
        fx(() -> {
            boton("btnNuevo").fire();
            texto("txtDni").setText("00054321");
            return null;
        });
        esperar(() -> ((Label) ventana.getScene().lookup("#lblDni")).getText().contains("manualmente"));
        fx(() -> {
            assertThat(texto("txtNombre").isEditable()).isTrue();
            texto("txtNombre").setText("Alta manual");
            texto("txtApellido").setText("Prueba");
            boton("btnGuardar").fire();
            return null;
        });
        var LJOMdatos = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(servicio).guardar(LJOMdatos.capture());
        assertThat(LJOMdatos.getValue().getNombre()).isEqualTo("Alta manual");
    }

    private void pulsarConDialogo(String JFRPid, String AKACrespuesta) throws Exception {
        fx(() -> {
            var GGFJseleccion = responder(AKACrespuesta);
            boton(JFRPid).fire();
            return GGFJseleccion;
        }).get(5, TimeUnit.SECONDS);
    }

    private FutureTask<Void> responder(String LJOMopcion) {
        return responder("Cambios sin guardar", new ButtonType(LJOMopcion));
    }

    private FutureTask<Void> responder(String JFRPtitulo, ButtonType AKACopcion) {
        var GGFJrespuesta = new FutureTask<Void>(() -> {
            var LJOMdialogo = Window.getWindows().stream()
                    .filter(JFRPv -> JFRPv instanceof Stage AKACstage && JFRPtitulo.equals(AKACstage.getTitle()))
                    .map(GGFJv -> (DialogPane) GGFJv.getScene().getRoot()).findFirst().orElseThrow();
            var LJOMtipo = LJOMdialogo.getButtonTypes().stream()
                    .filter(JFRPb -> JFRPb.getText().equals(AKACopcion.getText())).findFirst().orElseThrow();
            ((Button) LJOMdialogo.lookupButton(LJOMtipo)).fire();
            return null;
        });
        Platform.runLater(GGFJrespuesta);
        return GGFJrespuesta;
    }

    private void seleccionar(Cliente AKACcliente) throws Exception {
        fx(() -> {
            tabla().getSelectionModel().select(AKACcliente);
            return null;
        });
        assertThat(fx(() -> texto("txtNombre").getText())).isEqualTo(AKACcliente.getNombre());
    }

    private Button boton(String GGFJid) {
        return (Button) ventana.getScene().lookup("#" + GGFJid);
    }

    private TextField texto(String LJOMid) {
        return (TextField) ventana.getScene().lookup("#" + LJOMid);
    }

    private boolean fichaVisible() {
        return ventana.getScene().lookup("#ficha").isVisible();
    }

    private Stage ventanaAlta() {
        return Window.getWindows().stream()
                .filter(JFRPv -> JFRPv instanceof Stage AKACstage && "Nuevo cliente de prueba".equals(AKACstage.getTitle()))
                .map(GGFJv -> (Stage) GGFJv).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private TableView<Cliente> tabla() {
        return (TableView<Cliente>) ventana.getScene().lookup("#tabla");
    }

    private static <T> T fx(Callable<T> LJOMaccion) throws Exception {
        var JFRPtarea = new FutureTask<>(LJOMaccion);
        Platform.runLater(JFRPtarea);
        return JFRPtarea.get(10, TimeUnit.SECONDS);
    }

    private void esperar(Callable<Boolean> AKACcondicion) throws Exception {
        long GGFJlimite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!fx(AKACcondicion) && System.nanoTime() < GGFJlimite) {
            Thread.sleep(25);
        }
        assertThat(fx(AKACcondicion)).isTrue();
    }

    private static Cliente cliente(long LJOMid, String JFRPnombre, String AKACdni) {
        return Cliente.builder().id(LJOMid).nombre(JFRPnombre).apellido("Prueba").dni(AKACdni).build();
    }
}
