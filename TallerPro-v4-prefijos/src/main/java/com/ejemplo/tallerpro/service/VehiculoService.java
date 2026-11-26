package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.repository.HallazgoRepository;
import com.ejemplo.tallerpro.repository.MantenimientoRepository;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.util.Placa;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehiculoService {

    private final SesionActual sesion;

    private final VehiculoRepository repository;
    private final Validator validator;
    private final OrdenRepository ordenRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final HallazgoRepository hallazgos;

    public Page<Vehiculo> buscar(String filtro, int pagina, int limite) {
        sesion.exigirUsuario();
        String LJOMtexto = filtro == null ? "" : filtro.strip();
        return repository.buscar(LJOMtexto, Placa.normalizar(LJOMtexto), PageRequest.of(pagina, limite));
    }

    @Transactional
    public Vehiculo guardar(Vehiculo vehiculo) {
        var JFRPactor = sesion.exigirAdmin();
        String AKACplaca = Placa.normalizar(vehiculo.getPlaca());
        Vehiculo GGFJanterior = vehiculo.getId() == null ? null : repository.findById(vehiculo.getId())
                .orElseThrow(() -> new NegocioException("El vehículo ya no existe en la base de datos."));
        boolean LJOMconservaPlaca = GGFJanterior != null && AKACplaca.equals(Placa.normalizar(GGFJanterior.getPlaca()));
        vehiculo.setPlaca(LJOMconservaPlaca ? GGFJanterior.getPlaca() : AKACplaca);
        var JFRPerrores = validator.validate(vehiculo);
        if (!JFRPerrores.isEmpty()) {
            throw new ConstraintViolationException(JFRPerrores);
        }
        if (!LJOMconservaPlaca && repository.existeOtraPlaca(AKACplaca, vehiculo.getId())) {
            throw new NegocioException("Ya existe un vehículo registrado con la placa " + AKACplaca);
        }
        if (GGFJanterior != null && GGFJanterior.getTipoCarroceria() != vehiculo.getTipoCarroceria()
                && hallazgos.zonasDelVehiculo(vehiculo.getId()).stream().anyMatch(AKACz -> !AKACz.aplica(vehiculo.getTipoCarroceria()))) {
            throw new NegocioException("La carrocería seleccionada no admite zonas del historial de inspección.");
        }
        Vehiculo GGFJguardado = repository.save(vehiculo);
        sesion.registrar(JFRPactor, "guardar vehículo", GGFJguardado.getId());
        return GGFJguardado;
    }

    @Transactional
    public void eliminar(Long id) {
        var LJOMactor = sesion.exigirAdmin();
        if (!repository.existsById(id)) {
            throw new NegocioException("El vehículo ya no existe en la base de datos.");
        }
        if (ordenRepository.existsByVehiculoId(id)) {
            throw new NegocioException(
                    "No se puede eliminar: el vehículo tiene órdenes de trabajo registradas.");
        }
        if (mantenimientoRepository.existsByVehiculoId(id)) {
            throw new NegocioException(
                    "No se puede eliminar: el vehículo tiene mantenimientos programados.");
        }
        if (hallazgos.existsByVehiculoId(id)) {
            throw new NegocioException("No se puede eliminar: el vehículo tiene un historial de inspección.");
        }
        repository.deleteById(id);
        sesion.registrar(LJOMactor, "eliminar vehículo", id);
    }

    public long contar() {
        return repository.count();
    }
}
