-- Configuración de codificación UTF-8
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- Limpieza básica (reinicia AUTO_INCREMENT y respeta FKs)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE jornadas_cuestionarios;
TRUNCATE TABLE jornadas_combos;
TRUNCATE TABLE jornadas;
TRUNCATE TABLE combos_preguntas;
TRUNCATE TABLE cuestionarios_preguntas;
TRUNCATE TABLE combos;
TRUNCATE TABLE cuestionarios;
TRUNCATE TABLE preguntas;
SET FOREIGN_KEY_CHECKS = 1;

-- Usuarios mínimos
DELETE u FROM usuarios u
JOIN usuarios u2 ON u.nombre = u2.nombre AND u.id > u2.id;
INSERT INTO usuarios (nombre, password, rol, version)
SELECT 'admin', 'admin', 'ROLE_ADMIN', 0 WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE nombre='admin');

-- 100 preguntas (niveles alternando: _1LS, _2NLS, _3LS, _4NLS, _5LS, _5NLS)
-- Formato: (tematica, subtema, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, autor, fecha_creacion, version)
INSERT INTO preguntas (tematica, subtema, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, autor, fecha_creacion, version) VALUES
('Civismo', NULL, '¿Quién es el presidente del Gobierno de España en 2025?', 'Pedro Sánchez', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Francia?', 'París', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué gas respiramos principalmente los humanos?', 'Oxígeno', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿En qué continente está Marruecos?', 'África', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿En qué año llegó el hombre a la Luna?', '1969', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Arte', NULL, '¿Quién pintó La Gioconda?', 'Leonardo da Vinci', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Deporte', NULL, '¿Cuántos jugadores tiene un equipo de fútbol en el campo?', '11', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Literatura', NULL, '¿Quién escribió Don Quijote de la Mancha?', 'Miguel de Cervantes', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es el río más largo del mundo según la mayoría de fuentes?', 'Nilo', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Cuál es el planeta más grande del Sistema Solar?', 'Júpiter', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Civismo', NULL, '¿Qué colores tiene la bandera de España?', 'Rojo y amarillo', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Portugal?', 'Lisboa', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿Quién fue el primer presidente de Estados Unidos?', 'George Washington', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Cuál es la fórmula química del agua?', 'H2O', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Tecnología', NULL, '¿Qué significa la sigla CPU?', 'Unidad Central de Procesamiento', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿En qué país se encuentra el Taj Mahal?', 'India', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Música', NULL, '¿Qué banda compuso la canción Bohemian Rhapsody?', 'Queen', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cine', NULL, '¿Quién dirigió la película Titanic?', 'James Cameron', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Arte', NULL, '¿De qué estilo es La noche estrellada de Van Gogh?', 'Postimpresionismo', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Argentina?', 'Buenos Aires', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿En qué año cayó el Imperio Romano de Occidente?', '476', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué partícula tiene carga negativa?', 'Electrón', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es el océano más grande del mundo?', 'Pacífico', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Literatura', NULL, '¿Quién escribió Cien años de soledad?', 'Gabriel García Márquez', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿Qué idioma se habla principalmente en Brasil?', 'Portugués', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Cuál es el metal cuyo símbolo químico es Au?', 'Oro', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Deporte', NULL, '¿En qué deporte se utiliza una red elevada y un balón ligero?', 'Voleibol', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Canadá?', 'Ottawa', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿Qué tratado puso fin a la Primera Guerra Mundial?', 'Tratado de Versalles', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué científico propuso la teoría de la relatividad?', 'Albert Einstein', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿En qué país nació el flamenco?', 'España', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Noruega?', 'Oslo', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Tecnología', NULL, '¿Qué empresa creó el sistema operativo Android?', 'Google', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Cuál es la velocidad de la luz aproximada en km/s?', '300000', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Qué cordillera atraviesa Sudamérica de norte a sur?', 'Los Andes', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿Qué instrumento tiene 88 teclas?', 'Piano', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿En qué año empezó la Segunda Guerra Mundial?', '1939', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué molécula porta la información genética?', 'ADN', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Literatura', NULL, '¿Quién escribió La Odisea?', 'Homero', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Australia?', 'Canberra', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cine', NULL, '¿Qué actor interpretó a Forrest Gump?', 'Tom Hanks', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Música', NULL, '¿De qué país son originarios The Beatles?', 'Reino Unido', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Arte', NULL, '¿Qué pintor es famoso por los girasoles?', 'Vincent van Gogh', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Marruecos?', 'Rabat', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿En qué año comenzó la Edad Media según tradición europea?', '476', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Cuál es el símbolo químico del sodio?', 'Na', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Deporte', NULL, '¿Qué torneo de tenis se juega sobre hierba en Londres?', 'Wimbledon', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Suiza?', 'Berna', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
-- 49..60 (preguntas adicionales variadas)
('Cultura', NULL, '¿Qué ciudad es conocida como la Gran Manzana?', 'Nueva York', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Qué país tiene forma de bota?', 'Italia', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Tecnología', NULL, '¿Qué significa WWW?', 'World Wide Web', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿Qué pieza de ajedrez solo se mueve en diagonal?', 'Alfil', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿Quién fue el Libertador de gran parte de Sudamérica?', 'Simón Bolívar', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué órgano bombea la sangre?', 'Corazón', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿Cuál es el baile típico de Argentina?', 'Tango', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de México?', 'Ciudad de México', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué es un algoritmo?', 'Un conjunto de instrucciones', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Música', NULL, '¿Qué instrumento tocaba Mozart?', 'Piano', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Cultura', NULL, '¿Qué día se celebra la Navidad?', '25 de diciembre', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Egipto?', 'El Cairo', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
-- 61..96 (todas de nivel 5 para COMBOS; alternamos 5LS/5NLS)
('Historia', NULL, '¿En qué año se firmó la Declaración de Independencia de EE. UU.?', '1776', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 61
('Ciencia', NULL, '¿Cuál es la ecuación de la energía de Einstein?', 'E=mc^2', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 62
('Geografía', NULL, '¿Cuál es el punto más alto de la Tierra?', 'Everest', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 63
('Arte', NULL, '¿Qué pintor es autor de Guernica?', 'Pablo Picasso', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 64
('Literatura', NULL, '¿Quién escribió En busca del tiempo perdido?', 'Marcel Proust', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 65
('Ciencia', NULL, '¿Qué partícula compone el núcleo junto al protón?', 'Neutrón', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 66
('Música', NULL, '¿Quién compuso la Novena Sinfonía?', 'Beethoven', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 67
('Cine', NULL, '¿Qué película ganó el Óscar a mejor film en 1994?', 'Forrest Gump', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 68
('Geografía', NULL, '¿Cuál es el desierto más grande del mundo?', 'Sahara', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 69
('Tecnología', NULL, '¿Quién fundó Microsoft?', 'Bill Gates', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 70
('Ciencia', NULL, '¿Quién formuló las leyes del movimiento?', 'Isaac Newton', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 71
('Geografía', NULL, '¿Qué mar baña la costa oriental de España?', 'Mediterráneo', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 72
('Historia', NULL, '¿En qué año empezó la Revolución Francesa?', '1789', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 73
('Cultura', NULL, '¿Qué poeta escribió Veinte poemas de amor y una canción desesperada?', 'Pablo Neruda', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 74
('Arte', NULL, '¿Quién pintó Las Meninas?', 'Diego Velázquez', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 75
('Música', NULL, '¿De qué país es originario el tango?', 'Argentina', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 76
('Ciencia', NULL, '¿Qué científico descubrió la penicilina?', 'Alexander Fleming', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 77
('Geografía', NULL, '¿Cuál es la capital de Sudáfrica (administrativa)?', 'Pretoria', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 78
('Cultura', NULL, '¿Qué escritor creó a Sherlock Holmes?', 'Arthur Conan Doyle', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 79
('Ciencia', NULL, '¿Cómo se llama el proceso de las plantas de producir alimento?', 'Fotosíntesis', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 80
('Geografía', NULL, '¿En qué país está la ciudad de Estambul?', 'Turquía', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 81
('Historia', NULL, '¿Qué muro cayó en 1989?', 'El Muro de Berlín', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 82
('Cultura', NULL, '¿Cuál es el libro sagrado del Islam?', 'El Corán', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 83
('Ciencia', NULL, '¿Qué número atómico tiene el carbono?', '6', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 84
('Geografía', NULL, '¿Cuál es el lago más profundo del mundo?', 'Lago Baikal', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 85
('Arte', NULL, '¿Qué escultor creó El David?', 'Miguel Ángel', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 86
('Música', NULL, '¿Quién es conocido como el Rey del Pop?', 'Michael Jackson', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 87
('Cine', NULL, '¿Quién dirigió Ciudadano Kane?', 'Orson Welles', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 88
('Historia', NULL, '¿Qué emperador romano legalizó el cristianismo?', 'Constantino I', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 89
('Geografía', NULL, '¿Cuál es el país más poblado del mundo?', 'India', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 90
('Ciencia', NULL, '¿Qué escala mide la magnitud de los terremotos?', 'Escala de Richter', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 91
('Cultura', NULL, '¿Quién escribió Hamlet?', 'William Shakespeare', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 92
('Geografía', NULL, '¿Qué río atraviesa París?', 'Sena', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 93
('Arte', NULL, '¿Qué movimiento inició Claude Monet?', 'Impresionismo', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 94
('Historia', NULL, '¿Qué nave completó la primera vuelta al mundo?', 'La Victoria', '_5LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 95
('Ciencia', NULL, '¿Cuál es la unidad básica de la vida?', 'La célula', '_5NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0), -- 96
-- 97..100 (extra)
('Cultura', NULL, '¿Qué escritor argentino escribió El Aleph?', 'Jorge Luis Borges', '_1LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Geografía', NULL, '¿Cuál es la capital de Grecia?', 'Atenas', '_2NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Ciencia', NULL, '¿Qué vitamina se obtiene principalmente del sol?', 'Vitamina D', '_3LS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0),
('Historia', NULL, '¿Qué expedición llegó a América en 1492?', 'La de Cristóbal Colón', '_4NLS', 'aprobada', 'disponible', 'seed', 'admin', '2024-01-01', 0);

-- 12 cuestionarios (NORMAL) y asignación de 4 preguntas cada uno
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version) VALUES
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q1', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q2', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q3', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q4', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q5', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q6', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q7', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q8', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q9', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q10', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q11', '', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK Q12', '', 0);

-- Vincular 4 preguntas por cuestionario (bloques consecutivos)
-- Q1: 1-4, Q2: 5-8, ..., Q12: 45-48 (48 usadas; el resto quedan disponibles)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
((SELECT id FROM cuestionarios WHERE tematica='PACK Q1'), 1, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q1'), 2, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q1'), 3, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q1'), 4, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q2'), 5, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q2'), 6, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q2'), 7, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q2'), 8, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q3'), 9, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q3'), 10, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q3'), 11, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q3'), 12, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q4'), 13, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q4'), 14, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q4'), 15, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q4'), 16, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q5'), 17, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q5'), 18, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q5'), 19, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q5'), 20, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q6'), 21, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q6'), 22, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q6'), 23, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q6'), 24, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q7'), 25, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q7'), 26, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q7'), 27, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q7'), 28, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q8'), 29, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q8'), 30, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q8'), 31, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q8'), 32, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q9'), 33, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q9'), 34, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q9'), 35, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q9'), 36, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q10'), 37, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q10'), 38, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q10'), 39, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q10'), 40, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q11'), 41, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q11'), 42, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q11'), 43, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q11'), 44, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q12'), 45, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q12'), 46, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q12'), 47, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q12'), 48, 1);

-- 12 combos (NORMAL) y asignación de 3 preguntas cada uno (usamos 61..96)
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, version) VALUES
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C1', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C2', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C3', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C4', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C5', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C6', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C7', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C8', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C9', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C10', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C11', 0),
(1, '2024-01-01', 'adjudicado', 'NORMAL', 'PACK C12', 0);

INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
((SELECT id FROM combos WHERE tematica='PACK C1'), 61, 2),
((SELECT id FROM combos WHERE tematica='PACK C1'), 62, 3),
((SELECT id FROM combos WHERE tematica='PACK C1'), 63, 0),
((SELECT id FROM combos WHERE tematica='PACK C2'), 64, 2),
((SELECT id FROM combos WHERE tematica='PACK C2'), 65, 3),
((SELECT id FROM combos WHERE tematica='PACK C2'), 66, 0),
((SELECT id FROM combos WHERE tematica='PACK C3'), 67, 2),
((SELECT id FROM combos WHERE tematica='PACK C3'), 68, 3),
((SELECT id FROM combos WHERE tematica='PACK C3'), 69, 0),
((SELECT id FROM combos WHERE tematica='PACK C4'), 70, 2),
((SELECT id FROM combos WHERE tematica='PACK C4'), 71, 3),
((SELECT id FROM combos WHERE tematica='PACK C4'), 72, 0),
((SELECT id FROM combos WHERE tematica='PACK C5'), 73, 2),
((SELECT id FROM combos WHERE tematica='PACK C5'), 74, 3),
((SELECT id FROM combos WHERE tematica='PACK C5'), 75, 0),
((SELECT id FROM combos WHERE tematica='PACK C6'), 76, 2),
((SELECT id FROM combos WHERE tematica='PACK C6'), 77, 3),
((SELECT id FROM combos WHERE tematica='PACK C6'), 78, 0),
((SELECT id FROM combos WHERE tematica='PACK C7'), 79, 2),
((SELECT id FROM combos WHERE tematica='PACK C7'), 80, 3),
((SELECT id FROM combos WHERE tematica='PACK C7'), 81, 0),
((SELECT id FROM combos WHERE tematica='PACK C8'), 82, 2),
((SELECT id FROM combos WHERE tematica='PACK C8'), 83, 3),
((SELECT id FROM combos WHERE tematica='PACK C8'), 84, 0),
((SELECT id FROM combos WHERE tematica='PACK C9'), 85, 2),
((SELECT id FROM combos WHERE tematica='PACK C9'), 86, 3),
((SELECT id FROM combos WHERE tematica='PACK C9'), 87, 0),
((SELECT id FROM combos WHERE tematica='PACK C10'), 88, 2),
((SELECT id FROM combos WHERE tematica='PACK C10'), 89, 3),
((SELECT id FROM combos WHERE tematica='PACK C10'), 90, 0),
((SELECT id FROM combos WHERE tematica='PACK C11'), 91, 2),
((SELECT id FROM combos WHERE tematica='PACK C11'), 92, 3),
((SELECT id FROM combos WHERE tematica='PACK C11'), 93, 0),
((SELECT id FROM combos WHERE tematica='PACK C12'), 94, 2),
((SELECT id FROM combos WHERE tematica='PACK C12'), 95, 3),
((SELECT id FROM combos WHERE tematica='PACK C12'), 96, 0);

-- 2 jornadas y asignación de cuestionarios/combos (6 y 6 cada una)
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion)
VALUES ('Jornada 1', '2025-02-01', 'MADRID', 'preparacion', 1, NOW());

INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion)
VALUES ('Jornada 2', '2025-02-02', 'SEVILLA', 'preparacion', 1, NOW());

-- Cuestionarios
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 1'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM cuestionarios WHERE tematica IN ('PACK Q1','PACK Q2','PACK Q3','PACK Q4','PACK Q5','PACK Q6');
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 2'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM cuestionarios WHERE tematica IN ('PACK Q7','PACK Q8','PACK Q9','PACK Q10','PACK Q11','PACK Q12');

-- Combos
INSERT INTO jornadas_combos (jornada_id, combo_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 1'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM combos WHERE tematica IN ('PACK C1','PACK C2','PACK C3','PACK C4','PACK C5','PACK C6');
INSERT INTO jornadas_combos (jornada_id, combo_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 2'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM combos WHERE tematica IN ('PACK C7','PACK C8','PACK C9','PACK C10','PACK C11','PACK C12');


-- Añadir datos_extra a preguntas ya usadas en cuestionarios (IDs 1..48)
UPDATE preguntas
SET datos_extra = CONCAT(
    'Dato extra (', REPLACE(nivel, '_', ''), '): ',
    COALESCE(tematica, 'General'),
    '. Referencia: ', COALESCE(fuentes, '—'),
    '. Contexto: cultura general.'
)
WHERE id BETWEEN 1 AND 48;

-- Añadir datos_extra a preguntas usadas en combos (IDs 61..96, nivel 5)
UPDATE preguntas
SET datos_extra = CONCAT(
    'Dato extra (', REPLACE(nivel, '_', ''), '): ',
    COALESCE(tematica, 'General'),
    '. Referencia: ', COALESCE(fuentes, '—'),
    '. Uso: combinable con multiplicadores (X2/X3/X).'
)
WHERE id BETWEEN 61 AND 96;

-- =========================================================
-- NUEVOS DATOS: 84 preguntas adicionales (101..184) para 12 cuestionarios (Q13..Q24) y 12 combos (C13..C24)
--  - Cuestionarios: 12 x 4 preguntas (1LS,2NLS,3LS,4NLS) → IDs 101..148
--  - Combos: 12 x 3 preguntas (nivel 5) → IDs 149..184 (alternando 5LS/5NLS)
--  - 2 Jornadas nuevas (Jornada 3 y Jornada 4) con 6 cuestionarios y 6 combos cada una
-- =========================================================

INSERT INTO preguntas (tematica, subtema, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, autor, fecha_creacion, version) VALUES
-- 101..148 (bloques de 4: 1LS,2NLS,3LS,4NLS)
('Historia', NULL, '¿Qué faraón ordenó construir Abu Simbel?', 'Ramsés II', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la capital de Suecia?', 'Estocolmo', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Qué órgano humano filtra la sangre?', 'Riñón', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Arte', NULL, '¿En qué ciudad está el Museo del Prado?', 'Madrid', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Literatura', NULL, '¿Quién escribió El nombre de la rosa?', 'Umberto Eco', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Civismo', NULL, '¿Cómo se llama el parlamento de España?', 'Cortes Generales', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Deporte', NULL, '¿Cuántos aros tiene el símbolo olímpico?', 'Cinco', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿En qué país está la ciudad de Dubrovnik?', 'Croacia', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Ciencia', NULL, '¿Qué científico enunció las leyes de la herencia?', 'Gregor Mendel', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Música', NULL, '¿En qué ciudad nació Mozart?', 'Salzburgo', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es el país más grande de África?', 'Argelia', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Qué lengua se habla en Austria?', 'Alemán', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Historia', NULL, '¿Quién fue Juana de Arco?', 'Heroína francesa', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Qué mide un barómetro?', 'La presión atmosférica', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cine', NULL, '¿Qué director filmó “El laberinto del fauno”?', 'Guillermo del Toro', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Qué país tiene por capital a Varsovia?', 'Polonia', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Cultura', NULL, '¿Qué baile tradicional es típico de Brasil?', 'La samba', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Tecnología', NULL, '¿Qué significa la sigla URL?', 'Localizador Uniforme de Recursos', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Arte', NULL, '¿Quién pintó la Capilla Sixtina?', 'Miguel Ángel', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la capital de Eslovenia?', 'Liubliana', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Literatura', NULL, '¿Quién escribió Crimen y castigo?', 'Fiódor Dostoyevski', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Qué partícula subatómica tiene carga positiva?', 'Protón', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Qué instrumento es de la familia de cuerda?', 'Violín', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Qué país tiene por capital a Helsinki?', 'Finlandia', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Historia', NULL, '¿Quién fue el rey macedonio hijo de Filipo II?', 'Alejandro Magno', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la isla más grande del mundo?', 'Groenlandia', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Qué planeta es conocido como el Planeta Rojo?', 'Marte', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Qué religión tiene como libro sagrado la Biblia?', 'Cristianismo', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Arte', NULL, '¿Qué arquitecto diseñó la Sagrada Familia?', 'Antoni Gaudí', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Música', NULL, '¿Qué compositor creó El lago de los cisnes?', 'Chaikovski', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la capital de Marruecos?', 'Rabat', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Cuál es el metal líquido a temperatura ambiente?', 'Mercurio', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Cine', NULL, '¿Quién dirigió La lista de Schindler?', 'Steven Spielberg', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Civismo', NULL, '¿Qué institución emite el euro?', 'Banco Central Europeo', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Qué país tiene por capital a Copenhague?', 'Dinamarca', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Historia', NULL, '¿Qué guerra terminó con el Tratado de París (1783)?', 'Guerra de Independencia de EE. UU.', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Literatura', NULL, '¿Quién escribió La metamorfosis?', 'Franz Kafka', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la capital de Irlanda?', 'Dublín', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Qué vitamina se asocia al sol?', 'Vitamina D', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Qué baile es típico de España con castañuelas?', 'Flamenco', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Cine', NULL, '¿Quién interpretó a Indiana Jones?', 'Harrison Ford', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Arte', NULL, '¿Qué pintor fue conocido como “El Greco”?', 'Doménikos Theotokópoulos', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es la capital de Hungría?', 'Budapest', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Cuál es la capa más externa del Sol?', 'Corona solar', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Historia', NULL, '¿Qué reina inglesa fue hija de Enrique VIII y Ana Bolena?', 'Isabel I', '_1LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Cuál es la bebida nacional de Argentina?', 'El mate', '_2NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Música', NULL, '¿Qué compositor es autor de La Traviata?', 'Giuseppe Verdi', '_3LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Qué país tiene por capital a Praga?', 'Chequia', '_4NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

-- 149..184 (nivel 5 alternando 5LS/5NLS para combos)
('Ciencia', NULL, '¿Qué científico desarrolló el concepto de gravitación universal?', 'Isaac Newton', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es el río más caudaloso del mundo?', 'Amazonas', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Arte', NULL, '¿Quién esculpió La Piedad?', 'Miguel Ángel', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Cultura', NULL, '¿En qué ciudad se celebra el Oktoberfest?', 'Múnich', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Historia', NULL, '¿Qué tratado puso fin a la Primera Guerra Mundial?', 'Tratado de Versalles', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Cuál es el elemento con símbolo Fe?', 'Hierro', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Geografía', NULL, '¿Qué país es conocido como la tierra del sol naciente?', 'Japón', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cine', NULL, '¿Qué director filmó “Parásitos”?', 'Bong Joon-ho', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Literatura', NULL, '¿Quién escribió Rayuela?', 'Julio Cortázar', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Música', NULL, '¿Cuál es la sinfonía apodada “Destino”?', 'Quinta Sinfonía de Beethoven', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Cómo se llama el proceso de división celular?', 'Mitosis', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Cuál es el país con más volcanes activos?', 'Indonesia', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Cultura', NULL, '¿Qué tejido artístico es típico de Tapices de Bayeux?', 'Bordado', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Historia', NULL, '¿Qué civilización construyó Machu Picchu?', 'Inca', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Arte', NULL, '¿Qué pintor holandés es autor de La ronda de noche?', 'Rembrandt', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Geografía', NULL, '¿Cuál es la capital de Nueva Zelanda?', 'Wellington', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Ciencia', NULL, '¿Cuál es la fórmula química del dióxido de carbono?', 'CO2', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Literatura', NULL, '¿Quién escribió Pedro Páramo?', 'Juan Rulfo', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Música', NULL, '¿Qué compositor es autor de Carmina Burana?', 'Carl Orff', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cultura', NULL, '¿Qué ciudad italiana alberga la Capilla Sixtina?', 'Ciudad del Vaticano', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Geografía', NULL, '¿Qué estrecho separa Europa de África?', 'Estrecho de Gibraltar', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),

('Historia', NULL, '¿Qué reina inglesa fue llamada la “reina virgen”?', 'Isabel I', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),
('Cine', NULL, '¿Qué película popularizó la frase “May the Force be with you”?', 'Star Wars', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0);

-- Completar 172..184 (nivel 5 alternando 5NLS/5LS)
INSERT INTO preguntas (tematica, subtema, pregunta, respuesta, nivel, estado, estado_disponibilidad, fuentes, autor, fecha_creacion, version) VALUES
('Tecnología', NULL, '¿Qué protocolo cifra el tráfico web?', 'HTTPS', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 172
('Geografía', NULL, '¿Cuál es la capital de Croacia?', 'Zagreb', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),         -- 173
('Historia', NULL, '¿En qué año terminó la Segunda Guerra Mundial?', '1945', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 174
('Ciencia', NULL, '¿Qué planeta es famoso por sus anillos visibles?', 'Saturno', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 175
('Literatura', NULL, '¿Quién escribió La Ilíada?', 'Homero', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),          -- 176
('Arte', NULL, '¿Quién pintó El Jardín de las Delicias?', 'El Bosco', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),  -- 177
('Música', NULL, '¿Quién compuso El Mesías?', 'Georg Friedrich Händel', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 178
('Cine', NULL, '¿Quién dirigió El Padrino?', 'Francis Ford Coppola', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),   -- 179
('Tecnología', NULL, '¿Qué lenguaje creó Guido van Rossum?', 'Python', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 180
('Geografía', NULL, '¿Cuál es la capital de Austria?', 'Viena', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0),        -- 181
('Ciencia', NULL, '¿Qué escala mide la acidez de una disolución?', 'pH', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 182
('Historia', NULL, '¿Quién fue el primer emperador romano?', 'Augusto', '_5LS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0), -- 183
('Cultura', NULL, '¿En qué ciudad se encuentra la Ópera de Sídney?', 'Sídney', '_5NLS', 'aprobada', 'disponible', 'seed2', 'admin', '2024-01-02', 0); -- 184

-- Datos extra para nuevas preguntas
UPDATE preguntas
SET datos_extra = CONCAT('Dato extra (', REPLACE(nivel, '_', ''), '): ampliación cultural. Fuente: ', COALESCE(fuentes, '—'), '.')
WHERE id BETWEEN 101 AND 148;

UPDATE preguntas
SET datos_extra = CONCAT('Dato extra (', REPLACE(nivel, '_', ''), '): nivel 5 para combos. Fuente: ', COALESCE(fuentes, '—'), '.')
WHERE id BETWEEN 149 AND 184;

-- 12 nuevos cuestionarios PACK Q13..Q24
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version) VALUES
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q13', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q14', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q15', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q16', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q17', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q18', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q19', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q20', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q21', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q22', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q23', '', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK Q24', '', 0);

-- Vincular preguntas: Q13: 101-104, Q14: 105-108, ..., Q24: 145-148
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
((SELECT id FROM cuestionarios WHERE tematica='PACK Q13'), 101, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q13'), 102, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q13'), 103, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q13'), 104, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q14'), 105, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q14'), 106, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q14'), 107, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q14'), 108, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q15'), 109, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q15'), 110, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q15'), 111, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q15'), 112, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q16'), 113, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q16'), 114, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q16'), 115, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q16'), 116, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q17'), 117, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q17'), 118, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q17'), 119, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q17'), 120, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q18'), 121, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q18'), 122, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q18'), 123, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q18'), 124, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q19'), 125, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q19'), 126, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q19'), 127, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q19'), 128, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q20'), 129, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q20'), 130, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q20'), 131, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q20'), 132, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q21'), 133, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q21'), 134, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q21'), 135, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q21'), 136, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q22'), 137, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q22'), 138, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q22'), 139, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q22'), 140, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q23'), 141, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q23'), 142, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q23'), 143, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q23'), 144, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q24'), 145, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q24'), 146, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q24'), 147, 1),
((SELECT id FROM cuestionarios WHERE tematica='PACK Q24'), 148, 1);

