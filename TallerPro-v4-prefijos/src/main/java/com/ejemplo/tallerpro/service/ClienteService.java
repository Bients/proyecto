package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.repository.ClienteRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final SesionActual sesion;

    private final ClienteRepository repository;
    private final VehiculoRepository vehiculoRepository;

    public Page<Cliente> buscar(String filtro, int pagina, int limite) {
        sesion.exigirAdmin();
        String LJOMtexto = filtro == null ? "" : filtro.strip();
        return repository.buscar(LJOMtexto, PageRequest.of(pagina, limite));
    }

    public record FichaDni(Cliente cliente, List<Vehiculo> vehiculos, long totalVehiculos) {
    }

    public Optional<FichaDni> buscarPorDni(String dni) {
        sesion.exigirAdmin();
        String JFRPnumero = dni == null ? "" : dni.strip();
        if (!JFRPnumero.matches("[0-9]{8}")) {
            throw new NegocioException("El DNI debe tener exactamente 8 dígitos.");
        }
        return repository.findByDni(JFRPnumero).map(AKACcliente -> {
            var GGFJvehiculos = vehiculoRepository.findByClienteIdOrderByPlaca(AKACcliente.getId(), PageRequest.of(0, 5));
            return new FichaDni(AKACcliente, GGFJvehiculos.getContent(), GGFJvehiculos.getTotalElements());
        });
    }

    @Transactional
    public Cliente guardar(@Valid Cliente cliente) {
        var LJOMactor = sesion.exigirAdmin();
        repository.findByDni(cliente.getDni()).ifPresent(JFRPexistente -> {
            boolean AKACesOtro = cliente.getId() == null
                    || !JFRPexistente.getId().equals(cliente.getId());
            if (AKACesOtro) {
                throw new NegocioException(
                        "Ya existe un cliente registrado con el DNI " + cliente.getDni());
            }
        });
        Cliente GGFJguardado = repository.save(cliente);
        sesion.registrar(LJOMactor, "guardar cliente", GGFJguardado.getId());
        return GGFJguardado;
    }

    @Transactional
    public void eliminar(Long id) {
        var LJOMactor = sesion.exigirAdmin();
        if (!repository.existsById(id)) {
            throw new NegocioException("El cliente ya no existe en la base de datos.");
        }
        if (vehiculoRepository.existsByClienteId(id)) {
            throw new NegocioException(
                    "No se puede eliminar: el cliente tiene vehículos registrados. "
                            + "Elimina o reasigna primero sus vehículos.");
        }
        repository.deleteById(id);
        sesion.registrar(LJOMactor, "eliminar cliente", id);
    }

    public long contar() {
        return repository.count();
    }
}
