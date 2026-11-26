package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Compra;
import com.ejemplo.tallerpro.model.EstadoCompra;
import com.ejemplo.tallerpro.model.TipoComprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("""
            SELECT c FROM Compra c
            WHERE (:estado IS NULL OR c.estado = :estado)
              AND (LOWER(c.numeroComprobante) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(c.proveedor.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR c.proveedor.ruc LIKE CONCAT('%', :texto, '%'))
            ORDER BY c.fecha DESC, c.id DESC
            """)
    Page<Compra> buscar(@Param("texto") String texto, @Param("estado") EstadoCompra estado, Pageable pagina);

    @Query("""
            SELECT count(c) > 0 FROM Compra c WHERE c.proveedor.id = :proveedor AND c.tipoComprobante = :tipo
                AND c.numeroComprobante = :numero AND (:id IS NULL OR c.id <> :id)
            """)
    boolean existeOtroComprobante(@Param("proveedor") Long proveedor, @Param("tipo") TipoComprobante tipo,
                                   @Param("numero") String numero, @Param("id") Long id);

    @Query("SELECT count(d) > 0 FROM CompraDetalle d WHERE d.repuesto.id = :id")
    boolean usaRepuesto(@Param("id") Long id);
}
