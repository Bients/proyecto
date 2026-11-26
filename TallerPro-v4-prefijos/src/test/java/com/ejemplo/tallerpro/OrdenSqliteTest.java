package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.exception.StockInsuficienteException;
import com.ejemplo.tallerpro.exception.SaldoPendienteException;
import com.ejemplo.tallerpro.model.EstadoPago;
import com.ejemplo.tallerpro.model.TipoCarroceria;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Capa;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Vista;
import com.ejemplo.tallerpro.model.CatalogoInspeccion.Zona;
import com.ejemplo.tallerpro.model.HallazgoVehiculo;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Estado;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Gravedad;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Origen;
import com.ejemplo.tallerpro.model.HallazgoVehiculo.Tipo;
import com.ejemplo.tallerpro.service.HallazgoService;
import com.ejemplo.tallerpro.service.HallazgoService.Datos;
import com.ejemplo.tallerpro.service.HallazgoService.Filtros;
import com.ejemplo.tallerpro.model.MetodoPago;
import com.ejemplo.tallerpro.service.PagoService;
import com.ejemplo.tallerpro.service.PdfService;
import com.ejemplo.tallerpro.service.OrdenFotoService;
import com.ejemplo.tallerpro.service.RespaldoService;
import com.ejemplo.tallerpro.service.UsuarioService;
import com.ejemplo.tallerpro.service.MantenimientoService;
import com.ejemplo.tallerpro.service.IaService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import com.ejemplo.tallerpro.model.FaseFoto;
import com.ejemplo.tallerpro.util.AlmacenFotos;
import com.ejemplo.tallerpro.util.Restauracion;
import com.ejemplo.tallerpro.util.Rutas;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.ejemplo.tallerpro.service.ReporteService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.model.Vehiculo;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.Proveedor;
import com.ejemplo.tallerpro.model.EstadoCompra;
import com.ejemplo.tallerpro.model.TipoComprobante;
import com.ejemplo.tallerpro.model.TipoMovimiento;
import com.ejemplo.tallerpro.model.OrigenMovimiento;
import com.ejemplo.tallerpro.model.MovimientoInventario;
import com.ejemplo.tallerpro.service.ClienteService;
import com.ejemplo.tallerpro.service.CompraService;
import com.ejemplo.tallerpro.service.InventarioService;
import com.ejemplo.tallerpro.service.ProveedorService;
import com.ejemplo.tallerpro.repository.ClienteRepository;
import com.ejemplo.tallerpro.repository.UsuarioRepository;
import com.ejemplo.tallerpro.service.VehiculoService;
import com.ejemplo.tallerpro.service.RepuestoService;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.service.OrdenService;
import com.ejemplo.tallerpro.util.MigracionesSqlite;
import com.ejemplo.tallerpro.util.SesionActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenSqliteTest {

    @TempDir
    Path carpeta;

    @Test
    void migraEsquemaLegadoConDatosRespaldoYNumeracion() throws Exception {
        Path GGFJbase = crearBaseLegada();
        try (var LJOMctx = arrancar(GGFJbase)) {
            var JFRPsql = LJOMctx.getBean(JdbcTemplate.class);
            var AKACservicio = LJOMctx.getBean(OrdenService.class);
            assertThat(JFRPsql.queryForObject("PRAGMA user_version", Integer.class)).isEqualTo(9);
            assertThat(JFRPsql.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
            assertThat(JFRPsql.queryForList("PRAGMA foreign_key_check")).isEmpty();
            assertThat(JFRPsql.queryForList("PRAGMA index_list('orden_trabajo')"))
                    .anyMatch(GGFJindice -> "uk_orden_numero".equals(GGFJindice.get("name"))
                            && Integer.valueOf(1).equals(GGFJindice.get("unique")));

            OrdenTrabajo LJOManterior = AKACservicio.buscar(41L);
            assertThat(LJOManterior.getNumeroOrden()).isEqualTo("OT-0041");
            assertThat(LJOManterior.getEstado()).isEqualTo(EstadoOrden.REPARACION);
            assertThat(LJOManterior.getVehiculo().getCliente().getDni()).isEqualTo("12345678");
            assertThat(LJOManterior.getDetalles()).extracting(DetalleOrden::getId).containsExactlyInAnyOrder(7L, 8L);
            assertThat(stock(JFRPsql)).isEqualTo(5);
            assertThat(JFRPsql.queryForObject("SELECT ultimo_km FROM programa_mantenimiento", Integer.class)).isEqualTo(100);
            assertThat(JFRPsql.queryForObject("SELECT clave_hash FROM usuario", String.class)).isEqualTo("hash-legado");

            OrdenTrabajo JFRPnueva = new OrdenTrabajo();
            JFRPnueva.setVehiculo(LJOMctx.getBean(VehiculoRepository.class).findById(21L).orElseThrow());
            JFRPnueva.setProblema("Nueva orden");
            OrdenTrabajo AKACguardada = AKACservicio.guardar(JFRPnueva);
            assertThat(AKACguardada.getNumeroOrden()).isEqualTo("OT-0042");
            assertThat(AKACservicio.listar("ot-0042", 0, 15).getContent()).extracting(OrdenTrabajo::getId)
                    .containsExactly(AKACguardada.getId());
        }
        Path GGFJrespaldo;
        try (var LJOMarchivos = Files.list(carpeta.resolve("respaldos"))) {
            var JFRPrespaldos = LJOMarchivos.toList();
            assertThat(JFRPrespaldos).hasSize(1);
            GGFJrespaldo = JFRPrespaldos.get(0);
        }
        try (var AKACconexion = DriverManager.getConnection(url(GGFJrespaldo));
             var GGFJsql = AKACconexion.createStatement();
             var LJOMorden = GGFJsql.executeQuery("SELECT estado FROM orden_trabajo WHERE id = 41")) {
            assertThat(LJOMorden.next()).isTrue();
            assertThat(LJOMorden.getString(1)).isEqualTo("REPARACION");
        }

        try (var JFRPctx = arrancar(GGFJbase);
             var AKACarchivos = Files.list(carpeta.resolve("respaldos"))) {
            assertThat(JFRPctx.getBean(OrdenService.class).buscar(42L).getNumeroOrden()).isEqualTo("OT-0042");
            assertThat(AKACarchivos.count()).isEqualTo(1);
        }
    }

    @Test
    void creaBaseNuevaAntesDeConsultarConJpa() {
        try (var GGFJctx = arrancar(carpeta.resolve("tallerpro-v2.db"))) {
            var LJOMsql = GGFJctx.getBean(JdbcTemplate.class);
            assertThat(LJOMsql.queryForObject("PRAGMA user_version", Integer.class)).isEqualTo(9);
            assertThat(GGFJctx.getBean(OrdenRepository.class).count()).isZero();
            assertThat(carpeta.resolve("respaldos")).doesNotExist();
        }
    }

    @Test
    void anulacionConservaLineasDevuelveSoloLasMarcadasYNoRepite() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACsql = JFRPctx.getBean(JdbcTemplate.class);
            var GGFJservicio = JFRPctx.getBean(OrdenService.class);
            AKACsql.update("UPDATE orden_trabajo SET mecanico_id = 1 WHERE id = 41");
            iniciarSesion(JFRPctx, Rol.MECANICO);

            assertThatThrownBy(() -> GGFJservicio.anular(41L, "Selección inválida", Set.of(7L, 99L)))
                    .isInstanceOf(NegocioException.class);
            assertThat(stock(AKACsql)).isEqualTo(5);

            GGFJservicio.anular(41L, "  Cliente canceló  ", Set.of(7L));
            assertThat(stock(AKACsql)).isEqualTo(7);
            OrdenTrabajo LJOManulada = GGFJservicio.buscar(41L);
            assertThat(LJOManulada.getEstado()).isEqualTo(EstadoOrden.ANULADO);
            assertThat(LJOManulada.getMotivoAnulacion()).isEqualTo("Cliente canceló");
            assertThat(LJOManulada.getFechaAnulacion()).isNotBlank();
            assertThat(LJOManulada.getUsuarioAnulacion()).isEqualTo("operador");
            assertThat(LJOManulada.getDetalles()).hasSize(2).filteredOn(DetalleOrden::isDevueltoEnAnulacion)
                    .extracting(DetalleOrden::getId).containsExactly(7L);
            assertThatThrownBy(() -> GGFJservicio.anular(41L, "Reintento", Set.of(8L)))
                    .isInstanceOf(NegocioException.class);
            assertThatThrownBy(() -> GGFJservicio.guardar(LJOManulada)).isInstanceOf(NegocioException.class);
            assertThatThrownBy(() -> GGFJservicio.quitarDetalle(41L, 7L)).isInstanceOf(NegocioException.class);
            assertThat(stock(AKACsql)).isEqualTo(7);
        }
    }

    @Test
    void rollbackRealRevierteStockDetalleYAnulacionDespuesDeFlush() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACsql = JFRPctx.getBean(JdbcTemplate.class);
            var GGFJservicio = JFRPctx.getBean(OrdenService.class);
            iniciarSesion(JFRPctx, Rol.ADMIN);
            var LJOMtransaccion = new TransactionTemplate(JFRPctx.getBean(PlatformTransactionManager.class));

            assertThatThrownBy(() -> LJOMtransaccion.executeWithoutResult(JFRPestado -> {
                GGFJservicio.anular(41L, "Cancelación provisional", Set.of(7L));
                assertThat(stock(AKACsql)).isEqualTo(7);
                assertThat(AKACsql.queryForObject("SELECT estado FROM orden_trabajo WHERE id = 41", String.class))
                        .isEqualTo("ANULADO");
                assertThat(AKACsql.queryForObject("SELECT devuelto_en_anulacion FROM detalle_orden WHERE id = 7", Integer.class))
                        .isEqualTo(1);
                throw new IllegalStateException("Fallo simulado después de escribir");
            })).isInstanceOf(IllegalStateException.class).hasMessageContaining("Fallo simulado");

            assertThat(stock(AKACsql)).isEqualTo(5);
            OrdenTrabajo AKACorden = GGFJservicio.buscar(41L);
            assertThat(AKACorden.getEstado()).isEqualTo(EstadoOrden.REPARACION);
            assertThat(AKACorden.getMotivoAnulacion()).isNull();
            assertThat(AKACorden.getDetalles()).noneMatch(DetalleOrden::isDevueltoEnAnulacion);
        }
    }

    @Test
    void migracionFallidaConservaElEsquemaAnteriorYElRespaldo() throws Exception {
        Path GGFJbase = crearBaseLegada();
        try (var LJOMconexion = DriverManager.getConnection(url(GGFJbase)); var JFRPsql = LJOMconexion.createStatement()) {
            JFRPsql.executeUpdate("UPDATE detalle_orden SET orden_id = 999 WHERE id = 7");
        }
        assertThatThrownBy(() -> arrancar(GGFJbase)).hasStackTraceContaining("FOREIGN KEY constraint failed");
        try (var AKACconexion = DriverManager.getConnection(url(GGFJbase)); var GGFJsql = AKACconexion.createStatement()) {
            try (var LJOMversion = GGFJsql.executeQuery("PRAGMA user_version")) {
                assertThat(LJOMversion.next()).isTrue();
                assertThat(LJOMversion.getInt(1)).isZero();
            }
            try (var JFRPfila = GGFJsql.executeQuery("SELECT orden_id FROM detalle_orden WHERE id = 7")) {
                assertThat(JFRPfila.next()).isTrue();
                assertThat(JFRPfila.getLong(1)).isEqualTo(999);
            }
            try (var AKACtabla = GGFJsql.executeQuery("SELECT name FROM sqlite_schema WHERE name = 'orden_trabajo_nueva'")) {
                assertThat(AKACtabla.next()).isFalse();
            }
        }
        try (var GGFJcopias = Files.list(carpeta.resolve("respaldos"))) {
            assertThat(GGFJcopias.count()).isEqualTo(1);
        }
    }

    @Test
    void migraDesdeVersionDosYRelacionaSoloCoincidenciasUnicas() throws Exception {
        Path LJOMbase = crearBaseLegada();
        try (var JFRPconexion = DriverManager.getConnection(url(LJOMbase)); var AKACsql = JFRPconexion.createStatement()) {
            ScriptUtils.executeSqlScript(JFRPconexion, new ClassPathResource("db/migration/V2__ciclo_y_numero_de_orden.sql"));
            AKACsql.execute("PRAGMA user_version = 2");
            AKACsql.execute("UPDATE orden_trabajo SET mecanico = ' OPERADOR ' WHERE id = 41");
            AKACsql.execute("""
                    INSERT INTO usuario (id, rol, usuario, nombre, clave_hash) VALUES
                    (2,'MECANICO','alex1','Alex','hash'), (3,'MECANICO','alex2','Alex','hash')
                    """);
            AKACsql.execute("""
                    INSERT INTO orden_trabajo (vehiculo_id, estado, problema, fecha_ingreso, mecanico)
                    VALUES (21,'LISTO','Revisión','2025-02-02','Mecánico externo'),
                           (21,'ENTREGADO','Diagnóstico','2025-03-03','Alex')
                    """);
        }
        try (var GGFJctx = arrancar(LJOMbase)) {
            var LJOMservicio = GGFJctx.getBean(OrdenService.class);
            assertThat(LJOMservicio.buscar(41L).getMecanicoUsuario().getUsuario()).isEqualTo("operador");
            assertThat(LJOMservicio.buscar(41L).getMecanico()).isEqualTo(" OPERADOR ");
            assertThat(LJOMservicio.buscar(42L).getMecanicoUsuario()).isNull();
            assertThat(LJOMservicio.buscar(42L).getMecanico()).isEqualTo("Mecánico externo");
            assertThat(LJOMservicio.buscar(43L).getMecanicoUsuario()).isNull();
            assertThat(LJOMservicio.buscar(43L).getMecanico()).isEqualTo("Alex");

            var JFRPfiltro = new OrdenService.Filtros("abc-123", 21L, EstadoOrden.REPARACION, 1L,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
            assertThat(LJOMservicio.buscar(JFRPfiltro, 0, 15).getContent()).extracting(OrdenTrabajo::getId)
                    .containsExactly(41L);
            var AKACfueraDeFecha = new OrdenService.Filtros("", 21L, null, 1L, LocalDate.of(2025, 2, 1), null);
            assertThat(LJOMservicio.buscar(AKACfueraDeFecha, 0, 15)).isEmpty();
            var GGFJhistorico = new OrdenService.Filtros("externo", 21L, null, null, null, null);
            assertThat(LJOMservicio.buscar(GGFJhistorico, 0, 15)).singleElement().extracting(OrdenTrabajo::getId).isEqualTo(42L);
            assertThat(LJOMservicio.resumenHistorial(21L).visitas()).isEqualTo(3);
            assertThat(LJOMservicio.resumenHistorial(21L).saldo()).isEqualByComparingTo("60");

            var LJOMnuevo = new OrdenTrabajo();
            LJOMnuevo.setVehiculo(LJOMservicio.buscar(41L).getVehiculo());
            LJOMnuevo.setProblema("Nueva asignación");
            LJOMnuevo.setMecanicoUsuario(GGFJctx.getBean(UsuarioRepository.class).findById(2L).orElseThrow());
            OrdenTrabajo JFRPguardada = LJOMservicio.guardar(LJOMnuevo);
            assertThat(LJOMservicio.buscar(JFRPguardada.getId()).getMecanicoUsuario().getId()).isEqualTo(2);
            GGFJctx.getBean(JdbcTemplate.class).update("DELETE FROM usuario WHERE id = 2");
            assertThat(LJOMservicio.buscar(JFRPguardada.getId()).getMecanicoUsuario()).isNull();
            assertThat(LJOMservicio.buscar(JFRPguardada.getId()).getMecanico()).isEqualTo("Alex");
        }
    }

    @Test
    void buscaCatalogosConFiltrosPaginacionYPlacasLegadas() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada())) {
            var GGFJsql = AKACctx.getBean(JdbcTemplate.class);
            GGFJsql.update("UPDATE cliente SET telefono = '987654321' WHERE id = 11");
            GGFJsql.update("UPDATE vehiculo SET placa = 'ABC-123' WHERE id = 21");
            GGFJsql.update("""
                    INSERT INTO repuesto (id, nombre, stock, stock_minimo, precio)
                    VALUES (32,'Filtro de aire',0,1,15), (33,'Aceite',0,2,25)
                    """);
            iniciarSesion(AKACctx, Rol.ADMIN);
            AKACctx.getBean(InventarioService.class).ajustar(33L, 10, "Inventario de prueba");
            var LJOMpagina = PageRequest.of(0, 1);
            var JFRPclientes = AKACctx.getBean(ClienteRepository.class);
            for (String AKACtexto : Set.of("Ana", "Pérez", "Pérez Ana", "12345678", "987654321")) {
                assertThat(JFRPclientes.buscar(AKACtexto, LJOMpagina)).extracting(Cliente::getId).containsExactly(11L);
            }
            var GGFJvehiculos = AKACctx.getBean(VehiculoService.class);
            for (String LJOMtexto : Set.of("aBc 123", "abc-123", "Toyota", "Yaris", "Ana Pérez")) {
                assertThat(GGFJvehiculos.buscar(LJOMtexto, 0, 15)).extracting(Vehiculo::getId).containsExactly(21L);
            }
            Cliente JFRPdueno = JFRPclientes.findById(11L).orElseThrow();
            Vehiculo AKACduplicado = Vehiculo.builder().placa(" abc 123 ").marca("Kia").modelo("Rio")
                    .anio(2021).kilometraje(10).cliente(JFRPdueno).build();
            assertThatThrownBy(() -> GGFJvehiculos.guardar(AKACduplicado)).isInstanceOf(NegocioException.class);
            Vehiculo GGFJnuevo = Vehiculo.builder().placa(" xyz - 987 ").marca("Kia").modelo("Rio")
                    .anio(2021).kilometraje(10).cliente(JFRPdueno).build();
            assertThat(GGFJvehiculos.guardar(GGFJnuevo).getPlaca()).isEqualTo("XYZ987");
            assertThat(GGFJvehiculos.buscar("abc123", 0, 15)).extracting(Vehiculo::getPlaca).containsExactly("ABC-123");

            var LJOMrepuestos = AKACctx.getBean(RepuestoService.class);
            assertThat(LJOMrepuestos.buscar("filtro", false, 0, 1).getTotalElements()).isEqualTo(2);
            assertThat(LJOMrepuestos.buscar("filtro", false, 1, 1)).extracting(Repuesto::getId).containsExactly(32L);
            assertThat(LJOMrepuestos.buscar("#31", false, 0, 15)).extracting(Repuesto::getId).containsExactly(31L);
            assertThat(LJOMrepuestos.buscar("filtro", true, 0, 15)).extracting(Repuesto::getId).containsExactly(32L);
            assertThat(AKACctx.getBean(UsuarioRepository.class).buscar("operador", LJOMpagina))
                    .extracting(Usuario::getId).containsExactly(1L);
        }
    }

    @Test
    void migraVersionTresConSaldoInicialYRespaldo() throws Exception {
        Path JFRPbase = crearBaseLegada();
        try (var AKACconexion = DriverManager.getConnection(url(JFRPbase)); var GGFJsql = AKACconexion.createStatement()) {
            ScriptUtils.executeSqlScript(AKACconexion, new ClassPathResource("db/migration/V2__ciclo_y_numero_de_orden.sql"));
            ScriptUtils.executeSqlScript(AKACconexion, new ClassPathResource("db/migration/V3__busquedas_y_mecanico.sql"));
            GGFJsql.execute("INSERT INTO repuesto (id,nombre,precio,stock,stock_minimo) VALUES (32,'Aceite',10,0,1)");
            GGFJsql.execute("PRAGMA user_version = 3");
        }
        try (var LJOMctx = arrancar(JFRPbase)) {
            var JFRPsql = LJOMctx.getBean(JdbcTemplate.class);
            verificarKardex(JFRPsql, 5);
            assertThat(JFRPsql.queryForList("""
                    SELECT cantidad FROM movimiento_inventario
                    WHERE origen_tipo = 'SALDO_INICIAL' ORDER BY repuesto_id
                    """, Integer.class)).containsExactly(5, 0);
            assertThat(JFRPsql.queryForObject("PRAGMA user_version", Integer.class)).isEqualTo(9);
        }
        try (var AKACcopias = Files.list(carpeta.resolve("respaldos"))) {
            var GGFJarchivos = AKACcopias.toList();
            assertThat(GGFJarchivos).hasSize(1);
            try (var LJOMconexion = DriverManager.getConnection(url(GGFJarchivos.get(0))); var JFRPsql = LJOMconexion.createStatement();
                 var AKACresultado = JFRPsql.executeQuery("PRAGMA user_version")) {
                assertThat(AKACresultado.next()).isTrue();
                assertThat(AKACresultado.getInt(1)).isEqualTo(3);
            }
        }
    }

    @Test
    void mantieneLaSumaDelKardexTrasComprasConsumoDevolucionAnulacionesYAjustes() throws Exception {
        try (var GGFJctx = arrancar(crearBaseLegada())) {
            var LJOMsql = GGFJctx.getBean(JdbcTemplate.class);
            var JFRPcompras = GGFJctx.getBean(CompraService.class);
            var AKACinventario = GGFJctx.getBean(InventarioService.class);
            var GGFJordenes = GGFJctx.getBean(OrdenService.class);
            var LJOMrepuestos = GGFJctx.getBean(RepuestoService.class);
            iniciarSesion(GGFJctx, Rol.ADMIN);
            Long JFRPproveedor = crearProveedor(GGFJctx);
            Repuesto AKACfiltro = LJOMrepuestos.buscar("#31", false, 0, 15).getContent().get(0);
            assertThat(AKACinventario.buscar(31L, TipoMovimiento.AJUSTE, null, null, 0, 15))
                    .singleElement().extracting(MovimientoInventario::getOrigenTipo).isEqualTo(OrigenMovimiento.SALDO_INICIAL);
            verificarKardex(LJOMsql, 5);

            var GGFJdatos = datosCompra(null, JFRPproveedor, "F001-1", new CompraService.Linea(
                    31L, 5, new BigDecimal("10"), new BigDecimal("17.50")));
            var LJOMborrador = JFRPcompras.guardarBorrador(GGFJdatos);
            verificarKardex(LJOMsql, 5);
            var JFRPprimera = JFRPcompras.confirmar(LJOMborrador.getId(), true);
            assertThat(JFRPprimera.getTotal()).isEqualByComparingTo("50.00");
            verificarKardex(LJOMsql, 10);
            assertThat(LJOMrepuestos.buscar("#31", false, 0, 15).getContent().get(0).getCostoPromedio())
                    .isEqualByComparingTo("5.00");
            assertThat(JFRPcompras.buscar("f001-1", EstadoCompra.CONFIRMADA, 0, 15)).hasSize(1);
            assertThatThrownBy(() -> JFRPcompras.confirmar(JFRPprimera.getId(), false)).isInstanceOf(NegocioException.class);
            assertThatThrownBy(() -> JFRPcompras.guardarBorrador(datosCompra(JFRPprimera.getId(), JFRPproveedor, "F001-1")))
                    .isInstanceOf(NegocioException.class);

            var AKACsegunda = JFRPcompras.guardarYConfirmar(datosCompra(null, JFRPproveedor, "F001-2",
                    new CompraService.Linea(31L, 6, new BigDecimal("15"), new BigDecimal("30"))), false);
            verificarKardex(LJOMsql, 16);
            Repuesto GGFJactualizado = LJOMrepuestos.buscar("#31", false, 0, 15).getContent().get(0);
            assertThat(GGFJactualizado.getCostoPromedio()).isEqualByComparingTo("8.75");
            assertThat(GGFJactualizado.getPrecio()).isEqualByComparingTo("17.50");

            DetalleOrden LJOMdetalle = new DetalleOrden();
            LJOMdetalle.setTipo(TipoDetalle.REPUESTO);
            LJOMdetalle.setRepuesto(AKACfiltro);
            LJOMdetalle.setCantidad(12);
            LJOMdetalle.setDescripcion("Consumo de prueba");
            LJOMdetalle.setPrecioUnitario(new BigDecimal("17.5"));
            Long JFRPlineaOrden = GGFJordenes.agregarDetalle(41L, LJOMdetalle).getDetalles().stream()
                    .filter(AKACd -> AKACd.getDescripcion().equals("Consumo de prueba")).findFirst().orElseThrow().getId();
            verificarKardex(LJOMsql, 4);
            var GGFJreferencias = AKACinventario.buscarKardex(31L, null, null, null, 0, 3);
            assertThat(GGFJreferencias).extracting(InventarioService.MovimientoKardex::referencia)
                    .containsExactly("OT-0041", "Factura F001-2", "Factura F001-1");
            assertThat(GGFJreferencias).extracting(LJOMfila -> LJOMfila.movimiento().getSaldoResultante())
                    .containsExactly(4, 16, 10);
            assertThat(GGFJreferencias.getTotalElements()).isEqualTo(4);
            assertThatThrownBy(() -> JFRPcompras.anular(AKACsegunda.getId(), "Comprobante incorrecto"))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("Stock insuficiente");
            assertThat(JFRPcompras.buscar(AKACsegunda.getId()).getEstado()).isEqualTo(EstadoCompra.CONFIRMADA);
            verificarKardex(LJOMsql, 4);

            GGFJordenes.actualizarDetalle(41L, JFRPlineaOrden, "Consumo de prueba", 17.5, 9);
            verificarKardex(LJOMsql, 7);
            JFRPcompras.anular(AKACsegunda.getId(), "Comprobante incorrecto");
            verificarKardex(LJOMsql, 1);
            assertThatThrownBy(() -> JFRPcompras.anular(AKACsegunda.getId(), "Reintento")).isInstanceOf(NegocioException.class);
            GGFJordenes.anular(41L, "Piezas no instaladas", Set.of(JFRPlineaOrden));
            verificarKardex(LJOMsql, 10);

            iniciarSesion(GGFJctx, Rol.MECANICO);
            assertThatThrownBy(() -> AKACinventario.ajustar(31L, -3, "Conteo"))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("administrador");
            iniciarSesion(GGFJctx, Rol.ADMIN);
            assertThatThrownBy(() -> AKACinventario.ajustar(31L, -3, " ")).isInstanceOf(NegocioException.class);
            AKACinventario.ajustar(31L, -3, "Conteo físico");
            verificarKardex(LJOMsql, 7);
            assertThatThrownBy(() -> LJOMsql.update("UPDATE repuesto SET stock = 99 WHERE id = 31"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> LJOMsql.update("DELETE FROM movimiento_inventario WHERE repuesto_id = 31"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> LJOMsql.update("""
                    INSERT OR REPLACE INTO repuesto (id,nombre,precio,stock,stock_minimo)
                    VALUES (31,'Filtro',20,0,3)
                    """)).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> LJOMsql.update("""
                    INSERT OR REPLACE INTO movimiento_inventario
                        (id,repuesto_id,tipo,cantidad,saldo_resultante,costo_unitario,origen_tipo,fecha,usuario,motivo)
                    SELECT id,repuesto_id,tipo,cantidad,7 + cantidad,costo_unitario,origen_tipo,fecha,usuario,motivo
                    FROM movimiento_inventario WHERE repuesto_id = 31 ORDER BY id DESC LIMIT 1
                    """)).isInstanceOf(DataAccessException.class);
            verificarKardex(LJOMsql, 7);
            assertThat(AKACinventario.buscar(31L, TipoMovimiento.ENTRADA_COMPRA, LocalDate.now(), LocalDate.now(), 0, 15))
                    .extracting(MovimientoInventario::getCantidad).containsExactly(6, 5);

            Repuesto JFRPficha = Repuesto.builder().id(31L).nombre("Filtro").sku(" fil-31 ")
                    .stock(100).stockMinimo(3).precio(new BigDecimal("20.0")).build();
            LJOMrepuestos.guardar(JFRPficha);
            verificarKardex(LJOMsql, 7);
            assertThat(LJOMrepuestos.buscar("fil-31", false, 0, 15)).extracting(Repuesto::getSku).containsExactly("FIL-31");
            assertThatThrownBy(() -> LJOMrepuestos.eliminar(31L)).isInstanceOf(NegocioException.class);
        }
    }

    @Test
    void revierteConfirmacionCompletaSiFallaUnaPartida() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada())) {
            iniciarSesion(AKACctx, Rol.ADMIN);
            var GGFJsql = AKACctx.getBean(JdbcTemplate.class);
            var LJOMcompras = AKACctx.getBean(CompraService.class);
            var JFRPrepuestos = AKACctx.getBean(RepuestoService.class);
            Long AKACproveedor = crearProveedor(AKACctx);
            Repuesto GGFJlleno = JFRPrepuestos.guardar(Repuesto.builder().nombre("Al límite").stock(30)
                    .stockMinimo(1).precio(new BigDecimal("1.0")).build());
            assertThat(GGFJlleno.getStock()).isZero();
            AKACctx.getBean(InventarioService.class).ajustar(GGFJlleno.getId(), Integer.MAX_VALUE, "Saldo de prueba");
            var LJOMborrador = LJOMcompras.guardarBorrador(datosCompra(null, AKACproveedor, "F001-3",
                    new CompraService.Linea(31L, 2, BigDecimal.ONE, new BigDecimal("25")),
                    new CompraService.Linea(GGFJlleno.getId(), 1, BigDecimal.ONE, null)));

            assertThatThrownBy(() -> LJOMcompras.confirmar(LJOMborrador.getId(), true))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("máximo permitido");
            assertThat(LJOMcompras.buscar(LJOMborrador.getId()).getEstado()).isEqualTo(EstadoCompra.BORRADOR);
            verificarKardex(GGFJsql, 5);
            assertThat(GGFJsql.queryForObject("SELECT count(*) FROM movimiento_inventario WHERE origen_tipo = 'COMPRA'",
                    Integer.class)).isZero();
            assertThat(JFRPrepuestos.buscar("#31", false, 0, 15).getContent().get(0).getPrecio()).isEqualByComparingTo("20.0");
        }
    }

    @Test
    void migraLaVersionCuatroSinAtribuirMovimientosNiCostosHistoricos() throws Exception {
        Path JFRPbase = crearBaseLegada();
        try (var AKACconexion = DriverManager.getConnection(url(JFRPbase)); var GGFJsql = AKACconexion.createStatement()) {
            ScriptUtils.executeSqlScript(AKACconexion, new ClassPathResource("db/migration/V2__ciclo_y_numero_de_orden.sql"));
            ScriptUtils.executeSqlScript(AKACconexion, new ClassPathResource("db/migration/V3__busquedas_y_mecanico.sql"));
            ScriptUtils.executeSqlScript(AKACconexion,
                    new EncodedResource(new ClassPathResource("db/migration/V4__compras_y_kardex.sql"), StandardCharsets.UTF_8),
                    false, false, "--", "@@", "/*", "*/");
            GGFJsql.execute("PRAGMA user_version = 4");
            GGFJsql.execute("""
                    INSERT INTO detalle_orden (id, orden_id, tipo, descripcion, cantidad, precio_unitario)
                    VALUES (99, 41, 'REPUESTO', 'Pieza histórica sin inventario', 1, 18.25)
                    """);
            GGFJsql.execute("""
                    INSERT INTO movimiento_inventario
                    (repuesto_id,tipo,cantidad,saldo_resultante,costo_unitario,origen_tipo,origen_id,fecha,usuario,motivo)
                    VALUES (31,'SALIDA_ORDEN',-1,4,'0','ORDEN',41,'2025-01-02T12:00:00.000Z','operador','Movimiento V4')
                    """);
        }
        try (var LJOMctx = arrancar(JFRPbase)) {
            var JFRPordenes = LJOMctx.getBean(OrdenService.class);
            var AKACsql = LJOMctx.getBean(JdbcTemplate.class);
            assertThat(JFRPordenes.buscar(41L).getDetalles()).filteredOn(GGFJd -> GGFJd.getId().equals(99L))
                    .singleElement().satisfies(LJOMd -> {
                        assertThat(LJOMd.getTipo()).isEqualTo(TipoDetalle.REPUESTO_EXTERNO);
                        assertThat(LJOMd.getPrecioUnitario()).isEqualByComparingTo("18.25");
                        assertThat(LJOMd.getCostoUnitario()).isNull();
                    });
            assertThat(JFRPordenes.trazabilidad(41L, null, 0, 15)).singleElement()
                    .satisfies(JFRPm -> assertThat(JFRPm.getDetalleOrden()).isNull());
            assertThatThrownBy(() -> AKACsql.update("UPDATE detalle_orden SET tipo = 'REPUESTO' WHERE id = 99"))
                    .isInstanceOf(DataAccessException.class);
            verificarKardex(AKACsql, 4);
        }
    }

    @Test
    void conservaPrecioCostoYDatosDeLaOrdenYRegistraCadaOperacionPorPartida() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada())) {
            iniciarSesion(AKACctx, Rol.ADMIN);
            var GGFJsql = AKACctx.getBean(JdbcTemplate.class);
            var LJOMordenes = AKACctx.getBean(OrdenService.class);
            var JFRPvehiculos = AKACctx.getBean(VehiculoRepository.class);
            var AKACcatalogo = AKACctx.getBean(RepuestoService.class);
            GGFJsql.update("UPDATE cliente SET telefono = '987654321' WHERE id = 11");
            GGFJsql.update("UPDATE repuesto SET costo_promedio = '6.125' WHERE id = 31");
            var GGFJrepuesto = AKACcatalogo.buscar("#31", false, 0, 15).getContent().get(0);
            var LJOMorden = nuevaOrden(LJOMordenes, JFRPvehiculos, 21L);
            long JFRPid = LJOMorden.getId();
            var AKACsolicitudSinCatalogo = detalle(TipoDetalle.REPUESTO, null, 1);
            assertThatThrownBy(() -> LJOMordenes.agregarDetalle(JFRPid, AKACsolicitudSinCatalogo))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("inventario");

            var GGFJexterno = detalle(TipoDetalle.REPUESTO_EXTERNO, null, 2);
            GGFJexterno.registrarPrecio(new BigDecimal("15.50"));
            assertThatThrownBy(() -> LJOMordenes.agregarDetalle(JFRPid, GGFJexterno))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("costo unitario");
            GGFJexterno.setCostoUnitario(new BigDecimal("7.25"));
            LJOMordenes.agregarDetalle(JFRPid, GGFJexterno);
            assertThat(LJOMordenes.trazabilidad(JFRPid, null, 0, 15)).isEmpty();
            verificarKardex(GGFJsql, 5);

            var LJOMconPieza = LJOMordenes.agregarDetalle(JFRPid, detalle(TipoDetalle.REPUESTO, GGFJrepuesto, 2));
            long JFRPpartida = LJOMconPieza.getDetalles().stream().filter(AKACd -> AKACd.getTipo() == TipoDetalle.REPUESTO)
                    .findFirst().orElseThrow().getId();
            verificarKardex(GGFJsql, 3);
            assertThat(LJOMordenes.ganancia(JFRPid).orElseThrow()).isEqualByComparingTo("44.25");
            var GGFJmovimientos = LJOMordenes.trazabilidad(JFRPid, JFRPpartida, 0, 15).getContent();
            assertThat(GGFJmovimientos).singleElement().satisfies(LJOMm -> {
                assertThat(LJOMm.getTipo()).isEqualTo(TipoMovimiento.SALIDA_ORDEN);
                assertThat(LJOMm.getDetalleOrden().getId()).isEqualTo(JFRPpartida);
                assertThat(LJOMm.getCostoUnitario()).isEqualByComparingTo("6.125");
            });

            GGFJsql.update("UPDATE repuesto SET precio_centimos = 3300, costo_promedio = '9' WHERE id = 31");
            GGFJsql.update("INSERT INTO cliente (id, nombre, apellido, dni, telefono) VALUES (12,'Luis','Díaz','87654321','912345678')");
            GGFJsql.update("UPDATE vehiculo SET cliente_id = 12, placa = 'XYZ987', marca = 'Marca actual' WHERE id = 21");
            GGFJsql.update("""
                    INSERT INTO vehiculo (id, cliente_id, placa, marca, modelo, anio, kilometraje)
                    VALUES (22,12,'ABC222','Kia','Rio',2020,10)
                    """);
            var JFRPguardada = LJOMordenes.buscar(JFRPid);
            assertThat(JFRPguardada.getDatosHistoricos().getClienteNombre()).isEqualTo("Ana Pérez");
            assertThat(JFRPguardada.getDatosHistoricos().getClienteDni()).isEqualTo("12345678");
            assertThat(JFRPguardada.getDatosHistoricos().getClienteTelefono()).isEqualTo("987654321");
            assertThat(JFRPguardada.getDatosHistoricos().getPlaca()).isEqualTo("ABC123");
            assertThat(JFRPguardada.getDatosHistoricos().getMarca()).isEqualTo("Toyota");
            JFRPguardada.setVehiculo(JFRPvehiculos.findById(22L).orElseThrow());
            assertThatThrownBy(() -> LJOMordenes.guardar(JFRPguardada)).hasMessageContaining("ya registró consumo");
            iniciarSesion(AKACctx, Rol.MECANICO);
            assertThatThrownBy(() -> LJOMordenes.ganancia(JFRPid)).hasMessageContaining("administrador");
            iniciarSesion(AKACctx, Rol.ADMIN);

            var AKACeditada = LJOMordenes.actualizarDetalleConPrecio(JFRPid, JFRPpartida, "Filtro original", new BigDecimal("50"), 3);
            verificarKardex(GGFJsql, 2);
            assertThat(AKACeditada.getDetalles()).filteredOn(GGFJd -> GGFJd.getId().equals(JFRPpartida)).singleElement()
                    .satisfies(LJOMd -> {
                        assertThat(LJOMd.getPrecioUnitario()).isEqualByComparingTo("20");
                        assertThat(LJOMd.getCostoUnitario()).isEqualByComparingTo("6.125");
                    });
            LJOMordenes.actualizarDetalleConPrecio(JFRPid, JFRPpartida, "Filtro original", null, 1);
            verificarKardex(GGFJsql, 4);
            LJOMordenes.quitarDetalle(JFRPid, JFRPpartida);
            verificarKardex(GGFJsql, 5);
            assertThat(LJOMordenes.buscar(JFRPid).getDetalles()).noneMatch(JFRPd -> JFRPd.getId().equals(JFRPpartida));
            assertThat(LJOMordenes.trazabilidad(JFRPid, JFRPpartida, 0, 15))
                    .extracting(MovimientoInventario::getCantidad).containsExactly(1, 2, -1, -2);
            assertThat(LJOMordenes.trazabilidad(JFRPid, JFRPpartida, 0, 15))
                    .allMatch(AKACm -> AKACm.getDetalleOrden().isRetirado());
            assertThatThrownBy(() -> LJOMordenes.guardar(JFRPguardada)).hasMessageContaining("ya registró consumo");

            var GGFJparaAnular = LJOMordenes.agregarDetalle(JFRPid, detalle(TipoDetalle.REPUESTO, GGFJrepuesto, 1));
            long LJOMpartidaAnulada = GGFJparaAnular.getDetalles().stream().filter(JFRPd -> JFRPd.getTipo() == TipoDetalle.REPUESTO)
                    .findFirst().orElseThrow().getId();
            LJOMordenes.anular(JFRPid, "No instalada", Set.of(LJOMpartidaAnulada));
            verificarKardex(GGFJsql, 5);
            assertThat(LJOMordenes.trazabilidad(JFRPid, LJOMpartidaAnulada, 0, 15))
                    .extracting(MovimientoInventario::getTipo)
                    .containsExactly(TipoMovimiento.DEVOLUCION_ORDEN, TipoMovimiento.SALIDA_ORDEN);
            assertThatThrownBy(() -> LJOMordenes.anular(JFRPid, "Otra vez", Set.of(LJOMpartidaAnulada)))
                    .isInstanceOf(NegocioException.class);
            verificarKardex(GGFJsql, 5);
        }
    }

    @Test
    void dosConexionesCompitenPorElMismoSaldoSinSobrevender() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada(), 2)) {
            iniciarSesion(AKACctx, Rol.ADMIN);
            var GGFJordenes = AKACctx.getBean(OrdenService.class);
            var LJOMvehiculos = AKACctx.getBean(VehiculoRepository.class);
            var JFRPsql = AKACctx.getBean(JdbcTemplate.class);
            Repuesto AKACrepuesto = AKACctx.getBean(RepuestoService.class).buscar("#31", false, 0, 15).getContent().get(0);
            long GGFJprimera = nuevaOrden(GGFJordenes, LJOMvehiculos, 21L).getId();
            long LJOMsegunda = nuevaOrden(GGFJordenes, LJOMvehiculos, 21L).getId();
            var JFRPpreparadas = new CountDownLatch(2);
            var AKACinicio = new CountDownLatch(1);
            var GGFJejecutor = Executors.newFixedThreadPool(2);
            try {
                var LJOMuno = GGFJejecutor.submit(consumir(GGFJordenes, GGFJprimera, AKACrepuesto, JFRPpreparadas, AKACinicio));
                var JFRPdos = GGFJejecutor.submit(consumir(GGFJordenes, LJOMsegunda, AKACrepuesto, JFRPpreparadas, AKACinicio));
                assertThat(JFRPpreparadas.await(5, TimeUnit.SECONDS)).isTrue();
                AKACinicio.countDown();
                assertThat(List.of(LJOMuno.get(12, TimeUnit.SECONDS), JFRPdos.get(12, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(true, false);
            } finally {
                AKACinicio.countDown();
                GGFJejecutor.shutdownNow();
            }
            verificarKardex(JFRPsql, 1);
            assertThat(JFRPsql.queryForObject("""
                    SELECT count(*) FROM movimiento_inventario
                    WHERE tipo = 'SALIDA_ORDEN' AND detalle_orden_id IS NOT NULL
                    """, Integer.class)).isEqualTo(1);
        }
    }

    private static Callable<Boolean> consumir(OrdenService AKACordenes, long GGFJorden, Repuesto LJOMrepuesto,
                                                CountDownLatch JFRPpreparadas, CountDownLatch AKACinicio) {
        return () -> {
            JFRPpreparadas.countDown();
            assertThat(AKACinicio.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                AKACordenes.agregarDetalle(GGFJorden, detalle(TipoDetalle.REPUESTO, LJOMrepuesto, 4));
                return true;
            } catch (StockInsuficienteException GGFJesperado) {
                return false;
            }
        };
    }

    private static OrdenTrabajo nuevaOrden(OrdenService LJOMordenes, VehiculoRepository JFRPvehiculos, long AKACvehiculo) {
        OrdenTrabajo GGFJorden = new OrdenTrabajo();
        GGFJorden.setVehiculo(JFRPvehiculos.findById(AKACvehiculo).orElseThrow());
        GGFJorden.setProblema("Orden de trazabilidad");
        return LJOMordenes.guardar(GGFJorden);
    }

    private static DetalleOrden detalle(TipoDetalle LJOMtipo, Repuesto JFRPrepuesto, int AKACcantidad) {
        DetalleOrden GGFJdetalle = new DetalleOrden();
        GGFJdetalle.setTipo(LJOMtipo);
        GGFJdetalle.setRepuesto(JFRPrepuesto);
        GGFJdetalle.setDescripcion("Partida de prueba");
        GGFJdetalle.setCantidad(AKACcantidad);
        return GGFJdetalle;
    }

    @Test
    void migraCentesimosExactosDesdeVersionCincoSinInventarCobros() throws Exception {
        Path LJOMbase = crearBaseLegada();
        try (var JFRPconexion = DriverManager.getConnection(url(LJOMbase)); var AKACsql = JFRPconexion.createStatement()) {
            for (String GGFJarchivo : List.of("V2__ciclo_y_numero_de_orden.sql", "V3__busquedas_y_mecanico.sql",
                    "V4__compras_y_kardex.sql", "V5__trazabilidad_de_ordenes.sql")) {
                String LJOMseparador = GGFJarchivo.startsWith("V4") || GGFJarchivo.startsWith("V5") ? "@@" : ";";
                ScriptUtils.executeSqlScript(JFRPconexion,
                        new EncodedResource(new ClassPathResource("db/migration/" + GGFJarchivo), StandardCharsets.UTF_8),
                        false, false, "--", LJOMseparador, "/*", "*/");
            }
            AKACsql.execute("PRAGMA user_version = 5");
            AKACsql.execute("UPDATE detalle_orden SET precio_unitario = 1.005 WHERE id = 7");
            AKACsql.execute("UPDATE detalle_orden SET precio_unitario = 7, precio_unitario_exacto = '2.675' WHERE id = 8");
            AKACsql.execute("UPDATE repuesto SET precio = 10.005 WHERE id = 31");
        }
        try (var JFRPctx = arrancar(LJOMbase)) {
            var AKACordenes = JFRPctx.getBean(OrdenService.class);
            var GGFJsql = JFRPctx.getBean(JdbcTemplate.class);
            assertThat(GGFJsql.queryForObject("PRAGMA user_version", Integer.class)).isEqualTo(9);
            assertThat(GGFJsql.queryForList("SELECT precio_unitario_centimos FROM detalle_orden ORDER BY id", Long.class))
                    .containsExactly(101L, 268L);
            assertThat(GGFJsql.queryForObject("SELECT precio_centimos FROM repuesto WHERE id = 31", Long.class)).isEqualTo(1001);
            var LJOMorden = AKACordenes.buscar(41L);
            assertThat(LJOMorden.getSubtotal()).isEqualByComparingTo("4.70");
            assertThat(LJOMorden.getTotalCobrado()).isZero();
            assertThat(LJOMorden.getSaldoPendiente()).isEqualByComparingTo("4.70");
        }
    }

    @Test
    void calculaDescuentoAdelantosPagosParcialesYAnulacionDeCaja() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACordenes = JFRPctx.getBean(OrdenService.class);
            var GGFJpagos = JFRPctx.getBean(PagoService.class);
            var LJOMsql = JFRPctx.getBean(JdbcTemplate.class);
            var JFRPorden = nuevaOrden(AKACordenes, JFRPctx.getBean(VehiculoRepository.class), 21L);
            Long AKACid = JFRPorden.getId();
            var GGFJanticipo = GGFJpagos.registrar(AKACid, new BigDecimal("0.17"), MetodoPago.YAPE, "Adelanto");
            assertThat(AKACordenes.buscar(AKACid).getSaldoAFavor()).isEqualByComparingTo("0.17");

            var LJOMpieza = detalle(TipoDetalle.REPUESTO_EXTERNO, null, 3);
            LJOMpieza.registrarPrecio(new BigDecimal("0.10"));
            LJOMpieza.setCostoUnitario(new BigDecimal("0.033333"));
            AKACordenes.agregarDetalle(AKACid, LJOMpieza);
            var JFRPtarea = detalle(TipoDetalle.SERVICIO, null, 1);
            JFRPtarea.registrarPrecio(new BigDecimal("0.20"));
            AKACordenes.agregarDetalle(AKACid, JFRPtarea);
            JFRPorden = AKACordenes.buscar(AKACid);
            JFRPorden.setDescuento(new BigDecimal("0.05"));
            AKACordenes.guardar(JFRPorden);
            JFRPorden = AKACordenes.buscar(AKACid);
            assertThat(JFRPorden.getSubtotal()).isEqualByComparingTo("0.50");
            assertThat(JFRPorden.getTotal()).isEqualByComparingTo("0.45");
            assertThat(JFRPorden.getTotalCobrado()).isEqualByComparingTo("0.17");
            assertThat(JFRPorden.getSaldoPendiente()).isEqualByComparingTo("0.28");
            assertThat(JFRPorden.getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);

            assertThatThrownBy(() -> GGFJpagos.registrar(AKACid, new BigDecimal("0.001"), MetodoPago.PLIN, ""))
                    .isInstanceOf(NegocioException.class);
            GGFJpagos.registrar(AKACid, new BigDecimal("0.28"), MetodoPago.EFECTIVO, null);
            assertThat(AKACordenes.buscar(AKACid).getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
            assertThat(AKACordenes.buscar(AKACid).getSaldoPendiente()).isZero();
            assertThatThrownBy(() -> GGFJpagos.anular(AKACid, GGFJanticipo.getId(), " ")).isInstanceOf(NegocioException.class);

            GGFJpagos.anular(AKACid, GGFJanticipo.getId(), "Pago duplicado");
            assertThat(AKACordenes.buscar(AKACid).getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);
            assertThat(AKACordenes.buscar(AKACid).getSaldoPendiente()).isEqualByComparingTo("0.17");
            assertThat(GGFJpagos.listar(AKACid)).hasSize(2).anyMatch(AKACp -> AKACp.isAnulado() && "Pago duplicado".equals(AKACp.getMotivoAnulacion()));
            assertThatThrownBy(() -> GGFJpagos.anular(AKACid, GGFJanticipo.getId(), "Reintento")).isInstanceOf(NegocioException.class);
            assertThatThrownBy(() -> LJOMsql.update("DELETE FROM pago WHERE id = ?", GGFJanticipo.getId()))
                    .isInstanceOf(DataAccessException.class);
            assertThat(GGFJpagos.caja(LocalDate.now(), LocalDate.now())).filteredOn(GGFJm -> GGFJm.metodo() == MetodoPago.YAPE)
                    .singleElement().satisfies(LJOMm -> {
                        assertThat(LJOMm.cobros()).isEqualByComparingTo("0.17");
                        assertThat(LJOMm.anulaciones()).isEqualByComparingTo("0.17");
                        assertThat(LJOMm.neto()).isZero();
                    });
            var JFRPresumen = AKACordenes.resumenHistorial(21L);
            assertThat(JFRPresumen.cobrado()).isEqualByComparingTo("0.28");
            assertThat(JFRPresumen.saldo()).isEqualByComparingTo("60.17");
        }
    }

    @Test
    void bloqueaEntregaConSaldoDesdeCabeceraYEstadoYAuditaLaExcepcionAdmin() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada())) {
            iniciarSesion(AKACctx, Rol.MECANICO);
            var GGFJordenes = AKACctx.getBean(OrdenService.class);
            var LJOMpagos = AKACctx.getBean(PagoService.class);
            var JFRPsql = AKACctx.getBean(JdbcTemplate.class);
            JFRPsql.update("UPDATE orden_trabajo SET mecanico_id = 1 WHERE id = 41");
            GGFJordenes.cambiarEstado(41L, EstadoOrden.LISTO);
            assertThatThrownBy(() -> GGFJordenes.cambiarEstado(41L, EstadoOrden.ENTREGADO))
                    .isExactlyInstanceOf(NegocioException.class).hasMessageContaining("administrador");
            var AKACcabecera = GGFJordenes.buscar(41L);
            AKACcabecera.setEstado(EstadoOrden.ENTREGADO);
            assertThatThrownBy(() -> GGFJordenes.guardar(AKACcabecera))
                    .isExactlyInstanceOf(NegocioException.class).hasMessageContaining("administrador");
            assertThatThrownBy(() -> GGFJordenes.entregarConSaldo(41L, "Excepción")).hasMessageContaining("administrador");
            iniciarSesion(AKACctx, Rol.ADMIN);
            assertThatThrownBy(() -> GGFJordenes.cambiarEstado(41L, EstadoOrden.ENTREGADO))
                    .isInstanceOf(SaldoPendienteException.class);
            assertThatThrownBy(() -> GGFJordenes.entregarConSaldo(41L, "")).hasMessageContaining("motivo");

            LJOMpagos.registrar(41L, new BigDecimal("20.50"), MetodoPago.TRANSFERENCIA, null);
            assertThat(GGFJordenes.guardarConEntregaAutorizada(AKACcabecera, "Crédito aprobado").getEstado())
                    .isEqualTo(EstadoOrden.ENTREGADO);
            assertThat(JFRPsql.queryForObject("SELECT saldo_centimos FROM entrega_con_saldo", Long.class)).isEqualTo(3950L);
            assertThat(JFRPsql.queryForObject("SELECT motivo FROM entrega_con_saldo", String.class)).isEqualTo("Crédito aprobado");
            assertThat(JFRPsql.queryForObject("SELECT usuario FROM entrega_con_saldo", String.class)).isEqualTo("operador");

            LJOMpagos.registrar(41L, new BigDecimal("39.50"), MetodoPago.TARJETA, "Liquidación");
            GGFJordenes.reabrir(41L, "Corrección de atención");
            iniciarSesion(AKACctx, Rol.MECANICO);
            assertThat(GGFJordenes.cambiarEstado(41L, EstadoOrden.ENTREGADO).getEstado()).isEqualTo(EstadoOrden.ENTREGADO);
            assertThat(GGFJordenes.buscar(41L).getTotalCobrado()).isZero();
            Path GGFJpdfMecanico = carpeta.resolve("orden-mecanico.pdf");
            assertThatThrownBy(() -> AKACctx.getBean(PdfService.class).generarOrden(41L, GGFJpdfMecanico))
                    .isInstanceOf(NegocioException.class).hasMessageContaining("administrador");
            assertThat(GGFJpdfMecanico).doesNotExist();
            assertThat(JFRPsql.queryForObject("SELECT count(*) FROM entrega_con_saldo", Integer.class)).isEqualTo(1);
            iniciarSesion(AKACctx, Rol.ADMIN);
            assertThat(GGFJordenes.buscar(41L).getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
            Path LJOMpdf = carpeta.resolve("orden-pagos.pdf");
            AKACctx.getBean(PdfService.class).generarOrden(41L, LJOMpdf);
            PdfReader JFRPlector = new PdfReader(LJOMpdf.toString());
            try {
                StringBuilder AKACtexto = new StringBuilder();
                PdfTextExtractor GGFJextractor = new PdfTextExtractor(JFRPlector);
                for (int LJOMpagina = 1; LJOMpagina <= JFRPlector.getNumberOfPages(); LJOMpagina++) {
                    AKACtexto.append(GGFJextractor.getTextFromPage(LJOMpagina));
                }
                assertThat(AKACtexto.toString()).contains("Abonos", "Transferencia", "Tarjeta", "Saldo pendiente");
            } finally {
                JFRPlector.close();
            }
            Path JFRPexcel = carpeta.resolve("caja.xlsx");
            AKACctx.getBean(ReporteService.class).exportarExcel(JFRPexcel);
            try (var AKACentrada = Files.newInputStream(JFRPexcel); var GGFJlibro = new XSSFWorkbook(AKACentrada)) {
                var LJOMcaja = GGFJlibro.getSheet("Caja del día");
                assertThat(LJOMcaja.getLastRowNum()).isEqualTo(6);
                assertThat(LJOMcaja.getRow(6).getCell(4).getNumericCellValue()).isEqualTo(60.0);
                assertThat(GGFJlibro.getSheet("Órdenes").getRow(0).getCell(11).getStringCellValue()).contains("Cobrado");
            }
        }
    }

    @Test
    void fotosConPermisosExportacionYRespaldoRestaurable() throws Exception {
        Path JFRPbase = crearBaseLegada();
        Path AKACorigen = carpeta.resolve("foto-original-del-cliente.png");
        ImageIO.write(new BufferedImage(420, 300, BufferedImage.TYPE_INT_RGB), "png", AKACorigen.toFile());
        Path GGFJrespaldo = carpeta.resolve("copia.zip");
        String LJOMruta;
        byte[] JFRPcopiaOriginal;
        byte[] AKACcopiaMiniatura;
        long GGFJid;
        try (var LJOMctx = arrancar(JFRPbase)) {
            var JFRPordenes = LJOMctx.getBean(OrdenService.class);
            var AKACfotos = LJOMctx.getBean(OrdenFotoService.class);
            var GGFJalmacen = LJOMctx.getBean(AlmacenFotos.class);
            LJOMctx.getBean(JdbcTemplate.class).update("""
                    INSERT INTO usuario (id, usuario, nombre, clave_hash, rol)
                    VALUES (11, 'fotografo', 'Fotógrafo', 'hash', 'MECANICO')
                    """);
            Usuario LJOMautor = new Usuario();
            LJOMautor.setId(11L);
            LJOMautor.setUsuario("fotografo");
            LJOMautor.setRol(Rol.MECANICO);
            LJOMctx.getBean(SesionActual.class).iniciar(LJOMautor);
            long JFRPorden = nuevaOrden(JFRPordenes, LJOMctx.getBean(VehiculoRepository.class), 21L).getId();
            GGFJid = JFRPorden;
            Path AKACfalsa = Files.writeString(carpeta.resolve("falsa.jpg"), "no es una imagen");
            assertThatThrownBy(() -> AKACfotos.agregar(JFRPorden, FaseFoto.INGRESO, List.of(AKACorigen, AKACfalsa)))
                    .isInstanceOf(NegocioException.class);
            assertThat(AKACfotos.listar(JFRPorden)).isEmpty();

            assertThat(AKACfotos.agregar(JFRPorden, FaseFoto.INGRESO, List.of(AKACorigen))).isEqualTo(1);
            var GGFJfoto = AKACfotos.listar(JFRPorden).get(0);
            LJOMruta = GGFJfoto.getRutaRelativa();
            assertThat(LJOMruta).matches("fotos/OT-[0-9]+/[0-9a-f]{32}[.]jpg");
            assertThat(GGFJalmacen.resolver(LJOMruta)).exists();
            assertThat(GGFJalmacen.resolver(AlmacenFotos.miniatura(LJOMruta))).exists();
            JFRPcopiaOriginal = GGFJalmacen.leer(LJOMruta);
            AKACcopiaMiniatura = GGFJalmacen.leer(AlmacenFotos.miniatura(LJOMruta));
            assertThat(AKACfotos.galeria(JFRPorden, FaseFoto.INGRESO, 0, 15)).singleElement()
                    .satisfies(LJOMt -> assertThat(LJOMt.editable()).isTrue());
            AKACfotos.guardarNota(JFRPorden, GGFJfoto.getId(), "Estado al ingresar");

            Usuario JFRPotro = new Usuario();
            JFRPotro.setId(12L);
            JFRPotro.setUsuario("otro");
            JFRPotro.setRol(Rol.MECANICO);
            LJOMctx.getBean(SesionActual.class).iniciar(JFRPotro);
            assertThatThrownBy(() -> AKACfotos.eliminar(JFRPorden, GGFJfoto.getId())).hasMessageContaining("órdenes asignadas");

            iniciarSesion(LJOMctx, Rol.ADMIN);
            Path AKACexportado = AKACfotos.exportarZip(JFRPorden, carpeta.resolve("galeria.zip"));
            try (ZipFile GGFJzip = new ZipFile(AKACexportado.toFile())) {
                assertThat(GGFJzip.size()).isEqualTo(1);
                assertThat(GGFJzip.entries().nextElement().getName()).startsWith("INGRESO/");
            }
            assertThat(AKACfotos.exportarCarpeta(JFRPorden, carpeta).resolve("INGRESO").resolve(Path.of(LJOMruta).getFileName())).exists();

            Path LJOMpdf = carpeta.resolve("orden-fotos.pdf");
            LJOMctx.getBean(PdfService.class).generarOrden(JFRPorden, LJOMpdf, true);
            PdfReader JFRPlector = new PdfReader(LJOMpdf.toString());
            try {
                assertThat(new PdfTextExtractor(JFRPlector).getTextFromPage(JFRPlector.getNumberOfPages()))
                        .contains("Fotos de la orden", "Estado al ingresar");
            } finally {
                JFRPlector.close();
            }

            LJOMctx.getBean(RespaldoService.class).crearCopia(GGFJrespaldo);
            try (ZipFile AKACzip = new ZipFile(GGFJrespaldo.toFile())) {
                assertThat(AKACzip.getEntry(LJOMruta)).isNotNull();
                assertThat(AKACzip.getEntry(AlmacenFotos.miniatura(LJOMruta))).isNotNull();
                assertThat(AKACzip.getEntry("tallerpro-v2.db")).isNotNull();
            }
            AKACfotos.eliminar(JFRPorden, GGFJfoto.getId());
            assertThat(GGFJalmacen.resolver(LJOMruta)).doesNotExist();
        }

        Path GGFJmalicioso = carpeta.resolve("ruta-invalida.zip");
        try (var LJOMzip = new ZipOutputStream(Files.newOutputStream(GGFJmalicioso))) {
            LJOMzip.putNextEntry(new ZipEntry("../fuera.jpg"));
            LJOMzip.write(1);
            LJOMzip.closeEntry();
        }
        assertThatThrownBy(() -> Restauracion.aplicar(GGFJmalicioso, carpeta)).isInstanceOf(IOException.class);
        assertThat(carpeta.resolve("fuera.jpg")).doesNotExist();
        Path JFRPincompleto = carpeta.resolve("sin-miniatura.zip");
        try (ZipFile AKACzip = new ZipFile(GGFJrespaldo.toFile());
             var GGFJsalida = new ZipOutputStream(Files.newOutputStream(JFRPincompleto))) {
            var LJOMentradas = AKACzip.entries();
            while (LJOMentradas.hasMoreElements()) {
                var JFRPentrada = LJOMentradas.nextElement();
                if (!JFRPentrada.getName().endsWith(".mini.jpg")) {
                    GGFJsalida.putNextEntry(new ZipEntry(JFRPentrada.getName()));
                    try (var AKACorigenZip = AKACzip.getInputStream(JFRPentrada)) {
                        AKACorigenZip.transferTo(GGFJsalida);
                    }
                    GGFJsalida.closeEntry();
                }
            }
        }
        assertThatThrownBy(() -> Restauracion.aplicar(JFRPincompleto, carpeta)).hasMessageContaining("faltan fotos");
        try (var GGFJconexion = DriverManager.getConnection(url(JFRPbase)); var LJOMsql = GGFJconexion.createStatement();
             var JFRPfilas = LJOMsql.executeQuery("SELECT count(*) FROM orden_foto")) {
            assertThat(JFRPfilas.next()).isTrue();
            assertThat(JFRPfilas.getInt(1)).isZero();
        }
        Restauracion.aplicar(GGFJrespaldo, carpeta);

        try (var AKACctx = arrancar(JFRPbase)) {
            var GGFJfotos = AKACctx.getBean(OrdenFotoService.class);
            var LJOMordenes = AKACctx.getBean(OrdenService.class);
            var JFRPfoto = GGFJfotos.listar(GGFJid).get(0);
            assertThat(JFRPfoto.getNota()).isEqualTo("Estado al ingresar");
            assertThat(AKACctx.getBean(AlmacenFotos.class).resolver(LJOMruta)).exists();
            assertThat(GGFJfotos.original(GGFJid, JFRPfoto.getId())).isEqualTo(JFRPcopiaOriginal);
            assertThat(AKACctx.getBean(AlmacenFotos.class).leer(AlmacenFotos.miniatura(LJOMruta))).isEqualTo(AKACcopiaMiniatura);
            iniciarSesion(AKACctx, Rol.ADMIN);
            LJOMordenes.cambiarEstado(GGFJid, EstadoOrden.REPARACION);
            LJOMordenes.cambiarEstado(GGFJid, EstadoOrden.LISTO);
            LJOMordenes.cambiarEstado(GGFJid, EstadoOrden.ENTREGADO);
            assertThatThrownBy(() -> GGFJfotos.eliminar(GGFJid, JFRPfoto.getId())).hasMessageContaining("solo lectura");
        }
        Path AKACminiatura = carpeta.resolve(AlmacenFotos.miniatura(LJOMruta));
        Files.delete(AKACminiatura);
        Restauracion.aplicar(GGFJrespaldo, carpeta);
        assertThat(Files.readAllBytes(AKACminiatura)).isEqualTo(AKACcopiaMiniatura);
        try (var GGFJcopias = Files.list(carpeta.resolve("respaldos"))) {
            Path LJOMrecuperacion = GGFJcopias.filter(Files::isDirectory)
                    .filter(JFRParchivo -> JFRParchivo.getFileName().toString().startsWith("antes-de-restaurar-"))
                    .findFirst().orElseThrow();
            assertThat(LJOMrecuperacion.resolve("tallerpro-v2.db")).exists();
            assertThat(LJOMrecuperacion.resolve(LJOMruta)).exists();
        }
    }

    @Test
    void conservaLaMarcaPendienteSiLaVersionNoEsCompatibleYLaRetiraAlRestaurar() throws Exception {
        Path AKACbase = crearBaseLegada();
        Path GGFJcopia = carpeta.resolve("respaldo-aceptado.zip");
        try (var LJOMctx = arrancar(AKACbase)) {
            var JFRPservicio = LJOMctx.getBean(RespaldoService.class);
            JFRPservicio.crearCopia(GGFJcopia);
            JFRPservicio.programarRestauracion(GGFJcopia);
        }
        Path AKACrespaldos = carpeta.resolve("respaldos");
        Path GGFJmarca = AKACrespaldos.resolve(Restauracion.MARCA_PENDIENTE);
        String LJOMnombre = Files.readString(GGFJmarca);
        Path JFRPprogramada = AKACrespaldos.resolve(LJOMnombre);
        Path AKACentradaFutura = carpeta.resolve("futura.db");
        try (var GGFJconexion = DriverManager.getConnection(url(AKACbase));
                var LJOMsentencia = GGFJconexion.prepareStatement("VACUUM INTO ?")) {
            LJOMsentencia.setString(1, AKACentradaFutura.toString());
            LJOMsentencia.execute();
        }
        try (var JFRPconexion = DriverManager.getConnection(url(AKACentradaFutura));
                var AKACsentencia = JFRPconexion.createStatement()) {
            AKACsentencia.execute("PRAGMA user_version = 999");
        }
        Files.copy(AKACentradaFutura, JFRPprogramada, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThatThrownBy(() -> Restauracion.aplicarPendiente(carpeta)).hasMessageContaining("versión incompatible");
        assertThat(Files.readString(GGFJmarca)).isEqualTo(LJOMnombre);
        try (var GGFJconexion = DriverManager.getConnection(url(AKACbase));
                var LJOMsentencia = GGFJconexion.createStatement();
                var JFRPversion = LJOMsentencia.executeQuery("PRAGMA user_version")) {
            assertThat(JFRPversion.next()).isTrue();
            assertThat(JFRPversion.getInt(1)).isEqualTo(9);
        }
        try (var AKACconexion = DriverManager.getConnection(url(AKACentradaFutura));
                var GGFJsentencia = AKACconexion.createStatement()) {
            GGFJsentencia.execute("PRAGMA user_version = 9");
            GGFJsentencia.execute("PRAGMA ignore_check_constraints = ON");
            GGFJsentencia.execute("UPDATE usuario SET intentos_fallidos = -1 WHERE id = 1");
        }
        Files.copy(AKACentradaFutura, JFRPprogramada, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThatThrownBy(() -> Restauracion.aplicarPendiente(carpeta)).hasMessageContaining("integridad");
        assertThat(Files.readString(GGFJmarca)).isEqualTo(LJOMnombre);
        Files.copy(GGFJcopia, JFRPprogramada, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Restauracion.aplicarPendiente(carpeta);
        assertThat(GGFJmarca).doesNotExist();
        try (var LJOMarchivos = Files.list(AKACrespaldos)) {
            assertThat(LJOMarchivos.map(JFRParchivo -> JFRParchivo.getFileName().toString()).toList())
                    .anyMatch(AKACarchivo -> AKACarchivo.matches("antes-de-restaurar-[0-9]{8}-[0-9]{6}-.*[.]zip"));
        }
    }

    @Test
    void autenticaConBloqueoPersistenteYConservaClavesAnteriores() throws Exception {
        Path GGFJbase = crearBaseLegada();
        try (var LJOMctx = arrancar(GGFJbase)) {
            var JFRPacceso = LJOMctx.getBean(UsuarioService.class);
            var AKACsql = LJOMctx.getBean(JdbcTemplate.class);
            var GGFJbcrypt = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            AKACsql.update("UPDATE usuario SET clave_hash = ? WHERE id = 1", GGFJbcrypt.encode("previa"));
            assertThat(JFRPacceso.autenticar("OPERADOR", "previa")).isPresent();
            assertThatThrownBy(() -> JFRPacceso.crear("Nueva cuenta", "cuenta", "corta", Rol.MECANICO))
                    .hasMessageContaining("al menos 10");
            var LJOMnueva = JFRPacceso.crear("Técnica", "mecanica", "clave-de-prueba", Rol.MECANICO);
            for (int JFRPintento = 0; JFRPintento < 4; JFRPintento++) {
                assertThat(JFRPacceso.autenticar(" mecanica ", "incorrecta")).isEmpty();
            }
            assertThatThrownBy(() -> JFRPacceso.autenticar("mecanica", "incorrecta")).hasMessageContaining("15 minutos");
            assertThat(AKACsql.queryForObject("SELECT intentos_fallidos FROM usuario WHERE id = ?", Integer.class,
                    LJOMnueva.getId())).isEqualTo(5);
        }
        try (var AKACctx = arrancar(GGFJbase)) {
            var GGFJacceso = AKACctx.getBean(UsuarioService.class);
            var LJOMsql = AKACctx.getBean(JdbcTemplate.class);
            assertThatThrownBy(() -> GGFJacceso.autenticar("mecanica", "clave-de-prueba"))
                    .hasMessageContaining("temporalmente bloqueada");
            LJOMsql.update("UPDATE usuario SET bloqueado_hasta = 1 WHERE usuario = 'mecanica'");
            assertThat(GGFJacceso.autenticar("mecanica", "clave-de-prueba")).isPresent();
            assertThat(LJOMsql.queryForObject("SELECT intentos_fallidos FROM usuario WHERE usuario = 'mecanica'",
                    Integer.class)).isZero();
            assertThat(LJOMsql.queryForObject("SELECT bloqueado_hasta FROM usuario WHERE usuario = 'mecanica'",
                    Long.class)).isZero();
        }
    }

    @Test
    void aplicaMatrizAOrdenesFotosCostosYCobrosDesdeLosServicios() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACsql = JFRPctx.getBean(JdbcTemplate.class);
            var GGFJordenes = JFRPctx.getBean(OrdenService.class);
            var LJOMusuarios = JFRPctx.getBean(UsuarioService.class);
            var JFRPsesion = JFRPctx.getBean(SesionActual.class);
            var AKACcompra = JFRPctx.getBean(CompraService.class);
            var GGFJpagos = JFRPctx.getBean(PagoService.class);
            var LJOMrespaldo = JFRPctx.getBean(RespaldoService.class);
            var JFRPrepuestos = JFRPctx.getBean(RepuestoService.class);
            var AKACfotos = JFRPctx.getBean(OrdenFotoService.class);
            var GGFJana = LJOMusuarios.crear("Ana Técnica", "ana.t", "clave-de-prueba", Rol.MECANICO);
            var LJOMotra = LJOMusuarios.crear("Otra Técnica", "otra.t", "clave-de-prueba", Rol.MECANICO);
            AKACsql.update("UPDATE orden_trabajo SET mecanico_id = ? WHERE id = 41", LJOMotra.getId());
            JFRPsesion.iniciar(GGFJana);
            assertThat(GGFJordenes.listar("", 0, 15)).isEmpty();
            assertThatThrownBy(() -> GGFJordenes.buscar(41L)).hasMessageContaining("órdenes asignadas");
            assertThatThrownBy(() -> GGFJordenes.cambiarEstado(41L, EstadoOrden.LISTO)).hasMessageContaining("órdenes asignadas");
            assertThatThrownBy(() -> AKACfotos.galeria(41L, FaseFoto.INGRESO, 0, 10)).hasMessageContaining("órdenes asignadas");
            assertThatThrownBy(() -> AKACfotos.listar(41L)).hasMessageContaining("órdenes asignadas");
            var JFRPorden = nuevaOrden(GGFJordenes, JFRPctx.getBean(VehiculoRepository.class), 21L);
            assertThat(JFRPorden.getMecanicoUsuario().getId()).isEqualTo(GGFJana.getId());
            assertThat(JFRPorden.getMecanicoUsuario().getClaveHash()).isNull();
            Long AKACid = JFRPorden.getId();
            var GGFJpartida = GGFJordenes.agregarDetalle(AKACid, detalle(TipoDetalle.REPUESTO,
                    JFRPrepuestos.buscar("Filtro", false, 0, 10).getContent().get(0), 1)).getDetalles().get(0);
            assertThat(GGFJpartida.getCostoUnitario()).isNull();
            assertThat(GGFJpartida.getRepuesto().getCostoPromedio()).isNull();
            assertThat(GGFJordenes.buscar(AKACid).getDetalles()).singleElement().extracting(DetalleOrden::getCostoUnitario).isNull();
            assertThat(GGFJordenes.trazabilidad(AKACid, null, 0, 15)).singleElement()
                    .satisfies(LJOMmovimiento -> assertThat(LJOMmovimiento.getCostoUnitario()).isNull());
            assertThat(JFRPrepuestos.buscar("Filtro", false, 0, 10)).singleElement()
                    .satisfies(JFRPrepuesto -> assertThat(JFRPrepuesto.getCostoPromedio()).isNull());
            assertThatThrownBy(() -> GGFJordenes.ganancia(AKACid)).hasMessageContaining("administrador");
            assertThatThrownBy(() -> GGFJpagos.registrar(AKACid, BigDecimal.ONE, MetodoPago.EFECTIVO, null))
                    .hasMessageContaining("administrador");
            assertThatThrownBy(() -> GGFJpagos.listar(AKACid)).hasMessageContaining("administrador");
            assertThatThrownBy(() -> GGFJpagos.caja(LocalDate.now(), LocalDate.now())).hasMessageContaining("administrador");
            assertThatThrownBy(() -> AKACcompra.buscar("", null, 0, 15)).hasMessageContaining("administrador");
            assertThatThrownBy(() -> LJOMrespaldo.crearCopia(carpeta.resolve("denegada.zip"))).hasMessageContaining("administrador");
            assertThatThrownBy(() -> LJOMrespaldo.programarRestauracion(carpeta.resolve("copia.zip"))).hasMessageContaining("administrador");
            var AKACia = new IaService(JFRPsesion);
            assertThatThrownBy(() -> AKACia.guardarCredencial("")).hasMessageContaining("administrador");
            assertThatThrownBy(AKACia::quitarCredencial).hasMessageContaining("administrador");
            assertThatThrownBy(() -> LJOMusuarios.eliminar(LJOMotra.getId(), GGFJana.getId())).hasMessageContaining("administrador");
            JFRPorden.setMecanicoUsuario(LJOMotra);
            assertThatThrownBy(() -> GGFJordenes.guardar(JFRPorden)).hasMessageContaining("solo puede asignarse");
            assertThat(GGFJordenes.listar("", 0, 15)).singleElement().extracting(OrdenTrabajo::getId).isEqualTo(AKACid);
            assertThat(GGFJordenes.ordenesTablero(LocalDate.now(), false, "", 0, 15)).singleElement()
                    .satisfies(GGFJfila -> assertThat(GGFJfila.getId()).isEqualTo(AKACid));
            var LJOMexterno = detalle(TipoDetalle.REPUESTO_EXTERNO, null, 1);
            LJOMexterno.registrarPrecio(new BigDecimal("10.00"));
            var JFRPpartidaExterna = GGFJordenes.agregarDetalle(AKACid, LJOMexterno).getDetalles().stream()
                    .filter(AKACd -> AKACd.getTipo() == TipoDetalle.REPUESTO_EXTERNO).findFirst().orElseThrow();
            assertThatThrownBy(() -> GGFJordenes.actualizarDetalleConPrecio(AKACid, JFRPpartidaExterna.getId(),
                    "Pieza externa", JFRPpartidaExterna.getPrecioUnitario(), 1, BigDecimal.ONE))
                    .hasMessageContaining("administrador");
            iniciarSesion(JFRPctx, Rol.ADMIN);
            assertThat(GGFJordenes.ganancia(AKACid)).isEmpty();
            var GGFJconCosto = GGFJordenes.actualizarDetalleConPrecio(AKACid, JFRPpartidaExterna.getId(),
                    "Pieza externa", JFRPpartidaExterna.getPrecioUnitario(), 1, BigDecimal.ONE);
            assertThat(GGFJconCosto.getDetalles()).allSatisfy(LJOMd -> assertThat(LJOMd.getCostoUnitario()).isNotNull());
            assertThat(GGFJordenes.ganancia(AKACid)).isPresent();
            GGFJpagos.registrar(AKACid, new BigDecimal("2.75"), MetodoPago.EFECTIVO, null);
            assertThat(GGFJordenes.buscar(AKACid).getTotalCobrado()).isEqualByComparingTo("2.75");
            assertThat(GGFJordenes.resumenHistorial(21L).cobrado()).isEqualByComparingTo("2.75");
            assertThat(GGFJordenes.ordenesTablero(LocalDate.now(), true, "", 0, 15)).anySatisfy(JFRPfila -> {
                assertThat(JFRPfila.getId()).isEqualTo(AKACid);
                assertThat(JFRPfila.getCobrado()).isEqualTo(275L);
            });
            JFRPsesion.iniciar(GGFJana);
            assertThat(GGFJordenes.buscar(AKACid).getTotalCobrado()).isZero();
            assertThat(GGFJordenes.listar("", 0, 15)).singleElement()
                    .satisfies(AKACitem -> assertThat(AKACitem.getTotalCobrado()).isZero());
            var GGFJhistorial = GGFJordenes.resumenHistorial(21L);
            assertThat(GGFJhistorial.visitas()).isEqualTo(1);
            assertThat(GGFJhistorial.cobrado()).isZero();
            assertThat(GGFJhistorial.saldo()).isZero();
            assertThat(GGFJordenes.ordenesTablero(LocalDate.now(), false, "", 0, 15)).singleElement()
                    .satisfies(LJOMfila -> {
                        assertThat(LJOMfila.getCobrado()).isZero();
                        assertThat(LJOMfila.getTotal()).isZero();
                    });
            GGFJordenes.cambiarEstado(AKACid, EstadoOrden.REPARACION);
            GGFJordenes.cambiarEstado(AKACid, EstadoOrden.LISTO);
            assertThatThrownBy(() -> GGFJordenes.cambiarEstado(AKACid, EstadoOrden.ENTREGADO))
                    .isExactlyInstanceOf(NegocioException.class)
                    .hasMessageContaining("Un administrador debe autorizar");
            assertThat(AKACsql.queryForObject("SELECT usuario FROM movimiento_inventario WHERE origen_id = ?",
                    String.class, AKACid)).isEqualTo("ana.t");
        }
    }

    @Test
    void paginaCuatroMilOrdenesYSirveTableroAcotadoSinCargarPartidas() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACsql = JFRPctx.getBean(JdbcTemplate.class);
            AKACsql.update("""
                    WITH RECURSIVE filas(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM filas WHERE i < 4000)
                    INSERT INTO vehiculo(placa, marca, modelo, anio, kilometraje, cliente_id)
                    SELECT printf('QA%05d', i), 'Marca', 'Modelo', 2020, 100, 11 FROM filas
                    """);
            LocalDate GGFJhoy = LocalDate.now();
            AKACsql.update("""
                    INSERT INTO orden_trabajo(vehiculo_id, vehiculo_placa, cliente_nombre, estado, problema, fecha_ingreso)
                    SELECT id, placa, 'Cliente de prueba', 'RECIBIDO', 'Revisión', ? FROM vehiculo WHERE placa LIKE 'QA%'
                    """, GGFJhoy.toString());
            AKACsql.update("""
                    INSERT INTO detalle_orden(orden_id, tipo, descripcion, cantidad, precio_unitario, precio_unitario_centimos)
                    SELECT id, 'SERVICIO', 'Diagnóstico de prueba', 1, 15, 1500 FROM orden_trabajo WHERE fecha_ingreso = ?
                    """, GGFJhoy.toString());
            var LJOMstats = JFRPctx.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class).getStatistics();
            LJOMstats.setStatisticsEnabled(true);
            LJOMstats.clear();
            var JFRPordenes = JFRPctx.getBean(OrdenService.class);
            var AKACpagina = JFRPordenes.buscar(new OrdenService.Filtros("QA", null, null, null, GGFJhoy, GGFJhoy), 1, 15);
            assertThat(AKACpagina.getTotalElements()).isEqualTo(4000);
            assertThat(AKACpagina.getContent()).hasSize(15).allSatisfy(GGFJorden -> {
                assertThat(GGFJorden.getTotal()).isEqualByComparingTo("15");
                assertThat(org.hibernate.Hibernate.isInitialized(GGFJorden.getDetalles())).isFalse();
            });
            assertThat(LJOMstats.getPrepareStatementCount()).isLessThanOrEqualTo(2);
            assertThat(LJOMstats.getCollectionLoadCount()).isZero();
            LJOMstats.clear();
            var LJOMpendientesTablero = JFRPordenes.ordenesTablero(GGFJhoy, true, "QA", 1, 15);
            assertThat(LJOMpendientesTablero.getTotalElements()).isEqualTo(4000);
            assertThat(LJOMpendientesTablero.getContent()).hasSize(15)
                    .allSatisfy(JFRPfila -> assertThat(JFRPfila.getDias()).isZero());
            assertThat(JFRPordenes.resumenTablero().servicios()).singleElement()
                    .satisfies(AKACfila -> assertThat(AKACfila.getCantidad()).isEqualTo(4000));
            assertThat(LJOMstats.getPrepareStatementCount()).isEqualTo(4);
            assertThat(LJOMstats.getEntityLoadCount()).isZero();

            var GGFJmantenimiento = JFRPctx.getBean(MantenimientoService.class);
            AKACsql.update("""
                    INSERT INTO programa_mantenimiento (vehiculo_id, servicio, cada_km, cada_meses, ultimo_km, ultima_fecha)
                    VALUES (21, 'Pendiente por km', 500, 0, 100, ?), (21, 'Fuera de plazo de aviso', 1000, 0, 100, ?)
                    """, GGFJhoy.toString(), GGFJhoy.toString());
            var LJOMpendientes = GGFJmantenimiento.buscar("", true, 0, 15);
            assertThat(LJOMpendientes.getTotalElements()).isEqualTo(GGFJmantenimiento.contarPendientes());
            assertThat(LJOMpendientes.getContent()).hasSize(2).allSatisfy(JFRPprograma ->
                    assertThat(JFRPprograma.calcularEstado(GGFJhoy).name()).isNotEqualTo("AL_DIA"));
        }
    }

    @Test
    void tableroPriorizaListasYConservaPendientesDeOtrosDias() throws Exception {
        try (var AKACctx = arrancar(crearBaseLegada())) {
            var GGFJsql = AKACctx.getBean(JdbcTemplate.class);
            var LJOMordenes = AKACctx.getBean(OrdenService.class);
            var JFRPhoy = LocalDate.of(2026, 11, 25);
            GGFJsql.update("UPDATE orden_trabajo SET estado = 'ANULADO', fecha_ingreso = ? WHERE id=41",
                    JFRPhoy.minusDays(5).toString());
            GGFJsql.update("""
                    INSERT INTO orden_trabajo(vehiculo_id, vehiculo_placa, cliente_nombre, estado, problema, fecha_ingreso)
                    VALUES (21, 'ZA200', 'Cliente de prueba', 'DIAGNOSTICO', 'Antigua', ?),
                           (21, 'QA-123', 'Cliente de prueba', 'LISTO', 'Entrega', ?),
                           (21, 'QA124', 'Cliente de prueba', 'REPARACION', 'Revisión', ?),
                           (21, 'QA125', 'Cliente de prueba', 'ENTREGADO', 'Terminada', ?),
                           (21, 'QA126', 'Cliente de prueba', 'ANULADO', 'Cancelada', ?)
                    """, JFRPhoy.minusDays(3).toString(), JFRPhoy.minusDays(1).toString(), JFRPhoy.toString(),
                    JFRPhoy.toString(), JFRPhoy.toString());

            var AKACpendientes = LJOMordenes.ordenesTablero(JFRPhoy, false, "", 0, 2);
            assertThat(AKACpendientes.getTotalElements()).isEqualTo(3);
            assertThat(AKACpendientes).extracting(GGFJfila -> GGFJfila.getEstado()).containsExactly("LISTO", "DIAGNOSTICO");
            assertThat(AKACpendientes).extracting(LJOMfila -> LJOMfila.getDias()).containsExactly(1, 3);
            assertThat(LJOMordenes.ordenesTablero(JFRPhoy, false, "", 1, 2)).singleElement()
                    .satisfies(JFRPfila -> assertThat(JFRPfila.getPlaca()).isEqualTo("QA124"));
            assertThat(LJOMordenes.ordenesTablero(JFRPhoy, false, "qa - 123", 0, 15)).singleElement()
                    .satisfies(AKACfila -> assertThat(AKACfila.getPlaca()).isEqualTo("QA-123"));
            assertThat(LJOMordenes.ordenesTablero(JFRPhoy, true, "", 0, 15)).extracting(GGFJfila -> GGFJfila.getEstado())
                    .containsExactly("REPARACION", "ENTREGADO", "ANULADO");
        }
    }

    @Test
    void migraVersionOchoAInspeccionYConservaClientesVehiculosYFotos() throws Exception {
        Path LJOMbase = crearBaseLegada();
        try (var JFRPctx = arrancar(LJOMbase)) {
            Path AKACorigen = carpeta.resolve("evidencia-version-ocho.png");
            ImageIO.write(new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB), "png", AKACorigen.toFile());
            JFRPctx.getBean(OrdenFotoService.class).agregar(41L, FaseFoto.DIAGNOSTICO, List.of(AKACorigen));
        }
        try (var GGFJconexion = DriverManager.getConnection(url(LJOMbase)); var LJOMsql = GGFJconexion.createStatement()) {
            LJOMsql.execute("DROP TABLE hallazgo_evento");
            LJOMsql.execute("DROP TABLE hallazgo_vehiculo");
            LJOMsql.execute("DROP INDEX ix_vehiculo_cliente_placa");
            LJOMsql.execute("ALTER TABLE vehiculo DROP COLUMN tipo_carroceria");
            LJOMsql.execute("ALTER TABLE orden_trabajo DROP COLUMN recepcion_cerrada");
            LJOMsql.execute("ALTER TABLE orden_trabajo DROP COLUMN cierre_inspeccion");
            LJOMsql.execute("ALTER TABLE orden_trabajo DROP COLUMN carroceria_cierre");
            LJOMsql.execute("PRAGMA user_version = 8");
        }
        String JFRPruta;
        try (var AKACctx = arrancar(LJOMbase)) {
            var GGFJsql = AKACctx.getBean(JdbcTemplate.class);
            assertThat(GGFJsql.queryForObject("PRAGMA user_version", Integer.class)).isEqualTo(9);
            assertThat(GGFJsql.queryForObject("SELECT count(*) FROM cliente WHERE id = 11", Integer.class)).isEqualTo(1);
            assertThat(AKACctx.getBean(HallazgoService.class).ficha(21L).vehiculo().getTipoCarroceria()).isEqualTo(TipoCarroceria.SEDAN);
            JFRPruta = AKACctx.getBean(OrdenFotoService.class).listar(41L).get(0).getRutaRelativa();
        }
        try (var LJOMarchivos = Files.list(carpeta.resolve("respaldos"))) {
            var JFRPzipPrevio = LJOMarchivos.filter(AKACa -> AKACa.getFileName().toString().endsWith(".zip")).findFirst().orElseThrow();
            try (var GGFJzip = new ZipFile(JFRPzipPrevio.toFile())) {
                assertThat(GGFJzip.getEntry("tallerpro-v2.db")).isNotNull();
                assertThat(GGFJzip.getEntry(JFRPruta)).isNotNull();
                assertThat(GGFJzip.getEntry(AlmacenFotos.miniatura(JFRPruta))).isNotNull();
            }
        }
    }

    @Test
    void consultaDniLocalConVehiculosDelTitularEnSqlite() throws Exception {
        try (var LJOMctx = arrancar(crearBaseLegada())) {
            var JFRPsql = LJOMctx.getBean(JdbcTemplate.class);
            JFRPsql.update("""
                    INSERT INTO cliente (id, dni, nombre, apellido, telefono, email)
                    VALUES (12,'01234567','Elena','Lopez','987654321','elena@example.test')
                    """);
            JFRPsql.update("""
                    INSERT INTO vehiculo (placa, marca, modelo, anio, kilometraje, cliente_id)
                    VALUES ('XYZ888','Kia','Rio',2022,20000,12),('ABC777','Mazda','3',2023,10000,12)
                    """);

            var AKACclientes = LJOMctx.getBean(ClienteService.class);
            assertThat(AKACclientes.buscarPorDni("01234567")).hasValueSatisfying(GGFJficha -> {
                assertThat(GGFJficha.cliente().getNombreCompleto()).isEqualTo("Elena Lopez");
                assertThat(GGFJficha.cliente().getTelefono()).isEqualTo("987654321");
                assertThat(GGFJficha.vehiculos()).extracting(Vehiculo::getPlaca).containsExactly("ABC777", "XYZ888");
                assertThat(GGFJficha.totalVehiculos()).isEqualTo(2);
            });
            assertThat(AKACclientes.buscarPorDni("01234568")).isEmpty();
        }
    }

    @Test
    void inspeccionBuscaClienteConUnoYVariosVehiculosYFiltraAsignacion() throws Exception {
        try (var LJOMctx = arrancar(crearBaseLegada())) {
            var JFRPsql = LJOMctx.getBean(JdbcTemplate.class);
            var AKACinspeccion = LJOMctx.getBean(HallazgoService.class);
            var GGFJclientes = AKACinspeccion.buscarClientes("12345678", 15);
            assertThat(GGFJclientes).singleElement().extracting(Cliente::getId).isEqualTo(11L);
            assertThat(AKACinspeccion.vehiculosDeCliente(11L)).singleElement().satisfies(LJOMauto ->
                    assertThat(LJOMauto.getTipoCarroceria()).isEqualTo(TipoCarroceria.SEDAN));
            JFRPsql.update("""
                    INSERT INTO vehiculo(id, placa, marca, modelo, anio, kilometraje, cliente_id, tipo_carroceria)
                    VALUES (22,'SUV220','Marca','Modelo',2023,0,11,'SUV')
                    """);
            assertThat(AKACinspeccion.vehiculosDeCliente(11L)).extracting(Vehiculo::getPlaca).contains("SUV220").hasSize(2);
            assertThat(AKACinspeccion.ficha(22L).vehiculo().getTipoCarroceria()).isEqualTo(TipoCarroceria.SUV);
            JFRPsql.update("UPDATE orden_trabajo SET mecanico_id = 1 WHERE id = 41");
            iniciarSesion(LJOMctx, Rol.MECANICO);
            assertThat(AKACinspeccion.vehiculosDeCliente(11L)).singleElement().extracting(Vehiculo::getId).isEqualTo(21L);
            assertThat(AKACinspeccion.buscarClientes("12345678", 15)).hasSize(1);
            assertThatThrownBy(() -> AKACinspeccion.ficha(22L)).hasMessageContaining("órdenes asignadas");
            assertThatThrownBy(() -> AKACinspeccion.registrar(21L, null, evidencia(Zona.CAPO, Vista.SUPERIOR, Origen.DETECTADO, null)))
                    .hasMessageContaining("administrador");
        }
    }

    @Test
    void protegeLaRecepcionConvierteConStockResuelveAlEntregarYConservaElPdf() throws Exception {
        try (var JFRPctx = arrancar(crearBaseLegada())) {
            var AKACinspeccion = JFRPctx.getBean(HallazgoService.class);
            var GGFJordenes = JFRPctx.getBean(OrdenService.class);
            var LJOMvehiculos = JFRPctx.getBean(VehiculoRepository.class);
            var JFRPsql = JFRPctx.getBean(JdbcTemplate.class);
            var AKACorden = nuevaOrden(GGFJordenes, LJOMvehiculos, 21L);
            Long GGFJvisita = AKACorden.getId();
            JFRPsql.update("UPDATE orden_trabajo SET mecanico_id = 1 WHERE id = ?", GGFJvisita);
            iniciarSesion(JFRPctx, Rol.MECANICO);
            Path LJOMpng = carpeta.resolve("inspeccion.png");
            ImageIO.write(new BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB), "png", LJOMpng.toFile());
            var JFRPfotos = JFRPctx.getBean(OrdenFotoService.class);
            JFRPfotos.agregar(GGFJvisita, FaseFoto.INGRESO, List.of(LJOMpng));
            var AKACfoto = JFRPfotos.listar(GGFJvisita).get(0);
            var GGFJinicial = evidencia(Zona.MALETERA, Vista.TRASERA, Origen.PREEXISTENTE, AKACfoto.getId());
            var LJOMreparado = AKACinspeccion.registrar(21L, GGFJvisita, GGFJinicial);
            var JFRPpendiente = AKACinspeccion.registrar(21L, GGFJvisita,
                    evidencia(Zona.CAPO, Vista.SUPERIOR, Origen.PREEXISTENTE, null));
            assertThat(AKACinspeccion.fotoDeHallazgo(LJOMreparado.getId()).foto()).isEqualTo(AKACfoto.getId());
            assertThatThrownBy(() -> JFRPfotos.eliminar(GGFJvisita, AKACfoto.getId())).hasMessageContaining("vinculada");
            assertThat(AKACinspeccion.consultar(21L, new Filtros(Set.of(Estado.ABIERTO), Capa.CARROCERIA,
                    Gravedad.MEDIA, GGFJvisita, LocalDate.now(), LocalDate.now()))).hasSize(2);
            assertThatThrownBy(() -> AKACinspeccion.registrar(21L, GGFJvisita,
                    evidencia(Zona.PORTON, Vista.TRASERA, Origen.DETECTADO, null))).hasMessageContaining("carrocería");
            GGFJordenes.cambiarEstado(GGFJvisita, EstadoOrden.REPARACION);
            assertThatThrownBy(() -> AKACinspeccion.actualizar(LJOMreparado.getId(), GGFJvisita, GGFJinicial)).hasMessageContaining("recepción");
            assertThatThrownBy(() -> AKACinspeccion.registrar(21L, GGFJvisita, GGFJinicial)).hasMessageContaining("recepción");
            var AKACrepuesto = Repuesto.builder().id(31L).nombre("Filtro").build();
            int GGFJantes = stock(JFRPsql);
            var LJOMsinStock = detalle(TipoDetalle.REPUESTO, AKACrepuesto, GGFJantes + 1);
            assertThatThrownBy(() -> GGFJordenes.convertirHallazgo(GGFJvisita, JFRPpendiente.getId(), LJOMsinStock))
                    .isInstanceOf(StockInsuficienteException.class);
            assertThat(stock(JFRPsql)).isEqualTo(GGFJantes);
            assertThat(AKACinspeccion.historial(JFRPpendiente.getId())).hasSize(1);
            var JFRPservicio = detalle(TipoDetalle.SERVICIO, null, 1);
            JFRPservicio.setDescripcion("Reparación de maletera");
            JFRPservicio.registrarPrecio(BigDecimal.ZERO);
            GGFJordenes.convertirHallazgo(GGFJvisita, LJOMreparado.getId(), JFRPservicio);
            assertThat(AKACinspeccion.consultar(21L, new Filtros(Set.of(Estado.EN_REPARACION), null, null, null, null, null)))
                    .singleElement().satisfies(AKACh -> assertThat(AKACh.getDetalleOrdenId()).isNotNull());
            assertThatThrownBy(() -> GGFJordenes.convertirHallazgo(GGFJvisita, LJOMreparado.getId(), JFRPservicio)).hasMessageContaining("vinculado");
            GGFJordenes.cambiarEstado(GGFJvisita, EstadoOrden.LISTO);
            GGFJordenes.cambiarEstado(GGFJvisita, EstadoOrden.ENTREGADO);
            assertThat(AKACinspeccion.historial(LJOMreparado.getId()).get(0).getEstado()).isEqualTo(Estado.REPARADO);
            assertThatThrownBy(() -> AKACinspeccion.descartar(JFRPpendiente.getId(), GGFJvisita, "No procede"))
                    .hasMessageContaining("solo lectura");
            assertThat(AKACinspeccion.consultar(21L, Filtros.pendientes())).singleElement()
                    .extracting(HallazgoVehiculo::getId).isEqualTo(JFRPpendiente.getId());
            iniciarSesion(JFRPctx, Rol.ADMIN);
            AKACinspeccion.registrar(21L, null, new Datos(Zona.MOTOR, Vista.FRONTAL, null, null,
                    Tipo.RUIDO, Gravedad.LEVE, Origen.DETECTADO, "Daño de una visita posterior", null));
            Path GGFJarchivo = carpeta.resolve("inspeccion.pdf");
            JFRPctx.getBean(PdfService.class).generarOrden(GGFJvisita, GGFJarchivo, true);
            PdfReader LJOMlector = new PdfReader(GGFJarchivo.toString());
            try {
                assertThat(LJOMlector.getNumberOfPages()).isGreaterThanOrEqualTo(3);
                StringBuilder JFRPcontenido = new StringBuilder();
                PdfTextExtractor AKACextractor = new PdfTextExtractor(LJOMlector);
                for (int GGFJi = 1; GGFJi <= LJOMlector.getNumberOfPages(); GGFJi++) {
                    JFRPcontenido.append(AKACextractor.getTextFromPage(GGFJi)).append(' ');
                }
                assertThat(JFRPcontenido.toString()).contains("Inspección del vehículo", "Maletera", "Capó", "Reparado", "Abierto")
                        .doesNotContain("Daño de una visita posterior");
            } finally {
                LJOMlector.close();
            }
            Path LJOMcopia = carpeta.resolve("copia-inspeccion.zip");
            JFRPctx.getBean(RespaldoService.class).crearCopia(LJOMcopia);
            try (ZipFile JFRPzip = new ZipFile(LJOMcopia.toFile())) {
                assertThat(JFRPzip.getEntry(AKACfoto.getRutaRelativa())).isNotNull();
                Path AKACbaseCopia = carpeta.resolve("sqlite-inspeccion.db");
                try (var GGFJentrada = JFRPzip.getInputStream(JFRPzip.getEntry("tallerpro-v2.db"))) {
                    Files.copy(GGFJentrada, AKACbaseCopia);
                }
                try (var LJOMconexion = DriverManager.getConnection(url(AKACbaseCopia));
                     var JFRPsentencia = LJOMconexion.createStatement();
                     var AKACfilas = JFRPsentencia.executeQuery("SELECT estado, foto_id FROM hallazgo_vehiculo WHERE id = " + LJOMreparado.getId())) {
                    assertThat(AKACfilas.next()).isTrue();
                    assertThat(AKACfilas.getString(1)).isEqualTo("REPARADO");
                    assertThat(AKACfilas.getLong(2)).isEqualTo(AKACfoto.getId());
                }
            }
        }
    }

    @Test
    void reutilizaHallazgosEntreVisitasYDevuelveAlAnularSinBorrarHistorial() throws Exception {
        try (var GGFJctx = arrancar(crearBaseLegada())) {
            var LJOMinspeccion = GGFJctx.getBean(HallazgoService.class);
            var JFRPordenes = GGFJctx.getBean(OrdenService.class);
            var AKACvehiculos = GGFJctx.getBean(VehiculoRepository.class);
            var GGFJsql = GGFJctx.getBean(JdbcTemplate.class);
            var LJOMh = LJOMinspeccion.registrar(21L, null, evidencia(Zona.CAPO, Vista.SUPERIOR, Origen.PREEXISTENTE, null));
            var JFRPorden = nuevaOrden(JFRPordenes, AKACvehiculos, 21L);
            Long AKACvisita = JFRPorden.getId();
            assertThat(LJOMinspeccion.abiertosParaIncluir(AKACvisita)).singleElement()
                    .extracting(HallazgoVehiculo::getId).isEqualTo(LJOMh.getId());
            LJOMinspeccion.incluir(AKACvisita, List.of(LJOMh.getId()));
            assertThat(LJOMinspeccion.abiertosParaIncluir(AKACvisita)).isEmpty();
            int GGFJantes = stock(GGFJsql);
            var LJOMrepuesto = Repuesto.builder().id(31L).nombre("Filtro").build();
            var JFRPmodificada = JFRPordenes.convertirHallazgo(AKACvisita, LJOMh.getId(), detalle(TipoDetalle.REPUESTO, LJOMrepuesto, 1));
            Long AKACpartida = JFRPmodificada.getDetalles().get(0).getId();
            assertThat(stock(GGFJsql)).isEqualTo(GGFJantes - 1);
            JFRPordenes.quitarDetalle(AKACvisita, AKACpartida);
            assertThat(stock(GGFJsql)).isEqualTo(GGFJantes);
            var GGFJsegunda = JFRPordenes.convertirHallazgo(AKACvisita, LJOMh.getId(), detalle(TipoDetalle.REPUESTO, LJOMrepuesto, 1));
            JFRPordenes.anular(AKACvisita, "Visita cancelada", Set.of(GGFJsegunda.getDetalles().get(0).getId()));
            assertThat(stock(GGFJsql)).isEqualTo(GGFJantes);
            assertThat(LJOMinspeccion.consultar(21L, Filtros.pendientes())).singleElement().satisfies(LJOMactual -> {
                assertThat(LJOMactual.getEstado()).isEqualTo(Estado.ABIERTO);
                assertThat(LJOMactual.getOrdenSeguimientoId()).isNull();
                assertThat(LJOMactual.getDetalleOrdenId()).isNull();
            });
            assertThat(LJOMinspeccion.historial(LJOMh.getId())).extracting(JFRPe -> JFRPe.getAccion())
                    .contains("Orden anulada", "Partida vinculada", "Partida retirada");
            assertThatThrownBy(() -> LJOMinspeccion.descartar(LJOMh.getId(), null, "")).hasMessageContaining("motivo");
            LJOMinspeccion.descartar(LJOMh.getId(), null, "No correspondía al vehículo");
            assertThat(LJOMinspeccion.consultar(21L, Filtros.pendientes())).isEmpty();
            assertThat(LJOMinspeccion.consultar(21L, new Filtros(Set.of(Estado.DESCARTADO), null, null, AKACvisita, null, null)))
                    .singleElement().satisfies(AKACactual -> {
                        assertThat(AKACactual.getMotivoDescarte()).contains("No correspondía");
                        assertThat(AKACactual.getUsuarioActualizacion()).isEqualTo("operador");
                    });
        }
    }

    private static Datos evidencia(Zona GGFJzona, Vista LJOMvista, Origen JFRPorigen, Long AKACfoto) {
        return new Datos(GGFJzona, LJOMvista, null, null, Tipo.RAYON, Gravedad.MEDIA, JFRPorigen, "Daño documentado", AKACfoto);
    }

    private static CompraService.Datos datosCompra(Long GGFJid, Long LJOMproveedor, String JFRPnumero, CompraService.Linea... AKAClineas) {
        return new CompraService.Datos(GGFJid, LJOMproveedor, LocalDate.now(), TipoComprobante.FACTURA, JFRPnumero, List.of(AKAClineas));
    }

    private static Long crearProveedor(ConfigurableApplicationContext GGFJctx) {
        Proveedor LJOMproveedor = new Proveedor();
        LJOMproveedor.setNombre("Distribuidora de prueba");
        LJOMproveedor.setRuc("20123456789");
        return GGFJctx.getBean(ProveedorService.class).crear(LJOMproveedor).getId();
    }

    private static void verificarKardex(JdbcTemplate JFRPsql, int AKACsaldoEsperado) {
        assertThat(stock(JFRPsql)).isEqualTo(AKACsaldoEsperado);
        assertThat(JFRPsql.queryForList("""
                SELECT r.id FROM repuesto r LEFT JOIN movimiento_inventario m ON m.repuesto_id = r.id
                GROUP BY r.id, r.stock HAVING r.stock <> coalesce(sum(m.cantidad), 0)
                """)).isEmpty();
    }

    private Path crearBaseLegada() throws Exception {
        Path GGFJbase = carpeta.resolve("tallerpro-v2.db");
        try (var LJOMconexion = DriverManager.getConnection(url(GGFJbase))) {
            ScriptUtils.executeSqlScript(LJOMconexion, new EncodedResource(
                    new ClassPathResource("sqlite/tallerpro-v2-legado.sql"), StandardCharsets.UTF_8));
        }
        return GGFJbase;
    }

    private static ConfigurableApplicationContext arrancar(Path JFRPbase) {
        return arrancar(JFRPbase, 1);
    }

    private static ConfigurableApplicationContext arrancar(Path AKACbase, int GGFJconexiones) {
        var LJOMcontexto = new SpringApplicationBuilder(ConfigSqlite.class).web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + url(AKACbase), "--taller.datos.directorio=" + AKACbase.getParent(),
                        "--spring.datasource.hikari.maximum-pool-size=" + GGFJconexiones, "--spring.profiles.active=pruebas");
        iniciarSesion(LJOMcontexto, Rol.ADMIN);
        return LJOMcontexto;
    }

    private static String url(Path JFRPbase) {
        return "jdbc:sqlite:" + JFRPbase.toAbsolutePath();
    }

    private static int stock(JdbcTemplate AKACsql) {
        return AKACsql.queryForObject("SELECT stock FROM repuesto WHERE id = 31", Integer.class);
    }

    private static void iniciarSesion(ConfigurableApplicationContext GGFJctx, Rol LJOMrol) {
        Usuario JFRPusuario = new Usuario();
        JFRPusuario.setId(1L);
        JFRPusuario.setUsuario("operador");
        JFRPusuario.setRol(LJOMrol);
        GGFJctx.getBean(SesionActual.class).iniciar(JFRPusuario);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Cliente.class)
    @EnableJpaRepositories(basePackageClasses = OrdenRepository.class)
    @Import({MigracionesSqlite.class, OrdenService.class, SesionActual.class,
            ClienteService.class, VehiculoService.class, RepuestoService.class, CompraService.class,
            InventarioService.class, ProveedorService.class, PagoService.class, PdfService.class, ReporteService.class, OrdenFotoService.class,
            RespaldoService.class, HallazgoService.class, UsuarioService.class, MantenimientoService.class, Rutas.Configuracion.class})
    static class ConfigSqlite {
    }
}
