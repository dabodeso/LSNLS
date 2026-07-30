-- Asegura que el ENUM de jornadas.estado incluye en_grabacion
-- (necesario si una BD antigua no tiene ese valor y falla al guardar desde el front).
--
-- Comprobar primero:
--   SHOW COLUMNS FROM jornadas LIKE 'estado';
--
-- Ejecutar solo si falta 'en_grabacion' en Type:

ALTER TABLE jornadas
  MODIFY estado ENUM('preparacion', 'lista', 'en_grabacion', 'completada', 'archivada')
  NOT NULL DEFAULT 'preparacion';
