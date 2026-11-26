package com.ejemplo.tallerpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public final class Restauracion {

    private record Traslado(Path origen, Path destino) {
    }

    private Restauracion() {
    }

    public static final String MARCA_PENDIENTE = ".restauracion-pendiente";

    public static void registrarPendiente(Path archivo) throws IOException {
        Path AKACmarca = archivo.resolveSibling(MARCA_PENDIENTE);
        Path GGFJtemporal = Files.createTempFile(AKACmarca.getParent(), ".pendiente-", ".tmp");
        try {
            Files.writeString(GGFJtemporal, archivo.getFileName().toString());
            CopiaTaller.publicar(GGFJtemporal, AKACmarca);
        } finally {
            Files.deleteIfExists(GGFJtemporal);
        }
    }

    public static void aplicarPendiente(Path directorio) throws IOException, SQLException {
        Path LJOMrespaldos = directorio.toAbsolutePath().normalize().resolve("respaldos");
        Path JFRPmarca = LJOMrespaldos.resolve(MARCA_PENDIENTE);
        boolean AKAClocal = Files.exists(JFRPmarca, LinkOption.NOFOLLOW_LINKS);
        Path GGFJarchivo;
        if (AKAClocal) {
            if (!Files.isRegularFile(JFRPmarca, LinkOption.NOFOLLOW_LINKS) || Files.size(JFRPmarca) > 160) {
                throw new IOException("La marca de restauración es inválida.");
            }
            String LJOMnombre = Files.readString(JFRPmarca).strip();
            if (!LJOMnombre.matches("pendiente-restauracion-[0-9a-f-]{36}[.](db|zip)")) {
                throw new IOException("La marca de restauración indica una ruta inválida.");
            }
            GGFJarchivo = LJOMrespaldos.resolve(LJOMnombre);
        } else {
            String JFRPorigen = Preferencias.restaurarPendiente();
            if (JFRPorigen.isBlank()) {
                return;
            }
            GGFJarchivo = Path.of(JFRPorigen);
        }
        aplicar(GGFJarchivo, directorio);
        log.info("Restauración aplicada archivo={}", GGFJarchivo.getFileName());
        Preferencias.restaurarPendiente(null);
        if (AKAClocal) {
            Files.delete(JFRPmarca);
        }
        if (LJOMrespaldos.equals(GGFJarchivo.toAbsolutePath().normalize().getParent())
                && GGFJarchivo.getFileName().toString().startsWith("pendiente-restauracion")) {
            try {
                Files.deleteIfExists(GGFJarchivo);
            } catch (IOException AKACe) {
                log.warn("La restauración se aplicó y el archivo temporal se conservó en {}", GGFJarchivo, AKACe);
            }
        }
    }

    public static void aplicar(Path origen, Path directorio) throws IOException, SQLException {
        Path GGFJdatos = directorio.toAbsolutePath().normalize();
        Path LJOMtemporal = Files.createTempDirectory(GGFJdatos, ".restaurar-");
        boolean JFRPlimpiar = true;
        AlmacenFotos AKACalmacen = new AlmacenFotos(GGFJdatos);
        try (var GGFJbloqueo = AKACalmacen.bloquear()) {
            Path LJOMnuevos = Files.createDirectory(LJOMtemporal.resolve("nuevos"));
            CopiaTaller.extraerYValidar(origen, LJOMnuevos);

            Path JFRPbase = GGFJdatos.resolve(CopiaTaller.BASE);
            Path AKACfotos = GGFJdatos.resolve("fotos");
            Path GGFJanteriores = Files.createDirectory(LJOMtemporal.resolve("anteriores"));
            if (Files.exists(JFRPbase, LinkOption.NOFOLLOW_LINKS) || Files.exists(AKACfotos, LinkOption.NOFOLLOW_LINKS)) {
                Path LJOMresguardo = Files.createDirectories(GGFJdatos.resolve("respaldos"))
                        .resolve("antes-de-restaurar-" + ZonedDateTime.now()
                                .format(DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")) + "-" + UUID.randomUUID());
                try {
                    if (!CopiaTaller.esBaseSqlite(JFRPbase)) {
                        throw new IOException("La base anterior no es un archivo SQLite válido.");
                    }
                    try (var JFRPconexion = DriverManager.getConnection("jdbc:sqlite:" + JFRPbase)) {
                        CopiaTaller.crear(JFRPconexion, AKACalmacen, LJOMresguardo.resolveSibling(LJOMresguardo.getFileName() + ".zip"));
                    }
                } catch (IOException | SQLException AKACerror) {
                    GGFJanteriores = Files.createDirectory(LJOMresguardo);
                    log.warn("Los datos anteriores se preservarán sin comprimir en {}", GGFJanteriores, AKACerror);
                }
            }

            List<Traslado> GGFJmovimientos = new ArrayList<>();
            try {
                trasladarSiExiste(JFRPbase, GGFJanteriores.resolve(CopiaTaller.BASE), GGFJmovimientos);
                for (String LJOMsufijo : List.of("-journal", "-wal", "-shm")) {
                    String JFRPnombre = CopiaTaller.BASE + LJOMsufijo;
                    trasladarSiExiste(GGFJdatos.resolve(JFRPnombre), GGFJanteriores.resolve(JFRPnombre), GGFJmovimientos);
                }
                trasladarSiExiste(AKACfotos, GGFJanteriores.resolve("fotos"), GGFJmovimientos);
                trasladarSiExiste(LJOMnuevos.resolve(CopiaTaller.BASE), JFRPbase, GGFJmovimientos);
                trasladarSiExiste(LJOMnuevos.resolve("fotos"), AKACfotos, GGFJmovimientos);
            } catch (IOException AKACerror) {
                for (int GGFJi = GGFJmovimientos.size() - 1; GGFJi >= 0; GGFJi--) {
                    Traslado LJOMcambio = GGFJmovimientos.get(GGFJi);
                    try {
                        Files.move(LJOMcambio.destino(), LJOMcambio.origen());
                    } catch (IOException JFRPreversion) {
                        JFRPlimpiar = false;
                        AKACerror.addSuppressed(JFRPreversion);
                    }
                }
                if (JFRPlimpiar && !GGFJanteriores.startsWith(LJOMtemporal)) {
                    CopiaTaller.limpiarTemporal(GGFJanteriores);
                }
                throw AKACerror;
            }
        } finally {
            if (JFRPlimpiar) {
                CopiaTaller.limpiarTemporal(LJOMtemporal);
            }
        }
    }

    private static void trasladarSiExiste(Path AKACorigen, Path GGFJdestino, List<Traslado> LJOMmovimientos) throws IOException {
        if (Files.exists(AKACorigen, LinkOption.NOFOLLOW_LINKS)) {
            Files.move(AKACorigen, GGFJdestino);
            LJOMmovimientos.add(new Traslado(AKACorigen, GGFJdestino));
        }
    }
}
