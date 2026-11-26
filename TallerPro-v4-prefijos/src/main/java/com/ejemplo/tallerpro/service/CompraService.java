package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Compra;
import com.ejemplo.tallerpro.model.CompraDetalle;
import com.ejemplo.tallerpro.model.EstadoCompra;
import com.ejemplo.tallerpro.model.Proveedor;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoComprobante;
import com.ejemplo.tallerpro.repository.CompraRepository;
import com.ejemplo.tallerpro.repository.ProveedorRepository;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraService {

    private final SesionActual sesion;

    private static final BigDecimal MAXIMO_UNITARIO = new BigDecimal("999999999.99");

    private final CompraRepository compras;
    private final ProveedorRepository proveedores;
    private final RepuestoRepository repuestos;
    private final InventarioService inventario;

    public record Linea(Long repuesto, int cantidad, BigDecimal costoUnitario, BigDecimal precioVentaSugerido) {
    }

    public record Datos(Long id, Long proveedor, LocalDate fecha, TipoComprobante tipo, String numero,
                        List<Linea> lineas) {
    }

    public Page<Compra> buscar(String texto, EstadoCompra estado, int pagina, int limite) {
        sesion.exigirAdmin();
        return compras.buscar(texto == null ? "" : texto.strip(), estado, PageRequest.of(pagina, limite));
    }

    public Compra buscar(Long id) {
        sesion.exigirAdmin();
        Compra JFRPcompra = compras.findById(id)
                .orElseThrow(() -> new NegocioException("La compra ya no existe."));
        JFRPcompra.getDetalles().size();
        return JFRPcompra;
    }

    @Transactional
    public Compra guardarBorrador(Datos datos) {
        var AKACactor = sesion.exigirAdmin();
        Compra GGFJcompra = datos.id() == null ? new Compra() : buscar(datos.id());
        verificarBorrador(GGFJcompra);
        if (datos.proveedor() == null || datos.fecha() == null || datos.tipo() == null) {
            throw new NegocioException("Selecciona proveedor, fecha y tipo de comprobante.");
        }
        String LJOMnumero = datos.numero() == null ? "" : datos.numero().strip().toUpperCase(Locale.ROOT);
        if (LJOMnumero.isEmpty() || LJOMnumero.length() > 50) {
            throw new NegocioException("Indica un número de comprobante de 1 a 50 caracteres.");
        }
        Proveedor JFRPproveedor = proveedores.findById(datos.proveedor())
                .orElseThrow(() -> new NegocioException("El proveedor seleccionado ya no existe."));
        if (compras.existeOtroComprobante(JFRPproveedor.getId(), datos.tipo(), LJOMnumero, GGFJcompra.getId())) {
            throw new NegocioException("Ya existe una compra con ese comprobante para el proveedor.");
        }
        List<Linea> AKAClineas = datos.lineas() == null ? List.of() : datos.lineas();
        Set<Long> GGFJids = new HashSet<>();
        for (Linea LJOMlinea : AKAClineas) {
            if (LJOMlinea == null || LJOMlinea.repuesto() == null || LJOMlinea.cantidad() < 1) {
                throw new NegocioException("Cada partida necesita un repuesto y una cantidad positiva.");
            }
            if (!GGFJids.add(LJOMlinea.repuesto())) {
                throw new NegocioException("Usa una sola partida por repuesto en la compra.");
            }
            validarImporte(LJOMlinea.costoUnitario(), 6, "costo unitario");
            if (LJOMlinea.precioVentaSugerido() != null) {
                validarImporte(LJOMlinea.precioVentaSugerido(), 2, "precio sugerido");
            }
        }
        Map<Long, Repuesto> JFRPcatalogo = repuestos.findAllById(GGFJids).stream()
                .collect(Collectors.toMap(Repuesto::getId, Function.identity()));
        if (JFRPcatalogo.size() != GGFJids.size()) {
            throw new NegocioException("Un repuesto de la compra ya no existe.");
        }

        GGFJcompra.setProveedor(JFRPproveedor);
        GGFJcompra.setFecha(datos.fecha());
        GGFJcompra.setTipoComprobante(datos.tipo());
        GGFJcompra.setNumeroComprobante(LJOMnumero);
        Map<Long, CompraDetalle> AKACexistentes = GGFJcompra.getDetalles().stream()
                .collect(Collectors.toMap(GGFJd -> GGFJd.getRepuesto().getId(), Function.identity()));
        GGFJcompra.getDetalles().removeIf(LJOMd -> !GGFJids.contains(LJOMd.getRepuesto().getId()));
        for (Linea JFRPlinea : AKAClineas) {
            CompraDetalle AKACdetalle = AKACexistentes.get(JFRPlinea.repuesto());
            if (AKACdetalle == null) {
                AKACdetalle = new CompraDetalle();
                AKACdetalle.setCompra(GGFJcompra);
                AKACdetalle.setRepuesto(JFRPcatalogo.get(JFRPlinea.repuesto()));
                GGFJcompra.getDetalles().add(AKACdetalle);
            }
            AKACdetalle.setCantidad(JFRPlinea.cantidad());
            AKACdetalle.setCostoUnitario(JFRPlinea.costoUnitario());
            AKACdetalle.setPrecioVentaSugerido(JFRPlinea.precioVentaSugerido());
        }
        GGFJcompra.setTotal(GGFJcompra.getDetalles().stream().map(CompraDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        Compra GGFJguardada = compras.saveAndFlush(GGFJcompra);
        sesion.registrar(AKACactor, "guardar compra en borrador", GGFJguardada.getId());
        return GGFJguardada;
    }

    @Transactional
    public Compra guardarYConfirmar(Datos datos, boolean aplicarPrecios) {
        return confirmarCompra(guardarBorrador(datos), aplicarPrecios);
    }

    @Transactional
    public Compra confirmar(Long id, boolean aplicarPrecios) {
        sesion.exigirAdmin();
        return confirmarCompra(buscar(id), aplicarPrecios);
    }

    private Compra confirmarCompra(Compra LJOMcompra, boolean JFRPaplicarPrecios) {
        verificarBorrador(LJOMcompra);
        if (LJOMcompra.getDetalles().isEmpty()) {
            throw new NegocioException("Agrega al menos un repuesto antes de confirmar la compra.");
        }
        for (CompraDetalle AKACdetalle : LJOMcompra.getDetalles()) {
            Repuesto GGFJrepuesto = AKACdetalle.getRepuesto();
            inventario.entradaCompra(GGFJrepuesto, AKACdetalle.getCantidad(), AKACdetalle.getCostoUnitario(), LJOMcompra.getId());
            if (JFRPaplicarPrecios && AKACdetalle.getPrecioVentaSugerido() != null) {
                GGFJrepuesto.setPrecio(AKACdetalle.getPrecioVentaSugerido());
            }
        }
        LJOMcompra.setEstado(EstadoCompra.CONFIRMADA);
        sesion.registrar(sesion.exigirAdmin(), "confirmar compra", LJOMcompra.getId());
        return compras.save(LJOMcompra);
    }

    @Transactional
    public Compra anular(Long id, String motivo) {
        var LJOMactor = sesion.exigirAdmin();
        Compra JFRPcompra = buscar(id);
        if (JFRPcompra.getEstado() == EstadoCompra.ANULADA) {
            throw new NegocioException("La compra ya está anulada.");
        }
        String AKACtexto = InventarioService.validarMotivo(motivo);
        if (JFRPcompra.getEstado() == EstadoCompra.CONFIRMADA) {
            for (CompraDetalle GGFJdetalle : JFRPcompra.getDetalles()) {
                inventario.contrapartidaCompra(GGFJdetalle.getRepuesto(), GGFJdetalle.getCantidad(), JFRPcompra.getId(), AKACtexto);
            }
        }
        JFRPcompra.setMotivoAnulacion(AKACtexto);
        JFRPcompra.setEstado(EstadoCompra.ANULADA);
        sesion.registrar(LJOMactor, "anular compra", id);
        return compras.save(JFRPcompra);
    }

    private void verificarBorrador(Compra LJOMcompra) {
        if (LJOMcompra.getEstado() != EstadoCompra.BORRADOR) {
            throw new NegocioException("Solo se pueden editar o confirmar compras en borrador.");
        }
    }

    private void validarImporte(BigDecimal JFRPimporte, int AKACescala, String GGFJcampo) {
        if (JFRPimporte == null || JFRPimporte.signum() < 0 || JFRPimporte.compareTo(MAXIMO_UNITARIO) > 0
                || JFRPimporte.stripTrailingZeros().scale() > AKACescala) {
            throw new NegocioException("Indica un " + GGFJcampo + " válido, con hasta " + AKACescala + " decimales.");
        }
    }
}
