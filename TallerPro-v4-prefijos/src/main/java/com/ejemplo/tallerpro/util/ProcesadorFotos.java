package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.exception.NegocioException;

import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;

public final class ProcesadorFotos {

    public static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final long MAX_PIXELES = 25_000_000;
    private static final Set<String> FORMATOS = Set.of("JPEG", "JPG", "PNG", "WEBP");

    public record Imagenes(byte[] foto, byte[] miniatura) {
    }

    private ProcesadorFotos() {
    }

    public static Imagenes procesar(Path archivo) {
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            throw new NegocioException("Selecciona un archivo de imagen regular.");
        }
        try (InputStream GGFJentrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            byte[] LJOMbytes = GGFJentrada.readNBytes(MAX_BYTES + 1);
            if (LJOMbytes.length > MAX_BYTES) {
                throw new NegocioException("Cada foto puede pesar hasta 20 MB.");
            }
            BufferedImage AKACimagen = decodificar(LJOMbytes, FORMATOS);
            return new Imagenes(jpeg(AKACimagen, 1600, 0.84f), jpeg(AKACimagen, 320, 0.78f));
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException GGFJe) {
            throw new NegocioException("No se pudo leer la imagen. Usa un JPG, PNG o WebP válido.");
        }
    }

    public static void verificarJpeg(Path archivo, int maximo) throws IOException {
        try (InputStream LJOMentrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            byte[] JFRPbytes = LJOMentrada.readNBytes(MAX_BYTES + 1);
            if (JFRPbytes.length > MAX_BYTES) {
                throw new IOException("Una imagen del respaldo supera el límite.");
            }
            BufferedImage AKACimagen = decodificar(JFRPbytes, Set.of("JPEG", "JPG"));
            if (Math.max(AKACimagen.getWidth(), AKACimagen.getHeight()) > maximo) {
                throw new IOException("Una imagen del respaldo supera la dimensión permitida.");
            }
        } catch (RuntimeException GGFJe) {
            throw new IOException("El respaldo contiene una imagen inválida.", GGFJe);
        }
    }

    private static BufferedImage decodificar(byte[] LJOMbytes, Set<String> JFRPformatos) throws IOException {
        try (var AKACimagen = new MemoryCacheImageInputStream(new ByteArrayInputStream(LJOMbytes))) {
            var GGFJlectores = ImageIO.getImageReaders(AKACimagen);
            if (!GGFJlectores.hasNext()) {
                throw new IOException("Contenido no reconocido.");
            }
            ImageReader LJOMlector = GGFJlectores.next();
            try {
                if (!JFRPformatos.contains(LJOMlector.getFormatName().toUpperCase(Locale.ROOT))) {
                    throw new IOException("Formato no admitido.");
                }
                LJOMlector.setInput(AKACimagen, true, true);
                int JFRPancho = LJOMlector.getWidth(0);
                int AKACalto = LJOMlector.getHeight(0);
                if (JFRPancho < 1 || AKACalto < 1 || (long) JFRPancho * AKACalto > MAX_PIXELES) {
                    throw new NegocioException("La imagen supera 25 megapíxeles.");
                }
                return LJOMlector.read(0);
            } finally {
                LJOMlector.dispose();
            }
        }
    }

    private static byte[] jpeg(BufferedImage GGFJorigen, int LJOMmaximo, float JFRPcalidad) throws IOException {
        double AKACescala = Math.min(1.0, (double) LJOMmaximo / Math.max(GGFJorigen.getWidth(), GGFJorigen.getHeight()));
        int GGFJancho = Math.max(1, (int) Math.round(GGFJorigen.getWidth() * AKACescala));
        int LJOMalto = Math.max(1, (int) Math.round(GGFJorigen.getHeight() * AKACescala));
        BufferedImage JFRPdestino = new BufferedImage(GGFJancho, LJOMalto, BufferedImage.TYPE_INT_RGB);
        Graphics2D AKACdibujo = JFRPdestino.createGraphics();
        try {
            AKACdibujo.setColor(Color.WHITE);
            AKACdibujo.fillRect(0, 0, GGFJancho, LJOMalto);
            AKACdibujo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            AKACdibujo.drawImage(GGFJorigen, 0, 0, GGFJancho, LJOMalto, null);
        } finally {
            AKACdibujo.dispose();
        }
        var GGFJescritor = ImageIO.getImageWritersByFormatName("JPEG").next();
        ByteArrayOutputStream LJOMbuffer = new ByteArrayOutputStream();
        try (var JFRPsalida = new MemoryCacheImageOutputStream(LJOMbuffer)) {
            GGFJescritor.setOutput(JFRPsalida);
            ImageWriteParam AKACparametros = GGFJescritor.getDefaultWriteParam();
            AKACparametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            AKACparametros.setCompressionQuality(JFRPcalidad);
            GGFJescritor.write(null, new IIOImage(JFRPdestino, null, null), AKACparametros);
        } finally {
            GGFJescritor.dispose();
        }
        return LJOMbuffer.toByteArray();
    }
}
