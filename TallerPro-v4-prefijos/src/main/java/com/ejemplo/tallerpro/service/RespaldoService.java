package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.JavaFxApplication.CierreAplicacion;
import org.springframework.context.event.EventListener;
import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.util.AlmacenFotos;
import com.ejemplo.tallerpro.util.CopiaTaller;
import com.ejemplo.tallerpro.util.Restauracion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RespaldoService {

    private final SesionActual sesion;

    private static final int COPIAS_AUTOMATICAS = 7;

    private final DataSource dataSource;
    private final AlmacenFotos fotos;

    public void crearCopia(Path destino) throws SQLException, IOException {
        var LJOMactor = sesion.exigirAdmin();
        generarCopia(destino);
        sesion.registrar(LJOMactor, "crear respaldo", "manual");
    }

    private void generarCopia(Path JFRPdestino) throws SQLException, IOException {
        Path AKACabsoluto = JFRPdestino.toAbsolutePath();
        if (!AKACabsoluto.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")
                || AKACabsoluto.getParent().toRealPath().startsWith(fotos.datos().toRealPath().resolve("fotos"))) {
            throw new NegocioException("Guarda un archivo .zip fuera del almacén de fotos.");
        }
        synchronized (fotos) {
            try (var GGFJbloqueo = fotos.bloquear(); var LJOMconexion = dataSource.getConnection()) {
                CopiaTaller.crear(LJOMconexion, fotos, AKACabsoluto);
            }
        }
    }

    public Path directorioCopias() {
        return fotos.datos().resolve("respaldos");
    }

    @EventListener
    void respaldarAlCerrar(CierreAplicacion cierre) {
        try {
            respaldoAutomatico();
        } catch (SQLException | IOException | RuntimeException JFRPe) {
            log.warn("No se pudo crear la copia automática al cerrar.", JFRPe);
        }
    }

    private void respaldoAutomatico() throws SQLException, IOException {
        Path AKACrespaldos = Files.createDirectories(directorioCopias());
        generarCopia(AKACrespaldos.resolve("auto-" + LocalDate.now() + ".zip"));
        try (var GGFJarchivos = Files.list(AKACrespaldos)) {
            List<Path> LJOMantiguos = GGFJarchivos.filter(JFRPp -> JFRPp.getFileName().toString().matches("auto-\\d{4}-\\d{2}-\\d{2}[.]zip"))
                    .sorted().toList();
            for (int AKACi = 0; AKACi < LJOMantiguos.size() - COPIAS_AUTOMATICAS; AKACi++) {
                Files.deleteIfExists(LJOMantiguos.get(AKACi));
            }
        }
        var GGFJactor = sesion.getUsuario();
        if (GGFJactor == null) {
            log.info("Copia automática de base y fotos. Operador del sistema.");
        } else {
            sesion.registrar(GGFJactor, "crear respaldo automático", "cierre");
        }
    }

    public void programarRestauracion(Path origen) throws IOException, SQLException {
        var LJOMactor = sesion.exigirAdmin();
        if (!Files.isRegularFile(origen, LinkOption.NOFOLLOW_LINKS) || Files.size(origen) > CopiaTaller.LIMITE_TOTAL) {
            throw new IOException("El archivo no es un respaldo regular del tamaño admitido.");
        }
        Path JFRPrespaldos = Files.createDirectories(directorioCopias());
        Path AKACtemporal = Files.createTempDirectory(JFRPrespaldos, ".validacion-");
        try {
            Path GGFJcopia = AKACtemporal.resolve("origen");
            Files.copy(origen, GGFJcopia);
            Path LJOMverificacion = Files.createDirectory(AKACtemporal.resolve("verificacion"));
            CopiaTaller.extraerYValidar(GGFJcopia, LJOMverificacion);
            String JFRPextension = CopiaTaller.esBaseSqlite(GGFJcopia) ? ".db" : ".zip";
            Path AKACpendiente = JFRPrespaldos.resolve("pendiente-restauracion-" + UUID.randomUUID() + JFRPextension);
            CopiaTaller.publicar(GGFJcopia, AKACpendiente);
            try {
                Restauracion.registrarPendiente(AKACpendiente);
            } catch (IOException GGFJerror) {
                try {
                    Files.deleteIfExists(AKACpendiente);
                } catch (IOException LJOMlimpieza) {
                    GGFJerror.addSuppressed(LJOMlimpieza);
                }
                throw GGFJerror;
            }
            sesion.registrar(LJOMactor, "programar restauración", AKACpendiente.getFileName());
        } finally {
            CopiaTaller.limpiarTemporal(AKACtemporal);
        }
    }
}
