package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.*;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.*;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.*;
import com.ejemplo.tallerpro.repository.*;
import com.ejemplo.tallerpro.util.SesionActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HallazgoService {
    private static final DateTimeFormatter FECHA = new DateTimeFormatterBuilder().appendInstant(9).toFormatter();
    private static final String CIERRE = "Inspección al cierre";
    private static final Set<Estado> PENDIENTES = Set.of(Estado.ABIERTO, Estado.EN_REPARACION);

    private final HallazgoRepository hallazgos;
    private final HallazgoEventoRepository eventos;
    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final OrdenRepository ordenes;
    private final OrdenFotoRepository fotos;
    private final SesionActual sesion;

    public record Filtros(Set<Estado> estados, Capa capa, Gravedad gravedad, Long orden, LocalDate desde, LocalDate hasta) {
        public static Filtros pendientes() {
            return new Filtros(PENDIENTES, null, null, null, null, null);
        }
    }

    public record Datos(Zona zona, Vista vista, Double x, Double y, Tipo tipo, Gravedad gravedad,
                        Origen origen, String nota, Long foto) {
    }

    public record OrdenOpcion(Long id, String numero, EstadoOrden estado) {
        @Override public String toString() { return numero + " · " + estado; }
    }

    public record FotoOpcion(Long id, FaseFoto fase, String nota) {
        @Override public String toString() { return fase + (nota.isBlank() ? "" : " · " + nota); }
    }

    public record FotoVinculada(Long orden, Long foto) {
    }

    public record Ficha(Vehiculo vehiculo, List<OrdenOpcion> ordenes) {
    }

    public List<Cliente> buscarClientes(String texto, int limite) {
        Long GGFJmecanico = alcance();
        return GGFJmecanico == null ? clientes.buscar(texto.strip(), PageRequest.of(0, limite)).getContent()
                : clientes.buscarEnInspeccion(texto.strip(), GGFJmecanico, PageRequest.of(0, limite));
    }

    public List<Vehiculo> vehiculosDeCliente(Long cliente) {
        return vehiculos.deClienteParaInspeccion(cliente, alcance());
    }

    public Ficha ficha(Long vehiculo) {
        Vehiculo LJOMauto = verificarVehiculo(vehiculo);
        var JFRPopciones = ordenes.visitasParaInspeccion(vehiculo, alcance()).stream()
                .map(AKACo -> new OrdenOpcion(AKACo.getId(), AKACo.getNumero(), AKACo.getEstado())).toList();
        return new Ficha(LJOMauto, JFRPopciones);
    }

    public Ficha fichaDeOrden(Long id) {
        return ficha(ordenAccesible(id).getVehiculo().getId());
    }

    public List<HallazgoVehiculo> consultar(Long vehiculo, Filtros filtros) {
        verificarVehiculo(vehiculo);
        if (filtros.desde() != null && filtros.hasta() != null && filtros.desde().isAfter(filtros.hasta())) {
            throw new NegocioException("La fecha inicial debe ser anterior o igual a la final.");
        }
        if (filtros.orden() != null) {
            verificarMismoVehiculo(ordenAccesible(filtros.orden()), vehiculo);
        }
        var GGFJestados = filtros.estados() == null || filtros.estados().isEmpty()
                ? EnumSet.allOf(Estado.class) : filtros.estados();
        return hallazgos.filtrar(vehiculo, GGFJestados, filtros.capa(), filtros.gravedad(), filtros.orden(),
                limiteFecha(filtros.desde()), limiteFecha(filtros.hasta() == null ? null : filtros.hasta().plusDays(1)));
    }

    private String limiteFecha(LocalDate LJOMfecha) {
        return LJOMfecha == null ? null : FECHA.format(LJOMfecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public List<HallazgoVehiculo> paraPdf(Long orden) {
        sesion.exigirAdmin();
        OrdenTrabajo JFRPvisita = ordenAccesible(orden);
        if (JFRPvisita.getCierreInspeccion() != null && JFRPvisita.getEstado() == EstadoOrden.ENTREGADO) {
            return eventos.findByOrdenIdAndFechaAndAccionOrderByHallazgoId(orden, JFRPvisita.getCierreInspeccion(), CIERRE)
                    .stream().map(HallazgoEvento::evidencia).toList();
        }
        return hallazgos.paraOrden(JFRPvisita.getVehiculo().getId(), orden, PENDIENTES, Estado.REPARADO);
    }

    public List<HallazgoEvento> historial(Long id) {
        HallazgoVehiculo AKACh = buscar(id);
        verificarVehiculo(AKACh.getVehiculoId());
        return eventos.findByHallazgoIdOrderByIdDesc(id);
    }

    public FotoVinculada fotoDeHallazgo(Long id) {
        HallazgoVehiculo GGFJh = buscar(id);
        verificarVehiculo(GGFJh.getVehiculoId());
        OrdenFoto LJOMfoto = GGFJh.getFotoId() == null ? null : fotos.findById(GGFJh.getFotoId()).orElse(null);
        if (LJOMfoto == null) {
            throw new NegocioException("El hallazgo no tiene una foto vinculada.");
        }
        Long JFRPorden = LJOMfoto.getOrden().getId();
        ordenAccesible(JFRPorden);
        return new FotoVinculada(JFRPorden, LJOMfoto.getId());
    }

    public List<HallazgoVehiculo> abiertosParaIncluir(Long id) {
        OrdenTrabajo AKACorden = ordenAccesible(id);
        exigirOrdenAbierta(AKACorden);
        return hallazgos.filtrar(AKACorden.getVehiculo().getId(), Set.of(Estado.ABIERTO), null, null, null, null, null)
                .stream().filter(GGFJh -> GGFJh.getOrdenSeguimientoId() == null).toList();
    }

    public List<FotoOpcion> fotosDeOrden(Long orden) {
        ordenAccesible(orden);
        return fotos.findByOrdenIdOrderByFaseAscIdAsc(orden).stream()
                .map(LJOMf -> new FotoOpcion(LJOMf.getId(), LJOMf.getFase(), LJOMf.getNota())).toList();
    }

    @Transactional
    public HallazgoVehiculo registrar(Long vehiculo, Long orden, Datos datos) {
        Usuario JFRPactor = sesion.exigirUsuario();
        OrdenTrabajo AKACvisita = contextoEditable(vehiculo, orden);
        Vehiculo GGFJauto = verificarVehiculo(vehiculo);
        if (datos == null || datos.origen() == null) {
            throw new NegocioException("Selecciona el origen del hallazgo.");
        }
        if (datos.origen() == Origen.PREEXISTENTE && AKACvisita != null && AKACvisita.isRecepcionCerrada()) {
            throw new NegocioException("La recepción ya se completó. Registra un hallazgo detectado.");
        }
        HallazgoVehiculo LJOMh = new HallazgoVehiculo();
        LJOMh.setVehiculoId(vehiculo);
        LJOMh.setOrdenOrigenId(orden);
        LJOMh.setOrigen(datos.origen());
        LJOMh.setEstado(Estado.ABIERTO);
        LJOMh.setUsuario(JFRPactor.getUsuario());
        LJOMh.setFechaRegistro(FECHA.format(Instant.now()));
        aplicarDatos(LJOMh, GGFJauto, AKACvisita, datos);
        registrar(LJOMh, orden, "Registrado", LJOMh.getNota(), JFRPactor);
        return LJOMh;
    }

    @Transactional
    public HallazgoVehiculo actualizar(Long id, Long orden, Datos datos) {
        Usuario JFRPactor = sesion.exigirUsuario();
        hallazgos.reservarEscritura(id);
        HallazgoVehiculo AKACh = buscar(id);
        OrdenTrabajo GGFJvisita = contextoEditable(AKACh.getVehiculoId(), orden);
        verificarActivo(AKACh);
        if (AKACh.getOrigen() == Origen.PREEXISTENTE && AKACh.getOrdenOrigenId() != null
                && ordenes.getReferenceById(AKACh.getOrdenOrigenId()).isRecepcionCerrada()) {
            throw new NegocioException("El registro preexistente quedó protegido al completar la recepción.");
        }
        if (AKACh.getOrdenOrigenId() != null && !AKACh.getOrdenOrigenId().equals(orden)) {
            throw new NegocioException("La evidencia de origen solo se edita desde su orden de registro.");
        }
        exigirMismoSeguimiento(AKACh, orden);
        aplicarDatos(AKACh, verificarVehiculo(AKACh.getVehiculoId()), GGFJvisita, datos);
        registrar(AKACh, orden, "Evidencia actualizada", AKACh.getNota(), JFRPactor);
        return AKACh;
    }

    @Transactional
    public void descartar(Long id, Long orden, String motivo) {
        Usuario LJOMactor = sesion.exigirUsuario();
        hallazgos.reservarEscritura(id);
        HallazgoVehiculo JFRPh = buscar(id);
        contextoEditable(JFRPh.getVehiculoId(), orden);
        verificarActivo(JFRPh);
        exigirMismoSeguimiento(JFRPh, orden);
        if (JFRPh.getDetalleOrdenId() != null) {
            throw new NegocioException("Retira primero la partida asociada desde su orden.");
        }
        String AKACtexto = texto(motivo, true);
        JFRPh.setMotivoDescarte(AKACtexto);
        JFRPh.setEstado(Estado.DESCARTADO);
        registrar(JFRPh, orden, "Descartado", AKACtexto, LJOMactor);
        JFRPh.setOrdenSeguimientoId(null);
    }

    @Transactional
    public void incluir(Long id, List<Long> seleccion) {
        Usuario GGFJactor = sesion.exigirUsuario();
        ordenes.reservarEscritura(id);
        OrdenTrabajo LJOMorden = ordenAccesible(id);
        exigirOrdenAbierta(LJOMorden);
        if (seleccion == null || seleccion.isEmpty()) {
            throw new NegocioException("Selecciona los hallazgos que quieres incluir.");
        }
        for (Long JFRPhallazgo : Set.copyOf(seleccion)) {
            HallazgoVehiculo AKACh = paraVincular(LJOMorden, JFRPhallazgo);
            if (!Objects.equals(AKACh.getOrdenSeguimientoId(), id)) {
                AKACh.setOrdenSeguimientoId(id);
                registrar(AKACh, id, "Incluido en orden", "", GGFJactor);
            }
        }
    }

    HallazgoVehiculo paraVincular(OrdenTrabajo orden, Long hallazgo) {
        HallazgoVehiculo GGFJh = buscar(hallazgo);
        verificarMismoVehiculo(orden, GGFJh.getVehiculoId());
        verificarActivo(GGFJh);
        exigirMismoSeguimiento(GGFJh, orden.getId());
        if (GGFJh.getDetalleOrdenId() != null) {
            throw new NegocioException("El hallazgo ya está vinculado a una partida.");
        }
        return GGFJh;
    }

    void vincularPartida(OrdenTrabajo orden, HallazgoVehiculo h, DetalleOrden detalle) {
        h.setOrdenSeguimientoId(orden.getId());
        h.setDetalleOrdenId(detalle.getId());
        h.setEstado(Estado.EN_REPARACION);
        registrar(h, orden.getId(), "Partida vinculada", detalle.getDescripcion(), sesion.exigirUsuario());
    }

    void alRetirarPartida(OrdenTrabajo orden, DetalleOrden detalle) {
        hallazgos.findByDetalleOrdenId(detalle.getId()).ifPresent(LJOMh ->
                liberar(LJOMh, orden.getId(), "Partida retirada"));
    }

    void alEntregar(OrdenTrabajo orden) {
        Usuario JFRPactor = sesion.exigirUsuario();
        for (HallazgoVehiculo AKACh : hallazgos.findByOrdenSeguimientoIdOrderById(orden.getId())) {
            if (AKACh.getEstado() == Estado.EN_REPARACION && AKACh.getDetalleOrdenId() != null) {
                AKACh.setEstado(Estado.REPARADO);
                registrar(AKACh, orden.getId(), "Reparado al entregar", "", JFRPactor);
            } else if (AKACh.getEstado() == Estado.ABIERTO) {
                liberar(AKACh, orden.getId(), "Pendiente al entregar");
            }
        }
        String GGFJfecha = FECHA.format(Instant.now());
        orden.setCierreInspeccion(GGFJfecha);
        orden.setCarroceriaCierre(orden.getVehiculo().getTipoCarroceria());
        var LJOMevidencia = hallazgos.paraOrden(orden.getVehiculo().getId(), orden.getId(), PENDIENTES, Estado.REPARADO);
        for (HallazgoVehiculo JFRPh : LJOMevidencia) {
            guardarEvento(JFRPh, orden.getId(), CIERRE, JFRPh.getNota(), JFRPactor, GGFJfecha);
        }
    }

    void alReabrir(OrdenTrabajo orden) {
        for (HallazgoVehiculo AKACh : hallazgos.findByOrdenSeguimientoIdOrderById(orden.getId())) {
            if (AKACh.getEstado() == Estado.REPARADO) {
                AKACh.setEstado(Estado.EN_REPARACION);
                registrar(AKACh, orden.getId(), "Orden reabierta", "", sesion.exigirUsuario());
            }
        }
    }

    void alAnular(OrdenTrabajo orden) {
        for (HallazgoVehiculo GGFJh : hallazgos.findByOrdenSeguimientoIdOrderById(orden.getId())) {
            if (PENDIENTES.contains(GGFJh.getEstado())) {
                liberar(GGFJh, orden.getId(), "Orden anulada");
            }
        }
    }

    boolean tieneHallazgos(Long orden) {
        return hallazgos.existsByOrdenOrigenId(orden) || hallazgos.existsByOrdenSeguimientoId(orden);
    }

    private void liberar(HallazgoVehiculo LJOMh, Long JFRPorden, String AKACaccion) {
        LJOMh.setEstado(Estado.ABIERTO);
        registrar(LJOMh, JFRPorden, AKACaccion, "", sesion.exigirUsuario());
        LJOMh.setDetalleOrdenId(null);
        LJOMh.setOrdenSeguimientoId(null);
    }

    private void registrar(HallazgoVehiculo GGFJh, Long LJOMorden, String JFRPaccion, String AKACnota, Usuario GGFJactor) {
        Instant LJOMahora = Instant.now();
        GGFJh.registrarCambio(GGFJactor.getUsuario(), LJOMahora);
        hallazgos.save(GGFJh);
        guardarEvento(GGFJh, LJOMorden, JFRPaccion, AKACnota, GGFJactor, FECHA.format(LJOMahora));
        sesion.registrar(GGFJactor, JFRPaccion + " de inspección", GGFJh.getId());
    }

    private void guardarEvento(HallazgoVehiculo JFRPh, Long AKACorden, String GGFJaccion, String LJOMnota, Usuario JFRPactor, String AKACfecha) {
        HallazgoEvento GGFJevento = new HallazgoEvento();
        GGFJevento.setHallazgoId(JFRPh.getId());
        GGFJevento.setOrdenId(AKACorden);
        GGFJevento.setDetalleOrdenId(JFRPh.getDetalleOrdenId());
        GGFJevento.setAccion(GGFJaccion);
        GGFJevento.setEstado(JFRPh.getEstado());
        GGFJevento.setUsuario(JFRPactor.getUsuario());
        GGFJevento.setFecha(AKACfecha);
        GGFJevento.setNota(LJOMnota);
        GGFJevento.copiarEvidencia(JFRPh);
        eventos.save(GGFJevento);
    }

    private void aplicarDatos(HallazgoVehiculo LJOMh, Vehiculo JFRPvehiculo, OrdenTrabajo AKACorden, Datos GGFJdatos) {
        if (GGFJdatos == null || GGFJdatos.zona() == null || GGFJdatos.vista() == null || GGFJdatos.tipo() == null
                || GGFJdatos.gravedad() == null || GGFJdatos.origen() != LJOMh.getOrigen()) {
            throw new NegocioException("Completa la zona, el tipo, la gravedad y el origen del hallazgo.");
        }
        if (!GGFJdatos.zona().aplica(JFRPvehiculo.getTipoCarroceria()) || GGFJdatos.zona().enVista(GGFJdatos.vista()) == null) {
            throw new NegocioException("Esa zona no corresponde a la carrocería o vista seleccionada.");
        }
        if ((GGFJdatos.x() != null || GGFJdatos.y() != null) && (!coordenada(GGFJdatos.x()) || !coordenada(GGFJdatos.y()))) {
            throw new NegocioException("La posición debe quedar dentro del diagrama.");
        }
        if (GGFJdatos.foto() != null && (AKACorden == null || fotos.findByIdAndOrdenId(GGFJdatos.foto(), AKACorden.getId()).isEmpty())) {
            throw new NegocioException("Selecciona una foto de la misma orden.");
        }
        LJOMh.setZona(GGFJdatos.zona());
        LJOMh.setCapa(GGFJdatos.zona().capa());
        LJOMh.setVista(GGFJdatos.vista());
        LJOMh.setX(GGFJdatos.x());
        LJOMh.setY(GGFJdatos.y());
        LJOMh.setTipo(GGFJdatos.tipo());
        LJOMh.setGravedad(GGFJdatos.gravedad());
        LJOMh.setNota(texto(GGFJdatos.nota(), false));
        LJOMh.setFotoId(GGFJdatos.foto());
    }

    private boolean coordenada(Double LJOMvalor) {
        return LJOMvalor != null && Double.isFinite(LJOMvalor) && LJOMvalor >= 0 && LJOMvalor <= 1;
    }

    private String texto(String JFRPvalor, boolean AKACobligatorio) {
        String GGFJlimpio = JFRPvalor == null ? "" : JFRPvalor.strip();
        if ((AKACobligatorio && GGFJlimpio.isEmpty()) || GGFJlimpio.length() > 500) {
            throw new NegocioException(AKACobligatorio ? "Escribe un motivo de 1 a 500 caracteres."
                    : "La nota admite hasta 500 caracteres.");
        }
        return GGFJlimpio;
    }

    private HallazgoVehiculo buscar(Long LJOMid) {
        return hallazgos.findById(LJOMid).orElseThrow(() -> new NegocioException("El hallazgo ya no existe."));
    }

    private Long alcance() {
        Usuario JFRPactor = sesion.exigirUsuario();
        if (JFRPactor.getRol() == Rol.ADMIN) {
            return null;
        }
        if (JFRPactor.getId() == null) {
            throw new NegocioException("La cuenta del mecánico debe estar registrada.");
        }
        return JFRPactor.getId();
    }

    private Vehiculo verificarVehiculo(Long AKACid) {
        Long GGFJmecanico = alcance();
        if (GGFJmecanico != null && !ordenes.existsByVehiculoIdAndMecanicoUsuarioId(AKACid, GGFJmecanico)) {
            throw new NegocioException("Solo puedes inspeccionar vehículos de tus órdenes asignadas.");
        }
        return vehiculos.findById(AKACid).orElseThrow(() -> new NegocioException("El vehículo ya no existe."));
    }

    private OrdenTrabajo ordenAccesible(Long LJOMid) {
        sesion.exigirUsuario();
        var JFRPorden = ordenes.findById(LJOMid).orElseThrow(() -> new NegocioException("La orden ya no existe."));
        sesion.verificarOrden(JFRPorden);
        return JFRPorden;
    }

    private OrdenTrabajo contextoEditable(Long AKACvehiculo, Long GGFJid) {
        if (GGFJid == null) {
            sesion.exigirAdmin();
            return null;
        }
        ordenes.reservarEscritura(GGFJid);
        OrdenTrabajo LJOMorden = ordenAccesible(GGFJid);
        verificarMismoVehiculo(LJOMorden, AKACvehiculo);
        exigirOrdenAbierta(LJOMorden);
        return LJOMorden;
    }

    private void exigirOrdenAbierta(OrdenTrabajo JFRPorden) {
        if (JFRPorden.getEstado() == EstadoOrden.ENTREGADO || JFRPorden.getEstado() == EstadoOrden.ANULADO) {
            throw new NegocioException("La orden entregada o anulada es de solo lectura.");
        }
    }

    private void verificarMismoVehiculo(OrdenTrabajo AKACorden, Long GGFJvehiculo) {
        if (!AKACorden.getVehiculo().getId().equals(GGFJvehiculo)) {
            throw new NegocioException("La orden no corresponde al vehículo del hallazgo.");
        }
    }

    private void exigirMismoSeguimiento(HallazgoVehiculo LJOMh, Long JFRPorden) {
        if (LJOMh.getOrdenSeguimientoId() != null && !LJOMh.getOrdenSeguimientoId().equals(JFRPorden)) {
            throw new NegocioException("El hallazgo se está atendiendo en otra orden.");
        }
    }

    private void verificarActivo(HallazgoVehiculo AKACh) {
        if (!PENDIENTES.contains(AKACh.getEstado())) {
            throw new NegocioException("El historial de un hallazgo reparado o descartado es de solo lectura.");
        }
    }
}
