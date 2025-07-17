-- =========================================================
-- MIGRACIÓN DE ESTADOS DE COMBOS EN BASE DE DATOS
-- =========================================================
-- 
-- Este script migra los combos existentes en la base de datos
-- para usar el nuevo sistema de control de asignaciones.
--
-- EJECUTAR EN EL SIGUIENTE ORDEN:
-- 1. Modificar ENUM de la tabla (ALTER TABLE)
-- 2. Verificar asignaciones existentes (SELECT)
-- 3. Migrar datos (UPDATE)
-- 4. Verificar resultado final (SELECT)
--
-- =========================================================

-- PASO 1: Modificar ENUM para incluir nuevos estados
-- =========================================================

ALTER TABLE combos 
MODIFY COLUMN estado ENUM('borrador', 'creado', 'adjudicado', 'grabado', 'asignado_jornada', 'asignado_concursantes') NOT NULL;

-- =========================================================

-- PASO 2: Verificar asignaciones actuales
-- =========================================================

-- Ver estado actual de combos
SELECT 'ESTADO ACTUAL DE COMBOS' as seccion;
SELECT estado, COUNT(*) as cantidad 
FROM combos 
GROUP BY estado 
ORDER BY estado;

-- Ver combos asignados a jornadas
SELECT 'COMBOS ASIGNADOS A JORNADAS' as seccion;
SELECT c.id, c.estado, j.id as jornada_id, j.nombre as jornada_nombre
FROM combos c
JOIN jornadas_combos jc ON c.id = jc.combo_id  
JOIN jornadas j ON j.id = jc.jornada_id
ORDER BY c.id;

-- Ver combos asignados a concursantes
SELECT 'COMBOS ASIGNADOS A CONCURSANTES' as seccion;
SELECT c.id, c.estado, con.id as concursante_id, con.nombre as concursante_nombre
FROM combos c
JOIN concursantes con ON c.id = con.combo_id
ORDER BY c.id;

-- =========================================================

-- PASO 3: Migración automática de datos
-- =========================================================

-- Actualizar combos asignados a concursantes
UPDATE combos c 
SET estado = 'asignado_concursantes'
WHERE c.id IN (
    SELECT DISTINCT combo_id 
    FROM concursantes 
    WHERE combo_id IS NOT NULL
);

-- Actualizar combos asignados a jornadas (pero no a concursantes)
UPDATE combos c 
SET estado = 'asignado_jornada'
WHERE c.id IN (
    SELECT DISTINCT combo_id 
    FROM jornadas_combos
) 
AND c.estado != 'asignado_concursantes';

-- =========================================================

-- PASO 4: Verificar resultado de la migración
-- =========================================================

SELECT 'ESTADO DESPUÉS DE LA MIGRACIÓN' as seccion;
SELECT estado, COUNT(*) as cantidad 
FROM combos 
GROUP BY estado 
ORDER BY estado;

-- Verificar combos en estado asignado_jornada
SELECT 'COMBOS EN ESTADO asignado_jornada' as seccion;
SELECT c.id, c.estado, j.nombre as jornada
FROM combos c
JOIN jornadas_combos jc ON c.id = jc.combo_id  
JOIN jornadas j ON j.id = jc.jornada_id
WHERE c.estado = 'asignado_jornada'
ORDER BY c.id;

-- Verificar combos en estado asignado_concursantes
SELECT 'COMBOS EN ESTADO asignado_concursantes' as seccion;
SELECT c.id, c.estado, con.nombre as concursante
FROM combos c
JOIN concursantes con ON c.id = con.combo_id
WHERE c.estado = 'asignado_concursantes'
ORDER BY c.id;

-- =========================================================

-- CONSULTAS DE MONITOREO PARA USO FUTURO
-- =========================================================

-- Consulta para ver distribución de estados
-- SELECT estado, COUNT(*) as cantidad FROM combos GROUP BY estado;

-- Consulta para ver combos asignados a jornadas
-- SELECT c.id, c.estado, j.nombre as jornada
-- FROM combos c
-- JOIN jornadas_combos jc ON c.id = jc.combo_id  
-- JOIN jornadas j ON j.id = jc.jornada_id
-- WHERE c.estado = 'asignado_jornada';

-- Consulta para ver combos asignados a concursantes
-- SELECT c.id, c.estado, con.nombre as concursante
-- FROM combos c
-- JOIN concursantes con ON c.id = con.combo_id
-- WHERE c.estado = 'asignado_concursantes';

-- =========================================================
-- FIN DEL SCRIPT DE MIGRACIÓN
-- ========================================================= 