package com.lsnls.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para registrar logs de auditoría de operaciones críticas
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó la operación
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Nombre del usuario (almacenado por si el usuario se elimina)
     */
    @Column(name = "usuario_nombre", length = 100)
    private String usuarioNombre;

    /**
     * Timestamp de cuando ocurrió la operación
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Tipo de operación realizada
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    /**
     * Tipo de entidad afectada
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    /**
     * ID de la entidad afectada
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Descripción detallada de la operación
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Datos antes de la operación (JSON)
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    /**
     * Datos después de la operación (JSON)
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    /**
     * IP desde donde se realizó la operación
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User Agent del cliente
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Resultado de la operación
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private OperationResult result;

    /**
     * Mensaje de error si la operación falló
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Duración de la operación en millisegundos
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Tipos de operaciones auditables
     */
    public enum OperationType {
        CREATE,          // Crear entidad
        UPDATE,          // Actualizar entidad
        DELETE,          // Eliminar entidad
        STATE_CHANGE,    // Cambio de estado
        APPROVE,         // Aprobar
        REJECT,          // Rechazar
        ASSIGN,          // Asignar
        UNASSIGN,        // Desasignar
        LOGIN,           // Inicio de sesión
        LOGOUT,          // Cierre de sesión
        EXPORT,          // Exportar datos
        VALIDATION,      // Validación del sistema
        SECURITY_EVENT   // Evento de seguridad
    }

    /**
     * Tipos de entidades auditables
     */
    public enum EntityType {
        PREGUNTA,
        CUESTIONARIO,
        COMBO,
        CONCURSANTE,
        JORNADA,
        PROGRAMA,
        USUARIO,
        CONFIGURACION_GLOBAL,
        SISTEMA
    }

    /**
     * Resultado de la operación
     */
    public enum OperationResult {
        SUCCESS,         // Operación exitosa
        FAILURE,         // Operación fallida
        PARTIAL_SUCCESS, // Operación parcialmente exitosa
        BLOCKED          // Operación bloqueada por seguridad/permisos
    }

    /**
     * Constructor para crear un log básico
     */
    public AuditLog(Usuario usuario, OperationType operationType, EntityType entityType, 
                    Long entityId, String description, OperationResult result) {
        this.usuario = usuario;
        this.usuarioNombre = usuario != null ? usuario.getNombre() : "SISTEMA";
        this.timestamp = LocalDateTime.now();
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.result = result;
    }

    /**
     * Constructor completo
     */
    public AuditLog(Usuario usuario, OperationType operationType, EntityType entityType, 
                    Long entityId, String description, String oldValues, String newValues,
                    String ipAddress, String userAgent, OperationResult result) {
        this(usuario, operationType, entityType, entityId, description, result);
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
} 