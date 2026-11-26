package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, updatable = false)
    private OrdenTrabajo orden;

    @Convert(converter = CentimosConverter.class)
    @Column(name = "monto_centimos", nullable = false, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MetodoPago metodo;

    @Column(nullable = false, updatable = false)
    private String fecha;

    @Column(updatable = false)
    private String nota;

    @Column(nullable = false, updatable = false)
    private String usuario;

    @Column(nullable = false)
    private boolean anulado;

    private String motivoAnulacion;
    private String fechaAnulacion;
    private String usuarioAnulacion;
}
