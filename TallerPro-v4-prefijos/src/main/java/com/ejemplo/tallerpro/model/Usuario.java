package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_login", columnNames = "usuario"))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String usuario;

    /** Hash BCrypt: la clave nunca se guarda en texto plano. */
    @Column(name = "clave_hash", nullable = false, length = 100)
    private String claveHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta", nullable = false)
    private long bloqueadoHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;
}
