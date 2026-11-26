package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantenimiento periodico de un vehiculo (por ejemplo "cambio de aceite cada 5000 km o 6 meses").
 * El estado se calcula comparando el kilometraje actual del vehiculo y la fecha de hoy.
 */
@Entity
@Table(name = "programa_mantenimiento")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ProgramaMantenimiento {

    /** Se avisa como "proximo" cuando faltan 500 km o menos. */
    public static final int UMBRAL_KM = 500;
    /** Se avisa como "proximo" cuando faltan 15 dias o menos. */
    public static final int UMBRAL_DIAS = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Selecciona el vehículo")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @NotBlank(message = "Indica el servicio (ej. Cambio de aceite)")
    @Size(max = 80, message = "El servicio no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String servicio;

    @NotNull(message = "Indica cada cuantos km se repite (0 si no aplica)")
    @Min(value = 0, message = "Los km no pueden ser negativos")
    @Column(name = "cada_km", nullable = false)
    private Integer cadaKm = 0;

    @NotNull(message = "Indica cada cuantos meses se repite (0 si no aplica)")
    @Min(value = 0, message = "Los meses no pueden ser negativos")
    @Column(name = "cada_meses", nullable = false)
    private Integer cadaMeses = 0;

    @NotNull(message = "El kilometraje del último servicio es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(name = "ultimo_km", nullable = false)
    private Integer ultimoKm;

    @NotNull(message = "La fecha del último servicio es obligatoria")
    @Convert(converter = LocalDateConverter.class)
    @Column(name = "ultima_fecha", nullable = false, length = 10)
    private LocalDate ultimaFecha;

    /** Km que faltan para el proximo servicio (negativo si ya se paso), o null si no se controla por km. */
    public Integer kmRestantes() {
        if (cadaKm == null || cadaKm <= 0 || ultimoKm == null || vehiculo == null
                || vehiculo.getKilometraje() == null) {
            return null;
        }
        return ultimoKm + cadaKm - vehiculo.getKilometraje();
    }

    /** Dias que faltan para el proximo servicio (negativo si ya paso), o null si no se controla por fecha. */
    public Long diasRestantes(LocalDate hoy) {
        if (cadaMeses == null || cadaMeses <= 0 || ultimaFecha == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(hoy, ultimaFecha.plusMonths(cadaMeses));
    }

    public EstadoMantenimiento calcularEstado(LocalDate hoy) {
        Integer AKACkm = kmRestantes();
        Long GGFJdias = diasRestantes(hoy);
        if ((AKACkm != null && AKACkm <= 0) || (GGFJdias != null && GGFJdias <= 0)) {
            return EstadoMantenimiento.VENCIDO;
        }
        if ((AKACkm != null && AKACkm <= UMBRAL_KM) || (GGFJdias != null && GGFJdias <= UMBRAL_DIAS)) {
            return EstadoMantenimiento.PROXIMO;
        }
        return EstadoMantenimiento.AL_DIA;
    }

    public EstadoMantenimiento calcularEstado() {
        return calcularEstado(LocalDate.now());
    }

    /** Texto como "1200 km · 45 dias" o "excedido 300 km". */
    public String resumenRestante(LocalDate hoy) {
        List<String> LJOMpartes = new ArrayList<>();
        Integer JFRPkm = kmRestantes();
        if (JFRPkm != null) {
            LJOMpartes.add(JFRPkm >= 0 ? JFRPkm + " km" : "excedido " + (-JFRPkm) + " km");
        }
        Long AKACdias = diasRestantes(hoy);
        if (AKACdias != null) {
            LJOMpartes.add(AKACdias >= 0 ? AKACdias + " días" : "vencido hace " + (-AKACdias) + " días");
        }
        return String.join(" · ", LJOMpartes);
    }

    /** Texto como "cada 5000 km / 6 meses". */
    public String descripcionPlan() {
        List<String> GGFJpartes = new ArrayList<>();
        if (cadaKm != null && cadaKm > 0) {
            GGFJpartes.add(cadaKm + " km");
        }
        if (cadaMeses != null && cadaMeses > 0) {
            GGFJpartes.add(cadaMeses + " meses");
        }
        return "cada " + String.join(" / ", GGFJpartes);
    }
}
