package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "movimiento_inventario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "saldo_resultante", nullable = false)
    private int saldoResultante;

    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private OrigenMovimiento origenTipo;

    @Column(name = "origen_id")
    private Long origenId;

    @ManyToOne
    @JoinColumn(name = "detalle_orden_id")
    private DetalleOrden detalleOrden;

    @Column(nullable = false, length = 24)
    private String fecha;

    @Column(nullable = false, length = 30)
    private String usuario;

    @Column(nullable = false, length = 500)
    private String motivo;

    public MovimientoInventario sinCosto() {
        var GGFJcopia = new MovimientoInventario(repuesto.sinCosto(), tipo, cantidad, saldoResultante, null,
                origenTipo, origenId, detalleOrden == null ? null : detalleOrden.sinCosto(), fecha, usuario, motivo);
        GGFJcopia.id = id;
        return GGFJcopia;
    }

    public MovimientoInventario(Repuesto repuesto, TipoMovimiento tipo, int cantidad, int saldoResultante,
                                 BigDecimal costoUnitario, OrigenMovimiento origenTipo, Long origenId,
                                 DetalleOrden detalleOrden, String fecha, String usuario, String motivo) {
        this.repuesto = repuesto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.saldoResultante = saldoResultante;
        this.costoUnitario = costoUnitario;
        this.origenTipo = origenTipo;
        this.origenId = origenId;
        this.detalleOrden = detalleOrden;
        this.fecha = fecha;
        this.usuario = usuario;
        this.motivo = motivo;
    }
}
