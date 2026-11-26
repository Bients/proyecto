package com.ejemplo.tallerpro.model;

import java.util.List;
import java.util.Set;

import static com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa.*;
import static com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista.*;
import static com.ejemplo.tallerpro.model.TipoCarroceria.*;

public final class CatalogoInspeccion {
    private CatalogoInspeccion() {
    }

    public enum Capa {
        CARROCERIA("Carrocería"), CHASIS_Y_BAJOS("Chasis y bajos"),
        MECANICA("Mecánica"), ELECTRICO_INTERIOR("Eléctrico e interior");
        private final String nombre;
        Capa(String nombre) { this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }

    public enum Vista {
        FRONTAL("Frontal"), TRASERA("Trasera"), IZQUIERDA("Lateral izquierda"),
        DERECHA("Lateral derecha"), SUPERIOR("Superior"), BAJOS("Bajos");
        private final String nombre;
        Vista(String nombre) { this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }

    public record Ubicacion(Vista vista, double x, double y, double ancho, double alto) {
    }

    public enum Zona {
        PARACHOQUES_DELANTERO(CARROCERIA, "Parachoques delantero", en(FRONTAL, .50, .72, .54, .14)),
        PARACHOQUES_TRASERO(CARROCERIA, "Parachoques trasero", en(TRASERA, .50, .74, .54, .13)),
        CAPO(CARROCERIA, "Capó", en(SUPERIOR, .23, .49, .17, .33), en(FRONTAL, .50, .50, .45, .13)),
        TECHO(CARROCERIA, "Techo", en(SUPERIOR, .52, .49, .27, .36)),
        PARABRISAS(CARROCERIA, "Parabrisas", en(FRONTAL, .50, .33, .42, .17), en(SUPERIOR, .37, .49, .11, .38)),
        GUARDABARROS_DI(CARROCERIA, "Guardabarros delantero izquierdo", en(IZQUIERDA, .26, .54, .12, .20)),
        GUARDABARROS_DD(CARROCERIA, "Guardabarros delantero derecho", en(DERECHA, .74, .54, .12, .20)),
        PUERTA_DI(CARROCERIA, "Puerta delantera izquierda", en(IZQUIERDA, .47, .58, .17, .23)),
        PUERTA_DD(CARROCERIA, "Puerta delantera derecha", en(DERECHA, .53, .58, .17, .23)),
        PUERTA_TI(CARROCERIA, "Puerta trasera izquierda", Set.of(SEDAN, HATCHBACK, SUV, PICKUP), en(IZQUIERDA, .65, .57, .16, .23)),
        PUERTA_TD(CARROCERIA, "Puerta trasera derecha", Set.of(SEDAN, HATCHBACK, SUV, PICKUP), en(DERECHA, .35, .57, .16, .23)),
        MALETERA(CARROCERIA, "Maletera", Set.of(SEDAN), en(TRASERA, .50, .52, .41, .20), en(SUPERIOR, .78, .50, .16, .36)),
        PORTON(CARROCERIA, "Portón", Set.of(HATCHBACK, SUV), en(TRASERA, .50, .47, .42, .35)),
        TOLVA(CARROCERIA, "Tolva", Set.of(PICKUP), en(SUPERIOR, .74, .50, .21, .40)),
        COMPUERTA(CARROCERIA, "Compuerta", Set.of(PICKUP), en(TRASERA, .50, .51, .48, .22)),
        CORREDIZA_IZQUIERDA(CARROCERIA, "Puerta corrediza izquierda", Set.of(FURGONETA), en(IZQUIERDA, .66, .51, .22, .35)),
        CORREDIZA_DERECHA(CARROCERIA, "Puerta corrediza derecha", Set.of(FURGONETA), en(DERECHA, .34, .51, .22, .35)),
        TRASERA_IZQUIERDA(CARROCERIA, "Puerta trasera izquierda", Set.of(FURGONETA), en(TRASERA, .39, .46, .21, .38)),
        TRASERA_DERECHA(CARROCERIA, "Puerta trasera derecha", Set.of(FURGONETA), en(TRASERA, .61, .46, .21, .38)),
        LARGUERO_IZQUIERDO(CHASIS_Y_BAJOS, "Larguero izquierdo", en(BAJOS, .49, .32, .62, .09)),
        LARGUERO_DERECHO(CHASIS_Y_BAJOS, "Larguero derecho", en(BAJOS, .49, .66, .62, .09)),
        PISO(CHASIS_Y_BAJOS, "Piso", en(BAJOS, .55, .49, .27, .15)),
        EJE_DELANTERO(CHASIS_Y_BAJOS, "Eje delantero", en(BAJOS, .27, .50, .09, .38)),
        EJE_TRASERO(CHASIS_Y_BAJOS, "Eje trasero", en(BAJOS, .74, .50, .09, .38)),
        MOTOR(MECANICA, "Motor", en(FRONTAL, .50, .59, .30, .15), en(BAJOS, .20, .49, .15, .25)),
        TRANSMISION(MECANICA, "Transmisión", en(BAJOS, .39, .50, .14, .21)),
        ESCAPE(MECANICA, "Escape", en(BAJOS, .68, .56, .21, .10)),
        NEUMATICO_DI(MECANICA, "Neumático delantero izquierdo", en(IZQUIERDA, .27, .76, .12, .21), en(BAJOS, .27, .21, .14, .12)),
        NEUMATICO_DD(MECANICA, "Neumático delantero derecho", en(DERECHA, .73, .76, .12, .21), en(BAJOS, .27, .79, .14, .12)),
        NEUMATICO_TI(MECANICA, "Neumático trasero izquierdo", en(IZQUIERDA, .75, .76, .12, .21), en(BAJOS, .74, .21, .14, .12)),
        NEUMATICO_TD(MECANICA, "Neumático trasero derecho", en(DERECHA, .25, .76, .12, .21), en(BAJOS, .74, .79, .14, .12)),
        FARO_IZQUIERDO(ELECTRICO_INTERIOR, "Faro izquierdo", en(FRONTAL, .70, .54, .14, .12)),
        FARO_DERECHO(ELECTRICO_INTERIOR, "Faro derecho", en(FRONTAL, .30, .54, .14, .12)),
        LUZ_TRASERA_IZQUIERDA(ELECTRICO_INTERIOR, "Luz trasera izquierda", en(TRASERA, .29, .57, .12, .15)),
        LUZ_TRASERA_DERECHA(ELECTRICO_INTERIOR, "Luz trasera derecha", en(TRASERA, .71, .57, .12, .15)),
        TABLERO(ELECTRICO_INTERIOR, "Tablero e instrumentos", en(SUPERIOR, .39, .50, .12, .38)),
        ASIENTOS(ELECTRICO_INTERIOR, "Asientos", en(SUPERIOR, .54, .50, .23, .38)),
        BATERIA(ELECTRICO_INTERIOR, "Batería", en(FRONTAL, .58, .62, .12, .12));

        private final Capa capa;
        private final String nombre;
        private final Set<TipoCarroceria> tipos;
        private final List<Ubicacion> ubicaciones;

        Zona(Capa capa, String nombre, Ubicacion... ubicaciones) {
            this(capa, nombre, Set.of(), ubicaciones);
        }

        Zona(Capa capa, String nombre, Set<TipoCarroceria> tipos, Ubicacion... ubicaciones) {
            this.capa = capa;
            this.nombre = nombre;
            this.tipos = tipos;
            this.ubicaciones = List.of(ubicaciones);
        }

        public Capa capa() { return capa; }
        public boolean aplica(TipoCarroceria tipo) { return tipos.isEmpty() || tipos.contains(tipo); }
        public List<Ubicacion> ubicaciones() { return ubicaciones; }
        public Ubicacion enVista(Vista vista) {
            return ubicaciones.stream().filter(AKACu -> AKACu.vista() == vista).findFirst().orElse(null);
        }
        @Override public String toString() { return nombre; }
    }

    private static Ubicacion en(Vista GGFJvista, double LJOMx, double JFRPy, double AKACancho, double GGFJalto) {
        return new Ubicacion(GGFJvista, LJOMx, JFRPy, AKACancho, GGFJalto);
    }
}
