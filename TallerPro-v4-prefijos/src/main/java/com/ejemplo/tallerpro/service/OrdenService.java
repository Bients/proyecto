package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.exception.StockInsuficienteException;
import com.ejemplo.tallerpro.exception.SaldoPendienteException;
import com.ejemplo.tallerpro.model.CentimosConverter;
import com.ejemplo.tallerpro.model.DatosOrden;
import com.ejemplo.tallerpro.model.MovimientoInventario;
import com.ejemplo.tallerpro.model.OrigenMovimiento;
import com.ejemplo.tallerpro.model.TipoMovimiento;
import com.ejemplo.tallerpro.repository.MovimientoInventarioRepository;
import com.ejemplo.tallerpro.repository.DetalleOrdenRepository;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.repository.UsuarioRepository;
import com.ejemplo.tallerpro.util.Placa;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenService {

    public static final int TAMANIO_PAGINA = 15;
    public static final String MSG_ORDEN_ENTREGADA =
            "La orden ya fue entregada. Un administrador debe reabrirla para modificarla.";

    private final OrdenRepository repository;
    private final VehiculoRepository vehiculoRepository;
    private final RepuestoRepository repuestoRepository;
    private final SesionActual sesion;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventario;
    private final MovimientoInventarioRepository movimientos;
    private final DetalleOrdenRepository detalles;
    private final HallazgoService hallazgos;

    private static final BigDecimal MAXIMO_UNITARIO = new BigDecimal("999999999.99");

    public record Filtros(String texto, Long vehiculo, EstadoOrden estado, Long mecanico,
                          LocalDate desde, LocalDate hasta) {
    }

    public record ResumenHistorial(long visitas, BigDecimal cobrado, BigDecimal saldo) {
    }

    public Page<OrdenTrabajo> buscar(Filtros filtros, int pagina, int limite) {
        if (filtros.desde() != null && filtros.hasta() != null && filtros.desde().isAfter(filtros.hasta())) {
            throw new NegocioException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
        Long AKACalcance = mecanicoEnSesion();
        if (AKACalcance != null && filtros.mecanico() != null && !AKACalcance.equals(filtros.mecanico())) {
            return Page.empty(PageRequest.of(pagina, limite));
        }
        String GGFJtexto = filtros.texto() == null ? "" : filtros.texto().strip();
        var LJOMresultados = repository.buscarFiltradas(GGFJtexto, Placa.normalizar(GGFJtexto), filtros.vehiculo(), filtros.estado(),
                AKACalcance == null ? filtros.mecanico() : AKACalcance, filtros.desde(), filtros.hasta(),
                PageRequest.of(pagina, limite, Sort.by("id").descending()));
        return AKACalcance == null ? LJOMresultados : LJOMresultados.map(JFRPorden -> JFRPorden.sinCostos(false));
    }

    /**
     * Lista órdenes sin filtro de estado, con búsqueda de texto y paginación.
     * Utiliza el método repository.buscar() que busca en todos los estados.
     */
    public Page<OrdenTrabajo> listar(String filtro, int pagina, int tamanio) {
        if (mecanicoEnSesion() != null) {
            return buscar(new Filtros(filtro, null, null, null, null, null), pagina, tamanio);
        }
        Pageable AKACpageable = PageRequest.of(pagina, tamanio, Sort.by("id").descending());
        return repository.buscar(filtro != null ? filtro.trim() : "", AKACpageable);
    }

    /**
     * Lista órdenes filtradas por estado específico, con búsqueda de texto y paginación.
     * Utiliza el método repository.buscarPorEstado() que filtra por estado.
     */
    public Page<OrdenTrabajo> listarPorEstado(EstadoOrden estado, String filtro, int pagina, int tamanio) {
        if (mecanicoEnSesion() != null) {
            return buscar(new Filtros(filtro, null, estado, null, null, null), pagina, tamanio);
        }
        Pageable GGFJpageable = PageRequest.of(pagina, tamanio, Sort.by("id").descending());
        return repository.buscarPorEstado(estado, filtro != null ? filtro.trim() : "", GGFJpageable);
    }

    public OrdenTrabajo buscar(Long id) {
        return respuesta(ordenAccesible(id), true);
    }

    private OrdenTrabajo ordenAccesible(Long LJOMid) {
        sesion.exigirUsuario();
        var JFRPorden = repository.findById(LJOMid)
                .orElseThrow(() -> new NegocioException("La orden ya no existe en la base de datos."));
        sesion.verificarOrden(JFRPorden);
        return JFRPorden;
    }

    private OrdenTrabajo respuesta(OrdenTrabajo AKACorden, boolean GGFJdetallesCargados) {
        if (GGFJdetallesCargados) {
            Hibernate.initialize(AKACorden.getDetalles());
        }
        return sesion.esAdmin() ? AKACorden : AKACorden.sinCostos(GGFJdetallesCargados);
    }

    private Long mecanicoEnSesion() {
        Usuario LJOMusuario = sesion.exigirUsuario();
        if (LJOMusuario.getRol() == Rol.ADMIN) {
            return null;
        }
        if (LJOMusuario.getId() == null) {
            throw new NegocioException("La cuenta del mecánico debe estar registrada.");
        }
        return LJOMusuario.getId();
    }

    public ResumenHistorial resumenHistorial(Long vehiculoId) {
        Long JFRPmecanico = mecanicoEnSesion();
        if (JFRPmecanico != null) {
            return new ResumenHistorial(repository.countByVehiculoIdAndMecanicoUsuarioId(vehiculoId, JFRPmecanico),
                    CentimosConverter.CERO, CentimosConverter.CERO);
        }
        return resumir(repository.cobranzaVehiculo(vehiculoId, null));
    }

    private static ResumenHistorial resumir(OrdenRepository.CobranzaVehiculo AKACresumen) {
        return new ResumenHistorial(AKACresumen.getVisitas(), BigDecimal.valueOf(AKACresumen.getCobrado(), 2),
                BigDecimal.valueOf(AKACresumen.getPendiente(), 2));
    }

    /**
     * Crea o actualiza una orden. Al editar se copian solo los campos de cabecera
     * sobre la orden existente, para no perder su lista de servicios y repuestos.
     * Para una orden nueva, el kilometraje de ingreso actualiza el del vehiculo.
     */
    @Transactional
    public OrdenTrabajo guardar(@Valid OrdenTrabajo orden) {
        return guardarCabecera(orden, null);
    }

    @Transactional
    public OrdenTrabajo guardarConEntregaAutorizada(@Valid OrdenTrabajo orden, String motivo) {
        return guardarCabecera(orden, autorizarEntrega(motivo));
    }

    private OrdenTrabajo guardarCabecera(OrdenTrabajo GGFJorden, AutorizacionEntrega LJOMautorizacion) {
        Usuario JFRPactor = sesion.exigirUsuario();
        boolean AKACnueva = GGFJorden.getId() == null;
        OrdenTrabajo GGFJdestino = AKACnueva ? new OrdenTrabajo() : buscarParaEditar(GGFJorden.getId());
        if (AKACnueva && GGFJorden.getEstado() != EstadoOrden.RECIBIDO) {
            throw new NegocioException("Las órdenes nuevas deben comenzar en Recibido.");
        }
        if (!AKACnueva) {
            verificarEditable(GGFJdestino);
            if (GGFJdestino.getEstado() != GGFJorden.getEstado()) {
                verificarCambioNormal(GGFJdestino, GGFJorden.getEstado());
            }
        }

        Vehiculo LJOMvehiculo = vehiculoRepository.findById(GGFJorden.getVehiculo().getId())
                .orElseThrow(() -> new NegocioException("El vehículo seleccionado ya no existe."));
        if (AKACnueva || !LJOMvehiculo.getId().equals(GGFJdestino.getVehiculo().getId())) {
            if (!AKACnueva && tieneConsumo(GGFJdestino)) {
                throw new NegocioException("No se puede cambiar el vehículo de una orden que ya registró consumo de repuestos.");
            }
            if (!AKACnueva && hallazgos.tieneHallazgos(GGFJdestino.getId())) {
                throw new NegocioException("No se puede cambiar el vehículo de una orden con hallazgos de inspección.");
            }
            GGFJdestino.setVehiculo(LJOMvehiculo);
            GGFJdestino.setDatosHistoricos(new DatosOrden(LJOMvehiculo));
        }
        GGFJdestino.setProblema(GGFJorden.getProblema());
        if (JFRPactor.getRol() == Rol.ADMIN) {
            asignarMecanico(GGFJdestino, GGFJorden.getMecanicoUsuario());
            GGFJdestino.setDescuento(normalizarDescuento(GGFJorden.getDescuento()));
        } else {
            if (JFRPactor.getId() == null || (GGFJorden.getMecanicoUsuario() != null
                    && !JFRPactor.getId().equals(GGFJorden.getMecanicoUsuario().getId()))) {
                throw new NegocioException("Un mecánico solo puede asignarse sus propias órdenes.");
            }
            if (normalizarDescuento(GGFJorden.getDescuento()).compareTo(GGFJdestino.getDescuento()) != 0) {
                throw new NegocioException("Solo un administrador puede cambiar el descuento.");
            }
            if (AKACnueva) {
                asignarMecanico(GGFJdestino, JFRPactor);
            }
        }
        verificarImportes(GGFJdestino);
        establecerEstado(GGFJdestino, GGFJorden.getEstado(), LJOMautorizacion);
        if (AKACnueva) {
            GGFJdestino.setFechaIngreso(LocalDate.now());

            Integer JFRPkm = GGFJorden.getKilometraje();
            Integer AKACkmVehiculo = LJOMvehiculo.getKilometraje();
            if (JFRPkm == null) {
                JFRPkm = AKACkmVehiculo;
            } else if (AKACkmVehiculo != null && JFRPkm < AKACkmVehiculo) {
                throw new NegocioException("El kilometraje de ingreso (" + JFRPkm
                        + ") es menor al registrado del vehículo (" + AKACkmVehiculo
                        + "). Revisa el dato o corrige primero el vehículo.");
            }
            GGFJdestino.setKilometraje(JFRPkm);
            if (JFRPkm != null && (AKACkmVehiculo == null || JFRPkm > AKACkmVehiculo)) {
                LJOMvehiculo.setKilometraje(JFRPkm);
            }
        } else if (GGFJorden.getKilometraje() != null) {
            GGFJdestino.setKilometraje(GGFJorden.getKilometraje());
        }
        OrdenTrabajo GGFJguardada = repository.save(GGFJdestino);
        sesion.registrar(JFRPactor, AKACnueva ? "crear orden" : "editar orden", GGFJguardada.getId());
        return respuesta(GGFJguardada, true);
    }

    private OrdenTrabajo buscarParaEditar(Long LJOMid) {
        // SQLite reserva el único escritor antes de que la transacción lea el saldo.
        sesion.exigirUsuario();
        repository.reservarEscritura(LJOMid);
        OrdenTrabajo JFRPorden = ordenAccesible(LJOMid);
        Hibernate.initialize(JFRPorden.getDetalles());
        return JFRPorden;
    }

    private boolean tieneConsumo(OrdenTrabajo AKACorden) {
        return AKACorden.getDetalles().stream().anyMatch(GGFJd -> GGFJd.getTipo() == TipoDetalle.REPUESTO)
                || movimientos.existsByOrigenTipoAndOrigenIdAndTipo(
                        OrigenMovimiento.ORDEN, AKACorden.getId(), TipoMovimiento.SALIDA_ORDEN);
    }

    public Page<MovimientoInventario> trazabilidad(Long orden, Long detalle, int pagina, int limite) {
        ordenAccesible(orden);
        var LJOMresultado = movimientos.porOrigen(OrigenMovimiento.ORDEN, orden, detalle, PageRequest.of(pagina, limite));
        return sesion.esAdmin() ? LJOMresultado : LJOMresultado.map(MovimientoInventario::sinCosto);
    }

    public Optional<BigDecimal> ganancia(Long id) {
        if (!sesion.esAdmin()) {
            throw new NegocioException("Solo un administrador puede consultar la ganancia de la orden.");
        }
        OrdenTrabajo JFRPorden = buscar(id);
        if (JFRPorden.getEstado() == EstadoOrden.ANULADO
                || JFRPorden.getDetalles().stream().anyMatch(AKACd -> AKACd.getCostoUnitario() == null)) {
            return Optional.empty();
        }
        return Optional.of(JFRPorden.getDetalles().stream().map(GGFJd -> GGFJd.getSubtotal().subtract(
                GGFJd.getCostoUnitario().multiply(BigDecimal.valueOf(GGFJd.getCantidad()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add).subtract(JFRPorden.getDescuento())
                .setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private static BigDecimal validarImporte(BigDecimal LJOMvalor, int JFRPdecimales, String AKACcampo) {
        if (LJOMvalor == null || LJOMvalor.signum() < 0 || LJOMvalor.compareTo(MAXIMO_UNITARIO) > 0
                || LJOMvalor.stripTrailingZeros().scale() > JFRPdecimales) {
            throw new NegocioException("Escribe un " + AKACcampo + " válido, con hasta " + JFRPdecimales + " decimales.");
        }
        return LJOMvalor;
    }

    private void asignarMecanico(OrdenTrabajo GGFJorden, Usuario LJOMelegido) {
        if (LJOMelegido == null) {
            if (GGFJorden.getMecanicoUsuario() != null) {
                GGFJorden.setMecanicoUsuario(null);
                GGFJorden.setMecanico(null);
            }
            return;
        }
        if (LJOMelegido.getId() == null) {
            throw new NegocioException("Selecciona un usuario registrado como mecánico.");
        }
        Usuario JFRPusuario = usuarioRepository.findById(LJOMelegido.getId())
                .orElseThrow(() -> new NegocioException("El mecánico seleccionado ya no existe."));
        if (!JFRPusuario.equals(GGFJorden.getMecanicoUsuario())) {
            GGFJorden.setMecanicoUsuario(JFRPusuario);
            GGFJorden.setMecanico(JFRPusuario.getNombre());
        }
    }

    @Transactional
    public OrdenTrabajo agregarDetalle(Long ordenId, @Valid DetalleOrden solicitud) {
        OrdenTrabajo AKACorden = buscarParaEditar(ordenId);
        verificarEditable(AKACorden);
        crearDetalle(AKACorden, solicitud);
        return respuesta(repository.saveAndFlush(AKACorden), true);
    }

    @Transactional
    public OrdenTrabajo convertirHallazgo(Long ordenId, Long hallazgoId, @Valid DetalleOrden solicitud) {
        OrdenTrabajo GGFJorden = buscarParaEditar(ordenId);
        verificarEditable(GGFJorden);
        if (solicitud.getTipo() != TipoDetalle.SERVICIO && solicitud.getTipo() != TipoDetalle.REPUESTO) {
            throw new NegocioException("Convierte el hallazgo en un servicio o un repuesto de inventario.");
        }
        var LJOMhallazgo = hallazgos.paraVincular(GGFJorden, hallazgoId);
        var JFRPguardado = crearDetalle(GGFJorden, solicitud);
        hallazgos.vincularPartida(GGFJorden, LJOMhallazgo, JFRPguardado);
        return respuesta(repository.saveAndFlush(GGFJorden), true);
    }

    private DetalleOrden crearDetalle(OrdenTrabajo AKACorden, DetalleOrden GGFJsolicitud) {
        if (GGFJsolicitud.getTipo() == null || GGFJsolicitud.getCantidad() == null || GGFJsolicitud.getCantidad() < 1
                || GGFJsolicitud.getDescripcion() == null || GGFJsolicitud.getDescripcion().isBlank()
                || GGFJsolicitud.getDescripcion().length() > 120) {
            throw new NegocioException("Indica tipo, descripción de hasta 120 caracteres y cantidad positiva.");
        }

        DetalleOrden LJOMdetalle = new DetalleOrden();
        LJOMdetalle.setTipo(GGFJsolicitud.getTipo());
        LJOMdetalle.setDescripcion(GGFJsolicitud.getDescripcion());
        LJOMdetalle.setCantidad(GGFJsolicitud.getCantidad());
        if (LJOMdetalle.getTipo() == TipoDetalle.REPUESTO) {
            if (GGFJsolicitud.getRepuesto() == null || GGFJsolicitud.getRepuesto().getId() == null) {
                throw new NegocioException("Selecciona un repuesto del inventario.");
            }
            Repuesto JFRPrepuesto = repuestoRepository.findById(GGFJsolicitud.getRepuesto().getId())
                    .orElseThrow(() -> new NegocioException("El repuesto ya no existe en el inventario."));
            if (JFRPrepuesto.getStock() < LJOMdetalle.getCantidad()) {
                throw new StockInsuficienteException(mensajeStock(JFRPrepuesto.getStock(), JFRPrepuesto.getNombre()));
            }
            LJOMdetalle.setRepuesto(JFRPrepuesto);
            LJOMdetalle.registrarPrecio(JFRPrepuesto.getPrecio());
            LJOMdetalle.setCostoUnitario(JFRPrepuesto.getCostoPromedio());
        } else {
            if (GGFJsolicitud.getRepuesto() != null) {
                throw new NegocioException("Solo un repuesto de inventario puede vincularse a un artículo del catálogo.");
            }
            LJOMdetalle.registrarPrecio(validarImporte(GGFJsolicitud.getPrecioUnitario(), 2, "precio unitario"));
            if (LJOMdetalle.getTipo() == TipoDetalle.SERVICIO) {
                LJOMdetalle.setCantidad(1);
                LJOMdetalle.setCostoUnitario(BigDecimal.ZERO);
            } else if (sesion.esAdmin()) {
                LJOMdetalle.setCostoUnitario(validarImporte(GGFJsolicitud.getCostoUnitario(), 6, "costo unitario"));
            } else if (GGFJsolicitud.getCostoUnitario() != null) {
                throw new NegocioException("Solo un administrador puede registrar el costo del repuesto externo.");
            }
        }
        LJOMdetalle.setOrden(AKACorden);
        DetalleOrden AKACguardado = detalles.saveAndFlush(LJOMdetalle);
        AKACorden.getDetalles().add(AKACguardado);
        verificarImportes(AKACorden);
        if (AKACguardado.getTipo() == TipoDetalle.REPUESTO) {
            inventario.registrarOrden(AKACguardado, -AKACguardado.getCantidad(), AKACorden.getId(), "Repuesto añadido a la orden");
        }
        sesion.registrar(sesion.exigirUsuario(), "agregar partida", AKACorden.getId());
        return AKACguardado;
    }

    @Transactional
    public OrdenTrabajo quitarDetalle(Long ordenId, Long detalleId) {
        OrdenTrabajo GGFJorden = buscarParaEditar(ordenId);
        verificarEditable(GGFJorden);
        DetalleOrden LJOMdetalle = encontrarDetalle(GGFJorden, detalleId);
        devolverStock(LJOMdetalle, ordenId, "Repuesto retirado de la orden");
        hallazgos.alRetirarPartida(GGFJorden, LJOMdetalle);
        LJOMdetalle.setRetirado(true);
        GGFJorden.getDetalles().remove(LJOMdetalle);
        verificarImportes(GGFJorden);
        sesion.registrar(sesion.exigirUsuario(), "retirar partida", GGFJorden.getId());
        return respuesta(repository.saveAndFlush(GGFJorden), true);
    }

    /**
     * Edita un servicio o repuesto ya agregado. Servicio: descripcion y precio (cantidad siempre 1).
     * Repuesto: tambien la cantidad; la diferencia se descuenta o se devuelve al inventario.
     */
    @Transactional
    public OrdenTrabajo actualizarDetalle(Long ordenId, Long detalleId, String descripcion,
                                          Double precio, Integer cantidad) {
        BigDecimal JFRPimporte = precio == null || !Double.isFinite(precio) ? null : BigDecimal.valueOf(precio);
        return actualizarDetalleConPrecio(ordenId, detalleId, descripcion, JFRPimporte, cantidad);
    }

    @Transactional
    public OrdenTrabajo actualizarDetalleConPrecio(Long ordenId, Long detalleId, String descripcion,
                                                   BigDecimal precio, Integer cantidad) {
        return actualizarDetalleConPrecio(ordenId, detalleId, descripcion, precio, cantidad, null);
    }

    @Transactional
    public OrdenTrabajo actualizarDetalleConPrecio(Long ordenId, Long detalleId, String descripcion,
                                                    BigDecimal precio, Integer cantidad, BigDecimal costoPendiente) {
        OrdenTrabajo AKACorden = buscarParaEditar(ordenId);
        verificarEditable(AKACorden);
        DetalleOrden GGFJdetalle = encontrarDetalle(AKACorden, detalleId);

        String LJOMtexto = descripcion == null ? "" : descripcion.trim();
        if (LJOMtexto.isEmpty()) {
            throw new NegocioException("La descripción es obligatoria.");
        }
        if (LJOMtexto.length() > 120) {
            throw new NegocioException("La descripción no puede superar 120 caracteres.");
        }
        if (GGFJdetalle.getTipo() == TipoDetalle.SERVICIO) {
            GGFJdetalle.registrarPrecio(validarImporte(precio, 2, "precio unitario"));
        }
        if (costoPendiente != null) {
            sesion.exigirAdmin();
            if (GGFJdetalle.getTipo() != TipoDetalle.REPUESTO_EXTERNO || GGFJdetalle.getCostoUnitario() != null) {
                throw new NegocioException("Solo se puede registrar un costo pendiente de un repuesto externo.");
            }
            GGFJdetalle.setCostoUnitario(validarImporte(costoPendiente, 6, "costo unitario"));
        }
        int JFRPnueva = GGFJdetalle.getTipo() == TipoDetalle.SERVICIO ? 1 : (cantidad == null ? 0 : cantidad);
        if (JFRPnueva < 1) {
            throw new NegocioException("La cantidad debe ser al menos 1.");
        }

        if (GGFJdetalle.getTipo() == TipoDetalle.REPUESTO && GGFJdetalle.getRepuesto() != null) {
            Repuesto AKACrepuesto = repuestoRepository.findById(GGFJdetalle.getRepuesto().getId())
                    .orElseThrow(() -> new NegocioException("El repuesto ya no existe en el inventario."));
            int GGFJdiferencia = JFRPnueva - GGFJdetalle.getCantidad();
            if (GGFJdiferencia > 0 && AKACrepuesto.getStock() < GGFJdiferencia) {
                throw new StockInsuficienteException(mensajeStock(
                        (long) AKACrepuesto.getStock() + GGFJdetalle.getCantidad(), AKACrepuesto.getNombre()));
            }
            inventario.registrarOrden(GGFJdetalle, -GGFJdiferencia, ordenId, "Cantidad de repuesto editada en la orden");
        }
        GGFJdetalle.setDescripcion(LJOMtexto);
        GGFJdetalle.setCantidad(JFRPnueva);
        verificarImportes(AKACorden);
        sesion.registrar(sesion.exigirUsuario(), "editar partida", AKACorden.getId());
        return respuesta(repository.saveAndFlush(AKACorden), true);
    }

    public static List<EstadoOrden> transicionesDesde(EstadoOrden estado) {
        return switch (estado) {
            case RECIBIDO -> List.of(EstadoOrden.DIAGNOSTICO, EstadoOrden.REPARACION, EstadoOrden.ANULADO);
            case DIAGNOSTICO -> List.of(EstadoOrden.REPARACION, EstadoOrden.LISTO, EstadoOrden.ANULADO);
            case REPARACION -> List.of(EstadoOrden.LISTO, EstadoOrden.ANULADO);
            case LISTO -> List.of(EstadoOrden.ENTREGADO, EstadoOrden.ANULADO);
            case ENTREGADO, ANULADO -> List.of();
        };
    }

    @Transactional
    public OrdenTrabajo cambiarEstado(Long id, EstadoOrden estado) {
        return cambiarEstado(id, estado, null);
    }

    @Transactional
    public OrdenTrabajo entregarConSaldo(Long id, String motivo) {
        return cambiarEstado(id, EstadoOrden.ENTREGADO, autorizarEntrega(motivo));
    }

    private OrdenTrabajo cambiarEstado(Long LJOMid, EstadoOrden JFRPestado, AutorizacionEntrega AKACautorizacion) {
        OrdenTrabajo GGFJorden = buscarParaEditar(LJOMid);
        verificarCambioNormal(GGFJorden, JFRPestado);
        establecerEstado(GGFJorden, JFRPestado, AKACautorizacion);
        sesion.registrar(sesion.exigirUsuario(), "cambiar estado de orden a " + JFRPestado.name(), LJOMid);
        return respuesta(repository.save(GGFJorden), true);
    }

    private record AutorizacionEntrega(String usuario, String motivo) {
    }

    private AutorizacionEntrega autorizarEntrega(String LJOMmotivo) {
        Usuario JFRPusuario = usuarioEnSesion();
        if (JFRPusuario.getRol() != Rol.ADMIN) {
            throw new NegocioException("Solo un administrador puede autorizar una entrega con saldo pendiente.");
        }
        return new AutorizacionEntrega(JFRPusuario.getUsuario(), validarMotivo(LJOMmotivo));
    }

    private void establecerEstado(OrdenTrabajo AKACorden, EstadoOrden GGFJestado, AutorizacionEntrega LJOMautorizacion) {
        if (AKACorden.getEstado() != GGFJestado && GGFJestado == EstadoOrden.ENTREGADO && AKACorden.getSaldoPendiente().signum() > 0) {
            BigDecimal JFRPsaldo = AKACorden.getSaldoPendiente();
            if (LJOMautorizacion == null) {
                if (!sesion.esAdmin()) {
                    throw new NegocioException("Un administrador debe autorizar la entrega de esta orden.");
                }
                throw new SaldoPendienteException(JFRPsaldo);
            }
            repository.registrarEntregaConSaldo(AKACorden.getId(), new CentimosConverter().convertToDatabaseColumn(JFRPsaldo),
                    PagoService.fechaActual(), LJOMautorizacion.usuario(), LJOMautorizacion.motivo());
        }
        if (AKACorden.getEstado() != GGFJestado && GGFJestado == EstadoOrden.ENTREGADO) {
            hallazgos.alEntregar(AKACorden);
        }
        if (GGFJestado != EstadoOrden.RECIBIDO) {
            AKACorden.setRecepcionCerrada(true);
        }
        AKACorden.setEstado(GGFJestado);
    }

    private BigDecimal normalizarDescuento(BigDecimal AKACdescuento) {
        if (AKACdescuento == null) {
            return CentimosConverter.CERO;
        }
        if (AKACdescuento.signum() < 0 || AKACdescuento.stripTrailingZeros().scale() > 2) {
            throw new NegocioException("El descuento debe ser un importe no negativo con hasta dos decimales.");
        }
        return CentimosConverter.redondear(AKACdescuento);
    }

    private void verificarImportes(OrdenTrabajo GGFJorden) {
        BigDecimal LJOMsubtotal = GGFJorden.getSubtotal();
        if (GGFJorden.getDescuento().compareTo(LJOMsubtotal) > 0) {
            throw new NegocioException("El descuento supera el subtotal. Reduce el descuento para continuar.");
        }
        try {
            new CentimosConverter().convertToDatabaseColumn(LJOMsubtotal);
        } catch (ArithmeticException JFRPe) {
            throw new NegocioException("El subtotal supera el límite permitido para la orden.");
        }
    }

    @Transactional
    public OrdenTrabajo anular(Long id, String motivo, Set<Long> detallesNoInstalados) {
        Usuario AKACusuario = usuarioEnSesion();
        OrdenTrabajo GGFJorden = buscarParaEditar(id);
        verificarTransicion(GGFJorden, EstadoOrden.ANULADO);
        String LJOMtexto = validarMotivo(motivo);
        if (detallesNoInstalados == null) {
            throw new NegocioException("Indica qué repuestos no fueron instalados.");
        }
        List<DetalleOrden> JFRPseleccionados = GGFJorden.getDetalles().stream()
                .filter(AKACd -> detallesNoInstalados.contains(AKACd.getId()))
                .filter(GGFJd -> GGFJd.getTipo() == TipoDetalle.REPUESTO && GGFJd.getRepuesto() != null)
                .toList();
        if (JFRPseleccionados.size() != detallesNoInstalados.size()) {
            throw new NegocioException("La selección contiene líneas que no son repuestos de esta orden.");
        }
        for (DetalleOrden LJOMdetalle : JFRPseleccionados) {
            devolverStock(LJOMdetalle, id, LJOMtexto);
            LJOMdetalle.setDevueltoEnAnulacion(true);
        }
        GGFJorden.setMotivoAnulacion(LJOMtexto);
        GGFJorden.setFechaAnulacion(Instant.now().toString());
        GGFJorden.setUsuarioAnulacion(AKACusuario.getUsuario());
        hallazgos.alAnular(GGFJorden);
        GGFJorden.setRecepcionCerrada(true);
        GGFJorden.setEstado(EstadoOrden.ANULADO);
        sesion.registrar(AKACusuario, "anular orden", id);
        return respuesta(repository.saveAndFlush(GGFJorden), true);
    }

    @Transactional
    public OrdenTrabajo reabrir(Long id, String motivo) {
        Usuario JFRPusuario = usuarioEnSesion();
        if (JFRPusuario.getRol() != Rol.ADMIN) {
            throw new NegocioException("Solo un administrador puede reabrir una orden entregada.");
        }
        OrdenTrabajo AKACorden = buscarParaEditar(id);
        if (AKACorden.getEstado() != EstadoOrden.ENTREGADO) {
            throw new NegocioException("Solo se pueden reabrir órdenes entregadas.");
        }
        AKACorden.setMotivoReapertura(validarMotivo(motivo));
        AKACorden.setFechaReapertura(Instant.now().toString());
        AKACorden.setUsuarioReapertura(JFRPusuario.getUsuario());
        hallazgos.alReabrir(AKACorden);
        AKACorden.setEstado(EstadoOrden.LISTO);
        sesion.registrar(JFRPusuario, "reabrir orden", id);
        return respuesta(repository.save(AKACorden), true);
    }

    private void verificarCambioNormal(OrdenTrabajo GGFJorden, EstadoOrden LJOMestado) {
        if (LJOMestado == EstadoOrden.ANULADO) {
            throw new NegocioException("Usa Anular para registrar el motivo y los repuestos no instalados.");
        }
        verificarTransicion(GGFJorden, LJOMestado);
    }

    private void verificarTransicion(OrdenTrabajo JFRPorden, EstadoOrden AKACestado) {
        verificarEditable(JFRPorden);
        if (AKACestado == null || !transicionesDesde(JFRPorden.getEstado()).contains(AKACestado)) {
            throw new NegocioException("No se permite pasar de " + JFRPorden.getEstado() + " a "
                    + (AKACestado == null ? "un estado vacío" : AKACestado) + ".");
        }
    }

    private Usuario usuarioEnSesion() {
        return sesion.exigirUsuario();
    }

    private String validarMotivo(String GGFJmotivo) {
        String LJOMtexto = GGFJmotivo == null ? "" : GGFJmotivo.strip();
        if (LJOMtexto.isEmpty() || LJOMtexto.length() > 500) {
            throw new NegocioException("Escribe un motivo de 1 a 500 caracteres.");
        }
        return LJOMtexto;
    }

    public record ResumenTablero(List<OrdenRepository.ServicioFrecuente> servicios, ResumenHistorial cobranza) {
    }

    public Page<OrdenRepository.OrdenTablero> ordenesTablero(LocalDate hoy, boolean soloHoy, String placa,
                                                             int pagina, int limite) {
        String JFRPfecha = hoy.toString();
        return repository.ordenesTablero(JFRPfecha, soloHoy ? JFRPfecha : null, Placa.normalizar(placa),
                mecanicoEnSesion(), PageRequest.of(pagina, limite));
    }

    public ResumenTablero resumenTablero() {
        Long AKACmecanico = mecanicoEnSesion();
        return new ResumenTablero(repository.serviciosFrecuentes(AKACmecanico),
                AKACmecanico == null ? resumir(repository.cobranzaTaller()) : null);
    }

    private void verificarEditable(OrdenTrabajo GGFJorden) {
        if (GGFJorden.getEstado() == EstadoOrden.ENTREGADO) {
            throw new NegocioException(MSG_ORDEN_ENTREGADA);
        }
        if (GGFJorden.getEstado() == EstadoOrden.ANULADO) {
            throw new NegocioException("La orden está anulada y no se puede modificar.");
        }
    }

    private DetalleOrden encontrarDetalle(OrdenTrabajo LJOMorden, Long JFRPdetalleId) {
        return LJOMorden.getDetalles().stream()
                .filter(AKACd -> JFRPdetalleId.equals(AKACd.getId()))
                .findFirst()
                .orElseThrow(() -> new NegocioException("Ese servicio o repuesto ya no existe en la orden."));
    }

    private static String mensajeStock(long GGFJdisponible, String LJOMnombre) {
        return GGFJdisponible == 1
                ? "Solo hay 1 unidad disponible de \"" + LJOMnombre + "\"."
                : "Solo hay " + GGFJdisponible + " unidades disponibles de \"" + LJOMnombre + "\".";
    }

    private void devolverStock(DetalleOrden JFRPdetalle, Long AKACorden, String GGFJmotivo) {
        Repuesto LJOMrepuesto = JFRPdetalle.getRepuesto();
        if (LJOMrepuesto != null && JFRPdetalle.getCantidad() != null) {
            inventario.registrarOrden(JFRPdetalle, JFRPdetalle.getCantidad(), AKACorden, GGFJmotivo);
        }
    }
}
