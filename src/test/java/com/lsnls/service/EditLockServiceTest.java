package com.lsnls.service;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.EditLock;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.EditLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditLockServiceTest {

    @Mock
    private EditLockRepository editLockRepository;

    @Mock
    private AuthService authService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private EditLockService editLockService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombre("ana");
    }

    @Test
    void acquireCreaLockNuevo() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(editLockRepository.findForUpdate(AuditLog.EntityType.PREGUNTA, 10L))
                .thenReturn(Optional.empty());
        when(editLockRepository.save(any(EditLock.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = editLockService.acquire(AuditLog.EntityType.PREGUNTA, 10L);

        assertEquals("PREGUNTA", resultado.get("entityType"));
        assertEquals(10L, resultado.get("entityId"));
        assertEquals("ana", resultado.get("usuarioNombre"));
        assertEquals(EditLockService.LOCK_TTL_SECONDS, resultado.get("ttlSeconds"));
        verify(editLockRepository).save(any(EditLock.class));
    }

    @Test
    void acquireMismoUsuarioLanzaLocked() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(3L, "ana");
        when(editLockRepository.findForUpdate(AuditLog.EntityType.COMBO, 2L))
                .thenReturn(Optional.of(lock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.acquire(AuditLog.EntityType.COMBO, 2L));
        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        assertTrue(ex.getReason().contains("otra ventana"));
        verify(editLockRepository, never()).save(any());
    }

    @Test
    void acquireOtroUsuarioLanzaLocked() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(99L, "bruno");
        when(editLockRepository.findForUpdate(AuditLog.EntityType.CUESTIONARIO, 4L))
                .thenReturn(Optional.of(lock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.acquire(AuditLog.EntityType.CUESTIONARIO, 4L));
        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        assertTrue(ex.getReason().contains("bruno"));
    }

    @Test
    void acquireLockExpiradoLoReutiliza() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = new EditLock();
        lock.setUsuarioId(99L);
        lock.setUsuarioNombre("bruno");
        lock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        lock.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(editLockRepository.findForUpdate(AuditLog.EntityType.JORNADA, 8L))
                .thenReturn(Optional.of(lock));
        when(editLockRepository.save(any(EditLock.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = editLockService.acquire(AuditLog.EntityType.JORNADA, 8L);

        assertEquals("ana", resultado.get("usuarioNombre"));
        assertEquals(3L, lock.getUsuarioId());
        verify(editLockRepository).save(lock);
    }

    @Test
    void acquireSinUsuarioLanzaUnauthorized() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.acquire(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void renewExtiendeExpiracion() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(3L, "ana");
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PROGRAMA, 1L))
                .thenReturn(Optional.of(lock));
        when(editLockRepository.save(any(EditLock.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = editLockService.renew(AuditLog.EntityType.PROGRAMA, 1L);

        assertEquals(EditLockService.LOCK_TTL_SECONDS, resultado.get("ttlSeconds"));
        assertEquals(EditLockService.LOCK_TTL_SECONDS, resultado.get("remainingSeconds"));
        verify(editLockRepository).save(lock);
    }

    @Test
    void renewSinLockLanzaNotFound() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.renew(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void renewLockExpiradoLanzaGone() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = new EditLock();
        lock.setUsuarioId(3L);
        lock.setExpiresAt(LocalDateTime.now().minusSeconds(5));
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.of(lock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.renew(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(HttpStatus.GONE, ex.getStatus());
        verify(editLockRepository).delete(lock);
    }

    @Test
    void renewOtroUsuarioLanzaForbidden() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(99L, "bruno");
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.of(lock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.renew(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void releaseMismoUsuarioElimina() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(3L, "ana");
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.CONCURSANTE, 6L))
                .thenReturn(Optional.of(lock));

        editLockService.release(AuditLog.EntityType.CONCURSANTE, 6L);

        verify(editLockRepository).delete(lock);
    }

    @Test
    void releaseOtroUsuarioNoEliminaSiNoExpirado() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(99L, "bruno");
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.of(lock));

        editLockService.release(AuditLog.EntityType.PREGUNTA, 1L);

        verify(editLockRepository, never()).delete(any());
    }

    @Test
    void releaseOtroUsuarioEliminaSiExpirado() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = new EditLock();
        lock.setUsuarioId(99L);
        lock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.of(lock));

        editLockService.release(AuditLog.EntityType.PREGUNTA, 1L);

        verify(editLockRepository).delete(lock);
    }

    @Test
    void statusLocked() {
        EditLock lock = lockActivo(3L, "ana");
        when(editLockRepository.findByEntityTypeAndEntityId(eq(AuditLog.EntityType.PREGUNTA), eq(1L)))
                .thenReturn(Optional.of(lock));

        Map<String, Object> status = editLockService.status(AuditLog.EntityType.PREGUNTA, 1L);

        assertEquals(true, status.get("locked"));
        assertEquals("ana", status.get("usuarioNombre"));
        assertEquals(3L, status.get("usuarioId"));
    }

    @Test
    void statusUnlockedSiNoHayLock() {
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.empty());

        Map<String, Object> status = editLockService.status(AuditLog.EntityType.PREGUNTA, 1L);

        assertEquals(false, status.get("locked"));
    }

    @Test
    void statusUnlockedSiLockExpirado() {
        EditLock lock = new EditLock();
        lock.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(editLockRepository.findByEntityTypeAndEntityId(eq(AuditLog.EntityType.PREGUNTA), eq(1L)))
                .thenReturn(Optional.of(lock));

        Map<String, Object> status = editLockService.status(AuditLog.EntityType.PREGUNTA, 1L);

        assertEquals(false, status.get("locked"));
        verify(editLockRepository).delete(lock);
    }

    @Test
    void assertCanEditSinLockNoLanza() {
        when(editLockRepository.findByEntityTypeAndEntityId(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(Optional.empty());

        editLockService.assertCanEdit(AuditLog.EntityType.PREGUNTA, 1L);
    }

    @Test
    void assertCanEditMismoUsuarioNoLanza() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(3L, "ana");
        when(editLockRepository.findByEntityTypeAndEntityId(eq(AuditLog.EntityType.PREGUNTA), eq(1L)))
                .thenReturn(Optional.of(lock));

        editLockService.assertCanEdit(AuditLog.EntityType.PREGUNTA, 1L);
    }

    @Test
    void assertCanEditOtroUsuarioLanzaLocked() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        EditLock lock = lockActivo(99L, "bruno");
        when(editLockRepository.findByEntityTypeAndEntityId(eq(AuditLog.EntityType.PREGUNTA), eq(1L)))
                .thenReturn(Optional.of(lock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> editLockService.assertCanEdit(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(HttpStatus.LOCKED, ex.getStatus());
    }

    @Test
    void parseEntityTypeValido() {
        assertEquals(AuditLog.EntityType.PREGUNTA, EditLockService.parseEntityType("pregunta"));
        assertEquals(AuditLog.EntityType.COMBO, EditLockService.parseEntityType("COMBO"));
    }

    @Test
    void parseEntityTypeInvalido() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> EditLockService.parseEntityType("no-existe"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getReason().contains("no válido"));
    }

    @Test
    void labelParaCadaEntityType() {
        assertEquals("la pregunta", EditLockService.label(AuditLog.EntityType.PREGUNTA));
        assertEquals("el combo", EditLockService.label(AuditLog.EntityType.COMBO));
        assertEquals("el cuestionario", EditLockService.label(AuditLog.EntityType.CUESTIONARIO));
        assertEquals("la jornada", EditLockService.label(AuditLog.EntityType.JORNADA));
        assertEquals("el programa", EditLockService.label(AuditLog.EntityType.PROGRAMA));
        assertEquals("el concursante", EditLockService.label(AuditLog.EntityType.CONCURSANTE));
        assertEquals("el elemento", EditLockService.label(AuditLog.EntityType.USUARIO));
        assertEquals("el elemento", EditLockService.label(AuditLog.EntityType.SISTEMA));
        assertEquals("el elemento", EditLockService.label(AuditLog.EntityType.CONFIGURACION_GLOBAL));
    }

    @Test
    void logEntityUpdateDelegaEnAuditService() {
        editLockService.logEntityUpdate(AuditLog.EntityType.PREGUNTA, 9L, "actualizada");

        verify(auditService).logOperation(
                AuditLog.OperationType.UPDATE,
                AuditLog.EntityType.PREGUNTA,
                9L,
                "actualizada",
                AuditLog.OperationResult.SUCCESS);
    }

    @Test
    void purgeExpiredLocksConEliminados() {
        when(editLockRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(4);

        editLockService.purgeExpiredLocks();

        verify(editLockRepository).deleteExpiredBefore(any(LocalDateTime.class));
    }

    @Test
    void purgeExpiredLocksSinEliminados() {
        when(editLockRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(0);

        editLockService.purgeExpiredLocks();

        verify(editLockRepository).deleteExpiredBefore(any(LocalDateTime.class));
    }

    @Test
    void purgeExpiredOnStartupYScheduled() {
        when(editLockRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(1);

        editLockService.purgeExpiredOnStartup();
        editLockService.purgeExpiredScheduled();

        verify(editLockRepository, org.mockito.Mockito.times(2)).deleteExpiredBefore(any(LocalDateTime.class));
    }

    private EditLock lockActivo(Long usuarioId, String nombre) {
        EditLock lock = new EditLock();
        lock.setUsuarioId(usuarioId);
        lock.setUsuarioNombre(nombre);
        lock.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        lock.setCreatedAt(LocalDateTime.now());
        lock.setUpdatedAt(LocalDateTime.now());
        return lock;
    }
}
