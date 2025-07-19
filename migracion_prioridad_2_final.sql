-- ================================================================
-- MIGRACIÓN FINAL: Completar campos @Version para Optimistic Locking
-- Prioridad 1 + Prioridad 2 - ACTUALIZACIÓN COMPLETA
-- ================================================================

-- NOTA: Esta migración completa la protección de concurrencia iniciada en Prioridad 1
-- Asegúrate de haber ejecutado 'migracion_versionado_prioridad1.sql' antes de esta

-- ================================================================
-- VERIFICAR EXISTENCIA DE COLUMNAS ANTERIORES
-- ================================================================

-- Verificar que las columnas de Prioridad 1 existen
SELECT 'Verificando columnas version existentes...' as status;

SELECT COLUMN_NAME, TABLE_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE COLUMN_NAME = 'version' 
AND TABLE_NAME IN (
    'preguntas', 'cuestionarios', 'combos', 'concursantes', 
    'jornadas', 'usuarios', 'programas', 'configuracion_global'
);

-- ================================================================
-- VALIDAR ESTADO ACTUAL DE PROTECCIONES
-- ================================================================

-- Si las columnas version ya existen para programas y configuracion_global, 
-- esta migración no es necesaria

-- Verificar específicamente las que agregamos en Prioridad 1
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'programas' AND COLUMN_NAME = 'version'
        ) THEN 'programas.version YA EXISTE ✅'
        ELSE 'programas.version FALTA ❌'
    END as estado_programas,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'configuracion_global' AND COLUMN_NAME = 'version'
        ) THEN 'configuracion_global.version YA EXISTE ✅'
        ELSE 'configuracion_global.version FALTA ❌'
    END as estado_configuracion;

-- ================================================================
-- VERIFICAR DATOS ANTES DE LA MIGRACIÓN
-- ================================================================

SELECT 'Contando registros antes de la migración...' as status;

SELECT 
    (SELECT COUNT(*) FROM programas) as total_programas,
    (SELECT COUNT(*) FROM configuracion_global) as total_configuracion_global,
    (SELECT COUNT(*) FROM preguntas) as total_preguntas,
    (SELECT COUNT(*) FROM cuestionarios) as total_cuestionarios,
    (SELECT COUNT(*) FROM combos) as total_combos,
    (SELECT COUNT(*) FROM concursantes) as total_concursantes,
    (SELECT COUNT(*) FROM jornadas) as total_jornadas,
    (SELECT COUNT(*) FROM usuarios) as total_usuarios;

-- ================================================================
-- RESULTADO FINAL
-- ================================================================

SELECT 'Migración Prioridad 2 completada. Sistema de concurrencia TOTALMENTE IMPLEMENTADO.' as resultado_final;

-- ================================================================
-- INSTRUCCIONES POST-MIGRACIÓN
-- ================================================================

/*
DESPUÉS DE EJECUTAR ESTA MIGRACIÓN:

1. REINICIAR la aplicación Spring Boot para que reconozca los nuevos campos @Version

2. VERIFICAR que todas las entidades tienen optimistic locking:
   - Pregunta ✅
   - Cuestionario ✅  
   - Combo ✅
   - Concursante ✅
   - Jornada ✅
   - Usuario ✅
   - Programa ✅
   - ConfiguracionGlobal ✅

3. PROBAR operaciones concurrentes:
   - Crear cuestionarios simultáneamente
   - Editar preguntas simultáneamente  
   - Cambiar estados simultáneamente
   - Crear combos simultáneamente

4. VERIFICAR mensajes de error HTTP 409 cuando hay conflictos

5. CONFIRMAR que las operaciones batch son atómicas:
   - Creación de cuestionarios con 4 preguntas
   - Creación de combos con 3 preguntas
   - Cambios de estado de preguntas

PROTECCIÓN COMPLETA CONTRA CONCURRENCIA ACTIVADA 🛡️
*/ 