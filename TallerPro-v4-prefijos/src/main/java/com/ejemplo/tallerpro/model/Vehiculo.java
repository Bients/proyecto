package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "vehiculo",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehiculo_placa", columnNames = "placa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "[A-Z0-9-]{5,8}",
            message = "La placa debe tener de 5 a 8 caracteres (letras, números o guion)")
    @Column(nullable = false, length = 8)
    private String placa;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String modelo;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1950, message = "El año debe ser 1950 o mayor")
    @Max(value = 2100, message = "El año no es válido")
    @Column(nullable = false)
    private Integer anio;

    @NotNull(message = "El kilometraje es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(nullable = false)
    private Integer kilometraje;

    @NotNull(message = "Selecciona el cliente propietario")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull(message = "Selecciona el tipo de carrocería")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carroceria", nullable = false, length = 20)
    @Builder.Default
    private TipoCarroceria tipoCarroceria = TipoCarroceria.SEDAN;

    public String getDescripcion() {
        return marca + " " + modelo + " " + anio;
    }
}
