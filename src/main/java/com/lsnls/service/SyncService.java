package com.lsnls.service;

import com.lsnls.dto.EntityChangeDTO;
import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.entity.*;
import com.lsnls.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SyncService {

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private CuestionarioRepository cuestionarioRepository;

    @Autowired
    private JornadaRepository jornadaRepository;

    @Autowired
    private ProgramaRepository programaRepository;

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuthService authService;

    public List<EntityChangeDTO> checkVisibleChanges(List<VisibleEntityDTO> items) {
        List<EntityChangeDTO> changes = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return changes;
        }

        Long currentUserId = authService.getCurrentUser().map(Usuario::getId).orElse(null);

        for (VisibleEntityDTO item : items) {
            if (item == null || item.getEntityType() == null || item.getEntityId() == null) {
                continue;
            }
            AuditLog.EntityType type;
            try {
                type = EditLockService.parseEntityType(item.getEntityType());
            } catch (Exception e) {
                continue;
            }

            Long serverVersion = fetchVersion(type, item.getEntityId());
            if (serverVersion == null) {
                continue;
            }

            Long clientVersion = item.getVersion() != null ? item.getVersion() : 0L;
            if (serverVersion.equals(clientVersion)) {
                continue;
            }

            // Sin auditoría de otra persona (cambio propio o ningún log ajeno) no hay aviso:
            // no se devuelve el cambio y el cliente no vuelve a mostrar el toast en cada poll.
            String usuarioNombre = nombreSiCambioDeOtroUsuario(type, item.getEntityId(), currentUserId);
            if (usuarioNombre == null) {
                continue;
            }
            String label = entityLabel(type, item.getEntityId());
            String mensaje = usuarioNombre + " ha actualizado " + label + ". Refresca para ver los cambios.";

            changes.add(new EntityChangeDTO(
                    type.name(),
                    item.getEntityId(),
                    label,
                    usuarioNombre,
                    serverVersion,
                    mensaje
            ));
        }
        return changes;
    }

    /**
     * Nombre de quien hizo el último cambio relevante, solo si no es el usuario actual.
     * Null si el último cambio es propio o no hay auditoría de otra persona: en ese caso no se avisa.
     */
    private String nombreSiCambioDeOtroUsuario(AuditLog.EntityType type, Long entityId, Long currentUserId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(type, entityId);
        if (logs == null) {
            return null;
        }
        for (AuditLog log : logs) {
            if (log.getOperationType() != AuditLog.OperationType.UPDATE
                    && log.getOperationType() != AuditLog.OperationType.STATE_CHANGE
                    && log.getOperationType() != AuditLog.OperationType.CREATE) {
                continue;
            }
            if (log.getUsuario() != null && currentUserId != null && log.getUsuario().getId().equals(currentUserId)) {
                return null;
            }
            if (log.getUsuarioNombre() != null && !log.getUsuarioNombre().isBlank()) {
                return log.getUsuarioNombre();
            }
            return "Otro usuario";
        }
        return null;
    }

    private Long fetchVersion(AuditLog.EntityType type, Long id) {
        switch (type) {
            case PREGUNTA:
                return preguntaRepository.findById(id).map(Pregunta::getVersion).orElse(null);
            case COMBO:
                return comboRepository.findById(id).map(Combo::getVersion).orElse(null);
            case CUESTIONARIO:
                return cuestionarioRepository.findById(id).map(Cuestionario::getVersion).orElse(null);
            case JORNADA:
                return jornadaRepository.findById(id).map(Jornada::getVersion).orElse(null);
            case PROGRAMA:
                return programaRepository.findById(id).map(Programa::getVersion).orElse(null);
            case CONCURSANTE:
                return concursanteRepository.findById(id).map(Concursante::getVersion).orElse(null);
            default:
                return null;
        }
    }

    private String entityLabel(AuditLog.EntityType type, Long id) {
        switch (type) {
            case PREGUNTA:
                return preguntaRepository.findById(id).map(Pregunta::getPregunta).orElse("Pregunta " + id);
            case COMBO:
                return "Combo " + id;
            case CUESTIONARIO:
                return "Cuestionario " + id;
            case JORNADA:
                return jornadaRepository.findById(id).map(Jornada::getNombre).orElse("Jornada " + id);
            case PROGRAMA:
                return programaRepository.findById(id)
                        .map(p -> "Programa T" + p.getTemporada() + " #" + (p.getCodigo() != null ? p.getCodigo() : id))
                        .orElse("Programa " + id);
            case CONCURSANTE:
                return concursanteRepository.findById(id).map(Concursante::getNombre).orElse("Concursante " + id);
            default:
                return type.name() + " " + id;
        }
    }
}
