-- Script para corregir estados de preguntas problemáticos
-- Deshabilitar restricciones de claves foráneas temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Corregir valores NULL a 'borrador'
UPDATE preguntas 
SET estado = 'borrador' 
WHERE estado IS NULL;

-- 2. Corregir valores vacíos a 'borrador'
UPDATE preguntas 
SET estado = 'borrador' 
WHERE estado = '';

-- 3. Corregir valores que no están en el enum (ajustar según lo que encuentres)
-- Ejemplo: si hay valores como 'aprobado' en lugar de 'aprobada'
UPDATE preguntas 
SET estado = 'aprobada' 
WHERE estado = 'aprobado';

-- 4. Corregir otros valores comunes que podrían estar mal
UPDATE preguntas 
SET estado = 'verificada' 
WHERE estado = 'verificado';

UPDATE preguntas 
SET estado = 'rechazada' 
WHERE estado = 'rechazado';

-- 5. Si hay otros valores problemáticos, añadirlos aquí
-- UPDATE preguntas 
-- SET estado = 'borrador' 
-- WHERE estado = 'valor_problematico';

-- Rehabilitar restricciones de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- Verificar que todos los estados sean válidos
SELECT DISTINCT estado, COUNT(*) as cantidad
FROM preguntas 
GROUP BY estado
ORDER BY estado;
