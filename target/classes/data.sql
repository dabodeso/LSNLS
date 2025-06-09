-- Configurar UTF-8 para esta sesión
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- Limpiar datos existentes
DROP TABLE IF EXISTS cuestionarios_preguntas;
DROP TABLE IF EXISTS preguntas_subtemas;
DROP TABLE IF EXISTS concursantes;
DROP TABLE IF EXISTS cuestionarios;
DROP TABLE IF EXISTS preguntas;
DROP TABLE IF EXISTS subtemas;
DROP TABLE IF EXISTS programas;
DROP TABLE IF EXISTS usuarios;

-- Crear tabla de usuarios
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

-- Crear tabla de preguntas
CREATE TABLE preguntas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT,
    fecha_creacion datetime(6),
    fecha_verificacion datetime(6),
    verificacion_usuario_id BIGINT,
    respuesta VARCHAR(50) NOT NULL,
    tematica VARCHAR(100) NOT NULL,
    pregunta VARCHAR(150) NOT NULL,
    subtema VARCHAR(100),
    datos_extra VARCHAR(255),
    fuentes VARCHAR(255),
    notas TEXT,
    notas_verificacion TEXT,
    notas_direccion TEXT,
    estado ENUM('borrador', 'creada', 'verificada', 'rechazada', 'aprobada') NOT NULL,
    estado_disponibilidad ENUM('disponible', 'usada', 'liberada', 'descartada'),
    factor ENUM('X', 'X2', 'X3'),
    nivel ENUM('_1LS', '_2NLS', '_3LS', '_4NLS', '_5LS', '_5NLS') NOT NULL
);

-- Crear tabla de cuestionarios
CREATE TABLE cuestionarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    estado ENUM('borrador', 'creado', 'adjudicado', 'grabado') NOT NULL,
    nivel ENUM('_1LS', '_2NLS', '_3LS', '_4NLS', 'PM1', 'PM2', 'PM3', 'NORMAL') NOT NULL
);

-- Crear tabla de relación cuestionarios-preguntas
CREATE TABLE cuestionarios_preguntas (
    cuestionario_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    factor_multiplicacion INTEGER,
    PRIMARY KEY (cuestionario_id, pregunta_id)
);

-- Crear tabla de programas
CREATE TABLE programas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_emision DATE,
    duracion_acumulada TIME(6),
    resultado_acumulado DECIMAL(10,2),
    dato_audiencia_share DECIMAL(5,2),
    dato_audiencia_target DECIMAL(5,2),
    estado ENUM('borrador', 'programado', 'emitido') NOT NULL
);

-- Crear tabla de concursantes
CREATE TABLE concursantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad INTEGER,
    datos_interes TEXT,
    cuestionario_id BIGINT,
    fecha DATE,
    lugar VARCHAR(255),
    guionista VARCHAR(255),
    resultado VARCHAR(255),
    notas_grabacion TEXT,
    editor VARCHAR(255),
    notas_edicion TEXT,
    duracion INTEGER,
    programa_id BIGINT,
    orden_programa INTEGER,
    estado ENUM('BORRADOR', 'GRABADO', 'EDITADO', 'PROGRAMADO') NOT NULL,
    imagen MEDIUMTEXT,
    CONSTRAINT fk_programa FOREIGN KEY (programa_id) REFERENCES programas(id),
    CONSTRAINT fk_cuestionario FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios(id)
);

-- Añadir claves foráneas
ALTER TABLE preguntas
    ADD CONSTRAINT FK1c30dnbgrcbcia67aeeupir4v
    FOREIGN KEY (creacion_usuario_id) REFERENCES usuarios (id);

ALTER TABLE preguntas
    ADD CONSTRAINT FKqxd7f8ssnownfdc02lg2vjn9m
    FOREIGN KEY (verificacion_usuario_id) REFERENCES usuarios (id);

ALTER TABLE cuestionarios
    ADD CONSTRAINT FK4x1k648y3mm5aamds4j7edjui
    FOREIGN KEY (creacion_usuario_id) REFERENCES usuarios (id);

