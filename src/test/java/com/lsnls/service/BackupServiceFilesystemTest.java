package com.lsnls.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceFilesystemTest {

    @TempDir
    Path tempDir;

    @Test
    void listBackups_vacioSiNoHayDirectorio() {
        BackupService service = new BackupService();
        ReflectionTestUtils.setField(service, "backupDirectory", tempDir.resolve("no-existe").toString());

        List<BackupService.BackupInfo> backups = service.listBackups();

        assertTrue(backups.isEmpty());
    }

    @Test
    void cleanupOldBackups_borraSoloLosCaducados() throws Exception {
        BackupService service = new BackupService();
        ReflectionTestUtils.setField(service, "backupDirectory", tempDir.toString());
        ReflectionTestUtils.setField(service, "retentionDays", 7);

        Path antiguo = tempDir.resolve("lsnls_backup_20200101_220000.sql");
        Path reciente = tempDir.resolve("lsnls_backup_20991231_220000.sql");
        Path ajeno = tempDir.resolve("otro_archivo.sql");
        Files.writeString(antiguo, "-- old");
        Files.writeString(reciente, "-- new");
        Files.writeString(ajeno, "-- ignore");

        service.cleanupOldBackups();

        assertFalse(Files.exists(antiguo));
        assertTrue(Files.exists(reciente));
        assertTrue(Files.exists(ajeno));

        List<BackupService.BackupInfo> restantes = service.listBackups();
        assertEquals(1, restantes.size());
        assertEquals("lsnls_backup_20991231_220000.sql", restantes.get(0).getFileName());
    }
}
