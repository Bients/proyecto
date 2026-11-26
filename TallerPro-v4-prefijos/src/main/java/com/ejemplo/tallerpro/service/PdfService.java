package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.model.DatosOrden;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Pago;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.util.Ui;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Genera la orden de trabajo en PDF (cotizacion / comprobante) con un codigo QR de resumen. */
@Service
@RequiredArgsConstructor
public class PdfService {

    private static final Color NARANJA = new Color(249, 115, 22);
    private static final Color OSCURO = new Color(15, 23, 42);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color FONDO = new Color(241, 245, 249);

    @Value("${taller.nombre:TallerPro}")
    private String nombreTaller;

    private final OrdenService ordenes;
    private final PagoService pagos;
    private final OrdenFotoService fotos;
    private final HallazgoService hallazgos;
    private final SesionActual sesion;
    private static final DateTimeFormatter FECHA_PAGO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    @Transactional(readOnly = true)
    public void generarOrden(Long id, Path destino) throws IOException {
        generarOrden(id, destino, false);
    }

    @Transactional(readOnly = true)
    public void generarOrden(Long id, Path destino, boolean incluirFotos) throws IOException {
        sesion.exigirAdmin();
        OrdenTrabajo AKACorden = ordenes.buscar(id);
        List<Pago> JFRPabonos = pagos.listar(id);
        Path AKACabsoluto = destino.toAbsolutePath();
        Path GGFJtemporal = Files.createTempFile(AKACabsoluto.getParent(), ".orden-", ".pdf");
        try {
            escribirPdf(id, incluirFotos, AKACorden, JFRPabonos, GGFJtemporal);
            com.ejemplo.tallerpro.util.CopiaTaller.publicar(GGFJtemporal, AKACabsoluto);
        } finally {
            Files.deleteIfExists(GGFJtemporal);
        }
    }

