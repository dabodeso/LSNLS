-- ================================================================
-- MIGRACIÓN: Agregar campos @Version para Optimistic Locking
-- Prioridad 1 - CRÍTICA
-- ================================================================

-- Agregar columna version a tabla programas
ALTER TABLE programas ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla configuracion_global  
ALTER TABLE configuracion_global ADD COLUMN version BIGINT DEFAULT 0;

-- ================================================================
-- VERIFICACIÓN: Comprobar que las columnas se agregaron correctamente
-- ================================================================

-- Verificar que las columnas existen
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME IN ('programas', 'configuracion_global') 
AND COLUMN_NAME = 'version'
ORDER BY TABLE_NAME, COLUMN_NAME;

-- ================================================================
-- ACTUALIZACIÓN: Inicializar versiones existentes
-- ================================================================

-- Inicializar versión 0 para todos los registros existentes
UPDATE programas SET version = 0 WHERE version IS NULL;
UPDATE configuracion_global SET version = 0 WHERE version IS NULL;

-- ================================================================
-- CONSTRAINTS: Asegurar integridad
-- ================================================================

-- Hacer que las columnas version sean NOT NULL después de inicializar
ALTER TABLE programas ALTER COLUMN version SET NOT NULL;
ALTER TABLE configuracion_global ALTER COLUMN version SET NOT NULL;

-- ================================================================
-- RESULTADO ESPERADO:
-- - programas.version: BIGINT NOT NULL DEFAULT 0
-- - configuracion_global.version: BIGINT NOT NULL DEFAULT 0
-- ================================================================ 