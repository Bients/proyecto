package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Convert(converter = LocalDateConverter.class)
    @Column(nullable = false, length = 10)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", nullable = false, length = 50)
    private String numeroComprobante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado = EstadoCompra.BORRADOR;

    @Convert(converter = CentimosConverter.class)
    @Column(name = "total_centimos", nullable = false)
    private BigDecimal total = CentimosConverter.CERO;

    @Setter(AccessLevel.NONE)
    @Convert(converter = DecimalTextoConverter.class)
    @Column(name = "total", nullable = false, updatable = false)
    private BigDecimal totalLegado;

    @PrePersist
    private void conservarColumnaLegada() {
        totalLegado = total;
    }

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CompraDetalle> detalles = new ArrayList<>();
}