-- 12 nuevos combos PACK C13..C24
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, version) VALUES
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C13', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C14', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C15', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C16', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C17', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C18', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C19', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C20', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C21', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C22', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C23', 0),
(1, '2024-01-02', 'adjudicado', 'NORMAL', 'PACK C24', 0);

-- Vincular preguntas: C13: 149-151, C14: 152-154, ..., C24: 181-183 (usamos 3 por combo, dejamos 184 como reserva)
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
((SELECT id FROM combos WHERE tematica='PACK C13'), 149, 2),
((SELECT id FROM combos WHERE tematica='PACK C13'), 150, 3),
((SELECT id FROM combos WHERE tematica='PACK C13'), 151, 0),
((SELECT id FROM combos WHERE tematica='PACK C14'), 152, 2),
((SELECT id FROM combos WHERE tematica='PACK C14'), 153, 3),
((SELECT id FROM combos WHERE tematica='PACK C14'), 154, 0),
((SELECT id FROM combos WHERE tematica='PACK C15'), 155, 2),
((SELECT id FROM combos WHERE tematica='PACK C15'), 156, 3),
((SELECT id FROM combos WHERE tematica='PACK C15'), 157, 0),
((SELECT id FROM combos WHERE tematica='PACK C16'), 158, 2),
((SELECT id FROM combos WHERE tematica='PACK C16'), 159, 3),
((SELECT id FROM combos WHERE tematica='PACK C16'), 160, 0),
((SELECT id FROM combos WHERE tematica='PACK C17'), 161, 2),
((SELECT id FROM combos WHERE tematica='PACK C17'), 162, 3),
((SELECT id FROM combos WHERE tematica='PACK C17'), 163, 0),
((SELECT id FROM combos WHERE tematica='PACK C18'), 164, 2),
((SELECT id FROM combos WHERE tematica='PACK C18'), 165, 3),
((SELECT id FROM combos WHERE tematica='PACK C18'), 166, 0),
((SELECT id FROM combos WHERE tematica='PACK C19'), 167, 2),
((SELECT id FROM combos WHERE tematica='PACK C19'), 168, 3),
((SELECT id FROM combos WHERE tematica='PACK C19'), 169, 0),
((SELECT id FROM combos WHERE tematica='PACK C20'), 170, 2),
((SELECT id FROM combos WHERE tematica='PACK C20'), 171, 3),
((SELECT id FROM combos WHERE tematica='PACK C20'), 172, 0),
((SELECT id FROM combos WHERE tematica='PACK C21'), 173, 2),
((SELECT id FROM combos WHERE tematica='PACK C21'), 174, 3),
((SELECT id FROM combos WHERE tematica='PACK C21'), 175, 0),
((SELECT id FROM combos WHERE tematica='PACK C22'), 176, 2),
((SELECT id FROM combos WHERE tematica='PACK C22'), 177, 3),
((SELECT id FROM combos WHERE tematica='PACK C22'), 178, 0),
((SELECT id FROM combos WHERE tematica='PACK C23'), 179, 2),
((SELECT id FROM combos WHERE tematica='PACK C23'), 180, 3),
((SELECT id FROM combos WHERE tematica='PACK C23'), 181, 0),
((SELECT id FROM combos WHERE tematica='PACK C24'), 182, 2),
((SELECT id FROM combos WHERE tematica='PACK C24'), 183, 3),
((SELECT id FROM combos WHERE tematica='PACK C24'), 184, 0);

