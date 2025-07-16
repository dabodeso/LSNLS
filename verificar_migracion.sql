-- ===================================================================
-- VERIFICACIÓN: Comprobar estado de la tabla cuestionarios
-- ===================================================================
-- Ejecutar este script para verificar si necesitas hacer la migración
-- ===================================================================

USE lsnls;

-- Mostrar la estructura actual de la tabla cuestionarios
DESCRIBE cuestionarios;

-- Comprobar específicamente si existen los campos nuevos
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'lsnls' 
  AND TABLE_NAME = 'cuestionarios'
  AND COLUMN_NAME IN ('tematica', 'notas_direccion');

-- Contar cuestionarios existentes
SELECT 
    COUNT(*) as total_cuestionarios,
    COUNT(CASE WHEN COLUMN_EXISTS('cuestionarios', 'tematica') THEN 1 END) as con_tematica
FROM cuestionarios;

-- ===================================================================
-- INTERPRETACIÓN:
-- 
-- Si NO aparecen 'tematica' y 'notas_direccion' en el DESCRIBE:
--   → NECESITAS ejecutar migracion_cuestionarios_simple.sql
--
-- Si SÍ aparecen ambos campos:
--   → La migración ya está hecha, no hacer nada
--
-- Si solo aparece uno de los campos:
--   → Ejecutar solo la línea ALTER TABLE del campo faltante
-- =================================================================== 