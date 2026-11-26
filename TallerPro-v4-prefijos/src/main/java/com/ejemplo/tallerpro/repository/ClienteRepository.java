package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
            SELECT c FROM Cliente c WHERE EXISTS (
              SELECT o.id FROM OrdenTrabajo o WHERE o.vehiculo.cliente.id = c.id AND o.mecanicoUsuario.id = :mecanico)
              AND (LOWER(CONCAT(c.nombre, ' ', c.apellido)) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(CONCAT(c.apellido, ' ', c.nombre)) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR c.dni LIKE CONCAT('%', :texto, '%') OR c.telefono LIKE CONCAT('%', :texto, '%'))
            ORDER BY c.apellido, c.nombre, c.id
            """)
    java.util.List<Cliente> buscarEnInspeccion(@Param("texto") String texto, @Param("mecanico") Long mecanico,
                                             Pageable pagina);

    Optional<Cliente> findByDni(String dni);

    @Query("""
            SELECT c FROM Cliente c
            WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(CONCAT(c.nombre, ' ', c.apellido)) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(CONCAT(c.apellido, ' ', c.nombre)) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR c.dni LIKE CONCAT('%', :texto, '%')
               OR c.telefono LIKE CONCAT('%', :texto, '%')
            ORDER BY c.apellido, c.nombre, c.id
            """)
    Page<Cliente> buscar(@Param("texto") String texto, Pageable pagina);
}
