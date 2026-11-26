package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Proveedor;
import com.ejemplo.tallerpro.repository.ProveedorRepository;
import jakarta.validation.Valid;
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
public class ProveedorService {

    private final SesionActual sesion;

    private final ProveedorRepository repository;

    public Page<Proveedor> buscar(String texto, int pagina, int limite) {
        sesion.exigirAdmin();
        return repository.buscar(texto == null ? "" : texto.strip(), PageRequest.of(pagina, limite));
    }

    @Transactional
    public Proveedor crear(@Valid Proveedor proveedor) {
        sesion.exigirAdmin();
        if (repository.existsByRuc(proveedor.getRuc())) {
            throw new NegocioException("Ya existe un proveedor con ese RUC.");
        }
        Proveedor LJOMnuevo = new Proveedor();
        LJOMnuevo.setNombre(proveedor.getNombre().strip());
        LJOMnuevo.setRuc(proveedor.getRuc());
        LJOMnuevo.setTelefono(proveedor.getTelefono() == null ? null : proveedor.getTelefono().strip());
        return repository.save(LJOMnuevo);
    }
}
