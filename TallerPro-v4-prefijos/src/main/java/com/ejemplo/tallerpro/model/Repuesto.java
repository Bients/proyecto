package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "repuesto",
        uniqueConstraints = @UniqueConstraint(name = "uk_repuesto_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Repuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del repuesto es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Integer stock = 0;

    @Size(max = 40, message = "El SKU admite hasta 40 caracteres.")
    @Column(length = 40)
    private String sku;

    @Builder.Default
    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "costo_promedio", nullable = false)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Convert(converter = CentimosConverter.class)
    @Column(name = "precio_centimos", nullable = false)
    private BigDecimal precio;

    @Setter(AccessLevel.NONE)
    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "precio", nullable = false, updatable = false)
    private BigDecimal precioLegado;

    @PrePersist
    private void conservarColumnaLegada() {
        precioLegado = precio;
    }

    public Repuesto sinCosto() {
        return Repuesto.builder().id(id).nombre(nombre).sku(sku).stock(stock).stockMinimo(stockMinimo)
                .precio(precio).costoPromedio(null).build();
    }

    public String getCodigo() {
        return sku == null ? "" : sku;
    }

    /** true cuando el stock llego al minimo configurado o por debajo. */
    public boolean esStockBajo() {
        return stock != null && stockMinimo != null && stock <= stockMinimo;
    }
}
