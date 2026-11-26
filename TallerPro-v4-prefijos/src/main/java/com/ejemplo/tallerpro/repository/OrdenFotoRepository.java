package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.FaseFoto;
import com.ejemplo.tallerpro.model.OrdenFoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenFotoRepository extends JpaRepository<OrdenFoto, Long> {
    Page<OrdenFoto> findByOrdenIdAndFaseOrderByIdDesc(Long orden, FaseFoto fase, Pageable pagina);
    List<OrdenFoto> findByOrdenIdOrderByFaseAscIdAsc(Long orden);
    Optional<OrdenFoto> findByIdAndOrdenId(Long id, Long orden);
}
