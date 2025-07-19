-- ================================================================
-- MIGRACIÓN: Tabla de Auditoría y Configuraciones - Prioridad 3
-- ================================================================

-- Crear tabla de logs de auditoría
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT,
    usuario_nombre VARCHAR(100),
    timestamp DATETIME NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    description VARCHAR(1000),
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    result VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    duration_ms BIGINT,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_usuario (usuario_id),
    INDEX idx_audit_operation (operation_type),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_result (result),
    INDEX idx_audit_ip (ip_address),
    INDEX idx_audit_security (operation_type, result, timestamp)
);

-- ================================================================
-- ÍNDICES ADICIONALES PARA OPTIMIZACIÓN DE RENDIMIENTO
-- ================================================================

-- Índices para preguntas (optimización de búsquedas frecuentes)
CREATE INDEX idx_pregunta_nivel_estado ON preguntas(nivel, estado);
CREATE INDEX idx_pregunta_disponibilidad_nivel ON preguntas(estado_disponibilidad, nivel);
CREATE INDEX idx_pregunta_tematica ON preguntas(tematica);
CREATE INDEX idx_pregunta_fecha_creacion ON preguntas(fecha_creacion);
CREATE INDEX idx_pregunta_usuario_estado ON preguntas(creacion_usuario_id, estado);

-- Índices para cuestionarios
CREATE INDEX idx_cuestionario_estado_fecha ON cuestionarios(estado, fecha_creacion);
CREATE INDEX idx_cuestionario_usuario ON cuestionarios(creacion_usuario_id);
CREATE INDEX idx_cuestionario_nivel_estado ON cuestionarios(nivel, estado);

-- Índices para combos
CREATE INDEX idx_combo_estado_tipo ON combos(estado, tipo);
CREATE INDEX idx_combo_usuario ON combos(creacion_usuario_id);
CREATE INDEX idx_combo_fecha ON combos(fecha_creacion);

-- Índices para concursantes
CREATE INDEX idx_concursante_numero ON concursantes(numero_concursante);
CREATE INDEX idx_concursante_programa ON concursantes(programa_id);
CREATE INDEX idx_concursante_cuestionario ON concursantes(cuestionario_id);
CREATE INDEX idx_concursante_combo ON concursantes(combo_id);
CREATE INDEX idx_concursante_estado ON concursantes(estado);

-- Índices para jornadas
CREATE INDEX idx_jornada_fecha ON jornadas(fecha);
CREATE INDEX idx_jornada_estado ON jornadas(estado);
CREATE INDEX idx_jornada_programa ON jornadas(programa_id);

-- Índices para relaciones pregunta-cuestionario
CREATE INDEX idx_pregunta_cuestionario_factor ON cuestionarios_preguntas(factor_multiplicacion);

-- Índices para relaciones pregunta-combo
CREATE INDEX idx_pregunta_combo_factor ON combos_preguntas(factor_multiplicacion);

-- ================================================================
-- CONFIGURACIONES ADICIONALES PARA RENDIMIENTO
-- ================================================================

-- Configurar charset y collation para la tabla de auditoría
ALTER TABLE audit_logs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Configurar particionamiento por fecha en audit_logs (opcional, para MySQL 8.0+)
-- ALTER TABLE audit_logs 
-- PARTITION BY RANGE (TO_DAYS(timestamp)) (
--     PARTITION p_2024_01 VALUES LESS THAN (TO_DAYS('2024-02-01')),
--     PARTITION p_2024_02 VALUES LESS THAN (TO_DAYS('2024-03-01')),
--     PARTITION p_2024_03 VALUES LESS THAN (TO_DAYS('2024-04-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- ================================================================
-- INSERTAR CONFIGURACIONES INICIALES
-- ================================================================

-- Configuración para rate limiting
INSERT INTO configuracion_global (clave, valor, descripcion) VALUES 
('rate_limit_enabled', 'true', 'Habilitar rate limiting en endpoints críticos'),
('max_login_attempts', '5', 'Máximo intentos de login por IP en 15 minutos'),
('max_preguntas_per_minute', '20', 'Máximo preguntas que puede crear un usuario por minuto'),
('session_timeout_minutes', '30', 'Timeout de sesión para usuarios normales'),
('admin_session_timeout_minutes', '60', 'Timeout de sesión para administradores'),
('audit_retention_days', '90', 'Días de retención de logs de auditoría'),
('cache_enabled', 'true', 'Habilitar cache de aplicación'),
('validation_timeout_seconds', '15', 'Timeout para validaciones del sistema')
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- ================================================================
-- VERIFICACIÓN DE LA MIGRACIÓN
-- ================================================================

-- Verificar que la tabla audit_logs se creó correctamente
SELECT 'Tabla audit_logs creada' as status, COUNT(*) as indices_count
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_logs';

-- Verificar índices de rendimiento
SELECT CONCAT('Índices creados para tabla: ', TABLE_NAME) as status, COUNT(*) as count
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('preguntas', 'cuestionarios', 'combos', 'concursantes', 'jornadas')
AND INDEX_NAME NOT IN ('PRIMARY')
GROUP BY TABLE_NAME;

-- Verificar configuraciones
SELECT 'Configuraciones insertadas' as status, COUNT(*) as count
FROM configuracion_global 
WHERE clave IN ('rate_limit_enabled', 'audit_retention_days', 'cache_enabled');

-- ================================================================
-- ESTADÍSTICAS POST-MIGRACIÓN
-- ================================================================

SELECT 
    'MIGRACIÓN PRIORIDAD 3 COMPLETADA' as resultado,
    NOW() as timestamp,
    'Sistema preparado para validaciones avanzadas, auditoría, timeouts y rate limiting' as descripcion;

-- Mostrar tamaño de tablas principales
SELECT 
    TABLE_NAME as tabla,
    TABLE_ROWS as filas,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) as tamaño_mb
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('preguntas', 'cuestionarios', 'combos', 'concursantes', 'jornadas', 'audit_logs')
ORDER BY tamaño_mb DESC; 