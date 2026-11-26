package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.*;
import com.ejemplo.tallerpro.repository.OrdenFotoRepository;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.util.AlmacenFotos;
import com.ejemplo.tallerpro.util.CopiaTaller;
import com.ejemplo.tallerpro.util.ProcesadorFotos;
import com.ejemplo.tallerpro.util.SesionActual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrdenFotoService {

    public record Tarjeta(OrdenFoto foto, byte[] miniatura, boolean editable) {
    }

    private final OrdenRepository ordenes;
    private final OrdenFotoRepository fotos;
    private final com.ejemplo.tallerpro.repository.HallazgoRepository hallazgos;
    private final AlmacenFotos archivos;
    private final SesionActual sesion;
    private final PlatformTransactionManager transacciones;

    @Transactional(readOnly = true)
    public Page<Tarjeta> galeria(Long orden, FaseFoto fase, int pagina, int limite) {
        Usuario GGFJusuario = sesion.exigirUsuario();
        ordenAccesible(orden);
        return fotos.findByOrdenIdAndFaseOrderByIdDesc(orden, fase, PageRequest.of(pagina, limite))
                .map(LJOMfoto -> new Tarjeta(visible(LJOMfoto), leer(LJOMfoto, true), puedeEditar(LJOMfoto, GGFJusuario)));
    }

    @Transactional(readOnly = true)
    public List<OrdenFoto> listar(Long orden) {
        ordenAccesible(orden);
        return fotos.findByOrdenIdOrderByFaseAscIdAsc(orden).stream().map(this::visible).toList();
    }

    @Transactional(readOnly = true)
    public byte[] original(Long orden, Long id) {
        ordenAccesible(orden);
        return leer(buscar(orden, id), false);
    }

    @Transactional(propagation = Propagation.NEVER)
    public int agregar(Long id, FaseFoto fase, List<Path> origenes) {
        Usuario JFRPusuario = operador();
        if (fase == null || origenes.isEmpty()) {
            throw new NegocioException("Selecciona una fase y al menos una foto.");
        }
        List<String> AKACcreadas = new ArrayList<>();
        synchronized (archivos) {
            try (var GGFJbloqueo = archivos.bloquear()) {
                return new TransactionTemplate(transacciones).execute(LJOMestado -> {
                    OrdenTrabajo JFRPorden = ordenarEscritura(id);
                    verificarAbierta(JFRPorden);
                    for (Path AKACorigen : origenes) {
                        ProcesadorFotos.Imagenes GGFJimagen = ProcesadorFotos.procesar(AKACorigen);
                        OrdenFoto LJOMfoto = new OrdenFoto();
                        LJOMfoto.setOrden(JFRPorden);
                        LJOMfoto.setFase(fase);
                        LJOMfoto.setRutaRelativa(guardarImagenes(JFRPorden, GGFJimagen, AKACcreadas));
                        LJOMfoto.setNota("");
                        LJOMfoto.setFecha(Instant.now().toString());
                        LJOMfoto.setUsuario(JFRPusuario.getUsuario());
                        LJOMfoto.setUsuarioId(JFRPusuario.getId());
                        fotos.save(LJOMfoto);
                    }
                    sesion.registrar(JFRPusuario, "agregar fotos", id);
                    return origenes.size();
                });
            } catch (IOException JFRPe) {
                throw new NegocioException("No se pudo acceder al almacén de fotos.");
            } catch (RuntimeException AKACe) {
                AKACcreadas.forEach(this::borrarArchivo);
                throw AKACe;
            }
        }
    }

    @Transactional
    public void guardarNota(Long orden, Long id, String nota) {
        Usuario GGFJusuario = operador();
        verificarAbierta(ordenarEscritura(orden));
        OrdenFoto LJOMfoto = buscar(orden, id);
        verificarAutor(LJOMfoto, GGFJusuario);
        String JFRPtexto = nota == null ? "" : nota.strip();
        if (JFRPtexto.length() > 500) {
            throw new NegocioException("La nota admite hasta 500 caracteres.");
        }
        LJOMfoto.setNota(JFRPtexto);
        sesion.registrar(GGFJusuario, "editar nota de foto", id);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void eliminar(Long orden, Long id) {
        Usuario AKACusuario = operador();
        synchronized (archivos) {
            try (var GGFJbloqueo = archivos.bloquear()) {
                String LJOMruta = new TransactionTemplate(transacciones).execute(JFRPestado -> {
                    verificarAbierta(ordenarEscritura(orden));
                    OrdenFoto AKACfoto = buscar(orden, id);
                    verificarAutor(AKACfoto, AKACusuario);
                    verificarRuta(AKACfoto);
                    if (hallazgos.existsByFotoId(id)) {
                        throw new NegocioException("La foto está vinculada a un hallazgo de inspección.");
                    }
                    fotos.delete(AKACfoto);
                    fotos.flush();
                    return AKACfoto.getRutaRelativa();
                });
                borrarArchivo(LJOMruta);
                borrarArchivo(AlmacenFotos.miniatura(LJOMruta));
                sesion.registrar(AKACusuario, "eliminar foto", id);
            } catch (IOException GGFJe) {
                throw new NegocioException("No se pudo acceder al almacén de fotos.");
            }
        }
    }

    @Transactional(readOnly = true)
    public Path exportarCarpeta(Long orden, Path carpeta) {
        Path LJOMsalida = null;
        try {
            ordenAccesible(orden);
            verificarDestino(carpeta);
            List<OrdenFoto> JFRPimagenes = paraExportar(orden);
            LJOMsalida = Files.createTempDirectory(carpeta, JFRPimagenes.get(0).getOrden().getNumeroOrden() + "-fotos-");
            for (OrdenFoto AKACfoto : JFRPimagenes) {
                Path GGFJarchivo = LJOMsalida.resolve(nombreExportado(AKACfoto));
                Files.createDirectories(GGFJarchivo.getParent());
                Files.write(GGFJarchivo, leer(AKACfoto, false), StandardOpenOption.CREATE_NEW);
            }
            sesion.registrar(sesion.exigirUsuario(), "exportar fotos a carpeta", orden);
            return LJOMsalida;
        } catch (IOException | RuntimeException LJOMerror) {
            if (LJOMsalida != null) {
                CopiaTaller.limpiarTemporal(LJOMsalida);
            }
            if (LJOMerror instanceof NegocioException JFRPnegocio) {
                throw JFRPnegocio;
            }
            throw new NegocioException("No se pudieron exportar las fotos.");
        }
    }

    @Transactional(readOnly = true)
    public Path exportarZip(Long orden, Path destino) {
        Path AKACtemporal = null;
        try {
            ordenAccesible(orden);
            Path GGFJabsoluto = destino.toAbsolutePath();
            verificarDestino(GGFJabsoluto.getParent());
            if (!GGFJabsoluto.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                throw new NegocioException("El archivo de exportación debe terminar en .zip.");
            }
            AKACtemporal = Files.createTempFile(GGFJabsoluto.getParent(), ".fotos-", ".zip");
            try (var LJOMzip = new ZipOutputStream(Files.newOutputStream(AKACtemporal))) {
                for (OrdenFoto JFRPfoto : paraExportar(orden)) {
                    LJOMzip.putNextEntry(new ZipEntry(nombreExportado(JFRPfoto)));
                    LJOMzip.write(leer(JFRPfoto, false));
                    LJOMzip.closeEntry();
                }
            }
            CopiaTaller.publicar(AKACtemporal, GGFJabsoluto);
            sesion.registrar(sesion.exigirUsuario(), "exportar fotos a ZIP", orden);
            return GGFJabsoluto;
        } catch (IOException AKACe) {
            throw new NegocioException("No se pudo guardar el ZIP de fotos.");
        } finally {
            if (AKACtemporal != null) {
                CopiaTaller.limpiarTemporal(AKACtemporal);
            }
        }
    }

    private List<OrdenFoto> paraExportar(Long GGFJorden) {
        List<OrdenFoto> LJOMimagenes = listar(GGFJorden);
        if (LJOMimagenes.isEmpty()) {
            throw new NegocioException("Esta orden todavía no tiene fotos.");
        }
        return LJOMimagenes;
    }

    private String nombreExportado(OrdenFoto JFRPfoto) {
        verificarRuta(JFRPfoto);
        return JFRPfoto.getFase().name() + "/" + Path.of(JFRPfoto.getRutaRelativa()).getFileName();
    }

    private void verificarDestino(Path AKACpadre) throws IOException {
        if (AKACpadre.toRealPath().startsWith(archivos.datos().toRealPath().resolve("fotos"))) {
            throw new NegocioException("Elige una carpeta fuera del almacén de fotos de TallerPro.");
        }
    }

    private String guardarImagenes(OrdenTrabajo GGFJorden, ProcesadorFotos.Imagenes LJOMimagen, List<String> JFRPcreadas) {
        try {
            String AKACruta = archivos.nuevaRuta(GGFJorden.getNumeroOrden());
            String GGFJminiatura = AlmacenFotos.miniatura(AKACruta);
            JFRPcreadas.add(AKACruta);
            archivos.escribir(AKACruta, LJOMimagen.foto());
            JFRPcreadas.add(GGFJminiatura);
            archivos.escribir(GGFJminiatura, LJOMimagen.miniatura());
            return AKACruta;
        } catch (IOException LJOMe) {
            log.warn("No se pudo guardar una foto de {}", GGFJorden.getNumeroOrden(), LJOMe);
            throw new NegocioException("No se pudo guardar una foto en el directorio de datos.");
        }
    }

    private void borrarArchivo(String JFRPruta) {
        try {
            archivos.eliminar(JFRPruta);
        } catch (IOException AKACe) {
            log.warn("No se pudo limpiar un archivo de fotos {}", JFRPruta, AKACe);
        }
    }

    private byte[] leer(OrdenFoto GGFJfoto, boolean LJOMminiatura) {
        verificarRuta(GGFJfoto);
        String JFRPruta = GGFJfoto.getRutaRelativa();
        try {
            return archivos.leer(LJOMminiatura ? AlmacenFotos.miniatura(JFRPruta) : JFRPruta);
        } catch (IOException AKACe) {
            log.warn("No se pudo leer una foto de la orden", AKACe);
            throw new NegocioException("No se encontró un archivo de foto válido para esta orden.");
        }
    }

    private void verificarRuta(OrdenFoto GGFJfoto) {
        String LJOMruta = GGFJfoto.getRutaRelativa();
        if (!AlmacenFotos.valida(LJOMruta) || LJOMruta.endsWith(".mini.jpg")
                || !LJOMruta.startsWith("fotos/" + GGFJfoto.getOrden().getNumeroOrden() + "/")) {
            throw new NegocioException("La ruta registrada no corresponde a esta orden.");
        }
    }

    private OrdenTrabajo ordenarEscritura(Long JFRPid) {
        ordenes.reservarEscritura(JFRPid);
        return ordenAccesible(JFRPid);
    }

    private OrdenTrabajo ordenAccesible(Long AKACid) {
        sesion.exigirUsuario();
        var GGFJorden = ordenes.findById(AKACid).orElseThrow(() -> new NegocioException("La orden ya no existe."));
        sesion.verificarOrden(GGFJorden);
        return GGFJorden;
    }

    private OrdenFoto visible(OrdenFoto LJOMfoto) {
        return sesion.esAdmin() ? LJOMfoto : LJOMfoto.sinCostos();
    }

    private OrdenFoto buscar(Long JFRPorden, Long AKACid) {
        return fotos.findByIdAndOrdenId(AKACid, JFRPorden)
                .orElseThrow(() -> new NegocioException("La foto no pertenece a esta orden o ya fue retirada."));
    }

    private Usuario operador() {
        Usuario GGFJusuario = sesion.getUsuario();
        if (GGFJusuario == null) {
            throw new NegocioException("Inicia sesión para gestionar fotos.");
        }
        return GGFJusuario;
    }

    private void verificarAbierta(OrdenTrabajo LJOMorden) {
        if (cerrada(LJOMorden)) {
            throw new NegocioException("Las fotos de una orden entregada o anulada son de solo lectura.");
        }
    }

    private boolean cerrada(OrdenTrabajo JFRPorden) {
        return JFRPorden.getEstado() == EstadoOrden.ENTREGADO || JFRPorden.getEstado() == EstadoOrden.ANULADO;
    }

    private boolean puedeEditar(OrdenFoto AKACfoto, Usuario GGFJusuario) {
        return GGFJusuario != null && !cerrada(AKACfoto.getOrden())
                && (GGFJusuario.getRol() == Rol.ADMIN || GGFJusuario.getId() != null && GGFJusuario.getId().equals(AKACfoto.getUsuarioId()));
    }

    private void verificarAutor(OrdenFoto LJOMfoto, Usuario JFRPusuario) {
        if (!puedeEditar(LJOMfoto, JFRPusuario)) {
            throw new NegocioException("Solo el administrador o quien subió la foto puede modificarla o retirarla.");
        }
    }
}
