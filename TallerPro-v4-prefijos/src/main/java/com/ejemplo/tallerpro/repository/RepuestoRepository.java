package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Repuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Optional<Repuesto> findByNombre(String nombre);

    Optional<Repuesto> findBySkuIgnoreCase(String sku);

    @Query("""
            SELECT r FROM Repuesto r
            WHERE (:soloBajo = false OR r.stock <= r.stockMinimo)
              AND (LOWER(r.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(r.sku) LIKE LOWER(CONCAT('%', :texto, '%')) OR r.id = :codigo)
            ORDER BY r.nombre, r.id
            """)
    Page<Repuesto> buscar(@Param("texto") String texto, @Param("codigo") Long codigo,
                          @Param("soloBajo") boolean soloBajo, Pageable pagina);

    @Query("SELECT COUNT(r) FROM Repuesto r WHERE r.stock <= r.stockMinimo")
    long contarStockBajo();

    @Query("SELECT r FROM Repuesto r WHERE r.stock <= r.stockMinimo ORDER BY r.stock, r.nombre, r.id")
    List<Repuesto> alertasStock(Pageable pagina);
}
