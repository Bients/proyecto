package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_orden")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @NotNull(message = "Selecciona el tipo (servicio o repuesto)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDetalle tipo;

    @NotBlank(message = "La descripción del servicio o repuesto es obligatoria")
    @Size(max = 120, message = "La descripción no puede superar 120 caracteres")
    @Column(nullable = false, length = 120)
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Column(nullable = false)
    private Integer cantidad;

    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Convert(converter = CentimosConverter.class)
    @Column(name = "precio_unitario_centimos", nullable = false)
    private BigDecimal precioUnitario;

    @Setter(AccessLevel.NONE)
    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "precio_unitario", nullable = false, updatable = false)
    private BigDecimal precioLegado;

    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private boolean retirado;

    /** Obligatorio para una partida de inventario. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "repuesto_id")
    private Repuesto repuesto;

    @Column(name = "devuelto_en_anulacion", nullable = false)
    private boolean devueltoEnAnulacion;

    @PrePersist
    private void conservarColumnaLegada() {
        precioLegado = precioUnitario;
    }

    public void registrarPrecio(BigDecimal precio) {
        precioUnitario = CentimosConverter.redondear(precio);
    }

    public DetalleOrden sinCosto() {
        DetalleOrden LJOMcopia = new DetalleOrden();
        LJOMcopia.id = id;
        LJOMcopia.tipo = tipo;
        LJOMcopia.descripcion = descripcion;
        LJOMcopia.cantidad = cantidad;
        LJOMcopia.precioUnitario = precioUnitario;
        LJOMcopia.retirado = retirado;
        LJOMcopia.repuesto = repuesto == null ? null : repuesto.sinCosto();
        LJOMcopia.devueltoEnAnulacion = devueltoEnAnulacion;
        return LJOMcopia;
    }

    public BigDecimal getSubtotal() {
        return cantidad == null || precioUnitario == null ? CentimosConverter.CERO
                : CentimosConverter.redondear(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }
}
