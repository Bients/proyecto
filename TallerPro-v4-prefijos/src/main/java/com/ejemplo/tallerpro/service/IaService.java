package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.IaNoDisponibleException;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.util.Preferencias;
import com.ejemplo.tallerpro.util.SesionActual;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Asistente de IA del taller (API de Claude). Mantiene una conversacion: el usuario puede hacer
 * preguntas de seguimiento. Requiere internet y una clave de API valida (empieza con "sk-ant-"),
 * guardada en Ajustes o en la variable de entorno ANTHROPIC_API_KEY.
 */
@Service
public class IaService {

    /** Un turno de la conversacion. rol = "user" o "assistant". */
    public record Mensaje(String rol, String texto) {
    }

    private static final URI URL = URI.create("https://api.anthropic.com/v1/messages");
    private static final Duration ESPERA = Duration.ofSeconds(30);
    private static final String MODELO = "claude-sonnet-5-5";

    private static final String SISTEMA = """
            Eres TallerPro IA, el asistente de un taller automotriz. Eres un mecanico experto y tambien puedes \
            ayudar con cualquier otra pregunta que te hagan: responde siempre en español, de forma util, clara y \
            directa, sin negarte a responder por no ser un tema de autos.

            Cuando te describan un problema o sintoma de un vehiculo, responde con esta estructura:
            1) Posibles causas, de la mas a la menos probable (explica brevemente el porque).
            2) Como confirmarlo: revisiones y pruebas concretas, paso a paso.
            3) Soluciones: que hay que reparar o cambiar, que repuestos y servicios se necesitan, y que puede \
            hacer el propietario por su cuenta frente a lo que debe hacer el taller.
            4) Urgencia (baja, media o alta) y si es seguro seguir conduciendo.
            5) Consejos para prevenirlo.

            Si falta informacion importante, da igualmente tu mejor diagnostico y al final pregunta lo que \
            necesitas saber (kilometraje, cuando empezo, si hay luces en el tablero, etc.). Usa los datos del \
            vehiculo cuando te los den. Recuerda que es una orientacion que se confirma con inspeccion fisica.""";

    @Value("${taller.ia.modelo:" + MODELO + "}")
    private String modelo = MODELO;

    private final SesionActual sesion;
    private final URI destino;
    private final Duration espera;
    private final Supplier<String> claveLocal;
    private final Supplier<String> claveEntorno;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpClient http;

    @Autowired
    public IaService(SesionActual sesion) {
        this(sesion, URL, ESPERA, Preferencias::apiKey, () -> System.getenv("ANTHROPIC_API_KEY"));
    }

    IaService(SesionActual sesion, URI destino, Duration espera, Supplier<String> claveLocal,
              Supplier<String> claveEntorno) {
        this.sesion = sesion;
        this.destino = destino;
        this.espera = espera;
        this.claveLocal = claveLocal;
        this.claveEntorno = claveEntorno;
    }

    /** Quita espacios, saltos de linea y comillas que suelen colarse al pegar la clave. */
    public static String limpiarClave(String clave) {
        if (clave == null) {
            return "";
        }
        return clave.replaceAll("\\s+", "").replaceAll("^[\"'“”]+|[\"'“”]+$", "");
    }

    public static boolean formatoValido(String clave) {
        return clave != null && clave.matches("sk-ant-[A-Za-z0-9_-]{23,2041}");
    }

    public String estadoCredencial() {
        sesion.exigirAdmin();
        String LJOMguardada = limpiarClave(claveLocal.get());
        if (formatoValido(LJOMguardada)) {
            return "Hay una clave guardada en este equipo.";
        }
        if (!LJOMguardada.isBlank()) {
            return "La clave guardada tiene un formato inválido.";
        }
        return hayClave() ? "Se usará la clave de ANTHROPIC_API_KEY." : "El asistente usa el modo básico sin IA.";
    }

    public void guardarCredencial(String entrada) {
        var JFRPactor = sesion.exigirAdmin();
        String GGFJclave = limpiarClave(entrada);
        if (!formatoValido(GGFJclave)) {
            throw new NegocioException("Ingresa una clave de API válida que empiece con sk-ant-.");
        }
        Preferencias.apiKey(GGFJclave);
        sesion.registrar(JFRPactor, "guardar credencial de IA", "IA");
    }

    public void quitarCredencial() {
        var LJOMactor = sesion.exigirAdmin();
        Preferencias.apiKey(null);
        sesion.registrar(LJOMactor, "quitar credencial de IA", "IA");
    }

    public boolean hayClave() {
        return formatoValido(limpiarClave(claveLocal.get()))
                || formatoValido(limpiarClave(claveEntorno.get()));
    }

    /**
     * Envia la conversacion completa y devuelve la respuesta de la IA.
     * Bloquea hasta recibirla: llamar desde un hilo de fondo, no desde la interfaz.
     *
     * @throws IaNoDisponibleException si la IA no se puede usar (el mensaje explica por que)
     */
    public String conversar(String vehiculo, List<Mensaje> historial) {
        sesion.exigirUsuario();
        if (historial == null || historial.isEmpty()) {
            throw new NegocioException("Escribe tu consulta.");
        }
        ObjectNode JFRPcuerpo = mapper.createObjectNode();
        JFRPcuerpo.put("model", modelo);
        JFRPcuerpo.put("max_tokens", 2000);
        JFRPcuerpo.put("system", SISTEMA + (vehiculo == null || vehiculo.isBlank()
                ? "" : "\n\nVehiculo en consulta: " + vehiculo));
        ArrayNode AKACmensajes = JFRPcuerpo.putArray("messages");
        for (Mensaje GGFJm : historial) {
            ObjectNode LJOMnodo = AKACmensajes.addObject();
            LJOMnodo.put("role", GGFJm.rol());
            LJOMnodo.put("content", GGFJm.texto());
        }
        return enviar(claveDisponible(), JFRPcuerpo);
    }

