package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "compra_detalle")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;

    @Column(nullable = false)
    private int cantidad;

    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Convert(converter = CentimosConverter.class)
    @Column(name = "precio_venta_centimos")
    private BigDecimal precioVentaSugerido;

    public BigDecimal getSubtotal() {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }
}
