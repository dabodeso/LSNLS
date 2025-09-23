-- Script para verificar y corregir los valores de estado en la tabla cuestionarios
-- Ejecutar en la base de datos del servidor

-- 1. Ver todos los valores únicos de estado
SELECT DISTINCT estado, COUNT(*) as cantidad 
FROM cuestionarios 
GROUP BY estado 
ORDER BY estado;

-- 2. Verificar que los valores coinciden con el enum Java
-- Valores esperados: borrador, revisar, corregir, aprobado, adjudicado, grabado, reaprovechado, liberado
SELECT 
    CASE 
        WHEN estado IN ('borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado', 'reaprovechado', 'liberado') 
        THEN 'VÁLIDO' 
        ELSE 'INVÁLIDO' 
    END as estado_valido,
    estado,
    COUNT(*) as cantidad
FROM cuestionarios 
GROUP BY estado 
ORDER BY estado;

-- 3. Corregir valores comunes problemáticos
UPDATE cuestionarios 
SET estado = 'borrador' 
WHERE estado IS NULL;

UPDATE cuestionarios 
SET estado = 'borrador' 
WHERE estado = '';

UPDATE cuestionarios 
SET estado = 'borrador' 
WHERE estado = 'draft';

UPDATE cuestionarios 
SET estado = 'borrador' 
WHERE estado = 'DRAFT';

UPDATE cuestionarios 
SET estado = 'revisar' 
WHERE estado = 'pending';

UPDATE cuestionarios 
SET estado = 'revisar' 
WHERE estado = 'PENDING';

UPDATE cuestionarios 
SET estado = 'aprobado' 
WHERE estado = 'approved';

UPDATE cuestionarios 
SET estado = 'aprobado' 
WHERE estado = 'APPROVED';

UPDATE cuestionarios 
SET estado = 'adjudicado' 
WHERE estado = 'assigned';

UPDATE cuestionarios 
SET estado = 'adjudicado' 
WHERE estado = 'ASSIGNED';

UPDATE cuestionarios 
SET estado = 'grabado' 
WHERE estado = 'recorded';

UPDATE cuestionarios 
SET estado = 'grabado' 
WHERE estado = 'RECORDED';

-- 4. Verificar valores problemáticos específicos
SELECT estado, COUNT(*) as cantidad
FROM cuestionarios 
WHERE estado NOT IN ('borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado', 'reaprovechado', 'liberado')
GROUP BY estado;

-- 5. Mostrar algunos ejemplos de cuestionarios con cada estado
SELECT id, estado, nombre, fecha_creacion 
FROM cuestionarios 
ORDER BY estado, id 
LIMIT 10;

