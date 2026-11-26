package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hallazgo_evento")
@Getter
@Setter
public class HallazgoEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hallazgo_id", nullable = false)
    private Long hallazgoId;
    @Column(name = "orden_id")
    private Long ordenId;
    @Column(name = "detalle_orden_id")
    private Long detalleOrdenId;
    @Column(nullable = false)
    private String accion;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private HallazgoVehiculo.Estado estado;
    @Column(nullable = false)
    private String usuario;
    @Column(nullable = false)
    private String fecha;
    @Column(nullable = false)
    private String nota;

    @Enumerated(EnumType.STRING)
    private CatalogoInspeccion.Zona zona;
    @Enumerated(EnumType.STRING)
    private CatalogoInspeccion.Vista vista;
    private Double x;
    private Double y;
    @Enumerated(EnumType.STRING)
    private HallazgoVehiculo.Tipo tipo;
    @Enumerated(EnumType.STRING)
    private HallazgoVehiculo.Gravedad gravedad;

    public void copiarEvidencia(HallazgoVehiculo hallazgo) {
        zona = hallazgo.getZona();
        vista = hallazgo.getVista();
        x = hallazgo.getX();
        y = hallazgo.getY();
        tipo = hallazgo.getTipo();
        gravedad = hallazgo.getGravedad();
    }

    public HallazgoVehiculo evidencia() {
        HallazgoVehiculo JFRPhallazgo = new HallazgoVehiculo();
        JFRPhallazgo.setId(hallazgoId);
        JFRPhallazgo.setEstado(estado);
        JFRPhallazgo.setZona(zona);
        JFRPhallazgo.setCapa(zona.capa());
        JFRPhallazgo.setVista(vista);
        JFRPhallazgo.setX(x);
        JFRPhallazgo.setY(y);
        JFRPhallazgo.setTipo(tipo);
        JFRPhallazgo.setGravedad(gravedad);
        JFRPhallazgo.setNota(nota);
        return JFRPhallazgo;
    }
}
