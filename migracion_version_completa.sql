-- ================================================================
-- MIGRACIÓN COMPLETA: Agregar campos @Version para Optimistic Locking
-- Todas las tablas necesarias para Prioridad 1, 2 y 3
-- ================================================================

-- Verificar estado actual
SELECT 'Iniciando migración de columnas version...' as status;

-- ================================================================
-- AGREGAR COLUMNAS VERSION A TODAS LAS TABLAS
-- ================================================================

-- Tabla usuarios (CRÍTICA - esta faltaba y causa el error de login)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla preguntas 
ALTER TABLE preguntas ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla cuestionarios
ALTER TABLE cuestionarios ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla combos
ALTER TABLE combos ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla concursantes
ALTER TABLE concursantes ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla jornadas
ALTER TABLE jornadas ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla programas (ya debería existir de migración anterior)
ALTER TABLE programas ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Tabla configuracion_global (ya debería existir de migración anterior)
ALTER TABLE configuracion_global ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ================================================================
-- INICIALIZAR VALORES DE VERSION
-- ================================================================

-- Establecer version = 0 para todos los registros existentes
UPDATE usuarios SET version = 0 WHERE version IS NULL;
UPDATE preguntas SET version = 0 WHERE version IS NULL;
UPDATE cuestionarios SET version = 0 WHERE version IS NULL;
UPDATE combos SET version = 0 WHERE version IS NULL;
UPDATE concursantes SET version = 0 WHERE version IS NULL;
UPDATE jornadas SET version = 0 WHERE version IS NULL;
UPDATE programas SET version = 0 WHERE version IS NULL;
UPDATE configuracion_global SET version = 0 WHERE version IS NULL;

-- ================================================================
-- VERIFICAR QUE TODAS LAS COLUMNAS SE AGREGARON
-- ================================================================

SELECT 'Verificando columnas version agregadas...' as status;

SELECT 
    TABLE_NAME as tabla,
    COLUMN_NAME as columna,
    DATA_TYPE as tipo,
    IS_NULLABLE as nullable,
    COLUMN_DEFAULT as valor_defecto
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND COLUMN_NAME = 'version'
AND TABLE_NAME IN (
    'usuarios', 'preguntas', 'cuestionarios', 'combos', 
    'concursantes', 'jornadas', 'programas', 'configuracion_global'
)
ORDER BY TABLE_NAME;

-- ================================================================
-- CONTAR REGISTROS ACTUALIZADOS
-- ================================================================

SELECT 'Registros inicializados por tabla:' as status;

SELECT 
    'usuarios' as tabla, COUNT(*) as registros FROM usuarios
UNION ALL
SELECT 
    'preguntas' as tabla, COUNT(*) as registros FROM preguntas
UNION ALL
SELECT 
    'cuestionarios' as tabla, COUNT(*) as registros FROM cuestionarios
UNION ALL
SELECT 
    'combos' as tabla, COUNT(*) as registros FROM combos
UNION ALL
SELECT 
    'concursantes' as tabla, COUNT(*) as registros FROM concursantes
UNION ALL
SELECT 
    'jornadas' as tabla, COUNT(*) as registros FROM jornadas
UNION ALL
SELECT 
    'programas' as tabla, COUNT(*) as registros FROM programas
UNION ALL
SELECT 
    'configuracion_global' as tabla, COUNT(*) as registros FROM configuracion_global;

-- ================================================================
-- RESULTADO FINAL
-- ================================================================

SELECT 
    'MIGRACIÓN COMPLETADA EXITOSAMENTE' as resultado,
    'Optimistic Locking habilitado en todas las entidades' as descripcion,
    NOW() as timestamp;

-- ================================================================
-- INSTRUCCIONES POST-MIGRACIÓN
-- ================================================================

/*
DESPUÉS DE EJECUTAR ESTA MIGRACIÓN:

1. ✅ Todas las tablas tienen columna 'version BIGINT DEFAULT 0'
2. ✅ Todos los registros existentes tienen version = 0
3. ✅ El login debería funcionar correctamente 
4. ✅ La protección contra concurrencia está activada

SIGUIENTE PASO:
- Reiniciar la aplicación Spring Boot
- Probar el login (debería funcionar)
- Probar las nuevas funcionalidades de Prioridad 3

TABLAS PROTEGIDAS:
✅ usuarios - Para login y gestión de usuarios
✅ preguntas - Para edición simultánea de preguntas  
✅ cuestionarios - Para creación/edición de cuestionarios
✅ combos - Para creación/edición de combos
✅ concursantes - Para asignación de concursantes
✅ jornadas - Para gestión de jornadas
✅ programas - Para administración de programas
✅ configuracion_global - Para configuración del sistema
*/ 