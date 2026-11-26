package com.ejemplo.tallerpro.util;

import lombok.extern.slf4j.Slf4j;
import com.ejemplo.tallerpro.model.CentimosConverter;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class MigracionesSqlite {

    private record Migracion(String archivo, String separador) {
        private Migracion(String GGFJarchivo) {
            this(GGFJarchivo, ";");
        }
    }

    private static final List<Migracion> MIGRACIONES = List.of(
            new Migracion("V1__esquema_inicial.sql"),
            new Migracion("V2__ciclo_y_numero_de_orden.sql"),
            new Migracion("V3__busquedas_y_mecanico.sql"),
            new Migracion("V4__compras_y_kardex.sql", "@@"),
            new Migracion("V5__trazabilidad_de_ordenes.sql", "@@"),
            new Migracion("V6__pagos_y_centimos.sql", "@@"),
            new Migracion("V7__fotos_de_orden.sql"),
            new Migracion("V8__acceso_y_tablero.sql"),
            new Migracion("V9__inspeccion_vehicular.sql"));
    private static final Set<String> TABLAS_LEGADAS = Set.of(
            "cliente", "vehiculo", "repuesto", "usuario", "programa_mantenimiento", "orden_trabajo", "detalle_orden");

    public static int versionSoportada() {
        return MIGRACIONES.size();
    }

    @Bean
    static EntityManagerFactoryDependsOnPostProcessor jpaDespuesDeMigrar() {
        return new EntityManagerFactoryDependsOnPostProcessor("migrarBaseSqlite");
    }

    @Bean
    InitializingBean migrarBaseSqlite(DataSource dataSource) {
        return () -> migrar(dataSource);
    }

    private void migrar(DataSource LJOMdataSource) throws Exception {
        try (Connection JFRPconexion = LJOMdataSource.getConnection(); Statement AKACsql = JFRPconexion.createStatement()) {
            int GGFJinstalada = version(AKACsql);
            if (GGFJinstalada == MIGRACIONES.size()) {
                return;
            }
            Path LJOMarchivo = archivoBase(AKACsql);
            if (Files.size(LJOMarchivo) > 0) {
                respaldar(JFRPconexion, LJOMarchivo, GGFJinstalada);
            }

            AKACsql.execute("BEGIN IMMEDIATE");
            try {
                int JFRPversion = version(AKACsql);
                if (JFRPversion == 0 && esBaseLegada(AKACsql)) {
                    JFRPversion = 1;
                }
                for (int AKACi = JFRPversion; AKACi < MIGRACIONES.size(); AKACi++) {
                    Migracion GGFJmigracion = MIGRACIONES.get(AKACi);
                    ClassPathResource LJOMrecurso = new ClassPathResource("db/migration/" + GGFJmigracion.archivo());
                    if (AKACi == 5) {
                        aplicarMigracionMonetaria(JFRPconexion, LJOMrecurso, GGFJmigracion.separador());
                    } else {
                        ejecutarScript(JFRPconexion, LJOMrecurso, GGFJmigracion.separador());
                    }
                    AKACsql.execute("PRAGMA user_version = " + (AKACi + 1));
                }
                try (var JFRPinconsistencias = AKACsql.executeQuery("PRAGMA foreign_key_check")) {
                    if (JFRPinconsistencias.next()) {
                        throw new SQLException("La base tiene referencias inválidas en " + JFRPinconsistencias.getString(1) + ".");
                    }
                }
                AKACsql.execute("COMMIT");
            } catch (Exception AKACerror) {
                try {
                    AKACsql.execute("ROLLBACK");
                } catch (SQLException GGFJrollback) {
                    AKACerror.addSuppressed(GGFJrollback);
                }
                throw AKACerror;
            }
        }
    }

    private void ejecutarScript(Connection LJOMconexion, ClassPathResource JFRPrecurso, String AKACseparador) {
        ScriptUtils.executeSqlScript(LJOMconexion, new EncodedResource(JFRPrecurso, StandardCharsets.UTF_8),
                false, false, "--", AKACseparador, "/*", "*/");
    }

    private void aplicarMigracionMonetaria(Connection GGFJconexion, ClassPathResource LJOMrecurso, String JFRPseparador)
            throws SQLException {
        SQLiteConnection AKACsqlite = GGFJconexion.unwrap(SQLiteConnection.class);
        Function.create(AKACsqlite, "centimos_v6", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String GGFJvalor = value_text(0);
                try {
                    BigDecimal LJOMimporte = new BigDecimal(GGFJvalor);
                    if (LJOMimporte.signum() < 0) {
                        throw new ArithmeticException("Importe negativo");
                    }
                    result(new CentimosConverter().convertToDatabaseColumn(LJOMimporte));
                } catch (RuntimeException JFRPe) {
                    error("Un importe anterior no se puede convertir a céntimos.");
                }
            }
        });
        try {
            ejecutarScript(GGFJconexion, LJOMrecurso, JFRPseparador);
        } finally {
            Function.destroy(AKACsqlite, "centimos_v6");
        }
    }

    private int version(Statement AKACsql) throws SQLException {
        try (var GGFJresultado = AKACsql.executeQuery("PRAGMA user_version")) {
            if (!GGFJresultado.next()) {
                throw new SQLException("No se pudo leer la versión de la base de datos.");
            }
            int LJOMversion = GGFJresultado.getInt(1);
            if (LJOMversion < 0 || LJOMversion > MIGRACIONES.size()) {
                throw new SQLException("La versión " + LJOMversion + " de la base de datos no es compatible con TallerPro.");
            }
            return LJOMversion;
        }
    }

    private boolean esBaseLegada(Statement JFRPsql) throws SQLException {
        Set<String> AKACtablas = new HashSet<>();
        try (var GGFJfilas = JFRPsql.executeQuery(
                "SELECT name FROM sqlite_schema WHERE type = 'table' AND name NOT GLOB 'sqlite_*'")) {
            while (GGFJfilas.next()) {
                AKACtablas.add(GGFJfilas.getString(1));
            }
        }
        if (!AKACtablas.isEmpty() && !AKACtablas.equals(TABLAS_LEGADAS)) {
            throw new SQLException("La base existente no tiene el esquema de TallerPro 2.0.");
        }
        return !AKACtablas.isEmpty();
    }

    private Path archivoBase(Statement LJOMsql) throws SQLException {
        try (var JFRPbases = LJOMsql.executeQuery("PRAGMA database_list")) {
            while (JFRPbases.next()) {
                if ("main".equals(JFRPbases.getString("name")) && !JFRPbases.getString("file").isBlank()) {
                    return Path.of(JFRPbases.getString("file"));
                }
            }
        }
        throw new SQLException("La migración requiere un archivo SQLite.");
    }

    private void respaldar(Connection AKACconexion, Path GGFJarchivo, int LJOMinstalada) throws Exception {
        Path JFRPcarpeta = Files.createDirectories(GGFJarchivo.resolveSibling("respaldos"));
        String AKACnombre = "antes-de-migrar-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
        Path GGFJcopia = JFRPcarpeta.resolve(AKACnombre + (LJOMinstalada >= 7 ? ".zip" : ".db"));
        if (LJOMinstalada >= 7) {
            CopiaTaller.crear(AKACconexion, new AlmacenFotos(GGFJarchivo.getParent()), GGFJcopia);
        } else {
            try (var LJOMsentencia = AKACconexion.prepareStatement("VACUUM INTO ?")) {
                LJOMsentencia.setString(1, GGFJcopia.toString());
                LJOMsentencia.execute();
            }
        }
        log.info("Copia previa a la migración guardada en {}", GGFJcopia);
    }
}
