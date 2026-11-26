package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "cliente",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_dni", columnNames = "dni"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotBlank(message = "El apellido del cliente es obligatorio")
    @Size(max = 60, message = "El apellido no puede superar 60 caracteres")
    @Column(nullable = false, length = 60)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    @Column(nullable = false, length = 8)
    private String dni;

    @Pattern(regexp = "|\\d{9}", message = "El teléfono debe tener 9 dígitos")
    @Column(length = 9)
    private String telefono;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
