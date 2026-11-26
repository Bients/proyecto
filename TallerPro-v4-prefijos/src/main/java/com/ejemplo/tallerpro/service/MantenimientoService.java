package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.ProgramaMantenimiento;
import com.ejemplo.tallerpro.repository.MantenimientoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MantenimientoService {

    private final SesionActual sesion;

    private final MantenimientoRepository repository;

    public List<ProgramaMantenimiento> listar(String filtro) {
        sesion.exigirAdmin();
        if (filtro == null || filtro.isBlank()) {
            return repository.buscar("");
        }
        return repository.buscar(filtro.trim());
    }

    public Page<ProgramaMantenimiento> buscar(String filtro, boolean pendientes, int pagina, int limite) {
        sesion.exigirAdmin();
        var GGFJids = repository.buscarIds(filtro == null ? "" : filtro.strip(), pendientes,
                ProgramaMantenimiento.UMBRAL_KM, fechaLimite(), PageRequest.of(pagina, limite));
        var LJOMresultados = GGFJids.isEmpty() ? List.<ProgramaMantenimiento>of() : repository.cargarPagina(GGFJids.getContent());
        return new PageImpl<>(LJOMresultados, GGFJids.getPageable(), GGFJids.getTotalElements());
    }

    @Transactional
    public ProgramaMantenimiento guardar(@Valid ProgramaMantenimiento programa) {
        var JFRPactor = sesion.exigirAdmin();
        boolean AKACsinKm = programa.getCadaKm() == null || programa.getCadaKm() <= 0;
        boolean GGFJsinMeses = programa.getCadaMeses() == null || programa.getCadaMeses() <= 0;
        if (AKACsinKm && GGFJsinMeses) {
            throw new NegocioException("Indica cada cuantos km o cada cuantos meses se repite el servicio.");
        }
        ProgramaMantenimiento LJOMguardado = repository.save(programa);
        sesion.registrar(JFRPactor, "guardar mantenimiento", LJOMguardado.getId());
        return LJOMguardado;
    }

    /** Registra que el servicio se hizo hoy, con el kilometraje actual del vehiculo. */
    @Transactional
    public ProgramaMantenimiento marcarRealizado(Long id) {
        var JFRPactor = sesion.exigirAdmin();
        ProgramaMantenimiento AKACp = repository.findById(id)
                .orElseThrow(() -> new NegocioException("El mantenimiento ya no existe."));
        AKACp.setUltimoKm(AKACp.getVehiculo().getKilometraje());
        AKACp.setUltimaFecha(LocalDate.now());
        sesion.registrar(JFRPactor, "marcar mantenimiento realizado", id);
        return repository.save(AKACp);
    }

    @Transactional
    public void eliminar(Long id) {
        var GGFJactor = sesion.exigirAdmin();
        if (!repository.existsById(id)) {
            throw new NegocioException("El mantenimiento ya no existe en la base de datos.");
        }
        repository.deleteById(id);
        sesion.registrar(GGFJactor, "eliminar mantenimiento", id);
    }

    /** Cantidad de mantenimientos vencidos o proximos a vencer. */
    public long contarPendientes() {
        sesion.exigirAdmin();
        return repository.contarPendientes(ProgramaMantenimiento.UMBRAL_KM, fechaLimite());
    }

    private String fechaLimite() {
        return LocalDate.now().plusDays(ProgramaMantenimiento.UMBRAL_DIAS).toString();
    }
}
