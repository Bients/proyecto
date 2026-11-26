package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.repository.CompraRepository;
import com.ejemplo.tallerpro.repository.DetalleOrdenRepository;
import com.ejemplo.tallerpro.repository.MovimientoInventarioRepository;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepuestoService {

    private final SesionActual sesion;

    private final RepuestoRepository repository;
    private final DetalleOrdenRepository detalleRepository;
    private final CompraRepository compraRepository;
    private final MovimientoInventarioRepository movimientos;

    public Page<Repuesto> buscar(String filtro, boolean soloBajo, int pagina, int limite) {
        String JFRPtexto = filtro == null ? "" : filtro.strip();
        Long GGFJcodigo = null;
        try {
            if (JFRPtexto.matches("#?[0-9]{1,19}")) {
                GGFJcodigo = Long.valueOf(JFRPtexto.startsWith("#") ? JFRPtexto.substring(1) : JFRPtexto);
            }
        } catch (NumberFormatException LJOMe) {
            // Se conserva la búsqueda por nombre si el texto excede el rango de un ID.
        }
        sesion.exigirUsuario();
        var JFRPrepuestos = repository.buscar(JFRPtexto, GGFJcodigo, soloBajo, PageRequest.of(pagina, limite));
        return sesion.esAdmin() ? JFRPrepuestos : JFRPrepuestos.map(Repuesto::sinCosto);
    }

    @Transactional
    public Repuesto guardar(@Valid Repuesto repuesto) {
        var AKACactor = sesion.exigirAdmin();
        repository.findByNombre(repuesto.getNombre()).ifPresent(GGFJexistente -> {
            boolean LJOMesOtro = repuesto.getId() == null || !GGFJexistente.getId().equals(repuesto.getId());
            if (LJOMesOtro) {
                throw new NegocioException(
                        "Ya existe un repuesto llamado \"" + repuesto.getNombre() + "\".");
            }
        });
        String JFRPsku = repuesto.getSku() == null ? "" : repuesto.getSku().strip().toUpperCase(Locale.ROOT);
        if (JFRPsku.length() > 40) {
            throw new NegocioException("El SKU admite hasta 40 caracteres.");
        }
        if (!JFRPsku.isEmpty()) {
            repository.findBySkuIgnoreCase(JFRPsku).ifPresent(AKACexistente -> {
                if (!AKACexistente.getId().equals(repuesto.getId())) {
                    throw new NegocioException("Ya existe un repuesto con ese SKU.");
                }
            });
        }
        if (repuesto.getPrecio().stripTrailingZeros().scale() > 2
                || repuesto.getPrecio().compareTo(new java.math.BigDecimal("999999999.99")) > 0) {
            throw new NegocioException("Escribe un precio de venta dentro del rango, con hasta dos decimales.");
        }
        Repuesto GGFJdestino = repuesto.getId() == null ? new Repuesto() : repository.findById(repuesto.getId())
                .orElseThrow(() -> new NegocioException("El repuesto ya no existe en la base de datos."));
        GGFJdestino.setNombre(repuesto.getNombre());
        GGFJdestino.setSku(JFRPsku.isEmpty() ? null : JFRPsku);
        GGFJdestino.setStockMinimo(repuesto.getStockMinimo());
        GGFJdestino.setPrecio(repuesto.getPrecio());
        Repuesto LJOMguardado = repository.save(GGFJdestino);
        sesion.registrar(AKACactor, "guardar repuesto", LJOMguardado.getId());
        return LJOMguardado;
    }

    @Transactional
    public void eliminar(Long id) {
        var JFRPactor = sesion.exigirAdmin();
        if (!repository.existsById(id)) {
            throw new NegocioException("El repuesto ya no existe en la base de datos.");
        }
        if (movimientos.existsByRepuestoId(id) || detalleRepository.existsByRepuestoId(id)
                || compraRepository.usaRepuesto(id)) {
            throw new NegocioException("No se puede eliminar un repuesto con historial de inventario, compras u órdenes.");
        }
        repository.deleteById(id);
        sesion.registrar(JFRPactor, "eliminar repuesto", id);
    }

    public Optional<Repuesto> alertaStock(Long id) {
        sesion.exigirUsuario();
        var AKACrepuesto = repository.findById(id).filter(Repuesto::esStockBajo);
        return sesion.esAdmin() ? AKACrepuesto : AKACrepuesto.map(Repuesto::sinCosto);
    }

    public List<Repuesto> alertasStock(int limite) {
        sesion.exigirUsuario();
        var GGFJrepuestos = repository.alertasStock(PageRequest.of(0, limite));
        return sesion.esAdmin() ? GGFJrepuestos : GGFJrepuestos.stream().map(Repuesto::sinCosto).toList();
    }

    public long contarStockBajo() {
        return repository.contarStockBajo();
    }
}
