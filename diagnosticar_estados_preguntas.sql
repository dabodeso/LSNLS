-- Script para diagnosticar y corregir estados de preguntas
-- Verificar qué valores de estado existen en la base de datos

-- 1. Ver todos los valores únicos de estado en preguntas
SELECT DISTINCT estado, COUNT(*) as cantidad
FROM preguntas 
GROUP BY estado
ORDER BY estado;

-- 2. Ver si hay valores NULL
SELECT COUNT(*) as preguntas_con_estado_null
FROM preguntas 
WHERE estado IS NULL;

-- 3. Ver si hay valores vacíos
SELECT COUNT(*) as preguntas_con_estado_vacio
FROM preguntas 
WHERE estado = '';

-- 4. Ver los valores que podrían estar causando problemas
SELECT estado, COUNT(*) as cantidad
FROM preguntas 
WHERE estado NOT IN ('borrador', 'para_verificar', 'verificada', 'revisar', 'corregir', 'rechazada', 'aprobada', 'para_aprobar', 'usada')
GROUP BY estado;
