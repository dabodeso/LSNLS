package com.lsnls.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DatabaseMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/lsnls}")
    private String datasourceUrl;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void runMigrations() {
        try {
            final String schema = extractSchemaName(datasourceUrl);
            if (schema == null || schema.isEmpty()) {
                log.warn("[DB MIGRATION] No se pudo determinar el esquema a partir de la URL. Usando 'lsnls' por defecto.");
            }
            final String schemaName = (schema == null || schema.isEmpty()) ? "lsnls" : schema;

            // 1) programas.duracion_objetivo
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'programas' AND COLUMN_NAME = 'duracion_objetivo'",
                    Integer.class,
                    schemaName
            );
            if (count != null && count == 0) {
                log.info("[DB MIGRATION] Añadiendo columna programas.duracion_objetivo ...");
                jdbcTemplate.execute("ALTER TABLE programas ADD COLUMN duracion_objetivo VARCHAR(255) DEFAULT '45m'");
                log.info("[DB MIGRATION] Columna programas.duracion_objetivo añadida correctamente.");
            } else {
                log.info("[DB MIGRATION] Columna programas.duracion_objetivo ya existe. No se realizan cambios.");
            }

            // 2) Configuración global de duración objetivo: migrar solo el valor antiguo por defecto.
            Integer configuracionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM configuracion_global WHERE clave = 'DURACION_OBJETIVO_PROGRAMA'",
                    Integer.class);
            if (configuracionCount != null && configuracionCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO configuracion_global (clave, valor, descripcion, version) VALUES (?, ?, ?, 0)",
                    "DURACION_OBJETIVO_PROGRAMA", "45m",
                    "Duración objetivo por defecto para programas");
            } else {
                jdbcTemplate.update(
                    "UPDATE configuracion_global SET valor = '45m' "
                        + "WHERE clave = 'DURACION_OBJETIVO_PROGRAMA' AND valor = '1h 5m'");
            }

            // 3) programas.codigo y programas.notas
            Integer codigoCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'programas' AND COLUMN_NAME = 'codigo'",
                    Integer.class, schemaName);
            if (codigoCount != null && codigoCount == 0) {
                jdbcTemplate.execute("ALTER TABLE programas ADD COLUMN codigo VARCHAR(32) NULL AFTER id");
            }
            jdbcTemplate.execute("UPDATE programas SET codigo = CAST(id AS CHAR) WHERE codigo IS NULL OR TRIM(codigo) = ''");

            Integer codigoIndexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'programas' AND INDEX_NAME = 'uk_programas_codigo'",
                    Integer.class, schemaName);
            if (codigoIndexCount != null && codigoIndexCount == 0) {
                jdbcTemplate.execute("ALTER TABLE programas ADD UNIQUE INDEX uk_programas_codigo (codigo)");
            }

            Integer notasCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'programas' AND COLUMN_NAME = 'notas'",
                    Integer.class, schemaName);
            if (notasCount != null && notasCount == 0) {
                jdbcTemplate.execute("ALTER TABLE programas ADD COLUMN notas TEXT NULL");
            }

        } catch (Exception e) {
            log.error("[DB MIGRATION] Error ejecutando migraciones: {}", e.getMessage(), e);
        }
    }

    private String extractSchemaName(String url) {
        try {
            // Ej: jdbc:mysql://localhost:3306/lsnls?params
            Pattern pattern = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}


