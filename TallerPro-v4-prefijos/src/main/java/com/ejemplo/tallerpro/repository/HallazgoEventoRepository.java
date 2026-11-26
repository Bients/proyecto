package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.HallazgoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallazgoEventoRepository extends JpaRepository<HallazgoEvento, Long> {
    List<HallazgoEvento> findByHallazgoIdOrderByIdDesc(Long hallazgo);
    List<HallazgoEvento> findByOrdenIdAndFechaAndAccionOrderByHallazgoId(Long orden, String fecha, String accion);
}
