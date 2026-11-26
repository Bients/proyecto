package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.CentimosConverter;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.MetodoPago;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Pago;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.PagoRepository;
import com.ejemplo.tallerpro.util.SesionActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoService {

    private static final DateTimeFormatter FECHA = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private final OrdenRepository ordenes;
    private final PagoRepository pagos;
    private final SesionActual sesion;

    public record CajaMetodo(MetodoPago metodo, BigDecimal cobros, BigDecimal anulaciones) {
        public BigDecimal neto() {
            return cobros.subtract(anulaciones);
        }
    }

    public List<Pago> listar(Long orden) {
        sesion.exigirAdmin();
        return pagos.findByOrdenIdOrderByIdDesc(orden);
    }

    @Transactional
    public Pago registrar(Long ordenId, BigDecimal monto, MetodoPago metodo, String nota) {
        Usuario JFRPoperador = sesion.exigirAdmin();
        OrdenTrabajo AKACorden = buscarParaCobrar(ordenId);
        if (AKACorden.getEstado() == EstadoOrden.ANULADO) {
            throw new NegocioException("No se pueden registrar pagos en una orden anulada.");
        }
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new NegocioException("Escribe un monto positivo con hasta dos decimales.");
        }
        if (metodo == null) {
            throw new NegocioException("Selecciona el método de pago.");
        }
        try {
            long GGFJcentimos = new CentimosConverter().convertToDatabaseColumn(monto);
            Math.addExact(pagos.cobradoCentimos(ordenId), GGFJcentimos);
        } catch (ArithmeticException LJOMe) {
            throw new NegocioException("El importe supera el límite permitido para la orden.");
        }
        String JFRPcomentario = nota == null ? "" : nota.strip();
        if (JFRPcomentario.length() > 500) {
            throw new NegocioException("La nota admite hasta 500 caracteres.");
        }
        Pago AKACpago = new Pago();
        AKACpago.setOrden(AKACorden);
        AKACpago.setMonto(CentimosConverter.redondear(monto));
        AKACpago.setMetodo(metodo);
        AKACpago.setNota(JFRPcomentario.isEmpty() ? null : JFRPcomentario);
        AKACpago.setFecha(fechaActual());
        AKACpago.setUsuario(JFRPoperador.getUsuario());
        return pagos.saveAndFlush(AKACpago);
    }

    @Transactional
    public Pago anular(Long ordenId, Long pagoId, String motivo) {
        Usuario GGFJoperador = sesion.exigirAdmin();
        buscarParaCobrar(ordenId);
        Pago LJOMpago = pagos.findById(pagoId).filter(JFRPp -> ordenId.equals(JFRPp.getOrden().getId()))
                .orElseThrow(() -> new NegocioException("El pago no pertenece a esta orden."));
        if (LJOMpago.isAnulado()) {
            throw new NegocioException("El pago ya está anulado.");
        }
        String AKACrazon = motivo == null ? "" : motivo.strip();
        if (AKACrazon.isEmpty() || AKACrazon.length() > 500) {
            throw new NegocioException("Escribe un motivo de 1 a 500 caracteres.");
        }
        LJOMpago.setAnulado(true);
        LJOMpago.setMotivoAnulacion(AKACrazon);
        LJOMpago.setFechaAnulacion(fechaActual());
        LJOMpago.setUsuarioAnulacion(GGFJoperador.getUsuario());
        return pagos.saveAndFlush(LJOMpago);
    }

    public List<CajaMetodo> caja(LocalDate desde, LocalDate hasta) {
        sesion.exigirAdmin();
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Selecciona un rango de fechas válido para caja.");
        }
        ZoneId GGFJzona = ZoneId.systemDefault();
        String LJOMinicio = FECHA.format(desde.atStartOfDay(GGFJzona));
        String JFRPfin = FECHA.format(hasta.plusDays(1).atStartOfDay(GGFJzona));
        Map<MetodoPago, BigDecimal> AKACcobros = porMetodo(pagos.cobrosEntre(LJOMinicio, JFRPfin));
        Map<MetodoPago, BigDecimal> GGFJanulaciones = porMetodo(pagos.anulacionesEntre(LJOMinicio, JFRPfin));
        return Arrays.stream(MetodoPago.values()).map(LJOMm -> new CajaMetodo(LJOMm,
                AKACcobros.getOrDefault(LJOMm, CentimosConverter.CERO), GGFJanulaciones.getOrDefault(LJOMm, CentimosConverter.CERO)))
                .toList();
    }

    static String fechaActual() {
        return FECHA.format(Instant.now());
    }

    private Map<MetodoPago, BigDecimal> porMetodo(List<PagoRepository.ImportePorMetodo> JFRPfilas) {
        Map<MetodoPago, BigDecimal> AKACimportes = new EnumMap<>(MetodoPago.class);
        for (PagoRepository.ImportePorMetodo GGFJfila : JFRPfilas) {
            AKACimportes.put(MetodoPago.valueOf(GGFJfila.getMetodo()), BigDecimal.valueOf(GGFJfila.getCentimos(), 2));
        }
        return AKACimportes;
    }

    private OrdenTrabajo buscarParaCobrar(Long LJOMid) {
        ordenes.reservarEscritura(LJOMid);
        return ordenes.findById(LJOMid).orElseThrow(() -> new NegocioException("La orden ya no existe."));
    }

}
