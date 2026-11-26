package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.service.UsuarioService;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;

import java.util.List;

public final class FiltrosOrdenBusqueda extends HBox {

    private static final String TODOS = "Todos los estados";

    private final ComboBox<Object> estado = new ComboBox<>();
    private final DatePicker desde = new DatePicker();
    private final DatePicker hasta = new DatePicker();
    private final ComboBuscable<Usuario> mecanico = new ComboBuscable<>();

    public FiltrosOrdenBusqueda() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        estado.getItems().add(TODOS);
        estado.getItems().addAll(List.of(EstadoOrden.values()));
        estado.setValue(TODOS);
        estado.setAccessibleText("Filtrar por estado");
        desde.setPromptText("Desde");
        desde.setAccessibleText("Fecha inicial");
        desde.setPrefWidth(130);
        hasta.setPromptText("Hasta");
        hasta.setAccessibleText("Fecha final");
        hasta.setPrefWidth(130);
        mecanico.setPromptText("Filtrar mecánico");
        mecanico.setPrefWidth(190);
        getChildren().addAll(estado, desde, hasta, mecanico);
    }

    public void configurar(UsuarioService usuarios, boolean mostrarMecanico, Runnable cambio) {
        Ui.mostrar(mecanico, mostrarMecanico);
        mecanico.configurar(AKACtexto -> usuarios.buscar(AKACtexto, 0, ComboBuscable.LIMITE).getContent(),
                Usuario::getNombre, Ui::usuarioEnBusqueda);
        estado.valueProperty().addListener((GGFJobs, JFRPantes, AKACahora) -> cambio.run());
        desde.valueProperty().addListener((GGFJobs, LJOMantes, JFRPahora) -> cambio.run());
        hasta.valueProperty().addListener((AKACobs, GGFJantes, LJOMahora) -> cambio.run());
        mecanico.valueProperty().addListener((JFRPobs, AKACantes, GGFJahora) -> cambio.run());
    }

    public OrdenService.Filtros valores(String texto, Long vehiculo) {
        EstadoOrden LJOMseleccionado = estado.getValue() instanceof EstadoOrden JFRPvalor ? JFRPvalor : null;
        Long AKACusuario = mecanico.getValue() == null ? null : mecanico.getValue().getId();
        return new OrdenService.Filtros(texto, vehiculo, LJOMseleccionado, AKACusuario, desde.getValue(), hasta.getValue());
    }
}
