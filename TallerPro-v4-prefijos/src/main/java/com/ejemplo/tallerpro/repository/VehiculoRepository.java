package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    String PLACA_NORMALIZADA = "replace(replace(upper(v.placa), '-', ''), ' ', '')";

    @Query("SELECT count(v) > 0 FROM Vehiculo v WHERE " + PLACA_NORMALIZADA
            + " = :placa AND (:id IS NULL OR v.id <> :id)")
    boolean existeOtraPlaca(@Param("placa") String placa, @Param("id") Long id);

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = "cliente")
    Page<Vehiculo> findByClienteIdOrderByPlaca(Long clienteId, Pageable pagina);

    @EntityGraph(attributePaths = "cliente")
    @Query("""
            SELECT v FROM Vehiculo v WHERE v.cliente.id = :cliente
              AND (:mecanico IS NULL OR EXISTS (SELECT o.id FROM OrdenTrabajo o
                   WHERE o.vehiculo.id = v.id AND o.mecanicoUsuario.id = :mecanico))
            ORDER BY v.placa
            """)
    java.util.List<Vehiculo> deClienteParaInspeccion(@Param("cliente") Long cliente, @Param("mecanico") Long mecanico);

    @EntityGraph(attributePaths = "cliente")
    @Query("""
            SELECT v FROM Vehiculo v
            WHERE (:placa <> '' AND\s""" + PLACA_NORMALIZADA + """
                   LIKE CONCAT('%', :placa, '%'))
               OR LOWER(v.placa) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(v.cliente.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(v.cliente.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(CONCAT(v.cliente.nombre, ' ', v.cliente.apellido)) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(CONCAT(v.cliente.apellido, ' ', v.cliente.nombre)) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY v.placa, v.id
            """)
    Page<Vehiculo> buscar(@Param("texto") String texto, @Param("placa") String placa, Pageable pagina);
}
