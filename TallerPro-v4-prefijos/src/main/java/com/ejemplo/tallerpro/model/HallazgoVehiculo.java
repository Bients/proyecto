package com.ejemplo.tallerpro.model;

import com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Zona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "hallazgo_vehiculo")
@Getter
@Setter
public class HallazgoVehiculo {
    public enum Tipo {
        RAYON, ABOLLADURA, GOLPE, FUGA, RUIDO, FALLA, PIEZA_FALTANTE, DESGASTE, OTRO;
        @Override public String toString() {
            return this == RAYON ? "Rayón" : nombre(name());
        }
    }
    public enum Gravedad {
        LEVE, MEDIA, GRAVE;
        @Override public String toString() { return nombre(name()); }
    }
    public enum Origen {
        PREEXISTENTE, DETECTADO;
        @Override public String toString() { return nombre(name()); }
    }
    public enum Estado {
        ABIERTO, EN_REPARACION, REPARADO, DESCARTADO;
        @Override public String toString() {
            return this == EN_REPARACION ? "En reparación" : nombre(name());
        }
    }

    private static String nombre(String AKACvalor) {
        return AKACvalor.charAt(0) + AKACvalor.substring(1).toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "vehiculo_id", nullable = false, updatable = false)
    private Long vehiculoId;
    @Column(name = "orden_origen_id", updatable = false)
    private Long ordenOrigenId;
    @Column(name = "orden_seguimiento_id")
    private Long ordenSeguimientoId;
    @Column(name = "detalle_orden_id")
    private Long detalleOrdenId;
    @Column(name = "foto_id")
    private Long fotoId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Zona zona;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Capa capa;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Vista vista;
    private Double x;
    private Double y;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Gravedad gravedad;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false)
    private Origen origen;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Estado estado;
    @Column(nullable = false)
    private String nota;
    @Column(nullable = false, updatable = false)
    private String usuario;
    @Column(name = "usuario_actualizacion", nullable = false)
    private String usuarioActualizacion;
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private String fechaRegistro;
    @Column(name = "fecha_actualizacion", nullable = false)
    private String fechaActualizacion;
    @Column(name = "motivo_descarte")
    private String motivoDescarte;

    public void registrarCambio(String actor, Instant momento) {
        usuarioActualizacion = actor;
        fechaActualizacion = momento.toString();
    }
}
