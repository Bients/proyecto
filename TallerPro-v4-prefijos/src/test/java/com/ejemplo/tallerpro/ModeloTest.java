package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.EstadoMantenimiento;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.ProgramaMantenimiento;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Vehiculo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloTest {

    private DetalleOrden detalle(int JFRPcantidad, double AKACprecio) {
        DetalleOrden GGFJd = new DetalleOrden();
        GGFJd.setTipo(TipoDetalle.SERVICIO);
        GGFJd.setDescripcion("Servicio");
        GGFJd.setCantidad(JFRPcantidad);
        GGFJd.setPrecioUnitario(BigDecimal.valueOf(AKACprecio));
        return GGFJd;
    }

    private ProgramaMantenimiento programa(int LJOMkmActual, int JFRPultimoKm, int AKACcadaKm, int GGFJcadaMeses, LocalDate LJOMfecha) {
        Cliente JFRPc = Cliente.builder().id(1L).nombre("Ana").apellido("Lopez").dni("12345678").build();
        Vehiculo AKACv = Vehiculo.builder().id(1L).placa("ABC-123").marca("Toyota").modelo("Yaris")
                .anio(2018).kilometraje(LJOMkmActual).cliente(JFRPc).build();
        ProgramaMantenimiento GGFJp = new ProgramaMantenimiento();
        GGFJp.setVehiculo(AKACv);
        GGFJp.setServicio("Cambio de aceite");
        GGFJp.setCadaKm(AKACcadaKm);
        GGFJp.setCadaMeses(GGFJcadaMeses);
        GGFJp.setUltimoKm(JFRPultimoKm);
        GGFJp.setUltimaFecha(LJOMfecha);
        return GGFJp;
    }

    @Test
    void totalDeLaOrdenSumaTodosLosDetalles() {
        OrdenTrabajo LJOMorden = new OrdenTrabajo();
        LJOMorden.getDetalles().add(detalle(2, 45.50));
        LJOMorden.getDetalles().add(detalle(1, 100.0));

        assertThat(LJOMorden.getTotal()).isEqualByComparingTo("191.0");
    }

    @Test
    void mantenimientoPorKilometrajeCambiaDeEstado() {
        LocalDate JFRPhoy = LocalDate.of(2026, 11, 15);

        assertThat(programa(9000, 5000, 5000, 0, JFRPhoy).calcularEstado(JFRPhoy)).isEqualTo(EstadoMantenimiento.AL_DIA);
        assertThat(programa(9600, 5000, 5000, 0, JFRPhoy).calcularEstado(JFRPhoy)).isEqualTo(EstadoMantenimiento.PROXIMO);
        assertThat(programa(10500, 5000, 5000, 0, JFRPhoy).calcularEstado(JFRPhoy)).isEqualTo(EstadoMantenimiento.VENCIDO);
    }

    @Test
    void mantenimientoPorFechaCambiaDeEstado() {
        LocalDate AKAChoy = LocalDate.of(2026, 11, 15);

        assertThat(programa(1000, 1000, 0, 6, AKAChoy.minusMonths(2)).calcularEstado(AKAChoy))
                .isEqualTo(EstadoMantenimiento.AL_DIA);
        assertThat(programa(1000, 1000, 0, 6, AKAChoy.minusMonths(6).plusDays(10)).calcularEstado(AKAChoy))
                .isEqualTo(EstadoMantenimiento.PROXIMO);
        assertThat(programa(1000, 1000, 0, 6, AKAChoy.minusMonths(7)).calcularEstado(AKAChoy))
                .isEqualTo(EstadoMantenimiento.VENCIDO);
    }

    @Test
    void stockBajoCuandoLlegaAlMinimo() {
        Repuesto GGFJr = Repuesto.builder().nombre("Bujia").stock(2).stockMinimo(2).precio(new BigDecimal("10.0")).build();
        assertThat(GGFJr.esStockBajo()).isTrue();

        GGFJr.setStock(3);
        assertThat(GGFJr.esStockBajo()).isFalse();
    }
}
