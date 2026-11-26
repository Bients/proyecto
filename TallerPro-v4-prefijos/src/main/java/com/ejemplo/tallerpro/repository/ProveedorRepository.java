package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByRuc(String ruc);

    @Query("""
            SELECT p FROM Proveedor p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR p.ruc LIKE CONCAT('%', :texto, '%')
            ORDER BY p.nombre, p.id
            """)
    Page<Proveedor> buscar(@Param("texto") String texto, Pageable pagina);
}
