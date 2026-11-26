package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByOrdenIdOrderByIdDesc(Long orden);

    interface ImportePorMetodo {
        String getMetodo();
        Long getCentimos();
    }

    @Query(value = "SELECT COALESCE(SUM(monto_centimos), 0) FROM pago WHERE orden_id = :orden AND anulado = 0",
            nativeQuery = true)
    long cobradoCentimos(@Param("orden") Long orden);

    @Query(value = """
            SELECT metodo, SUM(monto_centimos) AS centimos FROM pago
            WHERE fecha >= :desde AND fecha < :hasta GROUP BY metodo
            """, nativeQuery = true)
    List<ImportePorMetodo> cobrosEntre(@Param("desde") String desde, @Param("hasta") String hasta);

    @Query(value = """
            SELECT metodo, SUM(monto_centimos) AS centimos FROM pago
            WHERE fecha_anulacion >= :desde AND fecha_anulacion < :hasta GROUP BY metodo
            """, nativeQuery = true)
    List<ImportePorMetodo> anulacionesEntre(@Param("desde") String desde, @Param("hasta") String hasta);
}
