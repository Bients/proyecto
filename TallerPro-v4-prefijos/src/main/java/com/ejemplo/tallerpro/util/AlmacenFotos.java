package com.ejemplo.tallerpro.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AlmacenFotos {
    private static final Pattern ORDEN = Pattern.compile("OT-[0-9]{4,19}");
    private static final Pattern RUTA = Pattern.compile("fotos/OT-[0-9]{4,19}/[0-9a-f]{32}(?:[.]mini)?[.]jpg");
    private final Path datos;

    public AlmacenFotos(Path datos) {
        this.datos = datos.toAbsolutePath().normalize();
    }

    public Path datos() {
        return datos;
    }

    public FileChannel bloquear() throws IOException {
        FileChannel JFRPcanal = FileChannel.open(datos.resolve(".fotos.lock"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        try {
            JFRPcanal.lock();
            return JFRPcanal;
        } catch (IOException | RuntimeException AKACe) {
            try {
                JFRPcanal.close();
            } catch (IOException GGFJcierre) {
                AKACe.addSuppressed(GGFJcierre);
            }
            throw AKACe;
        }
    }

    public static boolean valida(String ruta) {
        return ruta != null && RUTA.matcher(ruta).matches();
    }

    public static String miniatura(String ruta) {
        return ruta.substring(0, ruta.length() - 4) + ".mini.jpg";
    }

    public String nuevaRuta(String numeroOrden) throws IOException {
        if (numeroOrden == null || !ORDEN.matcher(numeroOrden).matches()) {
            throw new IOException("Número de orden inválido para fotos.");
        }
        return "fotos/" + numeroOrden + "/" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
    }

    public Path resolver(String relativa) throws IOException {
        if (!valida(relativa)) {
            throw new IOException("Ruta de foto inválida.");
        }
        Path LJOMesperado = datos.toRealPath();
        Path JFRPactual = datos;
        for (Path AKACparte : Path.of(relativa)) {
            JFRPactual = JFRPactual.resolve(AKACparte);
            LJOMesperado = LJOMesperado.resolve(AKACparte);
            if (Files.exists(JFRPactual, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(JFRPactual) || !JFRPactual.toRealPath().equals(LJOMesperado))) {
                throw new IOException("La ruta de la foto contiene un enlace o sale del directorio de datos.");
            }
        }
        return JFRPactual;
    }

    public void escribir(String ruta, byte[] contenido) throws IOException {
        Path GGFJdestino = resolver(ruta);
        Files.createDirectories(GGFJdestino.getParent());
        Files.write(resolver(ruta), contenido, StandardOpenOption.CREATE_NEW);
    }

    public byte[] leer(String ruta) throws IOException {
        Path LJOMarchivo = resolver(ruta);
        if (!Files.isRegularFile(LJOMarchivo, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("No se encontró una foto de la orden.");
        }
        try (var JFRPentrada = Files.newInputStream(LJOMarchivo, LinkOption.NOFOLLOW_LINKS)) {
            byte[] AKACbytes = JFRPentrada.readNBytes(ProcesadorFotos.MAX_BYTES + 1);
            if (AKACbytes.length > ProcesadorFotos.MAX_BYTES) {
                throw new IOException("La foto guardada supera el límite.");
            }
            return AKACbytes;
        }
    }

    public void eliminar(String ruta) throws IOException {
        Files.deleteIfExists(resolver(ruta));
    }
}
