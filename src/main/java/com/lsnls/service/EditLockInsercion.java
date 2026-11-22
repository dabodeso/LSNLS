package com.lsnls.service;

import com.lsnls.entity.EditLock;
import com.lsnls.repository.EditLockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Inserta el candado en una transacción propia. Si dos reservas chocan,
 * esta transacción se deshace sola y la de fuera puede responder 423.
 */
@Service
public class EditLockInsercion {

    @Autowired
    private EditLockRepository editLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> insertar(EditLock lock) {
        editLockRepository.saveAndFlush(lock);
        return Map.of(
                "entityType", lock.getEntityType().name(),
                "entityId", lock.getEntityId(),
                "usuarioNombre", lock.getUsuarioNombre(),
                "expiresAt", lock.getExpiresAt().toString(),
                "ttlSeconds", EditLockService.LOCK_TTL_SECONDS
        );
    }
}