ALTER TABLE cuestionarios_preguntas
    ADD CONSTRAINT FKth10ov6gek3qugxd2n14oaeuu
    FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios (id);

ALTER TABLE cuestionarios_preguntas
    ADD CONSTRAINT FKdraip2y9m64wkhhspa0twca35
    FOREIGN KEY (pregunta_id) REFERENCES preguntas (id);

-- Insertar datos iniciales
INSERT INTO usuarios (nombre, password, rol) VALUES 
('admin', 'admin', 'ROLE_ADMIN'),
('consulta', 'consulta', 'ROLE_CONSULTA'),
('guion', 'guion', 'ROLE_GUION'),
('verificacion', 'verificacion', 'ROLE_VERIFICACION'),
('direccion', 'direccion', 'ROLE_DIRECCION');

-- Insertar 50 preguntas variadas
INSERT INTO preguntas (creacion_usuario_id, nivel, tematica, pregunta, respuesta, subtema, datos_extra, fuentes, notas, notas_verificacion, notas_direccion, estado, estado_disponibilidad, fecha_creacion, verificacion_usuario_id, fecha_verificacion) VALUES
(1, '_1LS', 'GEOGRAFÍA', '¿Cuál es la capital de Francia?', 'PARÍS', 'Europa', 'Capitales', 'Atlas Mundial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'HISTORIA', '¿En qué año comenzó la Segunda Guerra Mundial?', '1939', 'Guerras Mundiales', 'Historia Universal', 'Enciclopedia', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'CIENCIA', '¿Cuál es el elemento químico con símbolo O?', 'OXÍGENO', 'Química', 'Elementos', 'Tabla Periódica', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'ARTE', '¿Quién pintó la Capilla Sixtina?', 'MIGUEL ÁNGEL', 'Renacimiento', 'Pintura', 'Historia del Arte', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'DEPORTE', '¿Cuántos jugadores tiene un equipo de fútbol?', '11', 'Fútbol', 'Deportes de equipo', 'Reglamento FIFA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5NLS', 'LITERATURA', '¿Quién escribió "Cien años de soledad"?', 'GABRIEL GARCÍA MÁRQUEZ', 'Realismo mágico', 'Novela', 'Premio Nobel', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'GEOGRAFÍA', '¿Cuál es el país más grande del mundo?', 'RUSIA', 'Países', 'Superficie', 'Atlas Mundial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'HISTORIA', '¿En qué año cayó el Muro de Berlín?', '1989', 'Historia Contemporánea', 'Europa', 'Enciclopedia', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'CIENCIA', '¿Cuál es el planeta más cercano al Sol?', 'MERCURIO', 'Sistema Solar', 'Astronomía', 'NASA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'ARTE', '¿Quién pintó "La noche estrellada"?', 'VAN GOGH', 'Pintura', 'Postimpresionismo', 'Museo MoMA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'DEPORTE', '¿En qué deporte se utiliza un "birdie"?', 'BÁDMINTON', 'Deportes de raqueta', 'Equipamiento', 'Reglamento oficial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'LITERATURA', '¿Quién escribió "Don Quijote"?', 'MIGUEL DE CERVANTES', 'Novela', 'Literatura española', 'Biblioteca Nacional', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'GEOGRAFÍA', '¿Cuál es el desierto más grande del mundo?', 'SAHARA', 'Desiertos', 'África', 'Atlas Mundial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'CIENCIA', '¿Cuál es el hueso más largo del cuerpo humano?', 'FÉMUR', 'Anatomía', 'Sistema óseo', 'Anatomía Gray', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'HISTORIA', '¿En qué año comenzó la Primera Guerra Mundial?', '1914', 'Guerras Mundiales', 'Historia Moderna', 'Enciclopedia', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'ARTE', '¿Quién esculpió "El Pensador"?', 'RODIN', 'Escultura', 'Arte moderno', 'Museo Rodin', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'DEPORTE', '¿Cuántos jugadores tiene un equipo de voleibol?', '6', 'Voleibol', 'Deportes de equipo', 'FIVB', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'CIENCIA', '¿Cuál es el elemento químico con símbolo Fe?', 'HIERRO', 'Química', 'Elementos', 'Tabla Periódica', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'GEOGRAFÍA', '¿Cuál es la montaña más alta del mundo?', 'EVEREST', 'Montañas', 'Asia', 'National Geographic', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'LITERATURA', '¿Quién escribió "Romeo y Julieta"?', 'SHAKESPEARE', 'Teatro', 'Literatura inglesa', 'Biblioteca Británica', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'HISTORIA', '¿En qué año se descubrió América?', '1492', 'Descubrimientos', 'Historia Moderna', 'Archivos históricos', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'ARTE', '¿Quién pintó "El grito"?', 'MUNCH', 'Pintura', 'Expresionismo', 'Museo Nacional', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'DEPORTE', '¿En qué deporte se usa un "green"?', 'GOLF', 'Deportes de precisión', 'Terminología', 'PGA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'CIENCIA', '¿Cuál es el planeta más caliente del sistema solar?', 'VENUS', 'Planetas', 'Astronomía', 'NASA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'GEOGRAFÍA', '¿Cuál es el océano más grande del mundo?', 'PACÍFICO', 'Océanos', 'Geografía física', 'Atlas Mundial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'LITERATURA', '¿Quién escribió "El Principito"?', 'SAINT-EXUPÉRY', 'Novela', 'Literatura francesa', 'Biblioteca Nacional', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'HISTORIA', '¿En qué año terminó la Segunda Guerra Mundial?', '1945', 'Guerras Mundiales', 'Historia Moderna', 'Archivos históricos', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'ARTE', '¿Quién pintó "Las Meninas"?', 'VELÁZQUEZ', 'Pintura', 'Barroco', 'Museo del Prado', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'DEPORTE', '¿Cuántos jugadores tiene un equipo de baloncesto?', '5', 'Baloncesto', 'Deportes de equipo', 'FIBA', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'CIENCIA', '¿Cuál es el elemento químico con símbolo Au?', 'ORO', 'Química', 'Elementos', 'Tabla Periódica', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'GEOGRAFÍA', '¿Cuál es el país más pequeño del mundo?', 'VATICANO', 'Países', 'Europa', 'Atlas Mundial', 'Pregunta aprobada', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'CIENCIA', '¿Cuál es el planeta más grande del sistema solar?', 'JÚPITER', NULL, 'Planetas', 'Astronomía', 'Verificada. Incluir dato sobre su tamaño en próxima revisión', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'CIENCIA', '¿En qué año llegó el hombre a la Luna?', '1969', NULL, 'Exploración espacial', 'NASA', 'Verificada. Considerar agregar misión Apollo 11', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'CIENCIA', '¿Cuál es el elemento químico más abundante en el universo?', 'HIDRÓGENO', NULL, 'Elementos químicos', 'Química', 'Verificada. Dato científico confirmado', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'CIENCIA', '¿Cuántos huesos tiene el cuerpo humano?', '206', NULL, 'Anatomía', 'Medicina', 'Verificada. Mencionar que es en adultos', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'DEPORTE', '¿Quién es conocido como "El Pibe de Oro"?', 'MARADONA', NULL, 'Fútbol argentino', 'Historia del Fútbol', 'Verificada. Incluir referencia al Mundial 86', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'CIENCIA', '¿Quién formuló la teoría de la relatividad?', 'EINSTEIN', NULL, 'Física teórica', 'Historia de la Física', 'Verificada. Especificar E=mc²', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5LS', 'HISTORIA', '¿Quién pintó la Mona Lisa?', 'LEONARDO DA VINCI', NULL, 'Arte renacentista', 'Historia del Arte', 'Verificada. Mencionar que está en el Louvre', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5NLS', 'DEPORTE', '¿En qué año se fundó la FIFA?', '1904', NULL, 'Organizaciones deportivas', 'Historia del Fútbol', 'Verificada. Agregar sede en Zurich', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5NLS', 'CIENCIA', '¿Cuál es el número atómico del oro?', '79', NULL, 'Tabla periódica', 'Química', 'Verificada. Incluir símbolo Au', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_5NLS', 'HISTORIA', '¿En qué año comenzó la Primera Guerra Mundial?', '1914', NULL, 'Guerras mundiales', 'Historia Moderna', 'Verificada. Mencionar atentado de Sarajevo', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_1LS', 'DEPORTES', '¿Quién ganó el mundial de 2022?', 'ARGENTINA', NULL, 'Selección de fútbol', 'Historia del Fútbol', 'Pregunta verificada y aprobada sin observaciones', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_2NLS', 'DEPORTES', '¿En qué año se jugó el primer mundial?', '1930', NULL, 'Selección de fútbol', 'Historia del Fútbol', 'Verificada. Considerar agregar más contexto histórico', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_3LS', 'DEPORTES', '¿Quién es el máximo goleador de la historia?', 'CRISTIANO RONALDO', NULL, 'Selección de fútbol', 'Historia del Fútbol', 'Verificada. Actualizar periódicamente este dato', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
(1, '_4NLS', 'DEPORTES', '¿Cuántos mundiales tiene Brasil?', 'CINCO', NULL, 'Selección de fútbol', 'Historia del Fútbol', 'Verificada y aprobada. Dato histórico estable', NULL, NULL, 'aprobada', 'disponible', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP);

-- Insertar cuestionarios
INSERT INTO cuestionarios (creacion_usuario_id, estado, nivel, fecha_creacion) VALUES
(1, 'creado', 'NORMAL', CURRENT_TIMESTAMP),
(1, 'creado', 'NORMAL', CURRENT_TIMESTAMP);

-- Relacionar cuestionarios con preguntas
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
-- Cuestionario 1 (Deportes)
(1, 1, 1),
(1, 2, 1),
(1, 3, 1),
(1, 4, 1),

-- Cuestionario 2 (Ciencia)
(2, 5, 1),
(2, 6, 1),
(2, 7, 1),
(2, 8, 1);

-- Insertar programas
INSERT INTO programas (id, fecha_emision, duracion_acumulada, resultado_acumulado, dato_audiencia_share, dato_audiencia_target, estado) VALUES
(1, '2023-04-01', '01:00:00', 10.0, 0.5, 0.5, 'emitido'),
(2, '2023-04-02', '00:45:00', 8.5, 0.4, 0.4, 'emitido');

-- Insertar concursantes (nueva estructura)
INSERT INTO concursantes (
    nombre, edad, datos_interes, cuestionario_id, fecha, lugar, guionista, resultado, notas_grabacion, editor, notas_edicion, duracion, programa_id, orden_programa, estado, imagen
) VALUES
('Concursante 1', 30, 'Intereses del concursante 1', 1, '2023-04-01', 'Lugar 1', 'Guionista 1', '8.5', 'Notas de grabación 1', 'Editor 1', 'Notas de edición 1', 30, 1, 1, 'GRABADO', 'Imagen 1'),
('Concursante 2', 25, 'Intereses del concursante 2', 2, '2023-04-02', 'Lugar 2', 'Guionista 2', '7.8', 'Notas de grabación 2', 'Editor 2', 'Notas de edición 2', 25, 2, 1, 'GRABADO', 'Imagen 2'),
('Concursante 3', 28, 'Intereses del concursante 3', 1, '2023-04-01', 'Lugar 3', 'Guionista 3', '9.2', 'Notas de grabación 3', 'Editor 3', 'Notas de edición 3', 32, 1, 2, 'GRABADO', 'Imagen 3'),
('Concursante 4', 22, 'Intereses del concursante 4', 2, '2023-04-02', 'Lugar 4', 'Guionista 4', '6.7', 'Notas de grabación 4', 'Editor 4', 'Notas de edición 4', 27, 2, 2, 'GRABADO', 'Imagen 4'),
('Concursante 5', 35, 'Intereses del concursante 5', NULL, '2023-04-01', 'Lugar 5', 'Guionista 5', '8.9', 'Notas de grabación 5', 'Editor 5', 'Notas de edición 5', 29, 1, 3, 'GRABADO', 'Imagen 5'); 