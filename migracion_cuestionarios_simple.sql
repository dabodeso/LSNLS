-- ===================================================================
-- MIGRACIÓN: Añadir campos temática y notas_direccion a cuestionarios
-- ===================================================================
-- Ejecutar este script en la base de datos existente LSNLS
-- 
-- INSTRUCCIONES:
-- 1. Conectar a la base de datos lsnls
-- 2. Ejecutar estas sentencias una por una
-- 3. Verificar que no hay errores
-- ===================================================================

USE lsnls;

-- Añadir campo temática (VARCHAR 100, puede ser NULL)
ALTER TABLE cuestionarios 
ADD COLUMN tematica VARCHAR(100) DEFAULT NULL;

-- Añadir campo notas de dirección (TEXT, puede ser NULL)  
ALTER TABLE cuestionarios 
ADD COLUMN notas_direccion TEXT DEFAULT NULL;

-- Verificar que los campos se añadieron correctamente
DESCRIBE cuestionarios;

-- Opcional: Ver algunos cuestionarios para confirmar que todo está bien
SELECT id, estado, tematica, notas_direccion, fecha_creacion 
FROM cuestionarios 
LIMIT 5;

-- ===================================================================
-- RESULTADO ESPERADO:
-- - Los campos 'tematica' y 'notas_direccion' deben aparecer en DESCRIBE
-- - Los cuestionarios existentes tendrán NULL en estos campos (normal)
-- - La aplicación Java podrá funcionar sin errores
-- =================================================================== 