    private void escribirPdf(Long LJOMid, boolean JFRPincluirFotos, OrdenTrabajo AKACorden, List<Pago> GGFJabonos, Path LJOMdestino)
            throws IOException {
        try (OutputStream JFRPsalida = Files.newOutputStream(LJOMdestino)) {
            Document AKACdoc = new Document(PageSize.A4, 40, 40, 40, 150);
            PdfWriter GGFJwriter = PdfWriter.getInstance(AKACdoc, JFRPsalida);
            AKACdoc.open();

            Font LJOMfTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.BOLD, NARANJA);
            Font JFRPfSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, GRIS);
            Font AKACfSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, OSCURO);
            Font GGFJfEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, GRIS);
            Font LJOMfTexto = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, OSCURO);
            Font JFRPfCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.WHITE);
            Font AKACfTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, OSCURO);

            Paragraph GGFJtitulo = new Paragraph(nombreTaller, LJOMfTitulo);
            AKACdoc.add(GGFJtitulo);
            AKACdoc.add(new Paragraph("ORDEN DE TRABAJO " + AKACorden.getNumeroOrden()
                    + "   |   Ingreso: " + AKACorden.getFechaIngreso(), JFRPfSub));
            AKACdoc.add(new Paragraph(" "));

            DatosOrden LJOMdatos = AKACorden.getDatosHistoricos();

            AKACdoc.add(new Paragraph("Datos del cliente y del vehículo", AKACfSeccion));
            PdfPTable JFRPinfo = new PdfPTable(new float[]{1.2f, 2.8f, 1.2f, 2.8f});
            JFRPinfo.setWidthPercentage(100);
            JFRPinfo.setSpacingBefore(6);
            fila(JFRPinfo, "Cliente", LJOMdatos.getClienteNombre(), "DNI", LJOMdatos.getClienteDni(), GGFJfEtiqueta, LJOMfTexto);
            fila(JFRPinfo, "Teléfono", Ui.texto(LJOMdatos.getClienteTelefono()), "Correo", Ui.texto(LJOMdatos.getClienteCorreo()), GGFJfEtiqueta, LJOMfTexto);
            fila(JFRPinfo, "Placa", LJOMdatos.getPlaca(), "Vehículo", LJOMdatos.getDescripcionVehiculo(), GGFJfEtiqueta, LJOMfTexto);
            fila(JFRPinfo, "Kilometraje", AKACorden.getKilometraje() == null ? "-" : AKACorden.getKilometraje() + " km",
                    "Estado", AKACorden.getEstado().toString(), GGFJfEtiqueta, LJOMfTexto);
            fila(JFRPinfo, "Mecánico", Ui.texto(AKACorden.getMecanico()), "", "", GGFJfEtiqueta, LJOMfTexto);
            AKACdoc.add(JFRPinfo);

            AKACdoc.add(new Paragraph(" "));
            AKACdoc.add(new Paragraph("Problema reportado", AKACfSeccion));
            Paragraph AKACproblema = new Paragraph(AKACorden.getProblema(), LJOMfTexto);
            AKACproblema.setSpacingBefore(4);
            AKACdoc.add(AKACproblema);

            AKACdoc.add(new Paragraph(" "));
            AKACdoc.add(new Paragraph("Servicios y repuestos", AKACfSeccion));
            PdfPTable GGFJdetalle = new PdfPTable(new float[]{1.3f, 4.2f, 0.9f, 1.5f, 1.6f});
            GGFJdetalle.setWidthPercentage(100);
            GGFJdetalle.setSpacingBefore(6);
            for (String LJOMcab : new String[]{"Tipo", "Descripción", "Cant.", "P. unit.", "Subtotal"}) {
                PdfPCell JFRPcelda = new PdfPCell(new Phrase(LJOMcab, JFRPfCabecera));
                JFRPcelda.setBackgroundColor(OSCURO);
                JFRPcelda.setPadding(6);
                GGFJdetalle.addCell(JFRPcelda);
            }
            boolean AKACalterna = false;
            for (DetalleOrden GGFJd : AKACorden.getDetalles()) {
                Color LJOMfondo = AKACalterna ? FONDO : Color.WHITE;
                celdaDetalle(GGFJdetalle, GGFJd.getTipo().toString(), LJOMfTexto, LJOMfondo, Element.ALIGN_LEFT);
                celdaDetalle(GGFJdetalle, GGFJd.getDescripcion(), LJOMfTexto, LJOMfondo, Element.ALIGN_LEFT);
                celdaDetalle(GGFJdetalle, GGFJd.getTipo() == TipoDetalle.SERVICIO ? "-" : String.valueOf(GGFJd.getCantidad()),
                        LJOMfTexto, LJOMfondo, Element.ALIGN_CENTER);
                celdaDetalle(GGFJdetalle, Ui.moneda(GGFJd.getPrecioUnitario()), LJOMfTexto, LJOMfondo, Element.ALIGN_RIGHT);
                celdaDetalle(GGFJdetalle, Ui.moneda(GGFJd.getSubtotal()), LJOMfTexto, LJOMfondo, Element.ALIGN_RIGHT);
                AKACalterna = !AKACalterna;
            }
            if (AKACorden.getDetalles().isEmpty()) {
                PdfPCell JFRPvacio = new PdfPCell(new Phrase("Sin servicios ni repuestos registrados.", JFRPfSub));
                JFRPvacio.setColspan(5);
                JFRPvacio.setPadding(8);
                GGFJdetalle.addCell(JFRPvacio);
            }
            AKACdoc.add(GGFJdetalle);

            Paragraph AKACdesglose = new Paragraph("Subtotal " + Ui.moneda(AKACorden.getSubtotal())
                    + "   |   Descuento " + Ui.moneda(AKACorden.getDescuento()), LJOMfTexto);
            AKACdesglose.setAlignment(Element.ALIGN_RIGHT);
            AKACdesglose.setSpacingBefore(8);
            AKACdoc.add(AKACdesglose);
            Paragraph GGFJtotal = new Paragraph("TOTAL: " + Ui.moneda(AKACorden.getTotal()), AKACfTotal);
            GGFJtotal.setAlignment(Element.ALIGN_RIGHT);
            GGFJtotal.setSpacingBefore(10);
            AKACdoc.add(GGFJtotal);

            AKACdoc.add(new Paragraph("Abonos", AKACfSeccion));
            if (GGFJabonos.isEmpty()) {
                AKACdoc.add(new Paragraph("Sin pagos registrados.", LJOMfTexto));
            }
            for (Pago LJOMpago : GGFJabonos) {
                String JFRPestado = LJOMpago.isAnulado() ? " (anulado)" : "";
                AKACdoc.add(new Paragraph(FECHA_PAGO.format(Instant.parse(LJOMpago.getFecha())) + "   "
                        + LJOMpago.getMetodo() + "   " + Ui.moneda(LJOMpago.getMonto()) + JFRPestado, LJOMfTexto));
            }
            AKACdoc.add(new Paragraph("Total cobrado: " + Ui.moneda(AKACorden.getTotalCobrado()), LJOMfTexto));
            AKACdoc.add(new Paragraph("Saldo pendiente: " + Ui.moneda(AKACorden.getSaldoPendiente()), AKACfTotal));
            if (AKACorden.getSaldoAFavor().signum() > 0) {
                AKACdoc.add(new Paragraph("Saldo a favor: " + Ui.moneda(AKACorden.getSaldoAFavor()), LJOMfTexto));
            }

            // Codigo QR con el resumen de la orden (zona reservada al pie de la pagina)
            dibujarQr(GGFJwriter.getDirectContent(), resumenQr(AKACorden), 40, 45, 90, JFRPfSub);

            agregarInspeccion(AKACdoc, AKACorden, LJOMfTitulo, AKACfSeccion, JFRPfCabecera, LJOMfTexto);
            if (JFRPincluirFotos) {
                var AKACadjuntos = fotos.listar(LJOMid);
                if (!AKACadjuntos.isEmpty()) {
                    AKACdoc.newPage();
                    AKACdoc.add(new Paragraph("Fotos de la orden", LJOMfTitulo));
                    for (var GGFJfoto : AKACadjuntos) {
                        String LJOMnota = GGFJfoto.getNota().isBlank() ? "" : " · " + GGFJfoto.getNota();
                        AKACdoc.add(new Paragraph(GGFJfoto.getFase() + LJOMnota, AKACfSeccion));
                        Image JFRPimagen = Image.getInstance(fotos.original(LJOMid, GGFJfoto.getId()));
                        JFRPimagen.scaleToFit(490, 385);
                        JFRPimagen.setSpacingAfter(10);
                        AKACdoc.add(JFRPimagen);
                    }
                }
            }
            AKACdoc.close();
        } catch (DocumentException | WriterException AKACe) {
            throw new IOException("No se pudo generar el PDF: " + AKACe.getMessage(), AKACe);
        }
    }

    private void agregarInspeccion(Document GGFJdoc, OrdenTrabajo LJOMorden, Font JFRPtitulo, Font AKACseccion, Font GGFJcabecera,
                                  Font LJOMtexto) throws IOException, DocumentException {
        var JFRPmarcas = com.ejemplo.tallerpro.util.PlantillasInspeccion.numerar(hallazgos.paraPdf(LJOMorden.getId()));
        if (JFRPmarcas.isEmpty()) {
            return;
        }
        var AKACtipo = LJOMorden.getEstado() == com.ejemplo.tallerpro.model.EstadoOrden.ENTREGADO
                && LJOMorden.getCarroceriaCierre() != null
                ? LJOMorden.getCarroceriaCierre() : LJOMorden.getVehiculo().getTipoCarroceria();
        GGFJdoc.newPage();
        GGFJdoc.add(new Paragraph("Inspección del vehículo", JFRPtitulo));
        GGFJdoc.add(new Paragraph(LJOMorden.getNumeroOrden() + " · " + LJOMorden.getDatosHistoricos().getPlaca() + " · " + AKACtipo, LJOMtexto));
        PdfPTable GGFJdiagramas = new PdfPTable(2);
        GGFJdiagramas.setWidthPercentage(100);
        GGFJdiagramas.setSpacingBefore(8);
        int LJOMvistas = 0;
        for (var JFRPvista : com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista.values()) {
            if (JFRPmarcas.stream().noneMatch(AKACm -> AKACm.hallazgo().getVista() == JFRPvista)) {
                continue;
            }
            var GGFJimagen = Image.getInstance(com.ejemplo.tallerpro.util.PlantillasInspeccion.imagenPdf(AKACtipo, JFRPvista, JFRPmarcas));
            GGFJimagen.scaleToFit(230, 126);
            var LJOMcelda = new PdfPCell();
            LJOMcelda.setPadding(5);
            LJOMcelda.setBorderColor(new Color(226, 232, 240));
            LJOMcelda.addElement(new Paragraph(JFRPvista.toString(), AKACseccion));
            LJOMcelda.addElement(GGFJimagen);
            GGFJdiagramas.addCell(LJOMcelda);
            LJOMvistas++;
        }
        if (LJOMvistas % 2 != 0) {
            GGFJdiagramas.addCell(sinBorde("", LJOMtexto));
        }
        GGFJdoc.add(GGFJdiagramas);
        PdfPTable JFRPfilas = new PdfPTable(new float[]{.44f, 1.75f, 1.10f, 1.25f, 2.2f});
        JFRPfilas.setWidthPercentage(100);
        JFRPfilas.setSpacingBefore(12);
        JFRPfilas.setHeaderRows(1);
        for (String AKACnombre : new String[]{"N.º", "Zona y vista", "Gravedad", "Resultado", "Hallazgo y nota"}) {
            var GGFJcelda = new PdfPCell(new Phrase(AKACnombre, GGFJcabecera));
            GGFJcelda.setBackgroundColor(OSCURO);
            GGFJcelda.setPadding(5);
            JFRPfilas.addCell(GGFJcelda);
        }
        for (var LJOMmarca : JFRPmarcas) {
            var JFRPh = LJOMmarca.hallazgo();
            Color AKACfondo = LJOMmarca.numero() % 2 == 0 ? FONDO : Color.WHITE;
            for (String GGFJvalor : new String[]{Integer.toString(LJOMmarca.numero()), JFRPh.getZona() + "\n" + JFRPh.getVista(),
                    JFRPh.getGravedad().toString(), JFRPh.getEstado().toString(),
                    JFRPh.getTipo() + (JFRPh.getNota().isBlank() ? "" : "\n" + JFRPh.getNota())}) {
                celdaDetalle(JFRPfilas, GGFJvalor, LJOMtexto, AKACfondo, Element.ALIGN_LEFT);
            }
        }
        GGFJdoc.add(JFRPfilas);
    }

    private String resumenQr(OrdenTrabajo LJOMo) {
        return nombreTaller + " | Orden " + LJOMo.getNumeroOrden() + " | Placa " + LJOMo.getDatosHistoricos().getPlaca()
                + " | Estado: " + LJOMo.getEstado() + " | Total: " + Ui.moneda(LJOMo.getTotal())
                + " | Ingreso: " + LJOMo.getFechaIngreso();
    }

    private void dibujarQr(PdfContentByte JFRPlienzo, String AKACtexto, float GGFJx0, float LJOMy0, float JFRPtamano, Font AKACfuente)
            throws WriterException {
        Map<EncodeHintType, Object> GGFJpistas = new EnumMap<>(EncodeHintType.class);
        GGFJpistas.put(EncodeHintType.MARGIN, 1);
        GGFJpistas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix LJOMmatriz = new QRCodeWriter().encode(AKACtexto, BarcodeFormat.QR_CODE, 0, 0, GGFJpistas);

        float JFRPcelda = JFRPtamano / LJOMmatriz.getWidth();
        JFRPlienzo.setColorFill(Color.BLACK);
        for (int AKACy = 0; AKACy < LJOMmatriz.getHeight(); AKACy++) {
            for (int GGFJx = 0; GGFJx < LJOMmatriz.getWidth(); GGFJx++) {
                if (LJOMmatriz.get(GGFJx, AKACy)) {
                    JFRPlienzo.rectangle(GGFJx0 + GGFJx * JFRPcelda, LJOMy0 + (LJOMmatriz.getHeight() - 1 - AKACy) * JFRPcelda, JFRPcelda, JFRPcelda);
                }
            }
        }
        JFRPlienzo.fill();

        ColumnText.showTextAligned(JFRPlienzo, Element.ALIGN_LEFT,
                new Phrase("Escanea para ver el resumen de la orden", AKACfuente), GGFJx0 + JFRPtamano + 12, LJOMy0 + JFRPtamano / 2, 0);
    }

    private void fila(PdfPTable LJOMtabla, String JFRPe1, String AKACv1, String GGFJe2, String LJOMv2, Font JFRPfe, Font AKACfv) {
        LJOMtabla.addCell(sinBorde(JFRPe1, JFRPfe));
        LJOMtabla.addCell(sinBorde(AKACv1, AKACfv));
        LJOMtabla.addCell(sinBorde(GGFJe2, JFRPfe));
        LJOMtabla.addCell(sinBorde(LJOMv2, AKACfv));
    }

    private PdfPCell sinBorde(String GGFJtexto, Font LJOMfuente) {
        PdfPCell JFRPcelda = new PdfPCell(new Phrase(GGFJtexto, LJOMfuente));
        JFRPcelda.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        JFRPcelda.setPadding(3);
        return JFRPcelda;
    }

    private void celdaDetalle(PdfPTable AKACtabla, String GGFJtexto, Font LJOMfuente, Color JFRPfondo, int AKACalineacion) {
        PdfPCell GGFJcelda = new PdfPCell(new Phrase(GGFJtexto, LJOMfuente));
        GGFJcelda.setBackgroundColor(JFRPfondo);
        GGFJcelda.setHorizontalAlignment(AKACalineacion);
        GGFJcelda.setPadding(6);
        GGFJcelda.setBorderColor(new Color(226, 232, 240));
        AKACtabla.addCell(GGFJcelda);
    }
}
