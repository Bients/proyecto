package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Escribe el nombre del proveedor.")
    @Size(max = 120, message = "El nombre admite hasta 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank(message = "Escribe el RUC del proveedor.")
    @Pattern(regexp = "[0-9]{11}", message = "El RUC debe tener 11 dígitos.")
    @Column(nullable = false, length = 11, unique = true)
    private String ruc;

    @Size(max = 24, message = "El teléfono admite hasta 24 caracteres.")
    @Column(length = 24)
    private String telefono;
}
