package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.DetalleOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {

    boolean existsByRepuestoId(Long repuestoId);
}
