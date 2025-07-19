-- ========================================
-- MIGRACIÓN: CONTROL DE CONCURRENCIA
-- Fecha: 2025-01-17
-- Propósito: Agregar columnas de versión para Optimistic Locking
-- ========================================

-- Agregar columna version a tabla preguntas
ALTER TABLE preguntas ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla cuestionarios  
ALTER TABLE cuestionarios ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla combos
ALTER TABLE combos ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla concursantes
ALTER TABLE concursantes ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla jornadas
ALTER TABLE jornadas ADD COLUMN version BIGINT DEFAULT 0;

-- Agregar columna version a tabla usuarios
ALTER TABLE usuarios ADD COLUMN version BIGINT DEFAULT 0;

-- Inicializar las versiones existentes
UPDATE preguntas SET version = 0 WHERE version IS NULL;
UPDATE cuestionarios SET version = 0 WHERE version IS NULL;
UPDATE combos SET version = 0 WHERE version IS NULL;
UPDATE concursantes SET version = 0 WHERE version IS NULL;
UPDATE jornadas SET version = 0 WHERE version IS NULL;
UPDATE usuarios SET version = 0 WHERE version IS NULL;

-- Hacer las columnas NOT NULL después de inicializar
ALTER TABLE preguntas MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cuestionarios MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE combos MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE concursantes MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE jornadas MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuarios MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ========================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- ========================================

-- Verificar que todas las tablas tienen la columna version
DESCRIBE preguntas;
DESCRIBE cuestionarios;
DESCRIBE combos;
DESCRIBE concursantes;
DESCRIBE jornadas;
DESCRIBE usuarios;

-- Verificar que todos los registros tienen version = 0
SELECT 'preguntas' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM preguntas
UNION ALL
SELECT 'cuestionarios' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM cuestionarios
UNION ALL  
SELECT 'combos' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM combos
UNION ALL
SELECT 'concursantes' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM concursantes
UNION ALL
SELECT 'jornadas' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM jornadas
UNION ALL
SELECT 'usuarios' as tabla, COUNT(*) as total, COUNT(version) as con_version 
FROM usuarios; 