package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface OrdenRepository extends JpaRepository<OrdenTrabajo, Long> {

    boolean existsByVehiculoId(Long vehiculoId);
    boolean existsByVehiculoIdAndMecanicoUsuarioId(Long vehiculo, Long mecanico);

    interface VisitaInspeccion {
        Long getId();
        String getNumero();
        EstadoOrden getEstado();
    }

    @Query("""
            SELECT o.id AS id, o.numeroOrden AS numero, o.estado AS estado
            FROM OrdenTrabajo o WHERE o.vehiculo.id = :vehiculo
              AND (:mecanico IS NULL OR o.mecanicoUsuario.id = :mecanico)
            ORDER BY o.id DESC
            """)
    List<VisitaInspeccion> visitasParaInspeccion(@Param("vehiculo") Long vehiculo, @Param("mecanico") Long mecanico);

    long countByVehiculoIdAndMecanicoUsuarioId(Long vehiculo, Long mecanico);

    @Modifying
    @Query(value = "UPDATE orden_trabajo SET estado = estado WHERE id = :id", nativeQuery = true)
    int reservarEscritura(@Param("id") Long id);

    interface OrdenTablero {
        long getId();
        String getNumero();
        String getEstado();
        String getPlaca();
        String getCliente();
        String getProblema();
        Integer getDias();
        long getCobrado();
        long getTotal();
    }

    String FILTRO_TABLERO = """
            FROM orden_trabajo o
            WHERE ((:fecha IS NULL AND o.estado NOT IN ('ENTREGADO', 'ANULADO')) OR o.fecha_ingreso = :fecha)
              AND (:mecanico IS NULL OR o.mecanico_id = :mecanico)
              AND (:placa = '' OR instr(replace(replace(upper(o.vehiculo_placa), '-', ''), ' ', ''), :placa) > 0)
            """;

    @Query(value = """
            SELECT o.id, o.numero_orden AS numero, o.estado, o.vehiculo_placa AS placa,
                o.cliente_nombre AS cliente, o.problema,
                CAST(MAX(0, julianday(:hoy) - julianday(o.fecha_ingreso)) AS INTEGER) AS dias,
                CASE WHEN :mecanico IS NULL THEN COALESCE((SELECT SUM(p.monto_centimos)
                    FROM pago p WHERE p.orden_id = o.id AND p.anulado = 0), 0) ELSE 0 END AS cobrado,
                CASE WHEN :mecanico IS NULL THEN MAX(0, COALESCE((SELECT SUM(d.precio_unitario_centimos * d.cantidad)
                    FROM detalle_orden d WHERE d.orden_id = o.id AND d.retirado = 0), 0)
                    - o.descuento_centimos) ELSE 0 END AS total
            """ + FILTRO_TABLERO + """
            ORDER BY CASE WHEN :fecha IS NULL THEN CASE WHEN o.estado = 'LISTO' THEN 0 ELSE 1 END
                         ELSE CASE o.estado WHEN 'RECIBIDO' THEN 1 WHEN 'DIAGNOSTICO' THEN 2
                              WHEN 'REPARACION' THEN 3 WHEN 'LISTO' THEN 4 WHEN 'ENTREGADO' THEN 5 ELSE 6 END END,
                     o.fecha_ingreso ASC NULLS LAST, o.id ASC
            """, countQuery = "SELECT COUNT(*) " + FILTRO_TABLERO, nativeQuery = true)
    Page<OrdenTablero> ordenesTablero(@Param("hoy") String hoy, @Param("fecha") String fecha,
                                     @Param("placa") String placa, @Param("mecanico") Long mecanico, Pageable pagina);

    interface ServicioFrecuente {
        String getNombre();
        long getCantidad();
    }

    @Query(value = """
            SELECT trim(d.descripcion) AS nombre, SUM(d.cantidad) AS cantidad
            FROM detalle_orden d JOIN orden_trabajo o ON o.id = d.orden_id
            WHERE d.tipo = 'SERVICIO' AND d.retirado = 0 AND o.estado <> 'ANULADO'
              AND (:mecanico IS NULL OR o.mecanico_id = :mecanico)
            GROUP BY lower(trim(d.descripcion)) ORDER BY cantidad DESC, nombre LIMIT 5
            """, nativeQuery = true)
    List<ServicioFrecuente> serviciosFrecuentes(@Param("mecanico") Long mecanico);

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN o.estado = 'ANULADO' THEN 0 ELSE MAX(0,
                COALESCE(d.subtotal, 0) - o.descuento_centimos - COALESCE(p.cobrado, 0)) END), 0) AS pendiente,
                COALESCE(SUM(p.cobrado), 0) AS cobrado, COUNT(*) AS visitas
            FROM orden_trabajo o
            LEFT JOIN (SELECT orden_id, SUM(cantidad * precio_unitario_centimos) subtotal
                FROM detalle_orden WHERE retirado = 0 GROUP BY orden_id) d ON d.orden_id = o.id
            LEFT JOIN (SELECT orden_id, SUM(monto_centimos) cobrado
                FROM pago WHERE anulado = 0 GROUP BY orden_id) p ON p.orden_id = o.id
            """, nativeQuery = true)
    CobranzaVehiculo cobranzaTaller();

    String COINCIDENCIA = """
            (o.numeroOrden = UPPER(:filtro)
             OR LOWER(o.datosHistoricos.placa) LIKE LOWER(CONCAT('%', :filtro, '%'))
             OR LOWER(o.datosHistoricos.clienteNombre) LIKE LOWER(CONCAT('%', :filtro, '%'))
             OR o.datosHistoricos.clienteDni LIKE CONCAT('%', :filtro, '%')
             OR LOWER(o.mecanico) LIKE LOWER(CONCAT('%', :filtro, '%'))
             OR LOWER(o.problema) LIKE LOWER(CONCAT('%', :filtro, '%')))
            """;

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente", "mecanicoUsuario"})
    @Query("""
            SELECT o FROM OrdenTrabajo o LEFT JOIN o.mecanicoUsuario u
            WHERE (:vehiculo IS NULL OR o.vehiculo.id = :vehiculo)
              AND (:estado IS NULL OR o.estado = :estado)
              AND (:mecanico IS NULL OR u.id = :mecanico)
              AND (:desde IS NULL OR o.fechaIngreso >= :desde)
              AND (:hasta IS NULL OR o.fechaIngreso <= :hasta)
              AND (
            """ + COINCIDENCIA + """
              OR (:placa <> '' AND replace(replace(upper(o.datosHistoricos.placa), '-', ''), ' ', '')
                    LIKE CONCAT('%', :placa, '%'))
              OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :filtro, '%'))
              OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :filtro, '%')))
            """)
    Page<OrdenTrabajo> buscarFiltradas(@Param("filtro") String filtro, @Param("placa") String placa,
            @Param("vehiculo") Long vehiculo, @Param("estado") EstadoOrden estado,
            @Param("mecanico") Long mecanico, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta, Pageable pageable);

    interface CobranzaVehiculo {
        long getVisitas();
        long getCobrado();
        long getPendiente();
    }

    @Query(value = """
            WITH objetivo AS (SELECT id, estado, descuento_centimos FROM orden_trabajo
                WHERE vehiculo_id = :vehiculo AND (:mecanico IS NULL OR mecanico_id = :mecanico)),
            detalle AS (
                SELECT d.orden_id, SUM(d.precio_unitario_centimos * d.cantidad) AS subtotal
                FROM detalle_orden d JOIN objetivo o ON o.id = d.orden_id WHERE d.retirado = 0 GROUP BY d.orden_id
            ),
            cobros AS (
                SELECT p.orden_id, SUM(p.monto_centimos) AS monto
                FROM pago p JOIN objetivo o ON o.id = p.orden_id WHERE p.anulado = 0 GROUP BY p.orden_id
            )
            SELECT COUNT(*) AS visitas, COALESCE(SUM(c.monto), 0) AS cobrado,
                COALESCE(SUM(CASE WHEN o.estado = 'ANULADO' THEN 0 ELSE
                    MAX(COALESCE(d.subtotal, 0) - o.descuento_centimos - COALESCE(c.monto, 0), 0) END), 0) AS pendiente
            FROM objetivo o LEFT JOIN detalle d ON d.orden_id = o.id LEFT JOIN cobros c ON c.orden_id = o.id
            """, nativeQuery = true)
    CobranzaVehiculo cobranzaVehiculo(@Param("vehiculo") Long vehiculo, @Param("mecanico") Long mecanico);

    @Modifying
    @Query(value = """
            INSERT INTO entrega_con_saldo (orden_id, saldo_centimos, fecha, usuario, motivo)
            VALUES (:orden, :saldo, :fecha, :usuario, :motivo)
            """, nativeQuery = true)
    void registrarEntregaConSaldo(@Param("orden") Long orden, @Param("saldo") long saldo,
            @Param("fecha") String fecha, @Param("usuario") String usuario, @Param("motivo") String motivo);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente", "mecanicoUsuario"})
    @Query("SELECT o FROM OrdenTrabajo o WHERE " + COINCIDENCIA)
    Page<OrdenTrabajo> buscar(@Param("filtro") String filtro, Pageable pageable);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente", "mecanicoUsuario"})
    @Query("SELECT o FROM OrdenTrabajo o WHERE o.estado = :estado AND " + COINCIDENCIA)
    Page<OrdenTrabajo> buscarPorEstado(@Param("estado") EstadoOrden estado,
                                        @Param("filtro") String filtro,
                                        Pageable pageable);
}
