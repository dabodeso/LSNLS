package com.lsnls.service;

import com.lsnls.dto.EntityChangeDTO;
import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.entity.*;
import com.lsnls.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EditLockService {

    private static final Logger log = LoggerFactory.getLogger(EditLockService.class);

    public static final int LOCK_TTL_SECONDS = 120;

    @Autowired
    private EditLockRepository editLockRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void purgeExpiredOnStartup() {
        purgeExpiredLocks();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void purgeExpiredScheduled() {
        purgeExpiredLocks();
    }

    @Transactional
    public void purgeExpiredLocks() {
        int removed = editLockRepository.deleteExpiredBefore(LocalDateTime.now());
        if (removed > 0) {
            log.info("[EDIT LOCK] Eliminados {} bloqueos expirados", removed);
        }
    }

    @Transactional
    public Map<String, Object> acquire(AuditLog.EntityType entityType, Long entityId) {
        Usuario usuario = requireCurrentUser();
        Optional<EditLock> existing = editLockRepository.findForUpdate(entityType, entityId);

        if (existing.isPresent()) {
            EditLock active = existing.get();
            if (!active.isExpired()) {
                if (active.getUsuarioId().equals(usuario.getId())) {
                    log.warn("[EDIT LOCK] Bloqueo activo del mismo usuario {} en {} {}",
                            usuario.getNombre(), entityType, entityId);
                    throw new ResponseStatusException(HttpStatus.LOCKED,
                            "Ya tienes " + label(entityType) + " " + entityId
                                    + " abierta en otra ventana. Ciérrala o espera a que expire el temporizador.");
                }
                log.warn("[EDIT LOCK] Bloqueo denegado para {}: {} editando {} {}",
                        usuario.getNombre(), active.getUsuarioNombre(), entityType, entityId);
                throw new ResponseStatusException(HttpStatus.LOCKED,
                        active.getUsuarioNombre() + " está editando " + label(entityType) + " " + entityId
                                + ". Espera un momento.");
            }
            log.info("[EDIT LOCK] Reutilizando bloqueo expirado en {} {}", entityType, entityId);
            return persistLock(active, entityType, entityId, usuario);
        }

        return persistLock(new EditLock(), entityType, entityId, usuario);
    }

    private Map<String, Object> persistLock(EditLock lock, AuditLog.EntityType entityType,
                                            Long entityId, Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        lock.setEntityType(entityType);
        lock.setEntityId(entityId);
        lock.setUsuarioId(usuario.getId());
        lock.setUsuarioNombre(usuario.getNombre());
        if (lock.getCreatedAt() == null) {
            lock.setCreatedAt(now);
        }
        lock.setUpdatedAt(now);
        lock.setExpiresAt(now.plusSeconds(LOCK_TTL_SECONDS));
        editLockRepository.save(lock);
        log.info("[EDIT LOCK] Adquirido por {} en {} {}", usuario.getNombre(), entityType, entityId);

        return Map.of(
                "entityType", entityType.name(),
                "entityId", entityId,
                "usuarioNombre", usuario.getNombre(),
                "expiresAt", lock.getExpiresAt().toString(),
                "ttlSeconds", LOCK_TTL_SECONDS
        );
    }

    @Transactional
    public Map<String, Object> renew(AuditLog.EntityType entityType, Long entityId) {
        Usuario usuario = requireCurrentUser();
        EditLock lock = editLockRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay bloqueo activo"));

        if (lock.isExpired()) {
            editLockRepository.delete(lock);
            throw new ResponseStatusException(HttpStatus.GONE, "La sesión de edición expiró");
        }
        if (!lock.getUsuarioId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El bloqueo pertenece a otro usuario");
        }

        LocalDateTime now = LocalDateTime.now();
        lock.setUpdatedAt(now);
        lock.setExpiresAt(now.plusSeconds(LOCK_TTL_SECONDS));
        editLockRepository.save(lock);

        return Map.of(
                "expiresAt", lock.getExpiresAt().toString(),
                "ttlSeconds", LOCK_TTL_SECONDS,
                "remainingSeconds", LOCK_TTL_SECONDS
        );
    }

    @Transactional
    public void release(AuditLog.EntityType entityType, Long entityId) {
        Usuario usuario = requireCurrentUser();
        editLockRepository.findByEntityTypeAndEntityId(entityType, entityId).ifPresent(lock -> {
            if (lock.getUsuarioId().equals(usuario.getId()) || lock.isExpired()) {
                editLockRepository.delete(lock);
                log.info("[EDIT LOCK] Liberado por {} en {} {}", usuario.getNombre(), entityType, entityId);
            }
        });
    }

    public Map<String, Object> status(AuditLog.EntityType entityType, Long entityId) {
        cleanupExpired(entityType, entityId);
        return editLockRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .filter(l -> !l.isExpired())
                .map(l -> Map.<String, Object>of(
                        "locked", true,
                        "usuarioNombre", l.getUsuarioNombre(),
                        "usuarioId", l.getUsuarioId(),
                        "expiresAt", l.getExpiresAt().toString(),
                        "remainingSeconds", Math.max(0,
                                java.time.Duration.between(LocalDateTime.now(), l.getExpiresAt()).getSeconds())
                ))
                .orElse(Map.of("locked", false));
    }

    public void assertCanEdit(AuditLog.EntityType entityType, Long entityId) {
        cleanupExpired(entityType, entityId);
        Optional<EditLock> lockOpt = editLockRepository.findByEntityTypeAndEntityId(entityType, entityId);
        if (lockOpt.isEmpty() || lockOpt.get().isExpired()) {
            return;
        }
        Usuario usuario = requireCurrentUser();
        EditLock lock = lockOpt.get();
        if (!lock.getUsuarioId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    lock.getUsuarioNombre() + " está editando " + label(entityType) + " " + entityId + ". Espera un momento.");
        }
    }

    public void logEntityUpdate(AuditLog.EntityType entityType, Long entityId, String description) {
        auditService.logOperation(
                AuditLog.OperationType.UPDATE,
                entityType,
                entityId,
                description,
                AuditLog.OperationResult.SUCCESS
        );
    }

    private void cleanupExpired(AuditLog.EntityType entityType, Long entityId) {
        editLockRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .filter(EditLock::isExpired)
                .ifPresent(editLockRepository::delete);
    }

    private Usuario requireCurrentUser() {
        return authService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    public static AuditLog.EntityType parseEntityType(String raw) {
        try {
            return AuditLog.EntityType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de entidad no válido: " + raw);
        }
    }

    public static String label(AuditLog.EntityType type) {
        switch (type) {
            case PREGUNTA: return "la pregunta";
            case COMBO: return "el combo";
            case CUESTIONARIO: return "el cuestionario";
            case JORNADA: return "la jornada";
            case PROGRAMA: return "el programa";
            case CONCURSANTE: return "el concursante";
            default: return "el elemento";
        }
    }
}
