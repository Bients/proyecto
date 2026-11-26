package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.BatchSize;
import org.hibernate.Hibernate;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_trabajo")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated
    @Setter(AccessLevel.NONE)
    @Column(name = "numero_orden", nullable = false, insertable = false, updatable = false, length = 23)
    private String numeroOrden;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion", length = 30)
    private String fechaAnulacion;

    @Column(name = "usuario_anulacion", length = 30)
    private String usuarioAnulacion;

    @Column(name = "motivo_reapertura", length = 500)
    private String motivoReapertura;

    @Column(name = "fecha_reapertura", length = 30)
    private String fechaReapertura;

    @Column(name = "usuario_reapertura", length = 30)
    private String usuarioReapertura;

    @NotNull(message = "Selecciona un vehículo")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Embedded
    private DatosOrden datosHistoricos;

    @NotBlank(message = "Describe el problema reportado")
    @Size(max = 500, message = "El problema no puede superar 500 caracteres")
    @Column(nullable = false, length = 500)
    private String problema;

    @Size(max = 60, message = "El nombre del mecánico no puede superar 60 caracteres")
    @Column(length = 60)
    private String mecanico;

    @ManyToOne
    @JoinColumn(name = "mecanico_id")
    private Usuario mecanicoUsuario;

    @NotNull(message = "Selecciona el estado de la orden")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.RECIBIDO;

    @Convert(converter = LocalDateConverter.class)
    @Column(name = "fecha_ingreso", length = 10)
    private LocalDate fechaIngreso;

    /** Kilometraje del vehiculo al ingresar al taller (historial). */
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(name = "kilometraje")
    private Integer kilometraje;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 40)
    @SQLRestriction("retirado = 0")
    private List<DetalleOrden> detalles = new ArrayList<>();

    public DatosOrden getDatosHistoricos() {
        return datosHistoricos != null ? datosHistoricos : new DatosOrden(vehiculo);
    }

    @Convert(converter = CentimosConverter.class)
    @Column(name = "descuento_centimos", nullable = false)
    private BigDecimal descuento = CentimosConverter.CERO;

    @Formula("(SELECT COALESCE(SUM(p.monto_centimos), 0) FROM pago p WHERE p.orden_id = id AND p.anulado = 0)")
    @Setter(AccessLevel.NONE)
    private Long cobradoCentimos;

    @Formula("(SELECT COALESCE(SUM(d.precio_unitario_centimos * d.cantidad), 0) "
            + "FROM detalle_orden d WHERE d.orden_id = id AND d.retirado = 0)")
    @Setter(AccessLevel.NONE)
    private Long subtotalCentimos;

    @Column(name = "recepcion_cerrada", nullable = false)
    private boolean recepcionCerrada;

    @Column(name = "cierre_inspeccion")
    private String cierreInspeccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "carroceria_cierre", length = 20)
    private TipoCarroceria carroceriaCierre;

    @Transient
    private boolean soloCabecera;

    public OrdenTrabajo sinCostos(boolean conDetalles) {
        OrdenTrabajo JFRPcopia = new OrdenTrabajo();
        JFRPcopia.id = id;
        JFRPcopia.numeroOrden = numeroOrden;
        JFRPcopia.motivoAnulacion = motivoAnulacion;
        JFRPcopia.fechaAnulacion = fechaAnulacion;
        JFRPcopia.usuarioAnulacion = usuarioAnulacion;
        JFRPcopia.motivoReapertura = motivoReapertura;
        JFRPcopia.fechaReapertura = fechaReapertura;
        JFRPcopia.usuarioReapertura = usuarioReapertura;
        JFRPcopia.vehiculo = vehiculo;
        JFRPcopia.datosHistoricos = getDatosHistoricos();
        JFRPcopia.problema = problema;
        JFRPcopia.mecanico = mecanico;
        if (mecanicoUsuario != null) {
            Usuario AKACresponsable = new Usuario();
            AKACresponsable.setId(mecanicoUsuario.getId());
            AKACresponsable.setUsuario(mecanicoUsuario.getUsuario());
            AKACresponsable.setNombre(mecanicoUsuario.getNombre());
            AKACresponsable.setRol(mecanicoUsuario.getRol());
            JFRPcopia.mecanicoUsuario = AKACresponsable;
        }
        JFRPcopia.estado = estado;
        JFRPcopia.recepcionCerrada = recepcionCerrada;
        JFRPcopia.cierreInspeccion = cierreInspeccion;
        JFRPcopia.carroceriaCierre = carroceriaCierre;
        JFRPcopia.fechaIngreso = fechaIngreso;
        JFRPcopia.kilometraje = kilometraje;
        JFRPcopia.descuento = descuento;
        if (conDetalles) {
            JFRPcopia.detalles = detalles.stream().map(DetalleOrden::sinCosto).toList();
        } else {
            JFRPcopia.subtotalCentimos = subtotalCentimos;
            JFRPcopia.soloCabecera = true;
        }
        return JFRPcopia;
    }

    public record Importes(BigDecimal subtotal, BigDecimal total, BigDecimal cobrado,
                           BigDecimal pendiente, BigDecimal aFavor) {
    }

    public BigDecimal getSubtotal() {
        if (soloCabecera || !Hibernate.isInitialized(detalles)) {
            return BigDecimal.valueOf(subtotalCentimos == null ? 0 : subtotalCentimos, 2);
        }
        return detalles.stream().map(DetalleOrden::getSubtotal).reduce(CentimosConverter.CERO, BigDecimal::add);
    }

    public BigDecimal getTotalCobrado() {
        return BigDecimal.valueOf(cobradoCentimos == null ? 0 : cobradoCentimos, 2);
    }

    public Importes importes(BigDecimal descuento) {
        BigDecimal GGFJsubtotal = getSubtotal();
        BigDecimal LJOMtotal = GGFJsubtotal.subtract(descuento).max(CentimosConverter.CERO);
        BigDecimal JFRPcobrado = getTotalCobrado();
        BigDecimal AKACexigible = estado == EstadoOrden.ANULADO ? CentimosConverter.CERO : LJOMtotal;
        return new Importes(GGFJsubtotal, LJOMtotal, JFRPcobrado, AKACexigible.subtract(JFRPcobrado).max(CentimosConverter.CERO),
                JFRPcobrado.subtract(AKACexigible).max(CentimosConverter.CERO));
    }

    public BigDecimal getTotal() {
        return importes(descuento).total();
    }

    public BigDecimal getSaldoPendiente() {
        return importes(descuento).pendiente();
    }

    public BigDecimal getSaldoAFavor() {
        return importes(descuento).aFavor();
    }

    public EstadoPago getEstadoPago() {
        BigDecimal GGFJcobrado = getTotalCobrado();
        if (GGFJcobrado.signum() == 0) {
            return EstadoPago.SIN_PAGAR;
        }
        return GGFJcobrado.compareTo(getTotal()) >= 0 ? EstadoPago.PAGADO : EstadoPago.PARCIAL;
    }
}
