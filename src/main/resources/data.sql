-- Configuración de codificación UTF-8
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- Insertar datos iniciales de usuarios
INSERT INTO usuarios (nombre, password, rol, version) VALUES 
('admin', 'admin', 'ROLE_ADMIN', 0),
('consulta', 'consulta', 'ROLE_CONSULTA', 0),
('guion', 'guion', 'ROLE_GUION', 0),
('verificacion', 'verificacion', 'ROLE_VERIFICACION', 0),
('direccion', 'direccion', 'ROLE_DIRECCION', 0);

-- Insertar 60 preguntas (todas aprobadas, fuentes Wikipedia, autor admin)
INSERT INTO preguntas (tematica, subtema, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, autor, fecha_creacion, version) VALUES
-- Preguntas originales (1-30) con autor admin
('Historia', NULL, '¿En qué año llegó el hombre a la Luna?', '1969', '_1LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Apolo_11', 'admin', '2023-01-01', 0),
('Ciencia', NULL, '¿Cuál es el planeta más grande del sistema solar?', 'Júpiter', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/J%C3%BApiter', 'admin', '2023-01-02', 0),
('Geografía', NULL, '¿Cuál es el río más largo del mundo?', 'Nilo', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Nilo', 'admin', '2023-01-03', 0),
('Deporte', NULL, '¿En qué país se originó el fútbol?', 'Inglaterra', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/F%C3%BAtbol', 'admin', '2023-01-04', 0),
('Arte', NULL, '¿Quién pintó la Mona Lisa?', 'Leonardo da Vinci', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Leonardo_da_Vinci', 'admin', '2023-01-05', 0),
('Literatura', NULL, '¿Quién escribió Don Quijote?', 'Miguel de Cervantes', '_2NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Don_Quijote_de_la_Mancha', 'admin', '2023-01-06', 0),
('Cine', NULL, '¿Quién dirigió Titanic?', 'James Cameron', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Titanic_(pel%C3%ADcula_de_1997)', 'admin', '2023-01-07', 0),
('Música', NULL, '¿Quién es el rey del pop?', 'Michael Jackson', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Michael_Jackson', 'admin', '2023-01-08', 0),
('Tecnología', NULL, '¿Qué significa HTML?', 'HyperText Markup Language', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/HTML', 'admin', '2023-01-09', 0),
('Cocina', NULL, '¿De qué país es originaria la pizza?', 'Italia', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Pizza', 'admin', '2023-01-10', 0),
('Historia', NULL, '¿Quién fue el primer presidente de EE.UU.?', 'George Washington', '_3LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/George_Washington', 'admin', '2023-01-11', 0),
('Ciencia', NULL, '¿Cuál es la fórmula del agua?', 'H2O', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Agua', 'admin', '2023-01-12', 0),
('Geografía', NULL, '¿En qué continente está Egipto?', 'África', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Egipto', 'admin', '2023-01-13', 0),
('Deporte', NULL, '¿Cuántos jugadores tiene un equipo de baloncesto?', '5', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Baloncesto', 'admin', '2023-01-14', 0),
('Arte', NULL, '¿Qué estilo artístico representa Picasso?', 'Cubismo', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Pablo_Picasso', 'admin', '2023-01-15', 0),
('Literatura', NULL, '¿Quién escribió Cien años de soledad?', 'Gabriel García Márquez', '_4NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Cien_a%C3%B1os_de_soledad', 'admin', '2023-01-16', 0),
('Cine', NULL, '¿En qué año se estrenó Star Wars?', '1977', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Star_Wars', 'admin', '2023-01-17', 0),
('Música', NULL, '¿Quién compuso la Novena Sinfonía?', 'Beethoven', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Sinfon%C3%ADa_n%C2%BA_9_(Beethoven)', 'admin', '2023-01-18', 0),
('Tecnología', NULL, '¿Quién inventó el teléfono?', 'Alexander Graham Bell', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Tel%C3%A9fono', 'admin', '2023-01-19', 0),
('Cocina', NULL, '¿Qué es el sushi?', 'Comida japonesa', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Sushi', 'admin', '2023-01-20', 0),
('Historia', NULL, '¿En qué año cayó el Imperio Romano de Occidente?', '476', '_5LS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Imperio_romano_de_Occidente', 'admin', '2023-01-21', 0),
('Ciencia', NULL, '¿Qué partícula tiene carga negativa?', 'Electrón', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Electr%C3%B3n', 'admin', '2023-01-22', 0),
('Geografía', NULL, '¿Cuál es la capital de Mongolia?', 'Ulán Bator', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Ul%C3%A1n_Bator', 'admin', '2023-01-23', 0),
('Deporte', NULL, '¿Cuántos Grand Slam tiene Nadal?', '22', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Rafael_Nadal', 'admin', '2023-01-24', 0),
('Arte', NULL, '¿Qué pintor es famoso por sus girasoles?', 'Van Gogh', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Vincent_van_Gogh', 'admin', '2023-01-25', 0),
('Literatura', NULL, '¿Quién escribió La Odisea?', 'Homero', '_5NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/La_Odisea', 'admin', '2023-01-26', 0),
('Cine', NULL, '¿Quién interpretó a Forrest Gump?', 'Tom Hanks', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Forrest_Gump', 'admin', '2023-01-27', 0),
('Música', NULL, '¿Qué banda compuso Bohemian Rhapsody?', 'Queen', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Queen', 'admin', '2023-01-28', 0),
('Tecnología', NULL, '¿Qué es un algoritmo?', 'Conjunto de instrucciones', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Algoritmo', 'admin', '2023-01-29', 0),
('Cocina', NULL, '¿Qué es el gazpacho?', 'Sopa fría', '_5NLS', 'aprobada', 'usada', 'https://es.wikipedia.org/wiki/Gazpacho', 'admin', '2023-01-30', 0),

-- Nuevas 30 preguntas (31-60) con variedad en niveles, temas y subtemas
('Historia', 'Antigua Roma', '¿Quién fue el primer emperador de Roma?', 'Augusto', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Augusto', 'admin', '2023-02-01', 0),
('Ciencia', 'Biología', '¿Cuántos huesos tiene el cuerpo humano adulto?', '206', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Hueso', 'admin', '2023-02-02', 0),
('Geografía', 'Europa', '¿Cuál es la capital de Portugal?', 'Lisboa', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Lisboa', 'admin', '2023-02-03', 0),
('Deporte', 'Tenis', '¿Cuántos puntos tiene un set de tenis?', '6', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Tenis', 'admin', '2023-02-04', 0),
('Arte', 'Renacimiento', '¿En qué ciudad nació Miguel Ángel?', 'Florencia', '_1LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Miguel_%C3%81ngel', 'admin', '2023-02-05', 0),
('Literatura', 'Poesía', '¿Quién escribió El Quijote?', 'Miguel de Cervantes', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Don_Quijote_de_la_Mancha', 'admin', '2023-02-06', 0),
('Cine', 'Acción', '¿Quién interpretó a Terminator?', 'Arnold Schwarzenegger', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Terminator', 'admin', '2023-02-07', 0),
('Música', 'Rock', '¿Qué banda es conocida como Los Beatles?', 'The Beatles', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/The_Beatles', 'admin', '2023-02-08', 0),
('Tecnología', 'Internet', '¿Qué significa WWW?', 'World Wide Web', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/World_Wide_Web', 'admin', '2023-02-09', 0),
('Cocina', 'Postres', '¿De qué país es originario el tiramisú?', 'Italia', '_2NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Tiramis%C3%BA', 'admin', '2023-02-10', 0),
('Historia', 'Edad Media', '¿En qué año comenzó la Edad Media?', '476', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Edad_Media', 'admin', '2023-02-11', 0),
('Ciencia', 'Química', '¿Cuál es el símbolo químico del oro?', 'Au', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Oro', 'admin', '2023-02-12', 0),
('Geografía', 'América', '¿Cuál es el país más grande de América del Sur?', 'Brasil', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Brasil', 'admin', '2023-02-13', 0),
('Deporte', 'Fútbol', '¿Cuántos jugadores tiene un equipo de fútbol?', '11', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/F%C3%BAtbol', 'admin', '2023-02-14', 0),
('Arte', 'Impresionismo', '¿Quién pintó Impresión, sol naciente?', 'Monet', '_3LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Claude_Monet', 'admin', '2023-02-15', 0),
('Literatura', 'Novela', '¿Quién escribió El Señor de los Anillos?', 'Tolkien', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/J._R._R._Tolkien', 'admin', '2023-02-16', 0),
('Cine', 'Ciencia Ficción', '¿En qué año se estrenó Matrix?', '1999', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/The_Matrix', 'admin', '2023-02-17', 0),
('Música', 'Clásica', '¿Quién compuso La Flauta Mágica?', 'Mozart', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/La_flauta_m%C3%A1gica', 'admin', '2023-02-18', 0),
('Tecnología', 'Computadoras', '¿Quién fundó Microsoft?', 'Bill Gates', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Bill_Gates', 'admin', '2023-02-19', 0),
('Cocina', 'Carnes', '¿Qué es el foie gras?', 'Hígado de pato', '_4NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Foie_gras', 'admin', '2023-02-20', 0),
('Historia', 'Revolución Francesa', '¿En qué año fue la Toma de la Bastilla?', '1789', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Toma_de_la_Bastilla', 'admin', '2023-02-21', 0),
('Ciencia', 'Física', '¿Cuál es la velocidad de la luz?', '300000 km/s', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Velocidad_de_la_luz', 'admin', '2023-02-22', 0),
('Geografía', 'Asia', '¿Cuál es la montaña más alta del mundo?', 'Everest', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Monte_Everest', 'admin', '2023-02-23', 0),
('Deporte', 'Atletismo', '¿Cuántos metros tiene una maratón?', '42195', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Marat%C3%B3n', 'admin', '2023-02-24', 0),
('Arte', 'Surrealismo', '¿Quién pintó La persistencia de la memoria?', 'Dalí', '_5LS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Salvador_Dal%C3%AD', 'admin', '2023-02-25', 0),
('Literatura', 'Teatro', '¿Quién escribió Romeo y Julieta?', 'Shakespeare', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Romeo_y_Julieta', 'admin', '2023-02-26', 0),
('Cine', 'Drama', '¿Quién dirigió El Padrino?', 'Francis Ford Coppola', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/El_Padrino', 'admin', '2023-02-27', 0),
('Música', 'Jazz', '¿Quién es conocido como el Rey del Jazz?', 'Louis Armstrong', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Louis_Armstrong', 'admin', '2023-02-28', 0),
('Tecnología', 'Inteligencia Artificial', '¿Qué significa IA?', 'Inteligencia Artificial', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Inteligencia_artificial', 'admin', '2023-03-01', 0),
('Cocina', 'Especias', '¿De dónde es originario el azafrán?', 'Asia', '_5NLS', 'aprobada', 'disponible', 'https://es.wikipedia.org/wiki/Azafr%C3%A1n', 'admin', '2023-03-02', 0);

-- Insertar cuestionarios de ejemplo con temáticas y notas
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version) VALUES
(1, '2023-02-01', 'adjudicado', 'NORMAL', NULL, 'Cuestionario genérico de ejemplo', 0),
(1, '2023-02-02', 'adjudicado', 'NORMAL', 'CUESTIONARIO MUSICAL', 'Especial música - revisar preguntas de pop', 0),
(1, '2023-02-03', 'adjudicado', 'NORMAL', 'NAVIDAD', 'Temática navideña para programa especial de diciembre', 0);

-- Insertar 1 combo con 3 preguntas nivel 5
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, version) VALUES
(1, '2023-02-02', 'adjudicado', 'NORMAL', 0);

-- Relacionar el cuestionario con 4 preguntas (niveles 1-4)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
(1, 1, 1),  -- Pregunta nivel 1LS
(1, 6, 1),  -- Pregunta nivel 2NLS  
(1, 11, 1), -- Pregunta nivel 3LS
(1, 16, 1); -- Pregunta nivel 4NLS

-- Relacionar el combo con 3 preguntas nivel 5
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
(1, 21, 2), -- Pregunta nivel 5LS para PM1
(1, 26, 3), -- Pregunta nivel 5NLS para PM2
(1, 25, 0); -- Pregunta nivel 5LS para PM3

-- Insertar 1 programa de ejemplo
INSERT INTO programas (temporada, fecha_emision, estado, total_concursantes, creditos_especiales, version) VALUES
(1, '2023-03-01', 'emitido', 3, 'Programa piloto de la primera temporada', 0);

-- Insertar 3 concursantes de ejemplo
INSERT INTO concursantes (numero_concursante, jornada, dia_grabacion, lugar, nombre, edad, ocupacion, cuestionario_id, combo_id, factor_x, resultado, estado, numero_programa, orden_escaleta, premio, version) VALUES
(1, 'Mañana', '2023-02-28', 'Madrid', 'María González', 28, 'Profesora', NULL, NULL, 'No', 'Ganó 15000€', 'grabado', 1, 1, 15000.00, 0),
(2, 'Mañana', '2023-02-28', 'Madrid', 'Carlos Ruiz', 35, 'Ingeniero', NULL, NULL, 'Sí', 'Perdió en combo', 'grabado', 1, 2, 0.00, 0),
(3, 'Tarde', '2023-02-28', 'Madrid', 'Ana López', 42, 'Médica', NULL, NULL, 'No', 'Ganó 8000€', 'grabado', 1, 3, 8000.00, 0);

-- Insertar configuración global
INSERT INTO configuracion_global (clave, valor, descripcion, version) VALUES
('programa_nombre', 'La Silla en la Nave de las Letras', 'Nombre del programa de televisión', 0),
('temporada_actual', '1', 'Temporada actual en emisión', 0),
('ultimo_numero_programa', '1', 'Último número de programa emitido', 0),
('upload_max_size', '10MB', 'Tamaño máximo de archivo para subida', 0),
('jwt_expiration', '86400000', 'Tiempo de expiración del token JWT en milisegundos', 0),
('backup_frequency', 'daily', 'Frecuencia de backup de la base de datos', 0);

-- Insertar jornadas de ejemplo
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas, version) VALUES
('Jornada de Mañana - Enero 2024', '2024-01-15', 'Madrid - Estudio A', 'completada', 1, '2024-01-10 09:00:00', 'Primera jornada de grabación de la temporada', 0),
('Jornada de Tarde - Enero 2024', '2024-01-15', 'Madrid - Estudio A', 'lista', 1, '2024-01-10 10:30:00', 'Segunda jornada del día', 0),
('Jornada Especial - San Valentín', '2024-02-14', 'Madrid - Estudio B', 'preparacion', 1, '2024-01-20 14:00:00', 'Programa especial temático de San Valentín', 0);

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