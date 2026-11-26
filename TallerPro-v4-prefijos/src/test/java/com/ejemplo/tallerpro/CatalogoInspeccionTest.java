package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Zona;
import com.ejemplo.tallerpro.model.TipoCarroceria;
import com.ejemplo.tallerpro.model.HallazgoVehiculo;
import com.ejemplo.tallerpro.util.PlantillasInspeccion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoInspeccionTest {
    @Test
    void separaMarcasCoincidentesSinMoverLaPosicionRegistrada() {
        var JFRPhallazgo = new HallazgoVehiculo();
        JFRPhallazgo.setZona(Zona.CAPO);
        JFRPhallazgo.setVista(Vista.SUPERIOR);
        var AKACubicaciones = PlantillasInspeccion.ubicar(Vista.SUPERIOR,
                PlantillasInspeccion.numerar(List.of(JFRPhallazgo, JFRPhallazgo)));
        assertThat(AKACubicaciones).extracting(PlantillasInspeccion.UbicacionMarca::anclaX).containsOnly(.23 * 480);
        assertThat(AKACubicaciones).extracting(PlantillasInspeccion.UbicacionMarca::x).doesNotHaveDuplicates();
    }

    @Test
    void cadaCarroceriaTieneSeisVistasCuatroCapasYCuatroNeumaticos() throws Exception {
        assertThat(Zona.values()).extracting(Enum::name).doesNotHaveDuplicates();
        for (TipoCarroceria GGFJtipo : TipoCarroceria.values()) {
            var LJOMaplicables = Arrays.stream(Zona.values()).filter(JFRPz -> JFRPz.aplica(GGFJtipo)).toList();
            assertThat(LJOMaplicables).extracting(Zona::capa).contains(Capa.values());
            assertThat(LJOMaplicables.stream().filter(AKACz -> AKACz.name().startsWith("NEUMATICO_"))).hasSize(4);
            for (Vista GGFJvista : Vista.values()) {
                assertThat(PlantillasInspeccion.obtener(GGFJtipo, GGFJvista).trazos()).isNotEmpty();
                assertThat(LJOMaplicables).anySatisfy(LJOMz -> assertThat(LJOMz.enVista(GGFJvista)).isNotNull());
                byte[] JFRPimagen = PlantillasInspeccion.imagenPdf(GGFJtipo, GGFJvista, List.of());
                assertThat(JFRPimagen).hasSizeGreaterThan(1000);
            }
        }
        assertThat(Arrays.stream(TipoCarroceria.values()).filter(Zona.MALETERA::aplica))
                .containsExactly(TipoCarroceria.SEDAN);
        assertThat(Arrays.stream(TipoCarroceria.values()).filter(Zona.PORTON::aplica))
                .containsExactly(TipoCarroceria.HATCHBACK, TipoCarroceria.SUV);
        assertThat(Arrays.stream(TipoCarroceria.values()).filter(Zona.TOLVA::aplica))
                .containsExactly(TipoCarroceria.PICKUP);
        assertThat(Arrays.stream(TipoCarroceria.values()).filter(Zona.CORREDIZA_DERECHA::aplica))
                .containsExactly(TipoCarroceria.FURGONETA);
        for (Zona AKACzona : Zona.values()) {
            assertThat(AKACzona.toString()).isNotBlank();
            assertThat(AKACzona.ubicaciones()).extracting(GGFJu -> GGFJu.vista()).doesNotHaveDuplicates();
            assertThat(AKACzona.ubicaciones()).allSatisfy(LJOMu -> {
                assertThat(LJOMu.x()).isBetween(0.0, 1.0);
                assertThat(LJOMu.y()).isBetween(0.0, 1.0);
            });
        }
    }
}
