-- Migración para añadir campos temática y notas_direccion a cuestionarios
-- Ejecutar este script en la base de datos existente

USE lsnls;

-- Verificar si las columnas ya existen antes de añadirlas
SET @sql = '';

-- Añadir columna tematica si no existe
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.columns 
WHERE table_schema = 'lsnls' 
  AND table_name = 'cuestionarios' 
  AND column_name = 'tematica';

IF @col_exists = 0 THEN
  SET @sql = CONCAT(@sql, 'ALTER TABLE cuestionarios ADD COLUMN tematica VARCHAR(100) DEFAULT NULL; ');
END IF;

-- Añadir columna notas_direccion si no existe
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.columns 
WHERE table_schema = 'lsnls' 
  AND table_name = 'cuestionarios' 
  AND column_name = 'notas_direccion';

IF @col_exists = 0 THEN
  SET @sql = CONCAT(@sql, 'ALTER TABLE cuestionarios ADD COLUMN notas_direccion TEXT DEFAULT NULL; ');
END IF;

-- Ejecutar las alteraciones si hay alguna
IF LENGTH(@sql) > 0 THEN
  SET @sql = CONCAT(@sql, 'SELECT "Campos añadidos correctamente a tabla cuestionarios" as resultado;');
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
ELSE
  SELECT "Los campos ya existen en la tabla cuestionarios" as resultado;
END IF;

-- Verificar que los campos se añadieron correctamente
DESCRIBE cuestionarios; 