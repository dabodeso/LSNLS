-- Script para corregir estados vacíos en preguntas
-- Deshabilitar restricciones de claves foráneas temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- Corregir todos los valores vacíos a 'borrador'
UPDATE preguntas 
SET estado = 'borrador' 
WHERE estado = '';

-- Verificar que se corrigieron
SELECT DISTINCT estado, COUNT(*) as cantidad
FROM preguntas 
GROUP BY estado
ORDER BY estado;

-- Rehabilitar restricciones de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;
