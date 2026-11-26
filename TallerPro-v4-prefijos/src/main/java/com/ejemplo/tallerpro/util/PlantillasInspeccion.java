package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista;
import com.ejemplo.tallerpro.model.HallazgoVehiculo;
import com.ejemplo.tallerpro.model.TipoCarroceria;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlantillasInspeccion {
    public static final int ANCHO = 480;
    public static final int ALTO = 280;
    private static final Map<TipoCarroceria, Map<Vista, Plantilla>> CACHE = new ConcurrentHashMap<>();

    public record Trazo(String clase, String svg) {
    }

    public record Plantilla(boolean espejo, List<Trazo> trazos) {
    }

    public record Marca(int numero, HallazgoVehiculo hallazgo) {
        public double x() {
            return hallazgo.getX() == null ? hallazgo.getZona().enVista(hallazgo.getVista()).x() : hallazgo.getX();
        }

        public double y() {
            return hallazgo.getY() == null ? hallazgo.getZona().enVista(hallazgo.getVista()).y() : hallazgo.getY();
        }
    }

    public record UbicacionMarca(Marca marca, double x, double y, double anclaX, double anclaY) {
    }

    public static List<UbicacionMarca> ubicar(Vista vista, List<Marca> marcas) {
        List<UbicacionMarca> GGFJresultado = new ArrayList<>();
        for (Marca LJOMmarca : marcas) {
            if (LJOMmarca.hallazgo().getVista() != vista) {
                continue;
            }
            double JFRPax = LJOMmarca.x() * ANCHO;
            double AKACay = LJOMmarca.y() * ALTO;
            double GGFJx = limitar(JFRPax, ANCHO);
            double LJOMy = limitar(AKACay, ALTO);
            int JFRPintento = 0;
            while (ocupa(GGFJresultado, GGFJx, LJOMy) && JFRPintento < 72) {
                double AKACradio = 34 * (1 + JFRPintento / 8);
                double GGFJangulo = (JFRPintento % 8) * Math.PI / 4;
                GGFJx = limitar(JFRPax + Math.cos(GGFJangulo) * AKACradio, ANCHO);
                LJOMy = limitar(AKACay + Math.sin(GGFJangulo) * AKACradio, ALTO);
                JFRPintento++;
            }
            GGFJresultado.add(new UbicacionMarca(LJOMmarca, GGFJx, LJOMy, JFRPax, AKACay));
        }
        return List.copyOf(GGFJresultado);
    }

    private static boolean ocupa(List<UbicacionMarca> LJOMmarcas, double JFRPx, double AKACy) {
        return LJOMmarcas.stream().anyMatch(GGFJm -> Math.abs(GGFJm.x() - JFRPx) < 31 && Math.abs(GGFJm.y() - AKACy) < 31);
    }

    private static double limitar(double LJOMpunto, int JFRPdimension) {
        return Math.max(15, Math.min(JFRPdimension - 15, LJOMpunto));
    }

    private PlantillasInspeccion() {
    }

    public static Plantilla obtener(TipoCarroceria tipo, Vista vista) {
        return CACHE.computeIfAbsent(tipo, PlantillasInspeccion::leer).get(vista);
    }

    private static Map<Vista, Plantilla> leer(TipoCarroceria AKACtipo) {
        String GGFJruta = "/inspeccion/" + AKACtipo.name().toLowerCase(Locale.ROOT) + ".svg";
        try (var LJOMarchivo = PlantillasInspeccion.class.getResourceAsStream(GGFJruta)) {
            if (LJOMarchivo == null) {
                throw new IOException("Falta " + GGFJruta);
            }
            var JFRPfabrica = DocumentBuilderFactory.newInstance();
            JFRPfabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            JFRPfabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            JFRPfabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var AKACsvg = JFRPfabrica.newDocumentBuilder().parse(LJOMarchivo).getDocumentElement();
            Map<Vista, Plantilla> GGFJresultado = new EnumMap<>(Vista.class);
            var LJOMgrupos = AKACsvg.getElementsByTagName("g");
            for (int JFRPi = 0; JFRPi < LJOMgrupos.getLength(); JFRPi++) {
                Element AKACgrupo = (Element) LJOMgrupos.item(JFRPi);
                Vista GGFJvista = Vista.valueOf(AKACgrupo.getAttribute("id"));
                List<Trazo> LJOMtrazos = new ArrayList<>();
                var JFRPcaminos = AKACgrupo.getElementsByTagName("path");
                for (int AKACj = 0; AKACj < JFRPcaminos.getLength(); AKACj++) {
                    Element GGFJcamino = (Element) JFRPcaminos.item(AKACj);
                    LJOMtrazos.add(new Trazo(GGFJcamino.getAttribute("class"), GGFJcamino.getAttribute("d")));
                }
                GGFJresultado.put(GGFJvista, new Plantilla(AKACgrupo.hasAttribute("transform"), List.copyOf(LJOMtrazos)));
            }
            if (GGFJresultado.size() != Vista.values().length || GGFJresultado.values().stream().anyMatch(LJOMp -> LJOMp.trazos().isEmpty())) {
                throw new IOException("La plantilla no contiene las seis vistas.");
            }
            return Map.copyOf(GGFJresultado);
        } catch (Exception JFRPe) {
            throw new IllegalStateException("No se pudo cargar la plantilla " + GGFJruta, JFRPe);
        }
    }

    public static List<Marca> numerar(List<HallazgoVehiculo> hallazgos) {
        List<Marca> AKACmarcas = new ArrayList<>(hallazgos.size());
        for (int GGFJi = 0; GGFJi < hallazgos.size(); GGFJi++) {
            AKACmarcas.add(new Marca(GGFJi + 1, hallazgos.get(GGFJi)));
        }
        return List.copyOf(AKACmarcas);
    }

    public static byte[] imagenPdf(TipoCarroceria tipo, Vista vista, List<Marca> marcas) throws IOException {
        BufferedImage LJOMimagen = new BufferedImage(ANCHO * 2, ALTO * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D JFRPg = LJOMimagen.createGraphics();
        try {
            JFRPg.scale(2, 2);
            JFRPg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            JFRPg.setColor(Color.WHITE);
            JFRPg.fillRect(0, 0, ANCHO, ALTO);
            var AKACplantilla = obtener(tipo, vista);
            AffineTransform GGFJoriginal = JFRPg.getTransform();
            if (AKACplantilla.espejo()) {
                JFRPg.translate(ANCHO, 0);
                JFRPg.scale(-1, 1);
            }
            JFRPg.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Trazo LJOMtrazo : AKACplantilla.trazos()) {
                Path2D JFRPforma = camino(LJOMtrazo.svg());
                Color AKACcolor = switch (LJOMtrazo.clase()) {
                    case "carroceria" -> new Color(235, 240, 246);
                    case "vidrio" -> new Color(183, 206, 226);
                    case "rueda" -> new Color(94, 107, 123);
                    default -> null;
                };
                if (AKACcolor != null) {
                    JFRPg.setColor(AKACcolor);
                    JFRPg.fill(JFRPforma);
                }
                JFRPg.setColor(new Color(48, 62, 78));
                JFRPg.draw(JFRPforma);
            }
            JFRPg.setTransform(GGFJoriginal);
            JFRPg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            for (UbicacionMarca GGFJpunto : ubicar(vista, marcas)) {
                int LJOMx = (int) Math.round(GGFJpunto.x());
                int JFRPy = (int) Math.round(GGFJpunto.y());
                String AKACnumero = Integer.toString(GGFJpunto.marca().numero());
                int GGFJancho = Math.max(24, JFRPg.getFontMetrics().stringWidth(AKACnumero) + 12);
                JFRPg.setColor(new Color(155, 52, 15));
                JFRPg.drawLine((int) GGFJpunto.anclaX(), (int) GGFJpunto.anclaY(), LJOMx, JFRPy);
                JFRPg.fillRoundRect(LJOMx - GGFJancho / 2, JFRPy - 12, GGFJancho, 24, 16, 16);
                JFRPg.setColor(Color.WHITE);
                JFRPg.drawString(AKACnumero, LJOMx - JFRPg.getFontMetrics().stringWidth(AKACnumero) / 2, JFRPy + 5);
            }
        } finally {
            JFRPg.dispose();
        }
        ByteArrayOutputStream LJOMsalida = new ByteArrayOutputStream();
        ImageIO.write(LJOMimagen, "png", LJOMsalida);
        return LJOMsalida.toByteArray();
    }

    private static Path2D camino(String JFRPsvg) {
        Path2D AKACfigura = new Path2D.Double();
        String[] GGFJpartes = JFRPsvg.strip().split("\\s+");
        for (int LJOMi = 0; LJOMi < GGFJpartes.length; ) {
            switch (GGFJpartes[LJOMi++]) {
                case "M" -> AKACfigura.moveTo(numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]));
                case "L" -> AKACfigura.lineTo(numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]));
                case "Q" -> AKACfigura.quadTo(numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]));
                case "C" -> AKACfigura.curveTo(numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]),
                        numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]), numero(GGFJpartes[LJOMi++]));
                case "Z" -> AKACfigura.closePath();
                default -> throw new IllegalArgumentException("Comando de plantilla SVG no reconocido.");
            }
        }
        return AKACfigura;
    }

    private static double numero(String JFRPvalor) {
        return Double.parseDouble(JFRPvalor);
    }
}
