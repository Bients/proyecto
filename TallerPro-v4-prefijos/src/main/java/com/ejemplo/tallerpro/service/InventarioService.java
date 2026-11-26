package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.MovimientoInventario;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.OrigenMovimiento;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.TipoMovimiento;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.repository.MovimientoInventarioRepository;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import com.ejemplo.tallerpro.util.SesionActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private static final DateTimeFormatter FECHA = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private static final int ESCALA_COSTO = 6;

    private final MovimientoInventarioRepository movimientos;
    private final RepuestoRepository repuestos;
    private final SesionActual sesion;

    public record MovimientoKardex(MovimientoInventario movimiento, String referencia) {
    }

    public Page<MovimientoInventario> buscar(Long repuesto, TipoMovimiento tipo, LocalDate desde, LocalDate hasta,
                                             int pagina, int limite) {
        return buscarKardex(repuesto, tipo, desde, hasta, pagina, limite).map(MovimientoKardex::movimiento);
    }

    public Page<MovimientoKardex> buscarKardex(Long repuesto, TipoMovimiento tipo, LocalDate desde, LocalDate hasta,
                                              int pagina, int limite) {
        sesion.exigirAdmin();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
        ZoneId GGFJzona = ZoneId.systemDefault();
        return movimientos.buscarKardex(repuesto, tipo, desde == null ? null : FECHA.format(desde.atStartOfDay(GGFJzona)),
                        hasta == null ? null : FECHA.format(hasta.plusDays(1).atStartOfDay(GGFJzona)), PageRequest.of(pagina, limite))
                .map(LJOMfila -> {
                    var JFRPmovimiento = LJOMfila.getMovimiento();
                    String AKACreferencia = switch (JFRPmovimiento.getOrigenTipo()) {
                        case ORDEN -> LJOMfila.getNumeroOrden() == null ? "Orden sin referencia" : LJOMfila.getNumeroOrden();
                        case COMPRA -> LJOMfila.getNumeroComprobante() == null ? "Compra sin referencia"
                                : LJOMfila.getTipoComprobante() + " " + LJOMfila.getNumeroComprobante();
                        default -> JFRPmovimiento.getOrigenTipo().toString();
                    };
                    return new MovimientoKardex(JFRPmovimiento, AKACreferencia);
                });
    }

    @Transactional
    public Repuesto ajustar(Long id, int cantidad, String motivo) {
        Usuario GGFJusuario = usuarioActual();
        if (GGFJusuario.getRol() != Rol.ADMIN) {
            throw new NegocioException("Solo un administrador puede ajustar el inventario.");
        }
        if (cantidad == 0) {
            throw new NegocioException("Indica una cantidad positiva o negativa distinta de cero.");
        }
        Repuesto LJOMrepuesto = repuestos.findById(id)
                .orElseThrow(() -> new NegocioException("El repuesto ya no existe."));
        registrar(LJOMrepuesto, TipoMovimiento.AJUSTE, cantidad, LJOMrepuesto.getCostoPromedio(),
                OrigenMovimiento.AJUSTE_MANUAL, null, null, validarMotivo(motivo), GGFJusuario);
        return LJOMrepuesto;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarOrden(DetalleOrden detalle, int cantidad, Long orden, String motivo) {
        sesion.verificarOrden(detalle.getOrden());
        if (cantidad == 0) {
            return;
        }
        Repuesto JFRPrepuesto = detalle.getRepuesto();
        BigDecimal AKACcosto = detalle.getCostoUnitario() == null ? JFRPrepuesto.getCostoPromedio() : detalle.getCostoUnitario();
        TipoMovimiento GGFJtipo = cantidad < 0 ? TipoMovimiento.SALIDA_ORDEN : TipoMovimiento.DEVOLUCION_ORDEN;
        registrar(JFRPrepuesto, GGFJtipo, cantidad, AKACcosto, OrigenMovimiento.ORDEN, orden, detalle, motivo, usuarioActual());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void entradaCompra(Repuesto repuesto, int cantidad, BigDecimal costo, Long compra) {
        sesion.exigirAdmin();
        BigDecimal LJOMvalorPrevio = repuesto.getCostoPromedio().multiply(BigDecimal.valueOf(repuesto.getStock()));
        BigDecimal JFRPvalorEntrada = costo.multiply(BigDecimal.valueOf(cantidad));
        int AKACnuevoSaldo = registrar(repuesto, TipoMovimiento.ENTRADA_COMPRA, cantidad, costo,
                OrigenMovimiento.COMPRA, compra, null, "Confirmación de compra", usuarioActual());
        repuesto.setCostoPromedio(LJOMvalorPrevio.add(JFRPvalorEntrada)
                .divide(BigDecimal.valueOf(AKACnuevoSaldo), ESCALA_COSTO, RoundingMode.HALF_UP));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void contrapartidaCompra(Repuesto repuesto, int cantidad, Long compra, String motivo) {
        sesion.exigirAdmin();
        registrar(repuesto, TipoMovimiento.AJUSTE, -cantidad, repuesto.getCostoPromedio(),
                OrigenMovimiento.COMPRA, compra, null, motivo, usuarioActual());
    }

    private int registrar(Repuesto GGFJrepuesto, TipoMovimiento LJOMtipo, int JFRPcantidad, BigDecimal AKACcosto,
                          OrigenMovimiento GGFJorigen, Long LJOMreferencia, DetalleOrden JFRPdetalle, String AKACmotivo, Usuario GGFJusuario) {
        long LJOMsaldo = (long) GGFJrepuesto.getStock() + JFRPcantidad;
        if (LJOMsaldo < 0) {
            throw new NegocioException("Stock insuficiente de " + GGFJrepuesto.getNombre()
                    + ". Disponible: " + GGFJrepuesto.getStock() + ".");
        }
        if (LJOMsaldo > Integer.MAX_VALUE) {
            throw new NegocioException("El saldo de " + GGFJrepuesto.getNombre() + " supera el máximo permitido.");
        }
        int JFRPnuevoSaldo = (int) LJOMsaldo;
        movimientos.saveAndFlush(new MovimientoInventario(GGFJrepuesto, LJOMtipo, JFRPcantidad, JFRPnuevoSaldo, AKACcosto,
                GGFJorigen, LJOMreferencia, JFRPdetalle, FECHA.format(Instant.now()), GGFJusuario.getUsuario(), AKACmotivo));
        // SQLite aplica el saldo al guardar el movimiento. Se sincroniza la entidad de la transacción.
        GGFJrepuesto.setStock(JFRPnuevoSaldo);
        return JFRPnuevoSaldo;
    }

    Usuario usuarioActual() {
        return sesion.exigirUsuario();
    }

    static String validarMotivo(String motivo) {
        String AKACtexto = motivo == null ? "" : motivo.strip();
        if (AKACtexto.isEmpty() || AKACtexto.length() > 500) {
            throw new NegocioException("Escribe un motivo de 1 a 500 caracteres.");
        }
        return AKACtexto;
    }
}
