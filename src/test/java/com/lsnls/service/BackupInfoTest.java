package com.lsnls.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupInfoTest {

    @Test
    void formateaTamanoSegunEscala() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 21, 1, 0);
        BackupService.BackupInfo bytes = new BackupService.BackupInfo("a.sql", now, 500, false);
        BackupService.BackupInfo kb = new BackupService.BackupInfo("b.sql.gz", now, 2048, true);
        BackupService.BackupInfo mb = new BackupService.BackupInfo("c.sql", now, 2 * 1024 * 1024, false);

        assertEquals("500 B", bytes.getFormattedSize());
        assertTrue(kb.getFormattedSize().contains("KB"));
        assertTrue(mb.getFormattedSize().contains("MB"));
        assertEquals("a.sql", bytes.getFileName());
        assertEquals(now, bytes.getCreatedAt());
        assertEquals(500, bytes.getSize());
        assertTrue(kb.isCompressed());
    }
}
