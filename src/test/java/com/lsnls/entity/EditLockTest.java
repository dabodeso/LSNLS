package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditLockTest {

    @Test
    void isExpiredTrueSiExpiresAtEsNulo() {
        EditLock lock = new EditLock();
        lock.setExpiresAt(null);

        assertTrue(lock.isExpired());
    }

    @Test
    void isExpiredTrueSiExpiresAtEstaEnElPasado() {
        EditLock lock = new EditLock();
        lock.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertTrue(lock.isExpired());
    }

    @Test
    void isExpiredFalseSiExpiresAtEstaEnElFuturo() {
        EditLock lock = new EditLock();
        lock.setExpiresAt(LocalDateTime.now().plusMinutes(2));

        assertFalse(lock.isExpired());
    }
}
