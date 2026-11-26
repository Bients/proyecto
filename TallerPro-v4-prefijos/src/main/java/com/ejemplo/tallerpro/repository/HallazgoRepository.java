package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Zona;
import com.ejemplo.tallerpro.model.HallazgoVehiculo;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Estado;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Gravedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HallazgoRepository extends JpaRepository<HallazgoVehiculo, Long> {
    @Query("""
            SELECT h FROM HallazgoVehiculo h
            WHERE h.vehiculoId = :vehiculo AND h.estado IN :estados
              AND (:capa IS NULL OR h.capa = :capa)
              AND (:gravedad IS NULL OR h.gravedad = :gravedad)
              AND (:orden IS NULL OR h.ordenOrigenId = :orden OR h.ordenSeguimientoId = :orden
                   OR EXISTS (SELECT e.id FROM HallazgoEvento e WHERE e.hallazgoId = h.id AND e.ordenId = :orden))
              AND (:desde IS NULL OR h.fechaRegistro >= :desde)
              AND (:hasta IS NULL OR h.fechaRegistro < :hasta)
            ORDER BY h.id
            """)
    List<HallazgoVehiculo> filtrar(@Param("vehiculo") Long vehiculo, @Param("estados") Collection<Estado> estados,
            @Param("capa") Capa capa, @Param("gravedad") Gravedad gravedad, @Param("orden") Long orden,
            @Param("desde") String desde, @Param("hasta") String hasta);

    @Query("""
            SELECT h FROM HallazgoVehiculo h WHERE h.vehiculoId = :vehiculo
              AND (h.estado IN :pendientes OR (h.estado = :reparado AND h.ordenSeguimientoId = :orden))
            ORDER BY h.id
            """)
    List<HallazgoVehiculo> paraOrden(@Param("vehiculo") Long vehiculo, @Param("orden") Long orden,
            @Param("pendientes") Collection<Estado> pendientes, @Param("reparado") Estado reparado);

    @Modifying
    @Query(value = "UPDATE hallazgo_vehiculo SET estado = estado WHERE id = :id", nativeQuery = true)
    void reservarEscritura(@Param("id") Long id);

    List<HallazgoVehiculo> findByOrdenSeguimientoIdOrderById(Long orden);
    boolean existsByOrdenSeguimientoId(Long orden);
    boolean existsByOrdenOrigenId(Long orden);
    boolean existsByFotoId(Long foto);
    boolean existsByVehiculoId(Long vehiculo);
    Optional<HallazgoVehiculo> findByDetalleOrdenId(Long detalle);

    @Query("SELECT DISTINCT h.zona FROM HallazgoVehiculo h WHERE h.vehiculoId = :vehiculo")
    List<Zona> zonasDelVehiculo(@Param("vehiculo") Long vehiculo);
}
