package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.exception.NegocioException;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.springframework.data.domain.Page;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class PaginadorBusqueda<T> extends HBox {

    public static final int LIMITE = 15;

    private final Button anterior = new Button("Anterior");
    private final Button siguiente = new Button("Siguiente");
    private final Label estado = new Label();
    private final BusquedaAsincrona<Page<T>> busqueda = new BusquedaAsincrona<>();
    private IntFunction<Supplier<Page<T>>> consulta;
    private Consumer<Page<T>> mostrar;
    private Consumer<String> error;
    private int pagina;

    public PaginadorBusqueda() {
        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(8);
        Ui.accion(anterior, "button-outlined", "accion-secundaria");
        Ui.accion(siguiente, "button-outlined", "accion-secundaria");
        estado.setWrapText(true);
        estado.setMinHeight(Region.USE_PREF_SIZE);
        anterior.setOnAction(JFRPe -> cargar(pagina - 1, false));
        siguiente.setOnAction(LJOMe -> cargar(pagina + 1, false));
        getChildren().addAll(estado, anterior, siguiente);
        vaciar();
    }

    public void configurar(TextField campo, IntFunction<Supplier<Page<T>>> consulta,
                           Consumer<Page<T>> mostrar, Consumer<String> error) {
        configurar(consulta, mostrar, error);
        campo.textProperty().addListener((JFRPobs, AKACantes, GGFJahora) -> cargar(0, true));
    }

    public void configurar(IntFunction<Supplier<Page<T>>> consulta, Consumer<Page<T>> mostrar, Consumer<String> error) {
        this.consulta = consulta;
        this.mostrar = mostrar;
        this.error = error;
    }

    public void reiniciar() {
        cargar(0, false);
    }

    public void recargar() {
        cargar(pagina, false);
    }

    public void vaciar() {
        busqueda.cancelar();
        pagina = 0;
        estado.setText("Página 1 de 1");
        anterior.setDisable(true);
        siguiente.setDisable(true);
    }

    private void cargar(int LJOMnumero, boolean JFRPdiferida) {
        if (consulta == null) {
            return;
        }
        int AKACsolicitada = Math.max(0, LJOMnumero);
        Supplier<Page<T>> GGFJoperacion = consulta.apply(AKACsolicitada);
        anterior.setDisable(true);
        siguiente.setDisable(true);
        estado.setText("Buscando…");
        busqueda.consultar(JFRPdiferida, GGFJoperacion, LJOMdatos -> {
            int JFRPultima = Math.max(0, LJOMdatos.getTotalPages() - 1);
            if (AKACsolicitada > JFRPultima) {
                cargar(JFRPultima, false);
                return;
            }
            pagina = AKACsolicitada;
            estado.setText("Página " + (pagina + 1) + " de " + (JFRPultima + 1)
                    + " · " + LJOMdatos.getTotalElements() + " registros");
            anterior.setDisable(pagina == 0);
            siguiente.setDisable(pagina >= JFRPultima);
            mostrar.accept(LJOMdatos);
        }, AKACfallo -> {
            estado.setText("No se pudo cargar la página");
            error.accept(AKACfallo instanceof NegocioException
                    ? AKACfallo.getMessage() : "No se pudo completar la búsqueda.");
        });
    }
}
