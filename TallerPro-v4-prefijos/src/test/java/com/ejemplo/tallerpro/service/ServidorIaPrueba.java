package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class ServidorIaPrueba implements AutoCloseable {
    public static final String CLAVE = "sk-ant-clave-ficticia-para-pruebas-000";
    private final HttpServer servidor;
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicInteger consultas = new AtomicInteger();
    private final CountDownLatch recibida = new CountDownLatch(1);
    private final CountDownLatch continuar;
    private boolean detenido;

    public ServidorIaPrueba(int estado, String respuesta, boolean esperar) throws IOException {
        continuar = new CountDownLatch(esperar ? 1 : 0);
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/messages", GGFJintercambio -> {
            try (GGFJintercambio) {
                GGFJintercambio.getRequestBody().readAllBytes();
                consultas.incrementAndGet();
                recibida.countDown();
                try {
                    continuar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException LJOMe) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!CLAVE.equals(GGFJintercambio.getRequestHeaders().getFirst("x-api-key"))) {
                    GGFJintercambio.sendResponseHeaders(403, -1);
                    return;
                }
                var JFRPcuerpo = json.createObjectNode();
                if (estado == 200) {
                    JFRPcuerpo.putArray("content").addObject().put("type", "text").put("text", respuesta);
                } else {
                    JFRPcuerpo.putObject("error").put("message", respuesta);
                }
                byte[] AKACbytes = JFRPcuerpo.toString().getBytes(StandardCharsets.UTF_8);
                GGFJintercambio.getResponseHeaders().set("Content-Type", "application/json");
                GGFJintercambio.sendResponseHeaders(estado, AKACbytes.length);
                GGFJintercambio.getResponseBody().write(AKACbytes);
            }
        });
        servidor.start();
    }

    public IaService servicio(SesionActual sesion, Supplier<String> clave) {
        return new IaService(sesion, URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/messages"),
                Duration.ofSeconds(4), clave, () -> "");
    }

    public int consultas() {
        return consultas.get();
    }

    public boolean recibida() throws InterruptedException {
        return recibida.await(3, TimeUnit.SECONDS);
    }

    public void responder() {
        continuar.countDown();
    }

    @Override
    public void close() {
        if (!detenido) {
            detenido = true;
            responder();
            servidor.stop(0);
        }
    }
}
