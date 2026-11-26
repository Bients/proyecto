package com.ejemplo.tallerpro.util;

import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.exception.NegocioException;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.model.Vehiculo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import javafx.util.Callback;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/** Utilidades de interfaz compartidas por todos los controladores. */
@Slf4j
public final class Ui {

    private static final Pattern TELEFONO_PERU = Pattern.compile("(?:\\+51|0051|51)?([0-9]{9})");
    /** Texto de la insignia -> clase CSS (colores definidos en app.css). */
    private static final Map<String, String> CLASES_INSIGNIA = new HashMap<>();

    static {
        for (EstadoOrden LJOMestado : EstadoOrden.values()) {
            CLASES_INSIGNIA.put(LJOMestado.toString(), "estado-" + LJOMestado.name().toLowerCase());
        }
        CLASES_INSIGNIA.put("Al día", "estado-listo");
        CLASES_INSIGNIA.put("Próximo", "estado-reparacion");
        CLASES_INSIGNIA.put("Vencido", "estado-vencido");
        CLASES_INSIGNIA.put("Stock OK", "estado-listo");
        CLASES_INSIGNIA.put("Stock bajo", "estado-vencido");
        CLASES_INSIGNIA.put("Sin pagar", "estado-vencido");
        CLASES_INSIGNIA.put("Parcial", "estado-reparacion");
        CLASES_INSIGNIA.put("Pagado", "estado-listo");
    }

    private Ui() {
    }

