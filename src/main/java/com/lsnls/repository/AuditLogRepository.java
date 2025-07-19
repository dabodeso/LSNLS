package com.lsnls.repository;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Busca logs por usuario
     */
    Page<AuditLog> findByUsuarioOrderByTimestampDesc(Usuario usuario, Pageable pageable);

    /**
     * Busca logs por tipo de operación
     */
    Page<AuditLog> findByOperationTypeOrderByTimestampDesc(AuditLog.OperationType operationType, Pageable pageable);

    /**
     * Busca logs por tipo de entidad
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(AuditLog.EntityType entityType, Pageable pageable);

    /**
     * Busca logs por entidad específica
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditLog.EntityType entityType, Long entityId);

    /**
     * Busca logs en un rango de fechas
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    /**
     * Busca logs por resultado de operación
     */
    Page<AuditLog> findByResultOrderByTimestampDesc(AuditLog.OperationResult result, Pageable pageable);

    /**
     * Busca logs de operaciones críticas (fallos y bloqueos)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.result IN ('FAILURE', 'BLOCKED') ORDER BY a.timestamp DESC")
    Page<AuditLog> findCriticalEvents(Pageable pageable);

    /**
     * Cuenta logs por usuario en un periodo
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.usuario = :usuario AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countByUsuarioAndTimestampBetween(@Param("usuario") Usuario usuario, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Obtiene estadísticas de operaciones por tipo
     */
    @Query("SELECT a.operationType, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.operationType")
    List<Object[]> getOperationStats(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Obtiene logs de eventos de seguridad
     */
    @Query("SELECT a FROM AuditLog a WHERE a.operationType = 'SECURITY_EVENT' OR a.result = 'BLOCKED' ORDER BY a.timestamp DESC")
    Page<AuditLog> findSecurityEvents(Pageable pageable);

    /**
     * Busca logs por IP address para detectar actividad sospechosa
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByIpAddressAndTimestampBetween(@Param("ipAddress") String ipAddress,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Elimina logs antiguos (para limpieza automática)
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    void deleteLogsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
} 