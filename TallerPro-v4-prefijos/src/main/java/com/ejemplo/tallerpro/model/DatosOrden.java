package com.ejemplo.tallerpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatosOrden {

    @Column(name = "cliente_nombre", length = 121)
    private String clienteNombre;

    @Column(name = "cliente_dni", length = 8)
    private String clienteDni;

    @Column(name = "cliente_telefono", length = 9)
    private String clienteTelefono;

    @Column(name = "cliente_correo", length = 120)
    private String clienteCorreo;

    @Column(name = "vehiculo_placa", length = 8)
    private String placa;

    @Column(name = "vehiculo_marca", length = 40)
    private String marca;

    @Column(name = "vehiculo_modelo", length = 40)
    private String modelo;

    @Column(name = "vehiculo_anio")
    private Integer anio;

    public DatosOrden(Vehiculo vehiculo) {
        Cliente LJOMcliente = vehiculo.getCliente();
        clienteNombre = LJOMcliente.getNombreCompleto();
        clienteDni = LJOMcliente.getDni();
        clienteTelefono = LJOMcliente.getTelefono();
        clienteCorreo = LJOMcliente.getEmail();
        placa = vehiculo.getPlaca();
        marca = vehiculo.getMarca();
        modelo = vehiculo.getModelo();
        anio = vehiculo.getAnio();
    }

    public String getDescripcionVehiculo() {
        return marca + " " + modelo + (anio == null ? "" : " " + anio);
    }
}
