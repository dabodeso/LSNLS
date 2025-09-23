-- Script para corregir estados de combos
-- Deshabilitar restricciones de claves foráneas temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- Deshabilitar modo seguro para permitir actualizaciones
SET SQL_SAFE_UPDATES = 0;

-- Corregir todos los valores vacíos a 'borrador'
UPDATE combos 
SET estado = 'borrador' 
WHERE estado = '';

-- Corregir valores NULL a 'borrador'
UPDATE combos 
SET estado = 'borrador' 
WHERE estado IS NULL;

-- Corregir valores inválidos a 'borrador'
UPDATE combos 
SET estado = 'borrador' 
WHERE estado NOT IN ('borrador', 'preparacion', 'adjudicado', 'finalizado', 'cancelado');

-- Verificar que se corrigieron
SELECT DISTINCT estado, COUNT(*) as cantidad
FROM combos 
GROUP BY estado
ORDER BY estado;

-- Rehabilitar modo seguro
SET SQL_SAFE_UPDATES = 1;

-- Rehabilitar restricciones de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;
