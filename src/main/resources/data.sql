-- Configuración de codificación UTF-8
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- Insertar datos iniciales de usuarios
INSERT INTO usuarios (nombre, password, rol) VALUES 
('admin', 'admin', 'ROLE_ADMIN'),
('consulta', 'consulta', 'ROLE_CONSULTA'),
('guion', 'guion', 'ROLE_GUION'),
('verificacion', 'verificacion', 'ROLE_VERIFICACION'),
('direccion', 'direccion', 'ROLE_DIRECCION');

-- Insertar 30 preguntas (todas aprobadas, fuentes Wikipedia)
INSERT INTO preguntas (tematica, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, fecha_creacion) VALUES
('Historia', '¿En qué año llegó el hombre a la Luna?', '1969', '_1LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Apolo_11', '2023-01-01'),
('Ciencia', '¿Cuál es el planeta más grande del sistema solar?', 'Júpiter', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/J%C3%BApiter', '2023-01-02'),
('Geografía', '¿Cuál es el río más largo del mundo?', 'Nilo', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Nilo', '2023-01-03'),
('Deporte', '¿En qué país se originó el fútbol?', 'Inglaterra', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/F%C3%BAtbol', '2023-01-04'),
('Arte', '¿Quién pintó la Mona Lisa?', 'Leonardo da Vinci', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Leonardo_da_Vinci', '2023-01-05'),
('Literatura', '¿Quién escribió Don Quijote?', 'Miguel de Cervantes', '_2NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Don_Quijote_de_la_Mancha', '2023-01-06'),
('Cine', '¿Quién dirigió Titanic?', 'James Cameron', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Titanic_(pel%C3%ADcula_de_1997)', '2023-01-07'),
('Música', '¿Quién es el rey del pop?', 'Michael Jackson', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Michael_Jackson', '2023-01-08'),
('Tecnología', '¿Qué significa HTML?', 'HyperText Markup Language', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/HTML', '2023-01-09'),
('Cocina', '¿De qué país es originaria la pizza?', 'Italia', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Pizza', '2023-01-10'),
('Historia', '¿Quién fue el primer presidente de EE.UU.?', 'George Washington', '_3LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/George_Washington', '2023-01-11'),
('Ciencia', '¿Cuál es la fórmula del agua?', 'H2O', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Agua', '2023-01-12'),
('Geografía', '¿En qué continente está Egipto?', 'África', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Egipto', '2023-01-13'),
('Deporte', '¿Cuántos jugadores tiene un equipo de baloncesto?', '5', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Baloncesto', '2023-01-14'),
('Arte', '¿Qué estilo artístico representa Picasso?', 'Cubismo', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Pablo_Picasso', '2023-01-15'),
('Literatura', '¿Quién escribió Cien años de soledad?', 'Gabriel García Márquez', '_4NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Cien_a%C3%B1os_de_soledad', '2023-01-16'),
('Cine', '¿En qué año se estrenó Star Wars?', '1977', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Star_Wars', '2023-01-17'),
('Música', '¿Quién compuso la Novena Sinfonía?', 'Beethoven', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Sinfon%C3%ADa_n%C2%BA_9_(Beethoven)', '2023-01-18'),
('Tecnología', '¿Quién inventó el teléfono?', 'Alexander Graham Bell', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Tel%C3%A9fono', '2023-01-19'),
('Cocina', '¿Qué es el sushi?', 'Comida japonesa', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Sushi', '2023-01-20'),
('Historia', '¿En qué año cayó el Imperio Romano de Occidente?', '476', '_5LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Imperio_romano_de_Occidente', '2023-01-21'),
('Ciencia', '¿Qué partícula tiene carga negativa?', 'Electrón', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Electr%C3%B3n', '2023-01-22'),
('Geografía', '¿Cuál es la capital de Mongolia?', 'Ulán Bator', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Ul%C3%A1n_Bator', '2023-01-23'),
('Deporte', '¿Cuántos Grand Slam tiene Nadal?', '22', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Rafael_Nadal', '2023-01-24'),
('Arte', '¿Qué pintor es famoso por sus girasoles?', 'Van Gogh', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Vincent_van_Gogh', '2023-01-25'),
('Literatura', '¿Quién escribió La Odisea?', 'Homero', '_5NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/La_Odisea', '2023-01-26'),
('Cine', '¿Quién interpretó a Forrest Gump?', 'Tom Hanks', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Forrest_Gump', '2023-01-27'),
('Música', '¿Qué banda compuso Bohemian Rhapsody?', 'Queen', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Queen', '2023-01-28'),
('Tecnología', '¿Qué es un algoritmo?', 'Conjunto de instrucciones', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Algoritmo', '2023-01-29'),
('Cocina', '¿Qué es el gazpacho?', 'Sopa fría', '_5NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Gazpacho', '2023-01-30');

-- Insertar cuestionarios de ejemplo con temáticas y notas
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion) VALUES
(1, '2023-02-01', 'creado', 'NORMAL', NULL, 'Cuestionario genérico de ejemplo'),
(1, '2023-02-02', 'borrador', 'NORMAL', 'CUESTIONARIO MUSICAL', 'Especial música - revisar preguntas de pop'),
(1, '2023-02-03', 'creado', 'NORMAL', 'NAVIDAD', 'Temática navideña para programa especial de diciembre');

-- Insertar 1 combo con 3 preguntas nivel 5
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel) VALUES
(1, '2023-02-02', 'creado', 'NORMAL');

-- Relacionar el cuestionario con 4 preguntas (niveles 1-4)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
(1, 1, 1),  -- Pregunta nivel 1LS
(1, 6, 2),  -- Pregunta nivel 2NLS  
(1, 11, 3), -- Pregunta nivel 3LS
(1, 16, 4); -- Pregunta nivel 4NLS

-- Relacionar el combo con 3 preguntas nivel 5
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
(1, 21, 5), -- Pregunta nivel 5LS
(1, 26, 5), -- Pregunta nivel 5NLS
(1, 25, 5); -- Pregunta nivel 5LS

-- Insertar 1 programa de ejemplo
INSERT INTO programas (temporada, fecha_emision, estado, total_concursantes, creditos_especiales) VALUES
(1, '2023-03-01', 'emitido', 3, 'Programa piloto de la primera temporada');

-- Insertar 3 concursantes de ejemplo
INSERT INTO concursantes (numero_concursante, jornada, dia_grabacion, lugar, nombre, edad, ocupacion, cuestionario_id, combo_id, factor_x, resultado, estado, numero_programa, orden_escaleta, premio) VALUES
(1, 'Mañana', '2023-02-28', 'Madrid', 'María González', 28, 'Profesora', 1, NULL, 'No', 'Ganó 15000€', 'grabado', 1, 1, 15000.00),
(2, 'Mañana', '2023-02-28', 'Madrid', 'Carlos Ruiz', 35, 'Ingeniero', NULL, 1, 'Sí', 'Perdió en combo', 'grabado', 1, 2, 0.00),
(3, 'Tarde', '2023-02-28', 'Madrid', 'Ana López', 42, 'Médica', 1, NULL, 'No', 'Ganó 8000€', 'grabado', 1, 3, 8000.00);

-- Insertar configuración global
INSERT INTO configuracion_global (clave, valor, descripcion) VALUES
('programa_nombre', 'La Silla en la Nave de las Letras', 'Nombre del programa de televisión'),
('temporada_actual', '1', 'Temporada actual en emisión'),
('ultimo_numero_programa', '1', 'Último número de programa emitido'),
('upload_max_size', '10MB', 'Tamaño máximo de archivo para subida'),
('jwt_expiration', '86400000', 'Tiempo de expiración del token JWT en milisegundos'),
('backup_frequency', 'daily', 'Frecuencia de backup de la base de datos');

-- Insertar jornadas de ejemplo
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas) VALUES
('Jornada de Mañana - Enero 2024', '2024-01-15', 'Madrid - Estudio A', 'completada', 1, '2024-01-10 09:00:00', 'Primera jornada de grabación de la temporada'),
('Jornada de Tarde - Enero 2024', '2024-01-15', 'Madrid - Estudio A', 'lista', 1, '2024-01-10 10:30:00', 'Segunda jornada del día'),
('Jornada Especial - San Valentín', '2024-02-14', 'Madrid - Estudio B', 'preparacion', 1, '2024-01-20 14:00:00', 'Programa especial temático de San Valentín');

-- Relacionar jornadas con cuestionarios (primera jornada)
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id) VALUES
(1, 1),
(1, 2),
(1, 3);

-- Relacionar jornadas con combos (primera jornada)
INSERT INTO jornadas_combos (jornada_id, combo_id) VALUES
(1, 1);

-- Relacionar segunda jornada con algunos elementos
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id) VALUES
(2, 2),
(2, 3);

INSERT INTO jornadas_combos (jornada_id, combo_id) VALUES
(2, 1); 