-- 2 jornadas adicionales
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion)
VALUES ('Jornada 3', '2025-02-03', 'VALENCIA', 'preparacion', 1, NOW());

INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion)
VALUES ('Jornada 4', '2025-02-04', 'BARCELONA', 'preparacion', 1, NOW());

-- Asignar 6 cuestionarios y 6 combos a cada jornada nueva
-- Jornada 3: Q13..Q18 y C13..C18
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 3'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM cuestionarios WHERE tematica IN ('PACK Q13','PACK Q14','PACK Q15','PACK Q16','PACK Q17','PACK Q18');
INSERT INTO jornadas_combos (jornada_id, combo_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 3'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM combos WHERE tematica IN ('PACK C13','PACK C14','PACK C15','PACK C16','PACK C17','PACK C18');

-- Jornada 4: Q19..Q24 y C19..C24
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 4'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM cuestionarios WHERE tematica IN ('PACK Q19','PACK Q20','PACK Q21','PACK Q22','PACK Q23','PACK Q24');
INSERT INTO jornadas_combos (jornada_id, combo_id, slot)
SELECT (SELECT id FROM jornadas WHERE nombre='Jornada 4'), id, ROW_NUMBER() OVER (ORDER BY id)
FROM combos WHERE tematica IN ('PACK C19','PACK C20','PACK C21','PACK C22','PACK C23','PACK C24');