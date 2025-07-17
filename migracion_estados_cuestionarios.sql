-- Migración para agregar nuevos estados de cuestionarios
-- IMPORTANTE: Ejecutar este script ANTES de reiniciar la aplicación

-- 1. Primero agregamos las nuevas opciones al ENUM
ALTER TABLE cuestionarios 
MODIFY COLUMN estado ENUM('borrador', 'creado', 'adjudicado', 'grabado', 'asignado_jornada', 'asignado_concursantes') NOT NULL;

-- 2. Verificar cuestionarios que podrían estar asignados a jornadas
-- (Este paso es informativo, para revisar qué cuestionarios pueden necesitar actualización)
SELECT 
    c.id as cuestionario_id,
    c.estado as estado_actual,
    j.id as jornada_id,
    j.nombre as jornada_nombre
FROM cuestionarios c
INNER JOIN jornadas_cuestionarios jc ON c.id = jc.cuestionario_id
INNER JOIN jornadas j ON j.id = jc.jornada_id
WHERE c.estado = 'creado'
ORDER BY c.id;

-- 3. Verificar cuestionarios que podrían estar asignados a concursantes
-- (Este paso es informativo, para revisar qué cuestionarios pueden necesitar actualización)
SELECT 
    c.id as cuestionario_id,
    c.estado as estado_actual,
    con.id as concursante_id,
    con.nombre as concursante_nombre
FROM cuestionarios c
INNER JOIN concursantes con ON c.id = con.cuestionario_id
WHERE c.estado IN ('creado', 'adjudicado')
ORDER BY c.id;

-- 4. Actualizar automáticamente los estados según las asignaciones existentes
-- NOTA: La aplicación Java manejará estos cambios automáticamente al reiniciarse,
-- pero puedes ejecutar estas consultas manualmente si es necesario:

-- Actualizar cuestionarios asignados a jornadas
UPDATE cuestionarios c
SET estado = 'asignado_jornada'
WHERE c.estado = 'creado'
  AND EXISTS (
    SELECT 1 FROM jornadas_cuestionarios jc 
    WHERE jc.cuestionario_id = c.id
  );

-- Actualizar cuestionarios asignados a concursantes
UPDATE cuestionarios c
SET estado = 'asignado_concursantes'
WHERE c.estado IN ('creado', 'adjudicado', 'asignado_jornada')
  AND EXISTS (
    SELECT 1 FROM concursantes con 
    WHERE con.cuestionario_id = c.id
  );

-- 5. Verificar el resultado final
SELECT 
    estado,
    COUNT(*) as total_cuestionarios
FROM cuestionarios
GROUP BY estado
ORDER BY estado;

-- Mostrar el resumen final de estados
SELECT 'MIGRACIÓN COMPLETADA - Resumen de estados:' as resultado;
SELECT 
    CASE 
        WHEN estado = 'borrador' THEN 'Borrador'
        WHEN estado = 'creado' THEN 'Creado'
        WHEN estado = 'adjudicado' THEN 'Adjudicado'
        WHEN estado = 'grabado' THEN 'Grabado'
        WHEN estado = 'asignado_jornada' THEN 'Asignado a Jornada'
        WHEN estado = 'asignado_concursantes' THEN 'Asignado a Concursantes'
        ELSE estado
    END as estado_descripcion,
    COUNT(*) as cantidad
FROM cuestionarios
GROUP BY estado
ORDER BY 
    CASE estado
        WHEN 'borrador' THEN 1
        WHEN 'creado' THEN 2
        WHEN 'asignado_jornada' THEN 3
        WHEN 'asignado_concursantes' THEN 4
        WHEN 'adjudicado' THEN 5
        WHEN 'grabado' THEN 6
        ELSE 7
    END; 