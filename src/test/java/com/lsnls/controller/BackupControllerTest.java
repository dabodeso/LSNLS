package com.lsnls.controller;

import com.lsnls.service.AuthorizationService;
import com.lsnls.service.BackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    @Mock
    private BackupService backupService;

    @Mock
    private AuthorizationService authService;

    @InjectMocks
    private BackupController backupController;

    private BackupService.BackupInfo backup(String name, long size) {
        return new BackupService.BackupInfo(name, LocalDateTime.now(), size, true);
    }

    @Test
    void listBackups_ok_devuelve200() {
        List<BackupService.BackupInfo> backups = Collections.singletonList(backup("a.sql", 100));
        when(backupService.listBackups()).thenReturn(backups);

        ResponseEntity<?> response = backupController.listBackups();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(backups, response.getBody());
    }

    @Test
    void listBackups_excepcion_devuelve500() {
        when(backupService.listBackups()).thenThrow(new RuntimeException("disco lleno"));

        ResponseEntity<?> response = backupController.listBackups();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("disco lleno"));
    }

    @Test
    void createBackup_ok_devuelve200() throws Exception {
        when(backupService.createBackup()).thenReturn("backup.sql");
        doNothing().when(backupService).cleanupOldBackups();

        ResponseEntity<Map<String, Object>> response = backupController.createBackup();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("backup.sql", response.getBody().get("fileName"));
        verify(backupService).cleanupOldBackups();
    }

    @Test
    void createBackup_excepcion_devuelve500() throws Exception {
        when(backupService.createBackup()).thenThrow(new RuntimeException("sin espacio"));

        ResponseEntity<Map<String, Object>> response = backupController.createBackup();

        assertEquals(500, response.getStatusCodeValue());
        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("sin espacio"));
    }

    @Test
    void restoreBackup_ok_devuelve200() {
        when(backupService.restoreBackup("ok.sql")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = backupController.restoreBackup("ok.sql");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("ok.sql", response.getBody().get("fileName"));
    }

    @Test
    void restoreBackup_fallo_devuelve400() {
        when(backupService.restoreBackup("bad.sql")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = backupController.restoreBackup("bad.sql");

        assertEquals(400, response.getStatusCodeValue());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void restoreBackup_excepcion_devuelve500() {
        when(backupService.restoreBackup("x.sql")).thenThrow(new RuntimeException("corrupto"));

        ResponseEntity<Map<String, Object>> response = backupController.restoreBackup("x.sql");

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("message").toString().contains("corrupto"));
    }

    @Test
    void cleanupBackups_ok_devuelve200() {
        doNothing().when(backupService).cleanupOldBackups();

        ResponseEntity<Map<String, Object>> response = backupController.cleanupBackups();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(true, response.getBody().get("success"));
    }

    @Test
    void cleanupBackups_excepcion_devuelve500() {
        doThrow(new RuntimeException("io")).when(backupService).cleanupOldBackups();

        ResponseEntity<Map<String, Object>> response = backupController.cleanupBackups();

        assertEquals(500, response.getStatusCodeValue());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void getBackupStatus_ok_formateaBytes() {
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backup("a.sql", 500)));

        ResponseEntity<?> response = backupController.getBackupStatus();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1, body.get("totalBackups"));
        assertEquals(500L, body.get("totalSize"));
        assertEquals("500 B", body.get("formattedTotalSize"));
    }

    @Test
    void getBackupStatus_ok_formateaKbYMb() {
        when(backupService.listBackups()).thenReturn(Arrays.asList(
                backup("a.sql", 2048),
                backup("b.sql", 2L * 1024 * 1024)));

        ResponseEntity<?> response = backupController.getBackupStatus();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(2, body.get("totalBackups"));
        assertTrue(body.get("formattedTotalSize").toString().contains("MB"));
    }

    @Test
    void getBackupStatus_listaVacia_devuelve500PorNullEnMapOf() {
        when(backupService.listBackups()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = backupController.getBackupStatus();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno"));
    }

    @Test
    void getBackupStatus_excepcion_devuelve500() {
        when(backupService.listBackups()).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = backupController.getBackupStatus();

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("error"));
    }
}
