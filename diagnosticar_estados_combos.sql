-- Script para diagnosticar estados de combos
-- Verificar qué valores de estado existen en la base de datos

-- 1. Ver todos los valores únicos de estado en combos
SELECT DISTINCT estado, COUNT(*) as cantidad
FROM combos
GROUP BY estado
ORDER BY estado;

-- 2. Ver si hay valores NULL
SELECT COUNT(*) as combos_con_estado_null
FROM combos
WHERE estado IS NULL;

-- 3. Ver si hay valores vacíos
SELECT COUNT(*) as combos_con_estado_vacio
FROM combos
WHERE estado = '';

-- 4. Ver los valores que podrían estar causando problemas
SELECT estado, COUNT(*) as cantidad
FROM combos
WHERE estado NOT IN ('borrador', 'preparacion', 'adjudicado', 'finalizado', 'cancelado')
GROUP BY estado;
