package com.lsnls.service;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar logs de auditoría
 * Registra automáticamente operaciones críticas del sistema
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Registra una operación básica de auditoría de forma asíncrona
     */
    @Async
    public void logOperation(AuditLog.OperationType operationType, 
                            AuditLog.EntityType entityType,
                            Long entityId, 
                            String description,
                            AuditLog.OperationResult result) {
        try {
            Optional<Usuario> currentUser = authService.getCurrentUser();
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog log = new AuditLog(
                currentUser.orElse(null),
                operationType,
                entityType,
                entityId,
                description,
                result
            );
            
            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            // No fallar la operación principal por errores de auditoría
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }

    /**
     * Registra una operación completa con valores antes y después
     */
    @Async
    public void logOperationWithValues(AuditLog.OperationType operationType,
                                      AuditLog.EntityType entityType,
                                      Long entityId,
                                      String description,
                                      Object oldValues,
                                      Object newValues,
                                      AuditLog.OperationResult result) {
        try {
            Optional<Usuario> currentUser = authService.getCurrentUser();
            HttpServletRequest request = getCurrentRequest();
            
            String oldValuesJson = oldValues != null ? objectMapper.writeValueAsString(oldValues) : null;
            String newValuesJson = newValues != null ? objectMapper.writeValueAsString(newValues) : null;
            
            AuditLog log = new AuditLog(
                currentUser.orElse(null),
                operationType,
                entityType,
                entityId,
                description,
                oldValuesJson,
                newValuesJson,
                request != null ? getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                result
            );
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría con valores: " + e.getMessage());
        }
    }

    /**
     * Registra un evento de seguridad
     */
    @Async
    public void logSecurityEvent(String description, String ipAddress, String userAgent) {
        try {
            Optional<Usuario> currentUser = authService.getCurrentUser();
            
            AuditLog log = new AuditLog(
                currentUser.orElse(null),
                AuditLog.OperationType.SECURITY_EVENT,
                AuditLog.EntityType.SISTEMA,
                null,
                description,
                AuditLog.OperationResult.BLOCKED
            );
            
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            System.err.println("Error al registrar evento de seguridad: " + e.getMessage());
        }
    }

    /**
     * Registra login exitoso
     */
    @Async
    public void logSuccessfulLogin(Usuario usuario) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog log = new AuditLog(
                usuario,
                AuditLog.OperationType.LOGIN,
                AuditLog.EntityType.USUARIO,
                usuario.getId(),
                "Inicio de sesión exitoso para usuario: " + usuario.getNombre(),
                AuditLog.OperationResult.SUCCESS
            );
            
            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            System.err.println("Error al registrar login exitoso: " + e.getMessage());
        }
    }

    /**
     * Registra intento de login fallido
     */
    @Async
    public void logFailedLogin(String username, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog log = new AuditLog(
                null,
                AuditLog.OperationType.LOGIN,
                AuditLog.EntityType.USUARIO,
                null,
                "Intento de login fallido para usuario: " + username + ". Razón: " + reason,
                AuditLog.OperationResult.FAILURE
            );
            
            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
            
            log.setErrorMessage(reason);
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            System.err.println("Error al registrar login fallido: " + e.getMessage());
        }
    }

    /**
     * Métodos de consulta de logs
     */
    public Page<AuditLog> findLogsByUser(Usuario usuario, Pageable pageable) {
        return auditLogRepository.findByUsuarioOrderByTimestampDesc(usuario, pageable);
    }

    public Page<AuditLog> findLogsByOperationType(AuditLog.OperationType operationType, Pageable pageable) {
        return auditLogRepository.findByOperationTypeOrderByTimestampDesc(operationType, pageable);
    }

    public Page<AuditLog> findLogsByEntityType(AuditLog.EntityType entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
    }

    public List<AuditLog> findLogsByEntity(AuditLog.EntityType entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    public Page<AuditLog> findLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate, pageable);
    }

    public Page<AuditLog> findCriticalEvents(Pageable pageable) {
        return auditLogRepository.findCriticalEvents(pageable);
    }

    public Page<AuditLog> findSecurityEvents(Pageable pageable) {
        return auditLogRepository.findSecurityEvents(pageable);
    }

    /**
     * Obtiene estadísticas de operaciones
     */
    public List<Object[]> getOperationStats(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.getOperationStats(startDate, endDate);
    }

    /**
     * Detecta actividad sospechosa por IP
     */
    public List<AuditLog> findSuspiciousActivity(String ipAddress, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByIpAddressAndTimestampBetween(ipAddress, startDate, endDate);
    }

    /**
     * Limpia logs antiguos (ejecutar periódicamente)
     */
    public void cleanOldLogs(LocalDateTime cutoffDate) {
        try {
            auditLogRepository.deleteLogsOlderThan(cutoffDate);
        } catch (Exception e) {
            System.err.println("Error al limpiar logs antiguos: " + e.getMessage());
        }
    }

    /**
     * Métodos auxiliares
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Métodos de conveniencia para operaciones comunes
     */
    public void logPreguntaCreated(Long preguntaId, String descripcion) {
        logOperation(AuditLog.OperationType.CREATE, AuditLog.EntityType.PREGUNTA, 
                    preguntaId, descripcion, AuditLog.OperationResult.SUCCESS);
    }

    public void logPreguntaStateChange(Long preguntaId, String oldState, String newState) {
        logOperation(AuditLog.OperationType.STATE_CHANGE, AuditLog.EntityType.PREGUNTA,
                    preguntaId, "Cambio de estado de " + oldState + " a " + newState, 
                    AuditLog.OperationResult.SUCCESS);
    }

    public void logCuestionarioCreated(Long cuestionarioId, int numPreguntas) {
        logOperation(AuditLog.OperationType.CREATE, AuditLog.EntityType.CUESTIONARIO,
                    cuestionarioId, "Cuestionario creado con " + numPreguntas + " preguntas",
                    AuditLog.OperationResult.SUCCESS);
    }

    public void logComboCreated(Long comboId, int numPreguntas) {
        logOperation(AuditLog.OperationType.CREATE, AuditLog.EntityType.COMBO,
                    comboId, "Combo creado con " + numPreguntas + " preguntas multiplicadoras",
                    AuditLog.OperationResult.SUCCESS);
    }

    public void logConcursanteAssigned(Long concursanteId, Long cuestionarioId, Long comboId) {
        String descripcion = "Concursante asignado";
        if (cuestionarioId != null) {
            descripcion += " - Cuestionario: " + cuestionarioId;
        }
        if (comboId != null) {
            descripcion += " - Combo: " + comboId;
        }
        
        logOperation(AuditLog.OperationType.ASSIGN, AuditLog.EntityType.CONCURSANTE,
                    concursanteId, descripcion, AuditLog.OperationResult.SUCCESS);
    }

    public void logValidationError(Long entityId, AuditLog.EntityType entityType, String error) {
        logOperation(AuditLog.OperationType.VALIDATION, entityType,
                    entityId, "Error de validación: " + error, AuditLog.OperationResult.FAILURE);
    }

    public void logConcurrencyConflict(AuditLog.EntityType entityType, Long entityId, String operation) {
        logOperation(AuditLog.OperationType.UPDATE, entityType,
                    entityId, "Conflicto de concurrencia en " + operation, AuditLog.OperationResult.BLOCKED);
    }
} 