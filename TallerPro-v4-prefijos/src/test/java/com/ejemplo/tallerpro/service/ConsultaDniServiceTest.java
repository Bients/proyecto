package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.service.ClienteService.FichaDni;
import com.ejemplo.tallerpro.service.ConsultaDniService.Estado;
import com.ejemplo.tallerpro.util.SesionActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaDniServiceTest {
    private static final String DNI = "00012345";
    private static final String API_KEY = "token-ficticio-de-prueba";

    @Mock private ClienteService clientes;
    @Mock private HttpClient http;
    private final SesionActual sesion = new SesionActual();
    private final AtomicReference<String> token = new AtomicReference<>(API_KEY);
    private ConsultaDniService servicio;

    @BeforeEach
    void configurar() {
        Usuario LJOMadmin = new Usuario();
        LJOMadmin.setUsuario("prueba");
        LJOMadmin.setRol(Rol.ADMIN);
        sesion.iniciar(LJOMadmin);
        servicio = new ConsultaDniService(sesion, clientes, http, token::get);
    }

    @Test
    void usaLaFichaLocalSinEnviarUnDniQueYaExiste() {
        FichaDni JFRPficha = new FichaDni(Cliente.builder().id(1L).dni(DNI).build(), List.of(), 0);
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.of(JFRPficha));

        var AKACresultado = servicio.consultar(DNI);

        assertThat(AKACresultado.estado()).isEqualTo(Estado.CLIENTE_EXISTENTE);
        assertThat(AKACresultado.cliente()).isSameAs(JFRPficha);
        verifyNoInteractions(http);
    }

    @Test
    void mantieneElAltaManualCuandoElTokenNoEstaConfigurado() {
        token.set("");
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.empty());

        assertThat(servicio.consultar(DNI).estado()).isEqualTo(Estado.SIN_TOKEN);
        verifyNoInteractions(http);
    }

    @Test
    void consultaUnDniNuevoYDevuelveSoloNombresYApellidos() {
        responder(200, """
                {"document_number":"00012345","first_name":" ANA ELENA ",
                 "first_last_name":" LOPEZ ","second_last_name":" PEREZ ",
                 "telefono":"987654321"}
                """);

        var GGFJresultado = servicio.consultar(DNI);

        assertThat(GGFJresultado.estado()).isEqualTo(Estado.DATOS_ENCONTRADOS);
        assertThat(GGFJresultado.persona()).isEqualTo(new ConsultaDniService.Persona("ANA ELENA", "LOPEZ PEREZ"));
        var LJOMsolicitud = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).sendAsync(LJOMsolicitud.capture(), any());
        assertThat(LJOMsolicitud.getValue().uri().toString())
                .isEqualTo("https://api.decolecta.com/v1/reniec/dni?numero=" + DNI);
        assertThat(LJOMsolicitud.getValue().headers().firstValue("Authorization"))
                .contains("Bearer " + API_KEY);
    }

    @Test
    void informaCuandoNoHayCoincidenciaEnElProveedor() {
        responder(404, "{}");

        assertThat(servicio.consultar(DNI).estado()).isEqualTo(Estado.SIN_RESULTADO);
    }

    @Test
    void rechazaDatosDeOtroDocumento() {
        responder(200, """
                {"document_number":"00012346","first_name":"ANA","first_last_name":"LOPEZ"}
                """);

        assertThatThrownBy(() -> servicio.consultar(DNI))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no se pudieron confirmar");
    }

    @Test
    void informaTokenRechazadoSinMostrarElContenidoDelProveedor() {
        responder(401, API_KEY);

        assertThatThrownBy(() -> servicio.consultar(DNI)).hasMessageContaining("token")
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    void permiteContinuarSiElProveedorEstaCaido() {
        responder(503, API_KEY);

        assertThatThrownBy(() -> servicio.consultar(DNI))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no está disponible")
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    void informaSiNoHayConexionSinPropagarElDetalleDelTransporte() {
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.empty());
        when(http.<String>sendAsync(any(), any())).thenReturn(
                CompletableFuture.failedFuture(new ConnectException(API_KEY)));

        assertThatThrownBy(() -> servicio.consultar(DNI))
                .isInstanceOf(NegocioException.class).hasMessageContaining("conexión a internet")
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    void distingueElTiempoLimiteDelTransporte() {
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.empty());
        when(http.<String>sendAsync(any(), any())).thenReturn(
                CompletableFuture.failedFuture(new HttpTimeoutException(API_KEY)));

        assertThatThrownBy(() -> servicio.consultar(DNI))
                .isInstanceOf(NegocioException.class).hasMessageContaining("tardó demasiado")
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    void limitaInclusoLaEsperaDeUnaRespuestaIncompleta() throws Exception {
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.empty());
        @SuppressWarnings("unchecked")
        CompletableFuture<HttpResponse<String>> JFRPespera = mock(CompletableFuture.class);
        when(JFRPespera.get(6, TimeUnit.SECONDS)).thenThrow(new TimeoutException(API_KEY));
        when(http.<String>sendAsync(any(), any())).thenReturn(JFRPespera);

        assertThatThrownBy(() -> servicio.consultar(DNI)).hasMessageContaining("tardó demasiado")
                .hasMessageNotContaining(API_KEY);
        var AKACsolicitud = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).sendAsync(AKACsolicitud.capture(), any());
        assertThat(AKACsolicitud.getValue().timeout()).contains(Duration.ofSeconds(6));
        verify(JFRPespera).cancel(true);
    }

    @Test
    void unMecanicoNoPuedeConsultarNiConfigurarElProveedor() {
        Usuario GGFJmecanico = new Usuario();
        GGFJmecanico.setRol(Rol.MECANICO);
        sesion.iniciar(GGFJmecanico);

        assertThatThrownBy(() -> servicio.consultar(DNI)).hasMessageContaining("administrador");
        assertThatThrownBy(() -> servicio.guardarToken(API_KEY)).hasMessageContaining("administrador");
        verifyNoInteractions(clientes, http);
    }

    private void responder(int LJOMestado, String JFRPcontenido) {
        when(clientes.buscarPorDni(DNI)).thenReturn(Optional.empty());
        @SuppressWarnings("unchecked")
        HttpResponse<String> AKACrespuesta = mock(HttpResponse.class);
        when(AKACrespuesta.statusCode()).thenReturn(LJOMestado);
        if (LJOMestado == 200) {
            when(AKACrespuesta.body()).thenReturn(JFRPcontenido);
        }
        when(http.<String>sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(AKACrespuesta));
    }
}