    /** Hace una consulta minima para comprobar que la clave y la conexion funcionan. */
    public void probarConexion() {
        var JFRPactor = sesion.exigirAdmin();
        ObjectNode AKACcuerpo = mapper.createObjectNode();
        AKACcuerpo.put("model", modelo);
        AKACcuerpo.put("max_tokens", 20);
        ObjectNode GGFJmensaje = AKACcuerpo.putArray("messages").addObject();
        GGFJmensaje.put("role", "user");
        GGFJmensaje.put("content", "Responde solo con la palabra OK.");
        enviar(claveDisponible(), AKACcuerpo);
        sesion.registrar(JFRPactor, "probar credencial de IA", "IA");
    }

    private synchronized HttpClient clienteHttp() {
        if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        return http;
    }

    private String enviar(String LJOMclave, ObjectNode JFRPcuerpo) {
        try {
            HttpRequest AKACpeticion = HttpRequest.newBuilder(destino)
                    .timeout(espera)
                    .header("content-type", "application/json")
                    .header("x-api-key", LJOMclave)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(JFRPcuerpo)))
                    .build();

            HttpResponse<String> GGFJrespuesta = recibir(AKACpeticion);
            int LJOMestado = GGFJrespuesta.statusCode();
            if (LJOMestado != 200) {
                throw new IaNoDisponibleException(explicar(LJOMestado, GGFJrespuesta.body()));
            }
            JsonNode JFRPraiz = mapper.readTree(GGFJrespuesta.body());
            StringBuilder AKACtexto = new StringBuilder();
            if (JFRPraiz != null) {
                for (JsonNode GGFJbloque : JFRPraiz.path("content")) {
                    if ("text".equals(GGFJbloque.path("type").asText())) {
                        AKACtexto.append(GGFJbloque.path("text").asText());
                    }
                }
            }
            if (AKACtexto.toString().isBlank()) {
                throw new IaNoDisponibleException("La IA devolvió una respuesta vacía. Intenta de nuevo.");
            }
            return AKACtexto.toString().replace(LJOMclave, "[credencial omitida]").strip();
        } catch (IOException LJOMe) {
            throw new IaNoDisponibleException("El servicio de IA devolvió una respuesta que no se pudo leer.");
        }
    }

    private HttpResponse<String> recibir(HttpRequest JFRPpeticion) {
        CompletableFuture<HttpResponse<String>> AKACpendiente;
        try {
            AKACpendiente = clienteHttp().sendAsync(JFRPpeticion, HttpResponse.BodyHandlers.ofString());
        } catch (UncheckedIOException | SecurityException GGFJe) {
            throw new IaNoDisponibleException("No se pudo conectar con el servicio de IA.");
        }
        try {
            return AKACpendiente.get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException LJOMe) {
            AKACpendiente.cancel(true);
            Thread.currentThread().interrupt();
            throw new IaNoDisponibleException("La consulta fue cancelada.");
        } catch (TimeoutException JFRPe) {
            AKACpendiente.cancel(true);
            throw new IaNoDisponibleException("El servicio de IA tardó demasiado en responder.");
        } catch (ExecutionException AKACe) {
            if (AKACe.getCause() instanceof HttpTimeoutException) {
                throw new IaNoDisponibleException("El servicio de IA tardó demasiado en responder.");
            }
            throw new IaNoDisponibleException("No se pudo conectar con el servicio de IA.");
        }
    }

    private String explicar(int GGFJestado, String LJOMdetalle) {
        if (GGFJestado == 401) {
            return "El servicio de IA rechazó la clave configurada.";
        }
        if (GGFJestado == 403) {
            return "Esta clave no tiene permiso para usar la API o el modelo configurado.";
        }
        if (GGFJestado == 404) {
            return "El modelo configurado no está disponible para la cuenta del taller.";
        }
        if (GGFJestado == 400 && LJOMdetalle != null && LJOMdetalle.toLowerCase(Locale.ROOT).contains("credit")) {
            return "La cuenta de IA del taller no tiene saldo.";
        }
        if (GGFJestado == 429) {
            return "Se hicieron demasiadas consultas seguidas. Espera un momento e intenta de nuevo.";
        }
        if (GGFJestado >= 500) {
            return "El servicio de IA está saturado o en mantenimiento. Intenta de nuevo en unos minutos.";
        }
        return "El servicio de IA no pudo responder (HTTP " + GGFJestado + ").";
    }

    private String claveDisponible() {
        String JFRPguardada = limpiarClave(claveLocal.get());
        String AKACentorno = limpiarClave(claveEntorno.get());
        if (formatoValido(JFRPguardada)) {
            return JFRPguardada;
        }
        if (formatoValido(AKACentorno)) {
            return AKACentorno;
        }
        if (!JFRPguardada.isBlank()) {
            throw new IaNoDisponibleException("La clave guardada tiene un formato inválido.");
        }
        throw new IaNoDisponibleException("El taller todavía no tiene una clave de IA configurada.");
    }
}
