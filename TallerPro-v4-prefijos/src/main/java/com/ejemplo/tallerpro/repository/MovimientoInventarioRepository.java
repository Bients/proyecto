package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.MovimientoInventario;
import com.ejemplo.tallerpro.model.TipoMovimiento;
import com.ejemplo.tallerpro.model.OrigenMovimiento;
import com.ejemplo.tallerpro.model.TipoComprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    interface FilaKardex {
        MovimientoInventario getMovimiento();
        String getNumeroOrden();
        TipoComprobante getTipoComprobante();
        String getNumeroComprobante();
    }

    boolean existsByRepuestoId(Long id);

    boolean existsByOrigenTipoAndOrigenIdAndTipo(OrigenMovimiento origen, Long id, TipoMovimiento tipo);

    @Query("""
            SELECT m FROM MovimientoInventario m LEFT JOIN m.detalleOrden d
            WHERE m.origenTipo = :origen AND m.origenId = :orden
              AND (:detalle IS NULL OR d.id = :detalle)
            ORDER BY m.id DESC
            """)
    Page<MovimientoInventario> porOrigen(@Param("origen") OrigenMovimiento origen, @Param("orden") Long orden,
                                        @Param("detalle") Long detalle, Pageable pagina);

    String FILTRO_KARDEX = """
            WHERE m.repuesto.id = :repuesto
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND (:desde IS NULL OR m.fecha >= :desde)
              AND (:hasta IS NULL OR m.fecha < :hasta)
            """;

    @Query(value = """
            SELECT m AS movimiento, o.numeroOrden AS numeroOrden,
                   c.tipoComprobante AS tipoComprobante, c.numeroComprobante AS numeroComprobante
            FROM MovimientoInventario m
            LEFT JOIN OrdenTrabajo o ON m.origenTipo = com.ejemplo.tallerpro.model.OrigenMovimiento.ORDEN
                AND o.id = m.origenId
            LEFT JOIN Compra c ON m.origenTipo = com.ejemplo.tallerpro.model.OrigenMovimiento.COMPRA
                AND c.id = m.origenId
            """ + FILTRO_KARDEX + " ORDER BY m.id DESC",
            countQuery = "SELECT count(m) FROM MovimientoInventario m " + FILTRO_KARDEX)
    Page<FilaKardex> buscarKardex(@Param("repuesto") Long repuesto, @Param("tipo") TipoMovimiento tipo,
                                @Param("desde") String desde, @Param("hasta") String hasta, Pageable pagina);
}
