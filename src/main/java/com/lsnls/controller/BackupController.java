package com.lsnls.controller;

import com.lsnls.service.BackupService;
import com.lsnls.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = "*")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private AuthorizationService authService;

    /**
     * Lista todos los backups disponibles
     */
    @GetMapping
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<List<BackupService.BackupInfo>> listBackups() {
        try {
            List<BackupService.BackupInfo> backups = backupService.listBackups();
            return ResponseEntity.ok(backups);
        } catch (Exception e) {
            log.error("Error al listar backups", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crea un backup manual
     */
    @PostMapping("/create")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<Map<String, Object>> createBackup() {
        try {
            String backupFile = backupService.createBackup();
            backupService.cleanupOldBackups();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Backup creado exitosamente",
                "fileName", backupFile
            ));
        } catch (Exception e) {
            log.error("Error al crear backup", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error al crear backup: " + e.getMessage()
            ));
        }
    }

    /**
     * Restaura la base de datos desde un backup
     */
    @PostMapping("/restore/{fileName}")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String fileName) {
        try {
            boolean success = backupService.restoreBackup(fileName);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Base de datos restaurada exitosamente",
                    "fileName", fileName
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error al restaurar la base de datos"
                ));
            }
        } catch (Exception e) {
            log.error("Error al restaurar backup", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error al restaurar backup: " + e.getMessage()
            ));
        }
    }

    /**
     * Ejecuta limpieza de backups antiguos
     */
    @PostMapping("/cleanup")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<Map<String, Object>> cleanupBackups() {
        try {
            backupService.cleanupOldBackups();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Limpieza de backups completada"
            ));
        } catch (Exception e) {
            log.error("Error al limpiar backups", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error al limpiar backups: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene información del estado del sistema de backup
     */
    @GetMapping("/status")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<Map<String, Object>> getBackupStatus() {
        try {
            List<BackupService.BackupInfo> backups = backupService.listBackups();
            
            long totalSize = backups.stream()
                .mapToLong(BackupService.BackupInfo::getSize)
                .sum();
            
            return ResponseEntity.ok(Map.of(
                "totalBackups", backups.size(),
                "totalSize", totalSize,
                "formattedTotalSize", formatSize(totalSize),
                "latestBackup", backups.isEmpty() ? null : backups.get(0),
                "backups", backups
            ));
        } catch (Exception e) {
            log.error("Error al obtener estado de backup", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Formatea el tamaño en bytes a formato legible
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
} 