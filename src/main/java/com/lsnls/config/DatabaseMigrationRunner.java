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
                jdbcTemplate.execute("ALTER TABLE programas ADD COLUMN duracion_objetivo VARCHAR(255) DEFAULT '1h 5m'");
                log.info("[DB MIGRATION] Columna programas.duracion_objetivo añadida correctamente.");
            } else {
                log.info("[DB MIGRATION] Columna programas.duracion_objetivo ya existe. No se realizan cambios.");
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


