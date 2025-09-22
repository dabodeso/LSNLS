-- Script para crear o corregir la tabla historial_jornadas
-- Ejecutar en la base de datos del servidor

-- 1. Verificar si la tabla existe
SELECT COUNT(*) as tabla_existe 
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name = 'historial_jornadas';

-- 2. Si la tabla no existe, crearla
CREATE TABLE IF NOT EXISTS historial_jornadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_id BIGINT,
    cuestionario_id BIGINT,
    jornada_id BIGINT,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    accion VARCHAR(100),
    detalles TEXT,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (combo_id) REFERENCES combos(id) ON DELETE CASCADE,
    FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios(id) ON DELETE CASCADE,
    FOREIGN KEY (jornada_id) REFERENCES jornadas(id) ON DELETE CASCADE
);

-- 3. Si la tabla existe pero no tiene las columnas necesarias, añadirlas
-- Verificar si existe la columna combo_id
SELECT COUNT(*) as columna_combo_id_existe 
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'historial_jornadas' 
AND column_name = 'combo_id';

-- Si no existe, añadirla
SET @combo_id_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'historial_jornadas' AND column_name = 'combo_id');
SET @sql = IF(@combo_id_exists = 0, 'ALTER TABLE historial_jornadas ADD COLUMN combo_id BIGINT', 'SELECT "Columna combo_id ya existe" as mensaje');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificar si existe la columna cuestionario_id
SELECT COUNT(*) as columna_cuestionario_id_existe 
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'historial_jornadas' 
AND column_name = 'cuestionario_id';

-- Si no existe, añadirla
SET @cuestionario_id_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'historial_jornadas' AND column_name = 'cuestionario_id');
SET @sql = IF(@cuestionario_id_exists = 0, 'ALTER TABLE historial_jornadas ADD COLUMN cuestionario_id BIGINT', 'SELECT "Columna cuestionario_id ya existe" as mensaje');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Verificar la estructura final
DESCRIBE historial_jornadas;
