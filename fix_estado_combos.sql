-- Script para verificar y corregir los valores de estado en la tabla combos
-- Ejecutar en la base de datos del servidor

-- 1. Ver todos los valores únicos de estado
SELECT DISTINCT estado, COUNT(*) as cantidad 
FROM combos 
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
FROM combos 
GROUP BY estado 
ORDER BY estado;

-- 3. Corregir valores comunes problemáticos
UPDATE combos 
SET estado = 'borrador' 
WHERE estado IS NULL;

UPDATE combos 
SET estado = 'borrador' 
WHERE estado = '';

UPDATE combos 
SET estado = 'borrador' 
WHERE estado = 'draft';

UPDATE combos 
SET estado = 'borrador' 
WHERE estado = 'DRAFT';

UPDATE combos 
SET estado = 'revisar' 
WHERE estado = 'pending';

UPDATE combos 
SET estado = 'revisar' 
WHERE estado = 'PENDING';

UPDATE combos 
SET estado = 'aprobado' 
WHERE estado = 'approved';

UPDATE combos 
SET estado = 'aprobado' 
WHERE estado = 'APPROVED';

UPDATE combos 
SET estado = 'adjudicado' 
WHERE estado = 'assigned';

UPDATE combos 
SET estado = 'adjudicado' 
WHERE estado = 'ASSIGNED';

UPDATE combos 
SET estado = 'grabado' 
WHERE estado = 'recorded';

UPDATE combos 
SET estado = 'grabado' 
WHERE estado = 'RECORDED';

-- 4. Verificar valores problemáticos específicos
SELECT estado, COUNT(*) as cantidad
FROM combos 
WHERE estado NOT IN ('borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado', 'reaprovechado', 'liberado')
GROUP BY estado;

-- 5. Mostrar algunos ejemplos de combos con cada estado
SELECT id, estado, tematica, fecha_creacion 
FROM combos 
ORDER BY estado, id 
LIMIT 10;

