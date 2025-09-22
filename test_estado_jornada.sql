-- Script para verificar los valores de estado en la tabla jornadas
-- Ejecutar en la base de datos del servidor

-- 1. Ver todos los valores únicos de estado
SELECT DISTINCT estado, COUNT(*) as cantidad 
FROM jornadas 
GROUP BY estado 
ORDER BY estado;

-- 2. Verificar que los valores coinciden con el enum Java
-- Valores esperados: preparacion, lista, en_grabacion, completada, archivada
SELECT 
    CASE 
        WHEN estado IN ('preparacion', 'lista', 'en_grabacion', 'completada', 'archivada') 
        THEN 'VÁLIDO' 
        ELSE 'INVÁLIDO' 
    END as estado_valido,
    estado,
    COUNT(*) as cantidad
FROM jornadas 
GROUP BY estado 
ORDER BY estado;

-- 3. Mostrar algunos ejemplos de jornadas con cada estado
SELECT id, nombre, estado, fecha_jornada 
FROM jornadas 
ORDER BY estado, id 
LIMIT 10;
