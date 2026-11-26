package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.ProgramaMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MantenimientoRepository extends JpaRepository<ProgramaMantenimiento, Long> {

    boolean existsByVehiculoId(Long vehiculoId);

    String TABLAS = """
            FROM programa_mantenimiento m JOIN vehiculo v ON v.id = m.vehiculo_id
                JOIN cliente c ON c.id = v.cliente_id
            """;
    // Se recorta el día al final del mes destino, igual que LocalDate.plusMonths.
    String PENDIENTE = """
            ((m.cada_km > 0 AND m.ultimo_km + m.cada_km - v.kilometraje <= :km)
             OR (m.cada_meses > 0 AND date(
                 m.ultima_fecha, 'start of month', '+' || m.cada_meses || ' months',
                 (min(CAST(strftime('%d', m.ultima_fecha) AS INTEGER),
                      CAST(strftime('%d', date(m.ultima_fecha, 'start of month',
                           '+' || (m.cada_meses + 1) || ' months', '-1 day')) AS INTEGER)) - 1)
                 || ' days') <= :fecha))
            """;
    String FILTRO = """
             WHERE (lower(m.servicio) LIKE '%' || lower(:texto) || '%'
                OR lower(v.placa) LIKE '%' || lower(:texto) || '%'
                OR lower(c.apellido) LIKE '%' || lower(:texto) || '%'
                OR lower(c.nombre) LIKE '%' || lower(:texto) || '%')
                AND (:soloPendientes = 0 OR
            """ + PENDIENTE + ")";

    @Query(value = "SELECT m.id " + TABLAS + FILTRO + " ORDER BY m.id DESC",
            countQuery = "SELECT count(*) " + TABLAS + FILTRO, nativeQuery = true)
    Page<Long> buscarIds(@Param("texto") String texto, @Param("soloPendientes") boolean soloPendientes,
                         @Param("km") int km, @Param("fecha") String fecha, Pageable pagina);

    @Query(value = "SELECT count(*) " + TABLAS + " WHERE " + PENDIENTE, nativeQuery = true)
    long contarPendientes(@Param("km") int km, @Param("fecha") String fecha);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente"})
    @Query("SELECT m FROM ProgramaMantenimiento m WHERE m.id IN :ids ORDER BY m.id DESC")
    List<ProgramaMantenimiento> cargarPagina(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente"})
    @Query("""
            SELECT m FROM ProgramaMantenimiento m
            WHERE LOWER(m.servicio) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(m.vehiculo.placa) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(m.vehiculo.cliente.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(m.vehiculo.cliente.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY m.id DESC
            """)
    List<ProgramaMantenimiento> buscar(@Param("texto") String texto);
}
