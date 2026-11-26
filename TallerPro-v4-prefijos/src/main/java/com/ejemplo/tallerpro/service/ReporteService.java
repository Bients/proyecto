package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.repository.ClienteRepository;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.util.Ui;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Exporta los datos del taller a un libro de Excel (.xlsx) con una hoja por tema. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final SesionActual sesion;

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final OrdenRepository ordenRepository;
    private final RepuestoRepository repuestoRepository;
    private final PagoService pagos;

    public void exportarExcel(Path destino) throws IOException {
        var JFRPactor = sesion.exigirAdmin();
        try (XSSFWorkbook AKAClibro = new XSSFWorkbook(); OutputStream GGFJsalida = Files.newOutputStream(destino)) {
            CellStyle LJOMcabecera = estiloCabecera(AKAClibro);

            List<Object[]> JFRPclientes = new ArrayList<>();
            for (Cliente AKACc : clienteRepository.findAll(Sort.by("apellido", "nombre"))) {
                JFRPclientes.add(new Object[]{AKACc.getId(), AKACc.getNombre(), AKACc.getApellido(), AKACc.getDni(),
                        Ui.texto(AKACc.getTelefono()), Ui.texto(AKACc.getEmail())});
            }
            hoja(AKAClibro, LJOMcabecera, "Clientes",
                    new String[]{"ID", "Nombre", "Apellido", "DNI", "Teléfono", "Correo"}, JFRPclientes);

            List<Object[]> GGFJvehiculos = new ArrayList<>();
            for (Vehiculo LJOMv : vehiculoRepository.findAll(Sort.by("placa"))) {
                GGFJvehiculos.add(new Object[]{LJOMv.getId(), LJOMv.getPlaca(), LJOMv.getMarca(), LJOMv.getModelo(), LJOMv.getAnio(),
                        LJOMv.getKilometraje(), LJOMv.getCliente().getNombreCompleto()});
            }
            hoja(AKAClibro, LJOMcabecera, "Vehículos",
                    new String[]{"ID", "Placa", "Marca", "Modelo", "Año", "Kilometraje", "Propietario"}, GGFJvehiculos);

            List<Object[]> JFRPordenes = new ArrayList<>();
            List<Object[]> AKACdetalles = new ArrayList<>();
            for (OrdenTrabajo GGFJo : ordenRepository.findAll(Sort.by("id"))) {
                JFRPordenes.add(new Object[]{GGFJo.getId(), String.valueOf(GGFJo.getFechaIngreso()),
                        GGFJo.getDatosHistoricos().getPlaca(), GGFJo.getDatosHistoricos().getClienteNombre(),
                        GGFJo.getEstado().toString(), Ui.texto(GGFJo.getMecanico()), GGFJo.getKilometraje(),
                        GGFJo.getProblema(), GGFJo.getSubtotal(), GGFJo.getDescuento(), GGFJo.getTotal(), GGFJo.getTotalCobrado(),
                        GGFJo.getSaldoPendiente(), GGFJo.getSaldoAFavor(), GGFJo.getEstadoPago().toString()});
                for (DetalleOrden LJOMd : GGFJo.getDetalles()) {
                    AKACdetalles.add(new Object[]{GGFJo.getId(), GGFJo.getDatosHistoricos().getPlaca(), LJOMd.getTipo().toString(),
                            LJOMd.getDescripcion(), LJOMd.getTipo() == TipoDetalle.SERVICIO ? "-" : LJOMd.getCantidad(),
                            LJOMd.getPrecioUnitario(), LJOMd.getSubtotal()});
                }
            }
            hoja(AKAClibro, LJOMcabecera, "Órdenes",
                    new String[]{"N° orden", "Ingreso", "Placa", "Cliente", "Estado", "Mecánico", "Km",
                            "Problema", "Subtotal (S/)", "Descuento (S/)", "Total (S/)", "Cobrado (S/)",
                            "Saldo pendiente (S/)", "Saldo a favor (S/)", "Estado de pago"}, JFRPordenes);
            hoja(AKAClibro, LJOMcabecera, "Servicios y repuestos",
                    new String[]{"N° orden", "Placa", "Tipo", "Descripción", "Cantidad", "P. unitario (S/)",
                            "Subtotal (S/)"}, AKACdetalles);

            List<Object[]> JFRPrepuestos = new ArrayList<>();
            for (Repuesto AKACr : repuestoRepository.findAll(Sort.by("nombre"))) {
                JFRPrepuestos.add(new Object[]{AKACr.getId(), AKACr.getNombre(), AKACr.getStock(), AKACr.getStockMinimo(),
                        AKACr.getPrecio(), AKACr.esStockBajo() ? "Stock bajo" : "Stock OK"});
            }
            hoja(AKAClibro, LJOMcabecera, "Inventario",
                    new String[]{"ID", "Repuesto", "Stock", "Stock mínimo", "Precio (S/)", "Estado"}, JFRPrepuestos);

            LocalDate GGFJhoy = LocalDate.now();
            List<PagoService.CajaMetodo> LJOMmetodos = pagos.caja(GGFJhoy, GGFJhoy);
            List<Object[]> JFRPcaja = new ArrayList<>();
            for (PagoService.CajaMetodo AKACmetodo : LJOMmetodos) {
                JFRPcaja.add(new Object[]{GGFJhoy.toString(), AKACmetodo.metodo().toString(), AKACmetodo.cobros(),
                        AKACmetodo.anulaciones(), AKACmetodo.neto()});
            }
            JFRPcaja.add(new Object[]{GGFJhoy.toString(), "Total",
                    LJOMmetodos.stream().map(PagoService.CajaMetodo::cobros).reduce(BigDecimal.ZERO, BigDecimal::add),
                    LJOMmetodos.stream().map(PagoService.CajaMetodo::anulaciones).reduce(BigDecimal.ZERO, BigDecimal::add),
                    LJOMmetodos.stream().map(PagoService.CajaMetodo::neto).reduce(BigDecimal.ZERO, BigDecimal::add)});
            hoja(AKAClibro, LJOMcabecera, "Caja del día",
                    new String[]{"Fecha", "Método", "Cobros (S/)", "Anulaciones (S/)", "Neto (S/)"}, JFRPcaja);
            AKAClibro.write(GGFJsalida);
            sesion.registrar(JFRPactor, "exportar reporte Excel", "taller");
        }
    }

    private CellStyle estiloCabecera(Workbook GGFJlibro) {
        Font LJOMfuente = GGFJlibro.createFont();
        LJOMfuente.setBold(true);
        LJOMfuente.setColor(IndexedColors.WHITE.getIndex());
        CellStyle JFRPestilo = GGFJlibro.createCellStyle();
        JFRPestilo.setFont(LJOMfuente);
        JFRPestilo.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        JFRPestilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return JFRPestilo;
    }

    private void hoja(Workbook AKAClibro, CellStyle GGFJcabecera, String LJOMnombre, String[] JFRPtitulos, List<Object[]> AKACfilas) {
        Sheet GGFJhoja = AKAClibro.createSheet(LJOMnombre);
        Row LJOMfilaTitulo = GGFJhoja.createRow(0);
        for (int JFRPi = 0; JFRPi < JFRPtitulos.length; JFRPi++) {
            Cell AKACcelda = LJOMfilaTitulo.createCell(JFRPi);
            AKACcelda.setCellValue(JFRPtitulos[JFRPi]);
            AKACcelda.setCellStyle(GGFJcabecera);
        }
        int GGFJn = 1;
        for (Object[] LJOMdatos : AKACfilas) {
            Row JFRPfila = GGFJhoja.createRow(GGFJn++);
            for (int AKACi = 0; AKACi < LJOMdatos.length; AKACi++) {
                Cell GGFJcelda = JFRPfila.createCell(AKACi);
                Object LJOMvalor = LJOMdatos[AKACi];
                if (LJOMvalor instanceof Number JFRPnumero) {
                    GGFJcelda.setCellValue(JFRPnumero.doubleValue());
                } else {
                    GGFJcelda.setCellValue(LJOMvalor == null ? "" : String.valueOf(LJOMvalor));
                }
            }
        }
        for (int AKACi = 0; AKACi < JFRPtitulos.length; AKACi++) {
            GGFJhoja.setColumnWidth(AKACi, Math.min(Math.max(JFRPtitulos[AKACi].length() + 6, 14), 40) * 256);
        }
        GGFJhoja.createFreezePane(0, 1);
    }
}
