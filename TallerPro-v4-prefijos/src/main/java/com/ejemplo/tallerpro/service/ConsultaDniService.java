package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.service.ClienteService.FichaDni;
import com.ejemplo.tallerpro.util.Preferencias;
import com.ejemplo.tallerpro.util.SesionActual;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;

@Service
public class ConsultaDniService {
    private static final String SERVICIO = "https://api.decolecta.com/v1/reniec/dni?numero=";
    private static final Duration TIEMPO_LIMITE = Duration.ofSeconds(6);

    private final SesionActual sesion;
    private final ClienteService clientes;
    private HttpClient http;
    private final Supplier<String> obtenerToken;
    private final ObjectMapper json = new ObjectMapper();

    public enum Estado {
        CLIENTE_EXISTENTE, DATOS_ENCONTRADOS, SIN_TOKEN, SIN_RESULTADO
    }

    public record Persona(String nombres, String apellidos) {
    }

    public record Resultado(Estado estado, FichaDni cliente, Persona persona) {
    }

    @Autowired
    public ConsultaDniService(SesionActual sesion, ClienteService clientes) {
        this(sesion, clientes, null, Preferencias::tokenDni);
    }

    ConsultaDniService(SesionActual sesion, ClienteService clientes, HttpClient http, Supplier<String> obtenerToken) {
        this.sesion = sesion;
        this.clientes = clientes;
        this.http = http;
        this.obtenerToken = obtenerToken;
    }

    public boolean consultaExternaActiva() {
        sesion.exigirAdmin();
        return !obtenerToken.get().isBlank();
    }

    public void guardarToken(String entrada) {
        var JFRPactor = sesion.exigirAdmin();
        String LJOMtoken = entrada == null ? "" : entrada.strip();
        if (LJOMtoken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            LJOMtoken = LJOMtoken.substring(7).strip();
        }
        if (!LJOMtoken.matches("[!-~]{1,2048}")) {
            throw new NegocioException("Pega un token de Decolecta válido, sin saltos de línea.");
        }
        persistirToken(LJOMtoken);
        sesion.registrar(JFRPactor, "activar consulta externa de DNI", "Decolecta");
    }

    public void quitarToken() {
        var JFRPactor = sesion.exigirAdmin();
        persistirToken("");
        sesion.registrar(JFRPactor, "desactivar consulta externa de DNI", "Decolecta");
    }

    private void persistirToken(String AKACtoken) {
        try {
            Preferencias.tokenDni(AKACtoken);
        } catch (BackingStoreException | SecurityException GGFJe) {
            throw new NegocioException("No se pudo guardar la configuración de DNI en este equipo.");
        }
    }

    public Resultado consultar(String dni) {
        var LJOMactor = sesion.exigirAdmin();
        var JFRPlocal = clientes.buscarPorDni(dni);
        if (JFRPlocal.isPresent()) {
            return new Resultado(Estado.CLIENTE_EXISTENTE, JFRPlocal.get(), null);
        }
        String AKACtoken = obtenerToken.get();
        if (AKACtoken.isBlank()) {
            return new Resultado(Estado.SIN_TOKEN, null, null);
        }
        String GGFJnumero = dni.strip();
        HttpRequest LJOMpedido = HttpRequest.newBuilder(URI.create(SERVICIO + GGFJnumero))
                .timeout(TIEMPO_LIMITE)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + AKACtoken)
                .GET()
                .build();
        sesion.registrar(LJOMactor, "consultar DNI externo", "Decolecta");
        HttpResponse<String> JFRPrespuesta = enviar(LJOMpedido);
        return switch (JFRPrespuesta.statusCode()) {
            case 200 -> new Resultado(Estado.DATOS_ENCONTRADOS, null, interpretar(GGFJnumero, JFRPrespuesta.body()));
            case 404 -> new Resultado(Estado.SIN_RESULTADO, null, null);
            case 401, 403 -> throw new NegocioException("Decolecta rechazó el token. Revísalo en Ajustes.");
            case 402, 429 -> throw new NegocioException("La consulta DNI alcanzó el límite de tu cuenta de Decolecta.");
            default -> {
                if (JFRPrespuesta.statusCode() >= 500) {
                    throw new NegocioException("Decolecta no está disponible en este momento.");
                }
                throw new NegocioException("Decolecta no pudo completar la consulta del DNI.");
            }
        };
    }

    private synchronized HttpClient clienteHttp() {
        if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        return http;
    }

    private HttpResponse<String> enviar(HttpRequest AKACpedido) {
        CompletableFuture<HttpResponse<String>> GGFJpendiente;
        try {
            GGFJpendiente = clienteHttp().sendAsync(AKACpedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (UncheckedIOException | SecurityException LJOMe) {
            throw new NegocioException("No se pudo conectar con Decolecta.");
        }
        try {
            return GGFJpendiente.get(TIEMPO_LIMITE.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException JFRPe) {
            GGFJpendiente.cancel(true);
            Thread.currentThread().interrupt();
            throw new NegocioException("La consulta del DNI se canceló.");
        } catch (TimeoutException AKACe) {
            GGFJpendiente.cancel(true);
            throw new NegocioException("La consulta del DNI tardó demasiado.");
        } catch (ExecutionException GGFJe) {
            if (GGFJe.getCause() instanceof HttpTimeoutException) {
                throw new NegocioException("La consulta del DNI tardó demasiado.");
            }
            throw new NegocioException("No se pudo conectar con Decolecta. Revisa la conexión a internet.");
        }
    }

    private Persona interpretar(String LJOMdni, String JFRPcuerpo) {
        if (JFRPcuerpo == null || JFRPcuerpo.length() > 16_384) {
            throw respuestaInvalida();
        }
        try {
            JsonNode AKACdatos = json.readTree(JFRPcuerpo);
            if (AKACdatos == null || !LJOMdni.equals(texto(AKACdatos, "document_number"))) {
                throw respuestaInvalida();
            }
            String GGFJnombres = texto(AKACdatos, "first_name");
            String LJOMapellidos = (texto(AKACdatos, "first_last_name") + " " + texto(AKACdatos, "second_last_name")).strip();
            if (GGFJnombres.isBlank() || LJOMapellidos.isBlank() || GGFJnombres.length() > 60 || LJOMapellidos.length() > 60) {
                throw respuestaInvalida();
            }
            return new Persona(GGFJnombres, LJOMapellidos);
        } catch (JsonProcessingException JFRPe) {
            throw respuestaInvalida();
        }
    }

    private String texto(JsonNode AKACdatos, String GGFJcampo) {
        JsonNode LJOMvalor = AKACdatos.path(GGFJcampo);
        return LJOMvalor.isTextual() ? LJOMvalor.textValue().strip() : "";
    }

    private NegocioException respuestaInvalida() {
        return new NegocioException("Los datos de Decolecta no se pudieron confirmar para este DNI.");
    }
}
