package com.lsnls.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BackupService {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${lsnls.backup.directory:./backup}")
    private String backupDirectory;

    @Value("${lsnls.backup.retention-days:7}")
    private int retentionDays;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Ejecuta backup automático todos los días a las 22:00 (10 PM) hora española
     */
    @Scheduled(cron = "0 0 22 * * ?", zone = "Europe/Madrid")
    public void executeScheduledBackup() {
        log.info("🔄 Iniciando backup automático programado");
        try {
            String backupFile = createBackup();
            cleanupOldBackups();
            log.info("✅ Backup automático completado: {}", backupFile);
        } catch (Exception e) {
            log.error("❌ Error en backup automático", e);
        }
    }

    /**
     * Crea un backup manual de la base de datos
     */
    public String createBackup() throws IOException {
        log.info("📦 Iniciando creación de backup");

        // Crear directorio de backup si no existe
        Path backupPath = Paths.get(backupDirectory);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
            log.info("📁 Directorio de backup creado: {}", backupPath);
        }

        // Generar nombre del archivo con timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String backupFileName = String.format("lsnls_backup_%s.sql", timestamp);
        Path backupFile = backupPath.resolve(backupFileName);

        // Extraer configuración de la base de datos
        DatabaseConfig dbConfig = extractDatabaseConfig();

        // Comando mysqldump
        List<String> command = Arrays.asList(
            "mysqldump",
            "-h", dbConfig.host,
            "-P", dbConfig.port,
            "-u", dbConfig.username,
            "-p" + dbConfig.password,
            "--single-transaction",
            "--routines",
            "--triggers",
            "--events",
            "--add-drop-database",
            "--create-options",
            "--complete-insert",
            "--extended-insert",
            "--set-charset",
            "--default-character-set=utf8mb4",
            dbConfig.database
        );

        log.info("🔧 Ejecutando comando: mysqldump -h {} -P {} -u {} -p*** {}", 
                dbConfig.host, dbConfig.port, dbConfig.username, dbConfig.database);

        // Ejecutar comando
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(backupFile.toFile());
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = processBuilder.start();
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Backup interrumpido", e);
        }

        if (exitCode == 0) {
            long fileSize = Files.size(backupFile);
            log.info("✅ Backup creado exitosamente: {} ({} bytes)", backupFileName, fileSize);
            
            // Comprimir backup
            compressBackup(backupFile);
            
            return backupFileName;
        } else {
            throw new IOException("Error en mysqldump. Código de salida: " + exitCode);
        }
    }

    /**
     * Comprime el archivo de backup usando PowerShell
     */
    private void compressBackup(Path backupFile) {
        try {
            String zipFile = backupFile.toString() + ".zip";
            List<String> command = Arrays.asList(
                "powershell",
                "-command",
                String.format("Compress-Archive -Path '%s' -DestinationPath '%s' -Force", 
                            backupFile.toString(), zipFile)
            );

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("📦 Backup comprimido: {}", zipFile);
                // Eliminar archivo original
                Files.delete(backupFile);
            } else {
                log.warn("⚠️ No se pudo comprimir el backup: {}", backupFile);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error al comprimir backup", e);
        }
    }

    /**
     * Limpia backups antiguos según la política de retención
     */
    public void cleanupOldBackups() {
        log.info("🧹 Iniciando limpieza de backups antiguos (retención: {} días)", retentionDays);

        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                log.info("📁 Directorio de backup no existe, no hay nada que limpiar");
                return;
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            int deletedCount = 0;

            // Buscar archivos de backup
            List<Path> backupFiles = Files.list(backupPath)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("lsnls_backup_") && 
                           (fileName.endsWith(".sql") || fileName.endsWith(".zip"));
                })
                .collect(Collectors.toList());

            for (Path backupFile : backupFiles) {
                try {
                    LocalDateTime fileDate = extractDateFromFileName(backupFile.getFileName().toString());
                    if (fileDate.isBefore(cutoffDate)) {
                        Files.delete(backupFile);
                        deletedCount++;
                        log.info("🗑️ Eliminado backup antiguo: {}", backupFile.getFileName());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Error al procesar archivo: {}", backupFile.getFileName(), e);
                }
            }

            log.info("✅ Limpieza completada. {} archivos eliminados", deletedCount);

        } catch (Exception e) {
            log.error("❌ Error durante la limpieza de backups", e);
        }
    }

    /**
     * Lista todos los backups disponibles
     */
    public List<BackupInfo> listBackups() {
        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                return List.of();
            }

            return Files.list(backupPath)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("lsnls_backup_") && 
                           (fileName.endsWith(".sql") || fileName.endsWith(".zip"));
                })
                .map(this::createBackupInfo)
                .sorted(Comparator.comparing(BackupInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error al listar backups", e);
            return List.of();
        }
    }

    /**
     * Restaura la base de datos desde un backup
     */
    public boolean restoreBackup(String backupFileName) {
        log.info("🔄 Iniciando restauración desde: {}", backupFileName);

        try {
            Path backupPath = Paths.get(backupDirectory);
            Path backupFile = backupPath.resolve(backupFileName);

            if (!Files.exists(backupFile)) {
                log.error("❌ Archivo de backup no encontrado: {}", backupFile);
                return false;
            }

            // Crear backup de seguridad antes de restaurar
            String safetyBackup = createBackup();
            log.info("🛡️ Backup de seguridad creado: {}", safetyBackup);

            // Extraer configuración de la base de datos
            DatabaseConfig dbConfig = extractDatabaseConfig();

            // Comando mysql para restaurar
            List<String> command = Arrays.asList(
                "mysql",
                "-h", dbConfig.host,
                "-P", dbConfig.port,
                "-u", dbConfig.username,
                "-p" + dbConfig.password,
                dbConfig.database
            );

            log.info("🔧 Ejecutando restauración...");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectInput(backupFile.toFile());
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("✅ Restauración completada exitosamente");
                return true;
            } else {
                log.error("❌ Error en la restauración. Código de salida: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Error durante la restauración", e);
            return false;
        }
    }

    /**
     * Extrae la configuración de la base de datos desde la URL
     */
    private DatabaseConfig extractDatabaseConfig() {
        // Ejemplo: jdbc:mysql://localhost:3306/lsnls
        String url = databaseUrl.replace("jdbc:mysql://", "");
        String[] parts = url.split("/");
        
        String hostPort = parts[0];
        String database = parts[1].split("\\?")[0]; // Remover parámetros de conexión
        
        String[] hostPortParts = hostPort.split(":");
        String host = hostPortParts[0];
        String port = hostPortParts.length > 1 ? hostPortParts[1] : "3306";

        return new DatabaseConfig(host, port, database, databaseUsername, databasePassword);
    }

    /**
     * Extrae la fecha del nombre del archivo
     */
    private LocalDateTime extractDateFromFileName(String fileName) {
        try {
            // Formato: lsnls_backup_20241201_220000.sql
            String datePart = fileName.replace("lsnls_backup_", "").replace(".sql", "").replace(".zip", "");
            return LocalDateTime.parse(datePart, TIMESTAMP_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo extraer fecha del archivo: {}", fileName);
            return LocalDateTime.now().minusDays(30); // Archivo muy antiguo
        }
    }

    /**
     * Crea información del backup
     */
    private BackupInfo createBackupInfo(Path backupFile) {
        try {
            String fileName = backupFile.getFileName().toString();
            LocalDateTime createdAt = extractDateFromFileName(fileName);
            long size = Files.size(backupFile);
            boolean isCompressed = fileName.endsWith(".zip");

            return new BackupInfo(fileName, createdAt, size, isCompressed);
        } catch (Exception e) {
            log.warn("⚠️ Error al crear BackupInfo para: {}", backupFile.getFileName());
            return new BackupInfo(backupFile.getFileName().toString(), LocalDateTime.now(), 0, false);
        }
    }

    /**
     * Clase para configuración de base de datos
     */
    private static class DatabaseConfig {
        final String host;
        final String port;
        final String database;
        final String username;
        final String password;

        DatabaseConfig(String host, String port, String database, String username, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Clase para información del backup
     */
    public static class BackupInfo {
        private final String fileName;
        private final LocalDateTime createdAt;
        private final long size;
        private final boolean compressed;

        public BackupInfo(String fileName, LocalDateTime createdAt, long size, boolean compressed) {
            this.fileName = fileName;
            this.createdAt = createdAt;
            this.size = size;
            this.compressed = compressed;
        }

        public String getFileName() { return fileName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public long getSize() { return size; }
        public boolean isCompressed() { return compressed; }
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
} 