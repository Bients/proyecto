package com.ejemplo.tallerpro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orden_foto")
@Getter
@Setter
public class OrdenFoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, updatable = false)
    private OrdenTrabajo orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FaseFoto fase;

    @Column(name = "ruta_relativa", nullable = false, updatable = false)
    private String rutaRelativa;

    @Column(nullable = false, length = 500)
    private String nota;

    @Column(nullable = false, updatable = false)
    private String fecha;

    @Column(nullable = false, updatable = false)
    private String usuario;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    public OrdenFoto sinCostos() {
        OrdenFoto LJOMcopia = new OrdenFoto();
        LJOMcopia.id = id;
        LJOMcopia.orden = orden.sinCostos(false);
        LJOMcopia.fase = fase;
        LJOMcopia.rutaRelativa = rutaRelativa;
        LJOMcopia.nota = nota;
        LJOMcopia.fecha = fecha;
        LJOMcopia.usuario = usuario;
        LJOMcopia.usuarioId = usuarioId;
        return LJOMcopia;
    }
}
