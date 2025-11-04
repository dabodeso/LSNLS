package com.lsnls.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

@Slf4j
@Component
public class DataSourceDiagnostics {

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void logDataSourceInfo() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            DatabaseMetaData md = conn.getMetaData();
            String url = md.getURL();
            String user = md.getUserName();
            String driver = md.getDriverName() + " " + md.getDriverVersion();

            String profiles = Arrays.toString(environment.getActiveProfiles());

            String dbName = null;
            String host = null;
            String version = null;
            try (ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                if (rs.next()) dbName = rs.getString(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT @@hostname, @@version")) {
                if (rs.next()) { host = rs.getString(1); version = rs.getString(2); }
            }

            log.info("[DS DEBUG] profiles={}, jdbcUrl={}, dbUser={}, driver={}, database={}, host={}, mysqlVersion={}",
                    profiles, url, user, driver, dbName, host, version);

            log.info("[SEC DEBUG] PasswordEncoder en uso: {}", passwordEncoder != null ? passwordEncoder.getClass().getName() : "null");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                if (rs.next()) log.info("[DS DEBUG] usuarios.count={}", rs.getLong(1));
            } catch (Exception ignore) {}

        } catch (Exception e) {
            log.warn("[DS DEBUG] No se pudo obtener información de DataSource: {}", e.getMessage());
        }
    }
}


