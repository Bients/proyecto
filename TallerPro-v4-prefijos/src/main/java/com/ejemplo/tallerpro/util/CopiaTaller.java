package com.ejemplo.tallerpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public final class CopiaTaller {

    public static final String BASE = "tallerpro-v2.db";
    private static final long LIMITE_BASE = 2L * 1024 * 1024 * 1024;
    public static final long LIMITE_TOTAL = 16L * 1024 * 1024 * 1024;

    private CopiaTaller() {
    }

    public static void crear(Connection conexion, AlmacenFotos fotos, Path destino) throws IOException, SQLException {
        Path LJOMabsoluto = destino.toAbsolutePath();
        Path JFRPtemporal = Files.createTempDirectory(LJOMabsoluto.getParent(), ".respaldo-");
        try {
            Path AKACsnapshot = JFRPtemporal.resolve(BASE);
            try (var GGFJconsulta = conexion.prepareStatement("VACUUM INTO ?")) {
                GGFJconsulta.setString(1, AKACsnapshot.toString());
                GGFJconsulta.execute();
            }
            Path LJOMarchivo = JFRPtemporal.resolve("taller.zip");
            try (var JFRPcopia = DriverManager.getConnection("jdbc:sqlite:" + AKACsnapshot);
                 var AKACzip = new ZipOutputStream(Files.newOutputStream(LJOMarchivo))) {
                long GGFJtotal = Files.size(AKACsnapshot);
                if (GGFJtotal > LIMITE_BASE) {
                    throw new IOException("La base supera el tamaño admitido para el respaldo.");
                }
                agregar(AKACzip, BASE, AKACsnapshot);
                for (String LJOMruta : rutasEsperadas(JFRPcopia, false)) {
                    Path JFRPfoto = fotos.resolver(LJOMruta);
                    long AKACbytes = Files.size(JFRPfoto);
                    GGFJtotal += AKACbytes;
                    if (AKACbytes > ProcesadorFotos.MAX_BYTES || GGFJtotal > LIMITE_TOTAL) {
                        throw new IOException("El respaldo supera el tamaño admitido.");
                    }
                    agregar(AKACzip, LJOMruta, JFRPfoto);
                }
            }
            publicar(LJOMarchivo, LJOMabsoluto);
        } finally {
            limpiarTemporal(JFRPtemporal);
        }
    }

    public static void extraerYValidar(Path archivo, Path staging) throws IOException, SQLException {
        Set<String> GGFJencontradas = new HashSet<>();
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El respaldo no es un archivo regular.");
        }
        if (esBaseSqlite(archivo)) {
            try (var LJOMentrada = Files.newInputStream(archivo)) {
                copiarLimitado(LJOMentrada, staging.resolve(BASE), LIMITE_BASE);
            }
        } else {
            try (var JFRPzip = new ZipInputStream(Files.newInputStream(archivo))) {
                ZipEntry AKACentrada;
                long GGFJtotal = 0;
                while ((AKACentrada = JFRPzip.getNextEntry()) != null) {
                    String LJOMnombre = AKACentrada.getName();
                    boolean JFRPbase = BASE.equals(LJOMnombre);
                    if (AKACentrada.isDirectory() || !(JFRPbase || AlmacenFotos.valida(LJOMnombre)) || !GGFJencontradas.add(LJOMnombre)) {
                        throw new IOException("El ZIP contiene una ruta inválida o duplicada.");
                    }
                    Path AKACsalida = staging.resolve(LJOMnombre);
                    Files.createDirectories(AKACsalida.getParent());
                    GGFJtotal += copiarLimitado(JFRPzip, AKACsalida, Math.min(LIMITE_TOTAL - GGFJtotal,
                            JFRPbase ? LIMITE_BASE : ProcesadorFotos.MAX_BYTES));
                    JFRPzip.closeEntry();
                }
            }
        }

        Path GGFJbase = staging.resolve(BASE);
        if (!esBaseSqlite(GGFJbase)) {
            throw new IOException("El respaldo no contiene una base SQLite válida.");
        }
        try (var LJOMconexion = DriverManager.getConnection("jdbc:sqlite:" + GGFJbase);
             var JFRPconsulta = LJOMconexion.createStatement()) {
            int AKACversion;
            try (var GGFJfila = JFRPconsulta.executeQuery("PRAGMA user_version")) {
                AKACversion = GGFJfila.next() ? GGFJfila.getInt(1) : -1;
                if (AKACversion < 0 || AKACversion > MigracionesSqlite.versionSoportada()) {
                    throw new IOException("La base usa una versión incompatible.");
                }
            }
            try (var LJOMintegridad = JFRPconsulta.executeQuery("PRAGMA integrity_check")) {
                if (!LJOMintegridad.next() || !"ok".equals(LJOMintegridad.getString(1))) {
                    throw new IOException("La base respaldada presenta errores de integridad.");
                }
            }
            try (var JFRPrelaciones = JFRPconsulta.executeQuery("PRAGMA foreign_key_check")) {
                if (JFRPrelaciones.next()) {
                    throw new IOException("La base respaldada contiene relaciones inválidas.");
                }
            }
            try (var AKACtablas = JFRPconsulta.executeQuery("""
                    SELECT count(*) FROM sqlite_schema WHERE type = 'table'
                    AND name IN ('orden_trabajo','cliente','vehiculo','repuesto','usuario','detalle_orden',
                                 'programa_mantenimiento')
                    """)) {
                if (!AKACtablas.next() || AKACtablas.getInt(1) != 7) {
                    throw new IOException("La base no corresponde a TallerPro.");
                }
            }
            if (AKACversion >= 4) {
                exigirTablas(JFRPconsulta, "proveedor", "compra", "compra_detalle", "movimiento_inventario");
            }
            if (AKACversion >= 6) {
                exigirTablas(JFRPconsulta, "pago", "entrega_con_saldo");
            }
            if (AKACversion >= 8) {
                try (var GGFJcolumnas = JFRPconsulta.executeQuery(
                        "SELECT intentos_fallidos, bloqueado_hasta FROM usuario LIMIT 0")) {
                    if (GGFJcolumnas.getMetaData().getColumnCount() != 2) {
                        throw new IOException("El esquema de usuarios es incompatible.");
                    }
                }
            }
            if (AKACversion >= 9) {
                exigirTablas(JFRPconsulta, "hallazgo_vehiculo", "hallazgo_evento");
            }
            Set<String> LJOMesperadas = rutasEsperadas(LJOMconexion, AKACversion >= 7);
            GGFJencontradas.remove(BASE);
            if (!LJOMesperadas.equals(GGFJencontradas)) {
                throw new IOException("Al respaldo le faltan fotos o contiene archivos que no pertenecen a la base.");
            }
            for (String JFRPruta : LJOMesperadas) {
                ProcesadorFotos.verificarJpeg(staging.resolve(JFRPruta), JFRPruta.endsWith(".mini.jpg") ? 320 : 1600);
            }
        }
    }

    private static void exigirTablas(Statement AKACconsulta, String... GGFJnombres) throws SQLException, IOException {
        Set<String> LJOMtablas = new HashSet<>();
        try (var JFRPfilas = AKACconsulta.executeQuery("SELECT name FROM sqlite_schema WHERE type = 'table'")) {
            while (JFRPfilas.next()) {
                LJOMtablas.add(JFRPfilas.getString(1));
            }
        }
        if (!LJOMtablas.containsAll(List.of(GGFJnombres))) {
            throw new IOException("Faltan tablas requeridas por la versión de la base.");
        }
    }

    private static Set<String> rutasEsperadas(Connection AKACconexion, boolean GGFJrequiereFotos)
            throws SQLException, IOException {
        Set<String> LJOMresultado = new LinkedHashSet<>();
        try (var JFRPconsulta = AKACconexion.createStatement()) {
            try (var AKACtablas = JFRPconsulta.executeQuery("SELECT 1 FROM sqlite_schema WHERE type='table' AND name='orden_foto'")) {
                if (!AKACtablas.next()) {
                    if (GGFJrequiereFotos) {
                        throw new IOException("La base no contiene la tabla de fotos de su versión.");
                    }
                    return LJOMresultado;
                }
            }
            try (var GGFJfilas = JFRPconsulta.executeQuery("""
                    SELECT f.ruta_relativa, o.numero_orden FROM orden_foto f
                    LEFT JOIN orden_trabajo o ON o.id = f.orden_id ORDER BY f.id
                    """)) {
                while (GGFJfilas.next()) {
                    String LJOMruta = GGFJfilas.getString(1);
                    if (!AlmacenFotos.valida(LJOMruta) || LJOMruta.endsWith(".mini.jpg")
                            || !LJOMruta.startsWith("fotos/" + GGFJfilas.getString(2) + "/") || !LJOMresultado.add(LJOMruta)) {
                        throw new IOException("La base contiene una referencia de foto inválida.");
                    }
                    LJOMresultado.add(AlmacenFotos.miniatura(LJOMruta));
                }
            }
        }
        return LJOMresultado;
    }

    private static long copiarLimitado(InputStream JFRPentrada, Path AKACsalida, long GGFJlimite) throws IOException {
        if (GGFJlimite < 1) {
            throw new IOException("El respaldo supera el tamaño admitido.");
        }
        try (var LJOMarchivo = Files.newOutputStream(AKACsalida, StandardOpenOption.CREATE_NEW)) {
            long JFRPtotal = 0;
            byte[] AKACbuffer = new byte[64 * 1024];
            int GGFJleidos;
            while ((GGFJleidos = JFRPentrada.read(AKACbuffer)) != -1) {
                JFRPtotal += GGFJleidos;
                if (JFRPtotal > GGFJlimite) {
                    throw new IOException("El respaldo supera el tamaño admitido.");
                }
                LJOMarchivo.write(AKACbuffer, 0, GGFJleidos);
            }
            return JFRPtotal;
        }
    }

    private static void agregar(ZipOutputStream LJOMzip, String JFRPnombre, Path AKACorigen) throws IOException {
        if (!Files.isRegularFile(AKACorigen, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Falta un archivo asociado a una foto.");
        }
        LJOMzip.putNextEntry(new ZipEntry(JFRPnombre));
        try (var GGFJentrada = Files.newInputStream(AKACorigen, LinkOption.NOFOLLOW_LINKS)) {
            GGFJentrada.transferTo(LJOMzip);
        }
        LJOMzip.closeEntry();
    }

    public static void publicar(Path temporal, Path destino) throws IOException {
        try {
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException LJOMe) {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void limpiarTemporal(Path temporal) {
        if (Files.notExists(temporal, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var JFRPrutas = Files.walk(temporal)) {
            for (Path AKACruta : JFRPrutas.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(AKACruta);
            }
        } catch (IOException GGFJe) {
            log.warn("No se pudo limpiar la carpeta temporal {}", temporal, GGFJe);
        }
    }

    public static boolean esBaseSqlite(Path archivo) {
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (var LJOMentrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            return "SQLite format 3\0".equals(new String(LJOMentrada.readNBytes(16), StandardCharsets.US_ASCII));
        } catch (IOException JFRPe) {
            return false;
        }
    }
}