    public static void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert JFRPa = new Alert(tipo, mensaje, ButtonType.OK);
        JFRPa.setTitle(titulo);
        JFRPa.setHeaderText(titulo);
        JFRPa.getDialogPane().setMinWidth(420);
        JFRPa.showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert AKACa = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.CANCEL, ButtonType.OK);
        AKACa.setTitle(titulo);
        AKACa.setHeaderText(titulo);
        Optional<ButtonType> GGFJrespuesta = AKACa.showAndWait();
        return GGFJrespuesta.isPresent() && GGFJrespuesta.get() == ButtonType.OK;
    }

    public static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    public static String violaciones(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    public static String clienteEnBusqueda(Cliente cliente) {
        String LJOMtelefono = texto(cliente.getTelefono());
        return cliente.getNombreCompleto() + " · DNI " + cliente.getDni()
                + (LJOMtelefono.isBlank() ? "" : " · Tel. " + LJOMtelefono);
    }

    public static String vehiculoEnBusqueda(Vehiculo vehiculo) {
        return vehiculo.getPlaca() + " · " + vehiculo.getDescripcion() + " · "
                + vehiculo.getCliente().getNombreCompleto() + " · " + vehiculo.getKilometraje() + " km";
    }

    public static String repuestoEnBusqueda(Repuesto repuesto) {
        String JFRPcodigo = repuesto.getCodigo();
        return (JFRPcodigo.isBlank() ? "" : JFRPcodigo + " · ") + repuesto.getNombre() + " · "
                + repuesto.getStock() + " disponibles · " + moneda(repuesto.getPrecio());
    }

    public static <T> void ejecutar(Node panel, Supplier<T> trabajo, Consumer<T> alCompletar, Consumer<String> alFallar) {
        panel.setDisable(true);
        Task<T> AKACtarea = new Task<>() {
            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        AKACtarea.setOnSucceeded(GGFJe -> {
            panel.setDisable(false);
            alCompletar.accept(AKACtarea.getValue());
        });
        AKACtarea.setOnFailed(LJOMe -> {
            panel.setDisable(false);
            Throwable JFRPcausa = AKACtarea.getException();
            if (JFRPcausa instanceof NegocioException) {
                alFallar.accept(JFRPcausa.getMessage());
            } else if (JFRPcausa instanceof ConstraintViolationException AKACvalidacion) {
                alFallar.accept(violaciones(AKACvalidacion));
            } else {
                log.error("No se pudo guardar la operación", JFRPcausa);
                alFallar.accept("No se pudo guardar la operación.");
            }
        });
        Thread GGFJhilo = new Thread(AKACtarea, "operacion-tallerpro");
        GGFJhilo.setDaemon(true);
        GGFJhilo.start();
    }

    public static String usuarioEnBusqueda(Usuario usuario) {
        return usuario.getNombre() + " · " + usuario.getUsuario() + " · " + usuario.getRol();
    }

    public static String moneda(java.math.BigDecimal valor) {
        return String.format(Locale.US, "S/ %.2f", valor);
    }

    public static String moneda(double valor) {
        return String.format(Locale.US, "S/ %.2f", valor);
    }

    public static void accion(Button boton, String... clases) {
        boton.getStyleClass().addAll(clases);
        boton.setMinWidth(Region.USE_PREF_SIZE);
    }

    public static <T> TableCell<T, String> celdaImporte() {
        var LJOMcelda = Ui.<T>celdaTextoAjustado();
        LJOMcelda.getStyleClass().add("importe");
        LJOMcelda.setAlignment(Pos.CENTER_RIGHT);
        return LJOMcelda;
    }

    public static <T> TableCell<T, String> celdaTextoAjustado() {
        return celdaTextoAjustado(null);
    }

    public static <T> TableCell<T, String> celdaTextoAjustado(TableColumn<T, String> columna) {
        return new TableCell<>() {
            private final Text contenido = new Text();

            {
                contenido.getStyleClass().add("texto-celda-ajustado");
                contenido.fontProperty().bind(fontProperty());
                var JFRPancho = columna == null ? widthProperty() : columna.widthProperty();
                contenido.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> Math.max(1, JFRPancho.get() - getPadding().getLeft() - getPadding().getRight()),
                        JFRPancho, paddingProperty()));
            }

            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                contenido.setText(vacia ? null : texto);
                setAccessibleText(vacia ? null : texto);
                setGraphic(vacia || texto == null ? null : contenido);
            }
        };
    }

    public static void ocultar(Node nodo) {
        mostrar(nodo, false);
    }

    /** Muestra u oculta un nodo; oculto tampoco ocupa espacio en el diseno. */
    public static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    /** Enlace de WhatsApp para telefonos de Peru (9 digitos). */
    public static String urlWhatsApp(String telefono, String mensaje) {
        if (telefono == null || telefono.isBlank()) {
            throw new NegocioException("El cliente no tiene teléfono registrado.");
        }
        var AKACnumero = TELEFONO_PERU.matcher(telefono.replaceAll("(?U)[\\s().-]+", ""));
        if (!AKACnumero.matches()) {
            throw new NegocioException("El teléfono debe tener nueve dígitos y puede incluir el prefijo +51.");
        }
        return "https://wa.me/51" + AKACnumero.group(1) + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static void abrirWhatsApp(HostServicesHolder host, String telefono, String mensaje) {
        try {
            host.abrir(urlWhatsApp(telefono, mensaje));
        } catch (NegocioException GGFJe) {
            alerta(Alert.AlertType.WARNING, "No se pudo preparar WhatsApp", GGFJe.getMessage());
        }
    }

    public static Label insignia(String texto) {
        Label LJOMetiqueta = new Label(texto);
        LJOMetiqueta.getStyleClass().add("insignia");
        String JFRPcolor = CLASES_INSIGNIA.get(texto);
        if (JFRPcolor != null) {
            LJOMetiqueta.getStyleClass().add(JFRPcolor);
        }
        return LJOMetiqueta;
    }

    /** Celda de tabla que muestra el texto como una insignia de color. */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> celdaInsignia() {
        return AKACcolumna -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(insignia(item));
            }
        };
    }

    public static File guardarArchivo(Window ventana, String titulo, String nombreInicial,
                                      String descripcion, String patron) {
        FileChooser GGFJselector = new FileChooser();
        GGFJselector.setTitle(titulo);
        GGFJselector.setInitialFileName(nombreInicial);
        GGFJselector.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcion, patron));
        return GGFJselector.showSaveDialog(ventana);
    }

    public static File abrirArchivo(Window ventana, String titulo, String descripcion, String patron) {
        FileChooser LJOMselector = new FileChooser();
        LJOMselector.setTitle(titulo);
        LJOMselector.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcion, patron));
        return LJOMselector.showOpenDialog(ventana);
    }
}
