-- Insertar temáticas básicas
INSERT INTO tematicas (nombre, creacion_usuario_id, fecha_creacion, version) VALUES
('GEOGRAFÍA', 1, NOW(), 0),
('HISTORIA', 1, NOW(), 0),
('DEPORTES', 1, NOW(), 0),
('CIENCIA', 1, NOW(), 0),
('ARTE', 1, NOW(), 0),
('MÚSICA', 1, NOW(), 0),
('CINE', 1, NOW(), 0),
('LITERATURA', 1, NOW(), 0),
('TECNOLOGÍA', 1, NOW(), 0),
('GENERAL', 1, NOW(), 0)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
