-- ============================================================
-- Datos de prueba LSNLS
-- ============================================================
-- Contenido:
--   - 675 preguntas reales con sentido:
--       ids   1-100 -> nivel _1LS  (100)
--       ids 101-200 -> nivel _2NLS (100)
--       ids 201-300 -> nivel _3LS  (100)
--       ids 301-400 -> nivel _4NLS (100)
--       ids 401-450 y 501-561 -> nivel _5LS  (111, para 37 combos)
--       ids 451-500 y 562-625 -> nivel _5NLS (114, para 38 combos)
--       ids 626-650 -> nivel _5LS  (25 libres, aprobadas)
--       ids 651-675 -> nivel _5NLS (25 libres, aprobadas)
--   - 75 cuestionarios: cada uno con 4 preguntas (una por nivel 1-4).
--       Cuestionario n usa las preguntas n, 100+n, 200+n y 300+n.
--       Quedan libres las preguntas 76-100 de cada nivel 1-4.
--   - 75 combos llenos, 3 preguntas de nivel 5 cada uno:
--       combos  1-37 (_5LS)  -> 401-450 y 501-561
--       combos 38-75 (_5NLS) -> 451-500 y 562-625
--       Se añaden preguntas 501-625 de nivel 5 para poder llenarlos.
--       Quedan libres 25 _5LS (626-650) y 25 _5NLS (651-675) para crear combos nuevos.
--   - 25 concursantes en estado 'grabado' (el estado inicial; no existe borrador),
--     con el cuestionario y combo del mismo número.
--
-- Coherencia de estados garantizada:
--   - Pregunta en cuestionario/combo -> estado 'usada' + disponibilidad 'usada'.
--   - Pregunta libre                 -> estado 'aprobada' + disponibilidad 'disponible'.
--   - Cuestionario/combo asignado a concursante -> 'grabado'.
--   - Cuestionarios: 1-25 grabado, 26-55 aprobado, 56-63 revisar,
--     64-68 corregir, 69-75 borrador.
--   - Combos: 1-25 grabado, 26-75 aprobado (todos con 3 preguntas).
-- ============================================================

USE lsnls;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection=utf8mb4;
SET character_set_client=utf8mb4;
SET character_set_results=utf8mb4;
SET collation_connection=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Limpieza total de datos de prueba
-- ------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE historial_jornadas;
TRUNCATE TABLE jornadas_combos;
TRUNCATE TABLE jornadas_cuestionarios;
TRUNCATE TABLE concursantes;
TRUNCATE TABLE jornadas;
TRUNCATE TABLE combos_preguntas;
TRUNCATE TABLE cuestionarios_preguntas;
TRUNCATE TABLE combos;
TRUNCATE TABLE cuestionarios;
TRUNCATE TABLE preguntas;
TRUNCATE TABLE tematicas_preguntas;
TRUNCATE TABLE tematicas_combos;
TRUNCATE TABLE subtemas_preguntas;
TRUNCATE TABLE programas;
TRUNCATE TABLE tematicas;
TRUNCATE TABLE operaciones_undo;
TRUNCATE TABLE edit_locks;
TRUNCATE TABLE usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- Usuario base
-- ------------------------------------------------------------
INSERT INTO usuarios (id, nombre, password, rol, version)
VALUES (1, 'admin', '123456', 'ROLE_ADMIN', 0)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  password = VALUES(password),
  rol = VALUES(rol);

-- ------------------------------------------------------------
-- Catálogo de temáticas de preguntas
-- ------------------------------------------------------------
INSERT INTO tematicas_preguntas (nombre, fecha_creacion, creacion_usuario_id)
VALUES
('GEOGRAFÍA', NOW(6), 1),
('HISTORIA', NOW(6), 1),
('CIENCIA', NOW(6), 1),
('ARTE', NOW(6), 1),
('LITERATURA', NOW(6), 1),
('DEPORTES', NOW(6), 1),
('CINE', NOW(6), 1),
('MÚSICA', NOW(6), 1),
('NATURALEZA', NOW(6), 1),
('GASTRONOMÍA', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- Secuencia auxiliar 1..100 (compatible con MySQL sin CTE recursivo)
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);
INSERT INTO tmp_seq (n)
SELECT (tens.d * 10 + ones.d + 1) AS n
FROM (
    SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
WHERE (tens.d * 10 + ones.d + 1) <= 100;

-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _1LS (ids 1-100) - muy fáciles
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(1, 'París', 'GEOGRAFÍA', '¿Cuál es la capital de Francia?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(2, 'Roma', 'GEOGRAFÍA', '¿Cuál es la capital de Italia?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(3, 'Portugal', 'GEOGRAFÍA', '¿Qué país limita con España al oeste?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(4, 'Mediterráneo', 'GEOGRAFÍA', '¿Qué mar baña la costa este de España?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(5, 'Everest', 'GEOGRAFÍA', '¿Cuál es la montaña más alta del mundo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(6, 'África', 'GEOGRAFÍA', '¿En qué continente está Egipto?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(7, 'Sevilla', 'GEOGRAFÍA', '¿Cuál es la capital de Andalucía?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(8, 'Atlántico', 'GEOGRAFÍA', '¿Qué océano separa Europa de América?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(9, 'Lisboa', 'GEOGRAFÍA', '¿Cuál es la capital de Portugal?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(10, 'Canarias', 'GEOGRAFÍA', '¿A qué archipiélago español pertenecen Tenerife y Gran Canaria?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(11, 'Cristóbal Colón', 'HISTORIA', '¿Qué navegante llegó a América en 1492?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(12, 'Los egipcios', 'HISTORIA', '¿Qué civilización construyó las pirámides de Guiza?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(13, '1945', 'HISTORIA', '¿En qué año terminó la Segunda Guerra Mundial?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(14, 'Los romanos', 'HISTORIA', '¿Qué civilización construyó el Coliseo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(15, 'Juana de Arco', 'HISTORIA', '¿Qué heroína francesa fue quemada en la hoguera en 1431?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(16, '1969', 'HISTORIA', '¿En qué año llegó el ser humano a la Luna?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(17, 'La Gran Muralla China', 'HISTORIA', '¿Cuál es la construcción defensiva más larga del mundo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(18, 'Napoleón Bonaparte', 'HISTORIA', '¿Qué emperador francés fue derrotado en Waterloo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(19, 'Titanic', 'HISTORIA', '¿Qué famoso transatlántico se hundió en 1912?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(20, 'Los vikingos', 'HISTORIA', '¿Qué navegantes escandinavos asolaron Europa en la Edad Media?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(21, 'H2O', 'CIENCIA', '¿Cuál es la fórmula química del agua?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(22, 'Mercurio', 'CIENCIA', '¿Cuál es el planeta más cercano al Sol?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(23, 'Hidrógeno', 'CIENCIA', '¿Qué elemento químico tiene el símbolo H?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(24, 'Oxígeno', 'CIENCIA', '¿Qué gas necesitamos respirar para vivir?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(25, 'El corazón', 'CIENCIA', '¿Qué órgano bombea la sangre por el cuerpo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(26, 'Marte', 'CIENCIA', '¿Qué planeta es conocido como el planeta rojo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(27, 'El cerebro', 'CIENCIA', '¿Qué órgano del cuerpo humano está protegido por el cráneo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(28, '100', 'CIENCIA', '¿A cuántos grados hierve el agua a nivel del mar?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(29, 'La Luna', 'CIENCIA', '¿Cuál es el satélite natural de la Tierra?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(30, 'El termómetro', 'CIENCIA', '¿Qué instrumento se usa para medir la temperatura?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(31, 'Pablo Picasso', 'ARTE', '¿Qué pintor malagueño es autor del Guernica?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(32, 'Leonardo da Vinci', 'ARTE', '¿Quién pintó La Gioconda?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(33, 'El Museo del Prado', 'ARTE', '¿Cómo se llama la gran pinacoteca de Madrid?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(34, 'Salvador Dalí', 'ARTE', '¿Qué pintor surrealista español era famoso por su bigote?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(35, 'Vincent van Gogh', 'ARTE', '¿Qué pintor de Los girasoles se cortó una oreja?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(36, 'La Torre Eiffel', 'ARTE', '¿Qué monumento de hierro es el símbolo de París?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(37, 'Miguel Ángel', 'ARTE', '¿Quién pintó la bóveda de la Capilla Sixtina?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(38, 'La Sagrada Familia', 'ARTE', '¿Qué basílica de Barcelona diseñó Antoni Gaudí?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(39, 'Diego Velázquez', 'ARTE', '¿Quién pintó Las Meninas?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(40, 'El Louvre', 'ARTE', '¿En qué museo de París se expone La Gioconda?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(41, 'Miguel de Cervantes', 'LITERATURA', '¿Quién escribió Don Quijote de la Mancha?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(42, 'Romeo y Julieta', 'LITERATURA', '¿Qué pareja de enamorados de Verona creó Shakespeare?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(43, 'Harry Potter', 'LITERATURA', '¿Qué joven mago estudia en el colegio Hogwarts?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(44, 'El Principito', 'LITERATURA', '¿Qué famoso personaje de Saint-Exupéry vivía en el asteroide B-612?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(45, 'Federico García Lorca', 'LITERATURA', '¿Qué poeta granadino escribió Bodas de sangre?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(46, 'Los tres mosqueteros', 'LITERATURA', '¿Qué novela de Dumas protagonizan Athos, Porthos y Aramis?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(47, 'Pinocho', 'LITERATURA', '¿A qué muñeco de madera le crecía la nariz al mentir?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(48, 'Julio Verne', 'LITERATURA', '¿Quién escribió Veinte mil leguas de viaje submarino?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(49, 'Caperucita Roja', 'LITERATURA', '¿Qué niña de cuento llevaba una cesta a casa de su abuela?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(50, 'Los molinos de viento', 'LITERATURA', '¿Contra qué luchó Don Quijote creyendo que eran gigantes?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(51, 'Once', 'DEPORTES', '¿Cuántos jugadores tiene un equipo de fútbol sobre el campo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(52, 'Baloncesto', 'DEPORTES', '¿Qué deporte se juega en la NBA?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(53, 'Rafael Nadal', 'DEPORTES', '¿Qué tenista español ha ganado 14 veces Roland Garros?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(54, 'El maratón', 'DEPORTES', '¿Qué carrera olímpica tiene 42 kilómetros y 195 metros?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(55, 'Cada cuatro años', 'DEPORTES', '¿Cada cuánto se celebran los Juegos Olímpicos de verano?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(56, 'El portero', 'DEPORTES', '¿Qué jugador de fútbol puede tocar el balón con las manos?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(57, 'El Tour de Francia', 'DEPORTES', '¿En qué carrera ciclista se viste el maillot amarillo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(58, 'Real Madrid', 'DEPORTES', '¿Qué club juega sus partidos en el Santiago Bernabéu?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(59, 'Natación', 'DEPORTES', '¿En qué deporte destacó Michael Phelps?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(60, 'Ajedrez', 'DEPORTES', '¿En qué juego se da jaque mate?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(61, 'Star Wars', 'CINE', '¿En qué saga galáctica aparece Darth Vader?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(62, 'Simba', 'CINE', '¿Cómo se llama el protagonista de El Rey León?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(63, 'Toy Story', 'CINE', '¿Cuál fue la primera película de Woody y Buzz Lightyear?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(64, 'Charlot', 'CINE', '¿Qué personaje de Chaplin llevaba bombín y bastón?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(65, 'Los Óscar', 'CINE', '¿Cómo se llaman los premios más famosos de Hollywood?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(66, 'Elsa', 'CINE', '¿Cómo se llama la reina de hielo de Frozen?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(67, 'Superman', 'CINE', '¿Qué superhéroe llegó a la Tierra desde el planeta Krypton?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(68, 'E.T.', 'CINE', '¿Qué extraterrestre de Spielberg quería llamar a su casa?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(69, 'Shrek', 'CINE', '¿Qué ogro verde vive en una ciénaga?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(70, 'Batman', 'CINE', '¿Qué superhéroe protege la ciudad de Gotham?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(71, 'The Beatles', 'MÚSICA', '¿Qué grupo de Liverpool cantaba Yellow Submarine?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(72, 'La guitarra', 'MÚSICA', '¿Qué instrumento de seis cuerdas es el más típico del flamenco?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(73, 'Michael Jackson', 'MÚSICA', '¿A qué cantante se le conoce como el rey del pop?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(74, 'El piano', 'MÚSICA', '¿Qué instrumento tiene 88 teclas blancas y negras?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(75, 'Elvis Presley', 'MÚSICA', '¿A quién se conoce como el rey del rock and roll?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(76, 'El violín', 'MÚSICA', '¿Qué instrumento de cuerda se toca con un arco apoyado en el hombro?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(77, 'Shakira', 'MÚSICA', '¿Qué cantante colombiana interpretó Waka Waka en el Mundial de 2010?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(78, 'Beethoven', 'MÚSICA', '¿Qué compositor alemán siguió componiendo tras quedarse sordo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(79, 'La batería', 'MÚSICA', '¿Qué instrumento de percusión combina bombo, caja y platillos?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(80, 'Julio Iglesias', 'MÚSICA', '¿Qué cantante español es el padre de Enrique Iglesias?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(81, 'El león', 'NATURALEZA', '¿A qué animal se le llama el rey de la selva?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(82, 'La ballena azul', 'NATURALEZA', '¿Cuál es el animal más grande del planeta?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(83, 'Ocho', 'NATURALEZA', '¿Cuántos brazos tiene un pulpo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(84, 'La jirafa', 'NATURALEZA', '¿Qué animal tiene el cuello más largo?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(85, 'La miel', 'NATURALEZA', '¿Qué alimento elaboran las abejas?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(86, 'El delfín', 'NATURALEZA', '¿Qué mamífero marino es famoso por su inteligencia y sus saltos?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(87, 'El camello', 'NATURALEZA', '¿Qué animal del desierto tiene dos jorobas?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(88, 'El otoño', 'NATURALEZA', '¿En qué estación del año caen las hojas de los árboles?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(89, 'El girasol', 'NATURALEZA', '¿Qué flor gira siguiendo la luz del sol?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(90, 'El pingüino', 'NATURALEZA', '¿Qué ave no voladora vive en la Antártida?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(91, 'La paella', 'GASTRONOMÍA', '¿Cuál es el plato de arroz más famoso de Valencia?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(92, 'La tortilla de patatas', 'GASTRONOMÍA', '¿Qué plato español lleva huevo, patata y, según gustos, cebolla?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(93, 'Italia', 'GASTRONOMÍA', '¿De qué país son originarias la pizza y la pasta?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(94, 'El gazpacho', 'GASTRONOMÍA', '¿Qué sopa fría andaluza se hace con tomate y hortalizas?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(95, 'El queso', 'GASTRONOMÍA', '¿Qué alimento se obtiene cuajando la leche?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(96, 'La naranja', 'GASTRONOMÍA', '¿Qué cítrico es el símbolo de la huerta valenciana?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(97, 'El chocolate', 'GASTRONOMÍA', '¿Qué dulce se elabora a partir del cacao?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(98, 'El aceite de oliva', 'GASTRONOMÍA', '¿A qué producto español se le llama oro líquido?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(99, 'Japón', 'GASTRONOMÍA', '¿De qué país es originario el sushi?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0),
(100, 'Los churros', 'GASTRONOMÍA', '¿Qué dulce frito y alargado se suele mojar en chocolate?', 'aprobada', 'disponible', '_1LS', NOW(6), 1, 0);

INSERT INTO tematicas_combos (nombre, fecha_creacion, creacion_usuario_id)
VALUES
('GEOGRAFÍA', NOW(6), 1),
('HISTORIA', NOW(6), 1),
('CIENCIA', NOW(6), 1),
('ARTE', NOW(6), 1),
('LITERATURA', NOW(6), 1),
('DEPORTES', NOW(6), 1),
('CINE', NOW(6), 1),
('MÚSICA', NOW(6), 1),
('NATURALEZA', NOW(6), 1),
('GASTRONOMÍA', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);


-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _2NLS (ids 101-200) - fáciles-medias
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(101, 'Berlín', 'GEOGRAFÍA', '¿Cuál es la capital de Alemania?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(102, 'Viena', 'GEOGRAFÍA', '¿Cuál es la capital de Austria?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(103, 'El Támesis', 'GEOGRAFÍA', '¿Qué río atraviesa Londres?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(104, 'El Amazonas', 'GEOGRAFÍA', '¿Cuál es el río más caudaloso del mundo?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(105, 'El Aconcagua', 'GEOGRAFÍA', '¿Cuál es la montaña más alta de América?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(106, 'Islandia', 'GEOGRAFÍA', '¿Qué país europeo es famoso por sus géiseres y volcanes?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(107, 'El Danubio', 'GEOGRAFÍA', '¿Qué río europeo pasa por Viena, Budapest y Belgrado?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(108, 'El Teide', 'GEOGRAFÍA', '¿Cuál es el pico más alto de España?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(109, 'Marruecos', 'GEOGRAFÍA', '¿Qué país africano está separado de España por el estrecho de Gibraltar?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(110, 'El Guadalquivir', 'GEOGRAFÍA', '¿Qué río andaluz pasa por Sevilla y Córdoba?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(111, '1789', 'HISTORIA', '¿En qué año estalló la Revolución Francesa?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(112, '1914', 'HISTORIA', '¿En qué año comenzó la Primera Guerra Mundial?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(113, 'Francisco Franco', 'HISTORIA', '¿Quién gobernó España como dictador entre 1939 y 1975?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(114, 'Cleopatra', 'HISTORIA', '¿Qué reina egipcia se relacionó con Julio César y Marco Antonio?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(115, 'La Bastilla', 'HISTORIA', '¿Qué prisión parisina fue tomada el 14 de julio de 1789?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(116, 'Magallanes', 'HISTORIA', '¿Qué expedición completó la primera vuelta al mundo tras la muerte de su capitán?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(117, 'Los Reyes Católicos', 'HISTORIA', '¿Qué monarcas unieron Castilla y Aragón con su matrimonio?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(118, '1989', 'HISTORIA', '¿En qué año cayó el Muro de Berlín?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(119, 'Tutankamón', 'HISTORIA', '¿De qué faraón se descubrió intacta la tumba en 1922?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(120, 'La Armada Invencible', 'HISTORIA', '¿Cómo se llamó la flota que Felipe II envió contra Inglaterra en 1588?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(121, 'Neptuno', 'CIENCIA', '¿Cuál es el planeta más lejano del sistema solar desde 2006?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(122, 'El helio', 'CIENCIA', '¿Qué gas noble se usa para inflar globos que suben?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(123, 'Isaac Newton', 'CIENCIA', '¿Qué científico formuló la ley de la gravitación universal?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(124, 'El hígado', 'CIENCIA', '¿Cuál es el órgano interno más grande del cuerpo humano?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(125, 'La fotosíntesis', 'CIENCIA', '¿Cómo se llama el proceso por el que las plantas fabrican alimento con luz?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(126, 'El diamante', 'CIENCIA', '¿Cuál es el mineral más duro de la escala de Mohs?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(127, 'Albert Einstein', 'CIENCIA', '¿Qué físico formuló la teoría de la relatividad?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(128, 'El páncreas', 'CIENCIA', '¿Qué órgano produce la insulina?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(129, 'Saturno', 'CIENCIA', '¿Qué planeta es famoso por sus anillos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(130, 'El carbono', 'CIENCIA', '¿Qué elemento químico tiene el símbolo C?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(131, 'Francisco de Goya', 'ARTE', '¿Qué pintor aragonés es autor de Los fusilamientos del 3 de mayo?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(132, 'El Guernica', 'ARTE', '¿Qué cuadro de Picasso denuncia el bombardeo de una villa vasca?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(133, 'Claude Monet', 'ARTE', '¿Qué pintor francés es considerado padre del impresionismo?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(134, 'La Alhambra', 'ARTE', '¿Qué palacio nazarí se alza en Granada?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(135, 'Andy Warhol', 'ARTE', '¿Qué artista pop retrató latas de sopa Campbell?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(136, 'El David', 'ARTE', '¿Qué escultura de Miguel Ángel representa a un joven bíblico con honda?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(137, 'Joan Miró', 'ARTE', '¿Qué pintor catalán es famoso por sus estrellas y formas oníricas?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(138, 'El Partenón', 'ARTE', '¿Qué templo griego se alza en la Acrópolis de Atenas?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(139, 'Rembrandt', 'ARTE', '¿Qué pintor holandés es autor de La ronda de noche?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(140, 'El Guggenheim', 'ARTE', '¿Qué museo de titanio diseñó Frank Gehry en Bilbao?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(141, 'Gabriel García Márquez', 'LITERATURA', '¿Quién escribió Cien años de soledad?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(142, 'La Odisea', 'LITERATURA', '¿En qué poema épico Homero narra el regreso de Ulises a Ítaca?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(143, 'J. R. R. Tolkien', 'LITERATURA', '¿Quién escribió El Señor de los Anillos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(144, 'La Celestina', 'LITERATURA', '¿Qué obra de Fernando de Rojas relata los amores de Calisto y Melibea?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(145, 'George Orwell', 'LITERATURA', '¿Quién escribió 1984 y Rebelión en la granja?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(146, 'El Lazarillo de Tormes', 'LITERATURA', '¿Qué novela anónima inaugura el género picaresco en España?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(147, 'Agatha Christie', 'LITERATURA', '¿Qué escritora británica creó a Hércules Poirot?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(148, 'Platero y yo', 'LITERATURA', '¿Qué obra de Juan Ramón Jiménez está dedicada a un burrito?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(149, 'Mary Shelley', 'LITERATURA', '¿Quién escribió Frankenstein?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(150, 'La Ilíada', 'LITERATURA', '¿En qué poema épico se narra la guerra de Troya?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(151, 'Wimbledon', 'DEPORTES', '¿Qué Grand Slam de tenis se juega sobre hierba?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(152, 'Michael Jordan', 'DEPORTES', '¿Qué jugador de los Chicago Bulls es considerado el mejor de la historia?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(153, 'El offside', 'DEPORTES', '¿Cómo se llama en inglés la posición de fuera de juego en fútbol?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(154, 'Cinco', 'DEPORTES', '¿Cuántos puntos vale un ensayo en rugby union?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(155, 'Fernando Alonso', 'DEPORTES', '¿Qué piloto asturiano ha sido dos veces campeón de Fórmula 1?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(156, 'El quinteto', 'DEPORTES', '¿Cuántos jugadores de un equipo de baloncesto hay en pista?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(157, 'Usain Bolt', 'DEPORTES', '¿Qué velocista jamaicano ostenta el récord de 100 metros lisos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(158, 'La Copa Davis', 'DEPORTES', '¿Cómo se llama el Mundial de tenis por equipos masculinos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(159, 'El FC Barcelona', 'DEPORTES', '¿Qué club catalán juega en el Spotify Camp Nou?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(160, 'El decatlón', 'DEPORTES', '¿Qué prueba atlética masculina combina diez disciplinas?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(161, 'Alfred Hitchcock', 'CINE', '¿Qué director británico es conocido como el maestro del suspense?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(162, 'El Padrino', 'CINE', '¿En qué saga de Coppola aparece la familia Corleone?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(163, 'Audrey Hepburn', 'CINE', '¿Qué actriz protagonizó Desayuno con diamantes?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(164, 'Jurassic Park', 'CINE', '¿En qué película de Spielberg reviven los dinosaurios?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(165, 'Pedro Almodóvar', 'CINE', '¿Qué director manchego rodó Todo sobre mi madre?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(166, 'Forrest Gump', 'CINE', '¿Qué personaje de Tom Hanks dice que la vida es como una caja de bombones?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(167, 'Studio Ghibli', 'CINE', '¿Qué estudio japonés creó El viaje de Chihiro?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(168, 'Casablanca', 'CINE', '¿En qué clásico Humphrey Bogart dice «Siempre nos quedará París»?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(169, 'Quentin Tarantino', 'CINE', '¿Qué director rodó Pulp Fiction y Kill Bill?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(170, 'Titanic', 'CINE', '¿Qué película de James Cameron ganó 11 Óscar en 1998?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(171, 'Mozart', 'MÚSICA', '¿Qué compositor austriaco escribió La flauta mágica de niño prodigio?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(172, 'Freddie Mercury', 'MÚSICA', '¿Quién fue el cantante de Queen?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(173, 'El flamenco', 'MÚSICA', '¿Qué arte andaluz combina cante, toque y baile?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(174, 'Madonna', 'MÚSICA', '¿A qué cantante estadounidense se llama la reina del pop?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(175, 'Paco de Lucía', 'MÚSICA', '¿Qué guitarrista gaditano revolucionó el flamenco?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(176, 'El saxofón', 'MÚSICA', '¿Qué instrumento de viento es típico del jazz?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(177, 'ABBA', 'MÚSICA', '¿Qué grupo sueco cantó Dancing Queen y Mamma Mia?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(178, 'Camarón de la Isla', 'MÚSICA', '¿Qué cantaor de San Fernando se llamaba José Monje Cruz?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(179, 'El arpa', 'MÚSICA', '¿Qué instrumento de cuerda punteada tiene forma triangular?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(180, 'Bob Dylan', 'MÚSICA', '¿Qué cantautor estadounidense recibió el Nobel de Literatura en 2016?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(181, 'El koala', 'NATURALEZA', '¿Qué marsupial australiano se alimenta casi solo de eucalipto?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(182, 'La secuoya', 'NATURALEZA', '¿Qué árbol de California puede superar los 100 metros de altura?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(183, 'El ornitorrinco', 'NATURALEZA', '¿Qué mamífero australiano pone huevos y tiene pico de pato?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(184, 'El baobab', 'NATURALEZA', '¿Qué árbol africano almacena agua en su enorme tronco?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(185, 'La mantis religiosa', 'NATURALEZA', '¿Qué insecto parece rezar antes de cazar?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(186, 'El cóndor', 'NATURALEZA', '¿Qué ave rapaz andina tiene una de las mayores envergaduras?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(187, 'El bambú', 'NATURALEZA', '¿De qué planta se alimenta casi exclusivamente el panda gigante?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(188, 'La medusa', 'NATURALEZA', '¿Qué animal marino gelatinoso puede picar con sus tentáculos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(189, 'El cactus', 'NATURALEZA', '¿Qué planta del desierto almacena agua en sus tejidos?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(190, 'El lince ibérico', 'NATURALEZA', '¿Cuál es el felino más amenazado de la península ibérica?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(191, 'El jamón ibérico', 'GASTRONOMÍA', '¿Qué embutido español se cura de cerdos de raza ibérica?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(192, 'El falafel', 'GASTRONOMÍA', '¿Qué croqueta de garbanzos es típica de Oriente Próximo?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(193, 'La sidra', 'GASTRONOMÍA', '¿Qué bebida de manzana es típica de Asturias?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(194, 'El couscous', 'GASTRONOMÍA', '¿Qué plato magrebí se elabora con sémola de trigo?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(195, 'El foie gras', 'GASTRONOMÍA', '¿Cómo se llama el hígado engordado de pato o oca?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(196, 'La fabada', 'GASTRONOMÍA', '¿Qué guiso asturiano lleva fabes, chorizo y morcilla?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(197, 'El wasabi', 'GASTRONOMÍA', '¿Qué pasta verde picante acompaña al sushi?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(198, 'El txakoli', 'GASTRONOMÍA', '¿Qué vino blanco ligeramente espumoso es típico del País Vasco?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(199, 'El hummus', 'GASTRONOMÍA', '¿Qué crema de garbanzos se come con pan pita?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0),
(200, 'La crema catalana', 'GASTRONOMÍA', '¿Qué postre lleva yema, leche y una costra de azúcar quemado?', 'aprobada', 'disponible', '_2NLS', NOW(6), 1, 0);


-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _3LS (ids 201-300) - medias
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(201, 'Ulan Bator', 'GEOGRAFÍA', '¿Cuál es la capital de Mongolia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(202, 'El Kilimanjaro', 'GEOGRAFÍA', '¿Cuál es la montaña más alta de África?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(203, 'El lago Baikal', 'GEOGRAFÍA', '¿Cuál es el lago de agua dulce más profundo del mundo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(204, 'Bhutan', 'GEOGRAFÍA', '¿Qué pequeño reino himalayo mide la felicidad nacional bruta?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(205, 'El cabo de Hornos', 'GEOGRAFÍA', '¿Cuál es el extremo más meridional de América del Sur?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(206, 'Tanzania', 'GEOGRAFÍA', '¿En qué país africano está el parque nacional del Serengueti?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(207, 'El río Mekong', 'GEOGRAFÍA', '¿Qué gran río del sudeste asiático desemboca en Vietnam?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(208, 'La Patagonia', 'GEOGRAFÍA', '¿Cómo se llama la región austral compartida por Argentina y Chile?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(209, 'Asmara', 'GEOGRAFÍA', '¿Cuál es la capital de Eritrea?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(210, 'El desierto de Atacama', 'GEOGRAFÍA', '¿Cuál es el desierto no polar más árido del mundo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(211, 'El Edicto de Nantes', 'HISTORIA', '¿Qué edicto de 1598 concedió libertad religiosa a los hugonotes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(212, 'Hammurabi', 'HISTORIA', '¿Qué rey babilonio promulgó uno de los primeros códigos legales?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(213, 'La Paz de Westfalia', 'HISTORIA', '¿Qué tratados de 1648 pusieron fin a la Guerra de los Treinta Años?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(214, 'Simón Bolívar', 'HISTORIA', '¿Qué militar venezolano es conocido como El Libertador de América?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(215, 'El motín del té', 'HISTORIA', '¿Qué protesta de 1773 en Boston precedió a la independencia de EE. UU.?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(216, 'Carlomagno', 'HISTORIA', '¿Qué rey franco fue coronado emperador en el año 800?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(217, 'La Comuna de París', 'HISTORIA', '¿Qué gobierno revolucionario controló París durante 72 días en 1871?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(218, 'Aníbal', 'HISTORIA', '¿Qué general cartaginés cruzó los Alpes con elefantes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(219, 'El Tratado de Tordesillas', 'HISTORIA', '¿Qué acuerdo de 1494 dividió el Nuevo Mundo entre España y Portugal?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(220, 'Hatshepsut', 'HISTORIA', '¿Qué mujer gobernó Egipto como faraón en el Imperio Nuevo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(221, 'Marie Curie', 'CIENCIA', '¿Qué científica ganó dos Nobel en distintas disciplinas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(222, 'El bosón de Higgs', 'CIENCIA', '¿Qué partícula se conoce popularmente como la partícula de Dios?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(223, 'Gregor Mendel', 'CIENCIA', '¿Qué monje austriaco es considerado padre de la genética?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(224, 'La mitocondria', 'CIENCIA', '¿Qué orgánulo celular produce la mayor parte del ATP?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(225, 'El número de Avogadro', 'CIENCIA', '¿Cómo se llama la cantidad de entidades en un mol de sustancia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(226, 'Titanio', 'CIENCIA', '¿Qué metal de símbolo Ti es ligero y muy resistente a la corrosión?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(227, 'Alan Turing', 'CIENCIA', '¿Qué matemático británico descifró Enigma y sentó las bases de la computación?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(228, 'El aparato de Golgi', 'CIENCIA', '¿Qué orgánulo empaqueta y distribuye proteínas en la célula?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(229, 'La constante de Planck', 'CIENCIA', '¿Qué constante física relaciona la energía de un fotón con su frecuencia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(230, 'Dmitri Mendeléyev', 'CIENCIA', '¿Quién ordenó los elementos en la tabla periódica?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(231, 'El Bosco', 'ARTE', '¿Qué pintor flamenco es autor de El jardín de las delicias?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(232, 'El expresionismo', 'ARTE', '¿A qué movimiento pertenecen El grito de Munch y los fauves alemanes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(233, 'Antoni Gaudí', 'ARTE', '¿Qué arquitecto diseñó el Park Güell además de la Sagrada Familia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(234, 'Caravaggio', 'ARTE', '¿Qué pintor italiano llevó el claroscuro al extremo en el Barroco?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(235, 'El cubismo', 'ARTE', '¿Qué movimiento artístico iniciaron Picasso y Braque hacia 1907?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(236, 'Frida Kahlo', 'ARTE', '¿Qué pintora mexicana es famosa por sus autorretratos y cejas pobladas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(237, 'El Taj Mahal', 'ARTE', '¿Qué mausoleo de mármol blanco mandó construir Sha Jahan en Agra?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(238, 'Jackson Pollock', 'ARTE', '¿Qué pintor estadounidense es el máximo exponente del dripping?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(239, 'El Greco', 'ARTE', '¿Qué pintor cretense se estableció en Toledo en el siglo XVI?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(240, 'La catedral de Chartres', 'ARTE', '¿Qué catedral gótica francesa es famosa por sus vidrieras azules?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(241, 'Fernando de Rojas', 'LITERATURA', '¿Quién es el autor de La Celestina?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(242, 'El Quijote apócrifo', 'LITERATURA', '¿Qué escritor firmó una continuación falsa del Quijote como Avellaneda?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(243, 'Virginia Woolf', 'LITERATURA', '¿Quién escribió La señora Dalloway y Una habitación propia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(244, 'La generación del 27', 'LITERATURA', '¿A qué grupo poético español pertenecieron Alberti y Cernuda?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(245, 'Franz Kafka', 'LITERATURA', '¿Quién escribió La metamorfosis, en la que Gregorio Samsa se vuelve insecto?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(246, 'Las Soledades', 'LITERATURA', '¿Qué poema gongorino quedó inacabado en dos partes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(247, 'James Joyce', 'LITERATURA', '¿Quién escribió Ulises, ambientado en un solo día en Dublín?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(248, 'El Buscón', 'LITERATURA', '¿Qué novela picaresca escribió Francisco de Quevedo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(249, 'Toni Morrison', 'LITERATURA', '¿Qué escritora estadounidense ganó el Nobel con obras como Beloved?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(250, 'La Regenta', 'LITERATURA', '¿Qué novela de Clarín transcurre en Vetusta, trasunto de Oviedo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(251, 'El albatros', 'DEPORTES', '¿Cómo se llama en golf un resultado de tres bajo par en un hoyo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(252, 'El Tourmalet', 'DEPORTES', '¿Qué puerto pirenaico es uno de los más míticos del Tour de Francia?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(253, 'Babe Ruth', 'DEPORTES', '¿Qué bateador de los Yankees fue apodado The Bambino?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(254, 'El heptatlón', 'DEPORTES', '¿Qué prueba atlética femenina combina siete disciplinas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(255, 'Ayrton Senna', 'DEPORTES', '¿Qué piloto brasileño de F1 falleció en Imola en 1994?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(256, 'El scrum', 'DEPORTES', '¿Cómo se llama la formación de melé en rugby?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(257, 'Nadia Comăneci', 'DEPORTES', '¿Qué gimnasta rumana logró el primer 10 perfecto olímpico?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(258, 'Wembley', 'DEPORTES', '¿En qué estadio londinense se juega tradicionalmente la final de la FA Cup?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(259, 'El curling', 'DEPORTES', '¿Qué deporte de hielo se juega deslizando piedras hacia una diana?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(260, 'Björn Borg', 'DEPORTES', '¿Qué tenista sueco ganó Wimbledon cinco veces seguidas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(261, 'Akira Kurosawa', 'CINE', '¿Qué director japonés rodó Rashomon y Los siete samuráis?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(262, 'Ciudadano Kane', 'CINE', '¿Qué ópera prima de Orson Welles suele encabezar las listas de mejores filmes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(263, 'Luis Buñuel', 'CINE', '¿Qué cineasta aragonés rodó Un perro andaluz con Salvador Dalí?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(264, 'El tercer hombre', 'CINE', '¿En qué noir de Carol Reed suena el cítara de Anton Karas en Viena?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(265, 'Federico Fellini', 'CINE', '¿Qué director italiano rodó La dolce vita y Ocho y medio?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(266, 'Psicosis', 'CINE', '¿En qué filme de Hitchcock ocurre la famosa escena de la ducha?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(267, 'Andrei Tarkovski', 'CINE', '¿Qué director soviético rodó Stalker y Solaris?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(268, 'Blade Runner', 'CINE', '¿Qué filme de Ridley Scott adapta ¿Sueñan los androides con ovejas eléctricas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(269, 'Ingmar Bergman', 'CINE', '¿Qué director sueco rodó El séptimo sello y Persona?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(270, 'Metrópolis', 'CINE', '¿Qué clásico expresionista de Fritz Lang imagina una ciudad futura?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(271, 'Johann Sebastian Bach', 'MÚSICA', '¿Qué compositor barroco escribió El clave bien temperado?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(272, 'Miles Davis', 'MÚSICA', '¿Qué trompetista grabó Kind of Blue, disco esencial del jazz?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(273, 'El contrapunto', 'MÚSICA', '¿Cómo se llama la superposición de melodías independientes?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(274, 'David Bowie', 'MÚSICA', '¿Qué músico británico creó el alter ego Ziggy Stardust?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(275, 'Manuel de Falla', 'MÚSICA', '¿Qué compositor gaditano escribió El amor brujo?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(276, 'El requinto', 'MÚSICA', '¿Qué clarinete más pequeño y agudo se usa a veces en bandas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(277, 'Nina Simone', 'MÚSICA', '¿Qué pianista y cantante grabó Feeling Good y Mississippi Goddam?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(278, 'El zortziko', 'MÚSICA', '¿Qué ritmo vasco se escribe habitualmente en 5/8?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(279, 'Igor Stravinski', 'MÚSICA', '¿Qué compositor ruso escandalizó París con La consagración de la primavera?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(280, 'El fado', 'MÚSICA', '¿Qué género lisboeta interpreta la saudade, popularizado por Amália Rodrigues?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(281, 'El axolotl', 'NATURALEZA', '¿Qué anfibio mexicano conserva branquias de larva toda su vida?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(282, 'La welwitschia', 'NATURALEZA', '¿Qué planta del desierto de Namibia vive siglos con solo dos hojas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(283, 'El tuátara', 'NATURALEZA', '¿Qué reptil de Nueva Zelanda es el único sobreviviente de su orden?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(284, 'El krill', 'NATURALEZA', '¿Qué crustáceo antártico es la base de la dieta de muchas ballenas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(285, 'La rana de Darwin', 'NATURALEZA', '¿Qué anfibio chileno incuba los huevos en el saco vocal del macho?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(286, 'El baobab de Madagascar', 'NATURALEZA', '¿En qué isla africana hay avenidas famosas de baobabs?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(287, 'El lobo ibérico', 'NATURALEZA', '¿Qué subespecie de Canis lupus habita la península ibérica?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(288, 'La posidonia', 'NATURALEZA', '¿Qué planta marina mediterránea forma praderas esenciales?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(289, 'El quetzal', 'NATURALEZA', '¿Qué ave centroamericana de plumaje verde era sagrada para los mayas?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(290, 'El urogallo', 'NATURALEZA', '¿Qué galliforme cantábrico está en grave peligro en España?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(291, 'El mole', 'GASTRONOMÍA', '¿Qué salsa mexicana combina chile y, a menudo, chocolate?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(292, 'El kimchi', 'GASTRONOMÍA', '¿Qué fermentado de col es el acompañamiento nacional de Corea?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(293, 'El marmitako', 'GASTRONOMÍA', '¿Qué guiso vasco de bonito se cocina en marmita?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(294, 'El pho', 'GASTRONOMÍA', '¿Qué sopa vietnamita de fideos y ternera se toma en el desayuno?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(295, 'El pimentón de la Vera', 'GASTRONOMÍA', '¿Qué condimento extremeño se ahumaba tradicionalmente con encina?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(296, 'El ceviche', 'GASTRONOMÍA', '¿Qué plato peruano marina pescado crudo en limón?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(297, 'El haggis', 'GASTRONOMÍA', '¿Qué embutido escocés se elabora con menudillos de oveja?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(298, 'El romesco', 'GASTRONOMÍA', '¿Qué salsa catalana lleva ñoras, tomate, almendras y pan?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(299, 'El injera', 'GASTRONOMÍA', '¿Qué pan etíope esponjoso se hace con teff fermentado?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0),
(300, 'El caldo gallego', 'GASTRONOMÍA', '¿Qué sopa del noroeste lleva grelos, cachelos y lacón?', 'aprobada', 'disponible', '_3LS', NOW(6), 1, 0);


-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _4NLS (ids 301-400) - medias-difíciles
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(301, 'Nuuk', 'GEOGRAFÍA', '¿Cuál es la capital de Groenlandia?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(302, 'El pico Jaya', 'GEOGRAFÍA', '¿Cuál es la montaña más alta de Oceanía, en Papúa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(303, 'El mar de Aral', 'GEOGRAFÍA', '¿Qué mar interior de Asia Central se ha reducido drásticamente por el riego?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(304, 'Suva', 'GEOGRAFÍA', '¿Cuál es la capital de Fiyi?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(305, 'El lago Titicaca', 'GEOGRAFÍA', '¿Cuál es el lago navegable más alto del mundo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(306, 'Lesoto', 'GEOGRAFÍA', '¿Qué reino africano está completamente rodeado por Sudáfrica?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(307, 'El río Yeniséi', 'GEOGRAFÍA', '¿Qué gran río siberiano desemboca en el Ártico cerca de Dudinka?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(308, 'Nagorno Karabaj', 'GEOGRAFÍA', '¿Qué enclave caucásico ha sido disputado por Armenia y Azerbaiyán?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(309, 'Palikir', 'GEOGRAFÍA', '¿Cuál es la capital de los Estados Federados de Micronesia?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(310, 'El macizo Vinson', 'GEOGRAFÍA', '¿Cuál es la montaña más alta de la Antártida?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(311, 'La Paz de Cateau-Cambrésis', 'HISTORIA', '¿Qué paz de 1559 cerró las guerras entre España y Francia en Italia?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(312, 'Ashoka', 'HISTORIA', '¿Qué emperador mauria se convirtió al budismo tras la batalla de Kalinga?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(313, 'El cisma de Oriente', 'HISTORIA', '¿En qué año 1054 se separaron las iglesias de Roma y Constantinopla?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(314, 'Túpac Amaru II', 'HISTORIA', '¿Qué líder andino se alzó contra los españoles en 1780?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(315, 'La Liga Hanseática', 'HISTORIA', '¿Cómo se llamó la alianza comercial de ciudades del Báltico medieval?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(316, 'Zenobia', 'HISTORIA', '¿Qué reina de Palmira desafió a Roma en el siglo III?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(317, 'El Congreso de Viena', 'HISTORIA', '¿Qué reunión de 1814-1815 reordenó Europa tras Napoleón?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(318, 'Mansa Musa', 'HISTORIA', '¿Qué emperador de Malí es considerado uno de los hombres más ricos de la historia?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(319, 'La revuelta de los comuneros', 'HISTORIA', '¿Qué alzamiento castellano de 1520-1521 se enfrentó a Carlos I?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(320, 'Sejong el Grande', 'HISTORIA', '¿Qué rey coreano impulsó el alfabeto hangul en el siglo XV?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(321, 'Lise Meitner', 'CIENCIA', '¿Qué física austriaca explicó la fisión nuclear junto a Otto Hahn?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(322, 'El neutrino', 'CIENCIA', '¿Qué partícula casi sin masa atraviesa la Tierra sin apenas interactuar?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(323, 'Barbara McClintock', 'CIENCIA', '¿Qué genetista descubrió los transposones o genes saltarines?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(324, 'El retículo endoplasmático', 'CIENCIA', '¿Qué red de membranas celulares puede ser rugoso o liso?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(325, 'La constante de Faraday', 'CIENCIA', '¿Qué constante equivale a la carga de un mol de electrones?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(326, 'El wolframio', 'CIENCIA', '¿Qué metal de símbolo W tiene el punto de fusión más alto?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(327, 'Rosalind Franklin', 'CIENCIA', '¿Qué científica obtuvo la foto 51, clave para la estructura del ADN?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(328, 'Los lisosomas', 'CIENCIA', '¿Qué orgánulos contienen enzimas digestivas en la célula animal?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(329, 'El efecto Compton', 'CIENCIA', '¿Qué fenómeno muestra que los rayos X se comportan como partículas al chocar?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(330, 'Glenn Seaborg', 'CIENCIA', '¿Qué químico estadounidense dio nombre a un elemento y amplió la tabla periódica?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(331, 'Piero della Francesca', 'ARTE', '¿Qué pintor del Quattrocento es autor de La leyenda de la Vera Cruz en Arezzo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(332, 'El neoplasticismo', 'ARTE', '¿A qué movimiento de Mondrian se asocia De Stijl?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(333, 'Le Corbusier', 'ARTE', '¿Qué arquitecto suizo-francés formuló los cinco puntos de una nueva arquitectura?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(334, 'Artemisia Gentileschi', 'ARTE', '¿Qué pintora barroca es autora de Judit decapitando a Holofernes?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(335, 'El orfismo', 'ARTE', '¿Qué corriente cubista de Delaunay se centra en el color y el ritmo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(336, 'Louise Bourgeois', 'ARTE', '¿Qué escultora creó la araña monumental titulada Maman?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(337, 'Angkor Wat', 'ARTE', '¿Qué templo jemer de Camboya es el mayor complejo religioso del mundo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(338, 'Mark Rothko', 'ARTE', '¿Qué pintor de campos de color es famoso por sus rectángulos flotantes?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(339, 'Zurbarán', 'ARTE', '¿Qué pintor extremeño es célebre por sus bodegones y monjes en éxtasis?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(340, 'La capilla Palatina de Aquisgrán', 'ARTE', '¿Qué capilla mandó construir Carlomagno como corazón de su palacio?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(341, 'Luis de Góngora', 'LITERATURA', '¿Quién escribió la Fábula de Polifemo y Galatea?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(342, 'El manuscrito de Voynich', 'LITERATURA', '¿Qué códice ilustrado permanece sin descifrar desde el siglo XV?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(343, 'Clarice Lispector', 'LITERATURA', '¿Qué escritora brasileña es autora de La hora de la estrella?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(344, 'Las Rimas de Bécquer', 'LITERATURA', '¿Qué conjunto poético sevillano comienza con «Yo sé un himno gigante y extraño»?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(345, 'Samuel Beckett', 'LITERATURA', '¿Quién escribió Esperando a Godot?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(346, 'El Cántico espiritual', 'LITERATURA', '¿Qué poema de San Juan de la Cruz dialoga entre el alma y el Esposo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(347, 'Chinua Achebe', 'LITERATURA', '¿Quién escribió Todo se desmorona, clásico nigeriano?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(348, 'Fortunata y Jacinta', 'LITERATURA', '¿Qué novela de Galdós contrapone a dos mujeres en el Madrid isabelino?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(349, 'Wisława Szymborska', 'LITERATURA', '¿Qué poeta polaca ganó el Nobel en 1996?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(350, 'La colmena', 'LITERATURA', '¿Qué novela de Cela retrata el Madrid de la posguerra en múltiples voces?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(351, 'El condor', 'DEPORTES', '¿Cómo se llama en golf un resultado de cuatro bajo par en un hoyo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(352, 'Fausto Coppi', 'DEPORTES', '¿Qué ciclista italiano, il Campionissimo, rivalizó con Bartali?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(353, 'Satchel Paige', 'DEPORTES', '¿Qué lanzador de las Ligas Negras llegó a MLB ya veterano y es leyenda?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(354, 'El pentatlón moderno', 'DEPORTES', '¿Qué prueba olímpica combina esgrima, natación, hípica, láser y carrera?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(355, 'Jim Clark', 'DEPORTES', '¿Qué piloto escocés de F1 murió en Hockenheim en 1968?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(356, 'El line-out', 'DEPORTES', '¿Cómo se llama el saque de banda en rugby con saltos alineados?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(357, 'Olga Korbut', 'DEPORTES', '¿Qué gimnasta soviética deslumbró en Múnich 1972 con un salto mortal atrás en barra?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(358, 'Anfield', 'DEPORTES', '¿En qué estadio de Liverpool suena You''ll Never Walk Alone?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(359, 'El bobsleigh', 'DEPORTES', '¿Qué deporte de hielo desciende por un tobogán en un trineo carenado?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(360, 'Rod Laver', 'DEPORTES', '¿Qué tenista australiano logró dos Grand Slam calendarios, en 1962 y 1969?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(361, 'Yasujirō Ozu', 'CINE', '¿Qué director japonés rodó Cuentos de Tokio con cámara baja y planos fijos?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(362, 'El acorazado Potemkin', 'CINE', '¿Qué filme de Eisenstein incluye la famosa secuencia de las escaleras de Odesa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(363, 'Víctor Erice', 'CINE', '¿Qué director vasco rodó El espíritu de la colmena?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(364, 'El gabinete del doctor Caligari', 'CINE', '¿Qué filme de 1920 es el gran icono del expresionismo alemán?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(365, 'Theo Angelopoulos', 'CINE', '¿Qué director griego rodó La mirada de Ulises?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(366, 'Vértigo', 'CINE', '¿En qué filme de Hitchcock James Stewart sigue a Kim Novak por San Francisco?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(367, 'Béla Tarr', 'CINE', '¿Qué director húngaro rodó Sátántangó en planos larguísimos?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(368, 'Stalker', 'CINE', '¿Qué filme de Tarkovski sigue a un guía hacia una Zona prohibida?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(369, 'Carl Theodor Dreyer', 'CINE', '¿Qué director danés rodó La pasión de Juana de Arco?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(370, 'El machinist', 'CINE', '¿Qué filme de Brad Anderson muestra a Christian Bale extremadamente delgado?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(371, 'Claudio Monteverdi', 'MÚSICA', '¿Qué compositor escribió L''Orfeo, considerada la primera gran ópera?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(372, 'John Coltrane', 'MÚSICA', '¿Qué saxofonista grabó A Love Supreme?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(373, 'El dodecafonismo', 'MÚSICA', '¿Qué sistema de Schoenberg usa las doce notas en series?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(374, 'Kate Bush', 'MÚSICA', '¿Qué cantautora británica lanzó Wuthering Heights a los 19 años?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(375, 'Isaac Albéniz', 'MÚSICA', '¿Qué compositor catalán escribió la suite Iberia para piano?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(376, 'El sitar', 'MÚSICA', '¿Qué instrumento indio de cuerda popularizó Ravi Shankar en Occidente?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(377, 'Fela Kuti', 'MÚSICA', '¿Qué músico nigeriano es el padre del afrobeat?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(378, 'La jota aragonesa', 'MÚSICA', '¿Qué danza del valle del Ebro se canta y se baila con castañuelas?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(379, 'Olivier Messiaen', 'MÚSICA', '¿Qué compositor francés transcribió cantos de pájaros en su música?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(380, 'El tango', 'MÚSICA', '¿Qué género rioplatense tiene como gran figura a Carlos Gardel?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(381, 'El aye-aye', 'NATURALEZA', '¿Qué lémur de Madagascar localiza larvas golpeando la madera?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(382, 'La rafflesia', 'NATURALEZA', '¿Qué planta parásita de Indonesia produce la flor más grande del mundo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(383, 'El okapi', 'NATURALEZA', '¿Qué pariente de la jirafa vive en las selvas del Congo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(384, 'El nautilo', 'NATURALEZA', '¿Qué molusco de concha espiral se considera un fósil viviente?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(385, 'La rana de cristal', 'NATURALEZA', '¿Qué anfibio centroamericano tiene la piel ventral transparente?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(386, 'El drago de Icod', 'NATURALEZA', '¿Qué árbol milenario de Tenerife es símbolo de Canarias?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(387, 'El lince boreal', 'NATURALEZA', '¿Qué felino euroasiático es mayor que el lince ibérico?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(388, 'El kelp', 'NATURALEZA', '¿Cómo se llaman los bosques submarinos de algas pardas gigantes?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(389, 'El kiwi', 'NATURALEZA', '¿Qué ave nocturna no voladora es el emblema de Nueva Zelanda?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(390, 'El quebrantahuesos', 'NATURALEZA', '¿Qué buitre de los Pirineos se alimenta principalmente de huesos?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(391, 'El rendang', 'GASTRONOMÍA', '¿Qué estofado de carne y coco es típico de Sumatra?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(392, 'El nattō', 'GASTRONOMÍA', '¿Qué fermentado japonés de soja es viscoso y de sabor intenso?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(393, 'El cocido maragato', 'GASTRONOMÍA', '¿Qué cocido leonés se come al revés: primero la carne y luego la sopa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(394, 'El bánh mì', 'GASTRONOMÍA', '¿Qué bocadillo vietnamita nace de la baguette francesa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(395, 'El queso Cabrales', 'GASTRONOMÍA', '¿Qué queso azul asturiano se cura en cuevas de los Picos de Europa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(396, 'El feijoada', 'GASTRONOMÍA', '¿Qué guiso de alubias negras y cerdo es el plato nacional de Brasil?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(397, 'El stargazy pie', 'GASTRONOMÍA', '¿Qué empanada de Cornualles saca las cabezas de sardina por la tapa?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(398, 'El allioli', 'GASTRONOMÍA', '¿Qué salsa mediterránea emulsionaba tradicionalmente solo ajo y aceite?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(399, 'El injera con doro wat', 'GASTRONOMÍA', '¿Qué guiso etíope de pollo y huevo se come sobre injera?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0),
(400, 'El caldo de piedra', 'GASTRONOMÍA', '¿Qué sopa oaxaqueña se cocina echando piedras al rojo en el caldo?', 'aprobada', 'disponible', '_4NLS', NOW(6), 1, 0);


-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _5LS (ids 401-450) - difíciles
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(401, 'N''Djamena', 'GEOGRAFÍA', '¿Cuál es la capital de Chad?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(402, 'El pico Korzhenevskaya', 'GEOGRAFÍA', '¿Qué cinco mil del Pamir es el tercero más alto de Tayikistán?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(403, 'El lago Vostok', 'GEOGRAFÍA', '¿Qué lago subglacial antártico permaneció aislado millones de años?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(404, 'Funafuti', 'GEOGRAFÍA', '¿Cuál es la capital de Tuvalu?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(405, 'El salar de Uyuni', 'GEOGRAFÍA', '¿Cuál es el desierto de sal más grande del mundo?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(406, 'Gambia', 'GEOGRAFÍA', '¿Qué país africano es una franja casi completamente rodeada por Senegal?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(407, 'El río Lena', 'GEOGRAFÍA', '¿Qué río siberiano nace cerca del Baikal y forma un enorme delta ártico?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(408, 'Transnistria', 'GEOGRAFÍA', '¿Qué región separatista de Moldavia tiene capital en Tiraspol?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(409, 'Yaren', 'GEOGRAFÍA', '¿Cuál es el distrito que funciona como capital de Nauru?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(410, 'El monte Erebus', 'GEOGRAFÍA', '¿Qué volcán activo se alza en la isla de Ross, en la Antártida?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(411, 'La Paz de Augsburgo', 'HISTORIA', '¿Qué acuerdo de 1555 estableció el cuius regio, eius religio en el Imperio?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(412, 'Qin Shi Huang', 'HISTORIA', '¿Qué emperador unificó China y fue enterrado con un ejército de terracota?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(413, 'El cisma de Occidente', 'HISTORIA', '¿Cómo se llamó el periodo con papas en Roma y Aviñón a la vez?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(414, 'Túpac Katari', 'HISTORIA', '¿Qué líder aimara sitió La Paz en 1781 junto a Bartolina Sisa?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(415, 'La Liga del Peloponeso', 'HISTORIA', '¿Qué alianza encabezada por Esparta se enfrentó a Atenas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(416, 'Boudica', 'HISTORIA', '¿Qué reina de los icenos se alzó contra Roma en Britania?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(417, 'El Tratado de Utrecht', 'HISTORIA', '¿Qué tratados de 1713 pusieron fin a la Guerra de Sucesión Española?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(418, 'Sundiata Keita', 'HISTORIA', '¿Qué fundador del Imperio de Malí es el héroe de la epopeya mandinga?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(419, 'La Germanía', 'HISTORIA', '¿Qué revuelta valenciana de 1519-1523 enfrentó a agermanats y nobles?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(420, 'Taejong', 'HISTORIA', '¿Qué rey Joseon, padre de Sejong, consolidó la dinastía coreana a inicios del XV?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(421, 'Emmy Noether', 'CIENCIA', '¿Qué matemática demostró el teorema que relaciona simetrías y leyes de conservación?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(422, 'El gluón', 'CIENCIA', '¿Qué partícula media la interacción fuerte entre quarks?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(423, 'Lynn Margulis', 'CIENCIA', '¿Qué bióloga propuso la teoría endosimbiótica del origen de mitocondrias?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(424, 'El nucleolo', 'CIENCIA', '¿Qué región del núcleo fabrica las subunidades de los ribosomas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(425, 'La constante de Rydberg', 'CIENCIA', '¿Qué constante aparece en la fórmula de las líneas espectrales del hidrógeno?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(426, 'El osmio', 'CIENCIA', '¿Qué elemento tiene la mayor densidad de todos los estables?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(427, 'Henrietta Leavitt', 'CIENCIA', '¿Qué astrónoma descubrió la relación periodo-luminosidad de las cefeidas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(428, 'Los peroxisomas', 'CIENCIA', '¿Qué orgánulos degradan peróxido de hidrógeno con catalasa?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(429, 'El efecto Casimir', 'CIENCIA', '¿Qué fuerza aparece entre dos placas metálicas en el vacío cuántico?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(430, 'Henry Moseley', 'CIENCIA', '¿Qué físico británico ordenó la tabla periódica por número atómico con rayos X?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(431, 'Masaccio', 'ARTE', '¿Qué pintor florentino pintó La Trinidad con perspectiva lineal pionera?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(432, 'El suprematismo', 'ARTE', '¿Qué movimiento de Malevich reduce la pintura al cuadrado negro?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(433, 'Louis Sullivan', 'ARTE', '¿Qué arquitecto de Chicago acuñó la forma sigue a la función?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(434, 'Sofonisba Anguissola', 'ARTE', '¿Qué pintora cremonesa fue dama de Isabel de Valois en la corte española?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(435, 'El vorticismo', 'ARTE', '¿Qué vanguardia británica lideró Wyndham Lewis hacia 1914?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(436, 'Eva Hesse', 'ARTE', '¿Qué escultora de postminimalismo usó látex, fibra de vidrio y cuerdas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(437, 'Borobudur', 'ARTE', '¿Qué estupa budista de Java es el mayor monumento de su tipo?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(438, 'Barnett Newman', 'ARTE', '¿Qué pintor de color field es famoso por sus zips verticales?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(439, 'Luis Meléndez', 'ARTE', '¿Qué bodegonista dieciochesco español retrató con rigor frutas y cacharros?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(440, 'San Vital de Rávena', 'ARTE', '¿En qué iglesia bizantina aparecen los mosaicos de Justiniano y Teodora?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(441, 'Sor Juana Inés de la Cruz', 'LITERATURA', '¿Qué monja novohispana escribió Primero sueño?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(442, 'El libro de Kells', 'LITERATURA', '¿Qué evangeliario insular iluminado se conserva en Dublín?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(443, 'Machado de Assis', 'LITERATURA', '¿Qué escritor brasileño es autor de Memorias póstumas de Brás Cubas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(444, 'Las Nubes de Aristófanes', 'LITERATURA', '¿En qué comedia griega se satiriza a Sócrates y su pensadero?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(445, 'Thomas Bernhard', 'LITERATURA', '¿Qué escritor austriaco es autor de Extinción y Helada?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(446, 'El Libro de buen amor', 'LITERATURA', '¿Qué obra del Arcipreste de Hita mezcla mester de clerecía y juglaría?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(447, 'Naguib Mahfuz', 'LITERATURA', '¿Qué novelista egipcio ganó el Nobel en 1988?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(448, 'Misericordia', 'LITERATURA', '¿Qué novela de Galdós protagoniza la servienta Benina?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(449, 'Odiseas Elytis', 'LITERATURA', '¿Qué poeta griego ganó el Nobel en 1979?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(450, 'Tiempo de silencio', 'LITERATURA', '¿Qué novela de Martín-Santos inaugura la renovación narrativa de los 60?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0);


-- ------------------------------------------------------------
-- PREGUNTAS NIVEL _5NLS (ids 451-500) - muy difíciles
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(451, 'Ngerulmud', 'GEOGRAFÍA', '¿Cuál es la capital de Palaos, en la isla de Babeldaob?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(452, 'El Khan Tengri', 'GEOGRAFÍA', '¿Qué pico de más de 7.000 m marca la frontera de Kazajistán, Kirguistán y China?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(453, 'El lago Karachay', 'GEOGRAFÍA', '¿Qué lago ruso se considera uno de los lugares más radiactivos del mundo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(454, 'South Tarawa', 'GEOGRAFÍA', '¿Cuál es la capital de Kiribati?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(455, 'El desierto de Lut', 'GEOGRAFÍA', '¿Qué desierto iraní ha registrado las temperaturas de superficie más altas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(456, 'Esuatini', 'GEOGRAFÍA', '¿Cuál es el nombre actual del antiguo Suazilandia?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(457, 'El río Kolymá', 'GEOGRAFÍA', '¿Qué río del extremo oriente ruso da nombre a una infame región de gulags?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(458, 'Artsaj', 'GEOGRAFÍA', '¿Qué nombre armenio recibe la región de Nagorno Karabaj?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(459, 'The Valley', 'GEOGRAFÍA', '¿Cuál es la capital de Anguila?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(460, 'El monte Sidley', 'GEOGRAFÍA', '¿Cuál es el volcán más alto de la Antártida?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(461, 'La Pragmática Sanción de 1713', 'HISTORIA', '¿Qué norma de Carlos VI de Habsburgo reguló la sucesión austriaca?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(462, 'Kanishka', 'HISTORIA', '¿Qué emperador kushán impulsó el budismo en el siglo II?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(463, 'El concilio de Constanza', 'HISTORIA', '¿Qué concilio de 1414-1418 puso fin al Cisma de Occidente?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(464, 'Gonzalo Pizarro', 'HISTORIA', '¿Qué hermano de Francisco se alzó en Perú contra las Leyes Nuevas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(465, 'La Liga de Delos', 'HISTORIA', '¿Qué alianza naval ateniense se convirtió de facto en un imperio?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(466, 'Tomoe Gozen', 'HISTORIA', '¿Qué onna-bugeisha luchó en las guerras Genpei del siglo XII?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(467, 'El Tratado de Nystad', 'HISTORIA', '¿Qué paz de 1721 puso fin a la Gran Guerra del Norte?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(468, 'Askia Mohamed', 'HISTORIA', '¿Qué emperador songhai reorganizó el Estado tras derrotar a Sunni Ali?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(469, 'La revuelta de las Germanías de Mallorca', 'HISTORIA', '¿Qué alzamiento mallorquín de 1521 paralela a las Germanías valencianas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(470, 'Yi Sun-sin', 'HISTORIA', '¿Qué almirante coreano venció a Japón con los barcos tortuga en el XVI?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(471, 'Sofia Kovalevskaya', 'CIENCIA', '¿Qué matemática rusa fue la primera catedrática de Europa en su campo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(472, 'El gravitón', 'CIENCIA', '¿Qué partícula hipotética mediaría la interacción gravitatoria?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(473, 'Nettie Stevens', 'CIENCIA', '¿Qué genetista identificó los cromosomas sexuales XY?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(474, 'El espliceosoma', 'CIENCIA', '¿Qué complejo de ARN y proteínas elimina los intrones del pre-ARNm?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(475, 'La constante de Stefan-Boltzmann', 'CIENCIA', '¿Qué constante relaciona la potencia radiada de un cuerpo negro con T⁴?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(476, 'El iridio', 'CIENCIA', '¿Qué metal del grupo del platino da nombre a la capa de la extinción K-Pg?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(477, 'Cecilia Payne-Gaposchkin', 'CIENCIA', '¿Qué astrónoma concluyó que el Sol está compuesto sobre todo de hidrógeno?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(478, 'Los glioxisomas', 'CIENCIA', '¿Qué peroxisomas especializados de las plantas convierten lípidos en azúcares?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(479, 'El efecto Aharonov-Bohm', 'CIENCIA', '¿Qué fenómeno cuántico muestra que el potencial vector influye aunque B sea cero?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(480, 'Henry Moseley', 'CIENCIA', '¿Qué físico murió en Galípoli tras ordenar los elementos por número atómico?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(481, 'Cimabue', 'ARTE', '¿Qué pintor toscano es considerado puente entre el arte bizantino y Giotto?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(482, 'El rayonismo', 'ARTE', '¿Qué vanguardia rusa de Larionov y Goncharova pintaba rayos de luz?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(483, 'Adolf Loos', 'ARTE', '¿Qué arquitecto vienés escribió Ornamento y delito?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(484, 'Lavinia Fontana', 'ARTE', '¿Qué pintora boloñesa del XVI recibió encargos de retrato de la nobleza?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(485, 'El precisionismo', 'ARTE', '¿Qué corriente estadounidense de Sheeler y Demuth geometrizó fábricas y silos?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(486, 'Lee Krasner', 'ARTE', '¿Qué pintora del expresionismo abstracto fue esposa de Jackson Pollock?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(487, 'Prambanan', 'ARTE', '¿Qué complejo hindú de Java central se alza cerca de Yogyakarta?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(488, 'Clyfford Still', 'ARTE', '¿Qué pionero del color field pintó grandes planos rasgados de color?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(489, 'Juan Sánchez Cotán', 'ARTE', '¿Qué cartujo toledano es maestro del bodegón de despensa en nicho?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(490, 'Hagia Irene', 'ARTE', '¿Qué iglesia bizantina de Estambul conservó su atrio y no se convirtió en mezquita?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(491, 'Marcela de San Félix', 'LITERATURA', '¿Qué hija de Lope de Vega escribió loas y coloquios desde el convento?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(492, 'El códice Calixtinus', 'LITERATURA', '¿Qué manuscrito compostelano guía a los peregrinos del Camino de Santiago?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(493, 'Lima Barreto', 'LITERATURA', '¿Qué escritor brasileño es autor de Triste fin de Policarpo Quaresma?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(494, 'Las Tesmoforias', 'LITERATURA', '¿En qué comedia de Aristófanes los hombres se disfrazan para una fiesta de mujeres?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(495, 'Ingeborg Bachmann', 'LITERATURA', '¿Qué escritora austriaca es autora de Malina?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(496, 'El Conde Lucanor', 'LITERATURA', '¿Qué colección de exempla escribió don Juan Manuel en el XIV?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(497, 'Yasunari Kawabata', 'LITERATURA', '¿Qué novelista japonés ganó el Nobel con obras como País de nieve?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(498, 'Miau', 'LITERATURA', '¿Qué novela de Galdós sigue al cesante Villaamil en el Madrid burocrático?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(499, 'Giorgos Seferis', 'LITERATURA', '¿Qué poeta griego ganó el Nobel en 1963?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(500, 'Señas de identidad', 'LITERATURA', '¿Qué novela de Juan Goytisolo inaugura su trilogía del exilio?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0);



-- ------------------------------------------------------------
-- PREGUNTAS extra de nivel 5 para llenar 75 combos (ids 501-625)
-- 501-561 _5LS (61) + 562-625 _5NLS (64) = 125
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(501, 'Ouagadougou', 'GEOGRAFÍA', '¿Cuál es la capital de Burkina Faso?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(502, 'El monte Ararat', 'GEOGRAFÍA', '¿Qué monte de Turquía es el símbolo nacional de Armenia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(503, 'El mar de Mármara', 'GEOGRAFÍA', '¿Qué mar interior une el Bósforo con los Dardanelos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(504, 'Port Moresby', 'GEOGRAFÍA', '¿Cuál es la capital de Papúa Nueva Guinea?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(505, 'El desierto de Kalahari', 'GEOGRAFÍA', '¿Qué desierto cubre gran parte de Botsuana?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(506, 'Malaui', 'GEOGRAFÍA', '¿Qué país africano tiene como capital Lilongüe, junto al lago Nyasa?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(507, 'El río Paraguay', 'GEOGRAFÍA', '¿Qué río da nombre al país y atraviesa Asunción?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(508, 'Abjasia', 'GEOGRAFÍA', '¿Qué región del Cáucaso tiene capital en Sujumi y se separó de Georgia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(509, 'Basseterre', 'GEOGRAFÍA', '¿Cuál es la capital de San Cristóbal y Nieves?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(510, 'El monte Kosciuszko', 'GEOGRAFÍA', '¿Cuál es el pico más alto de Australia continental?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(511, 'La Dieta de Worms', 'HISTORIA', '¿En qué asamblea de 1521 Lutero se negó a retractarse ante Carlos V?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(512, 'Ciro el Grande', 'HISTORIA', '¿Qué rey persa fundó el Imperio aqueménida y liberó a los judíos de Babilonia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(513, 'El concilio de Nicea', 'HISTORIA', '¿Qué concilio de 325 condenó el arrianismo y formuló el credo?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(514, 'Lautaro', 'HISTORIA', '¿Qué toqui mapuche derrotó a Valdivia en Tucapel?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(515, 'La Liga Aquea', 'HISTORIA', '¿Qué confederación del Peloponeso se enfrentó a Roma en el siglo II a. C.?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(516, 'Tomyris', 'HISTORIA', '¿Qué reina masaeta venció según Heródoto a Ciro el Grande?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(517, 'El Tratado de Karlowitz', 'HISTORIA', '¿Qué paz de 1699 marcó el retroceso otomano ante el Sacro Imperio?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(518, 'Osei Tutu', 'HISTORIA', '¿Qué asantehene unificó el Imperio asante con el taburete de oro?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(519, 'La rebelión de Hogen', 'HISTORIA', '¿Qué conflicto de 1156 en Kioto anunció el auge de los samuráis?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(520, 'Nurhaci', 'HISTORIA', '¿Qué jefe yurchen unificó las tribus que darían paso a la dinastía Qing?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(521, 'Hypatia', 'CIENCIA', '¿Qué matemática de Alejandría fue asesinada en el siglo V?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(522, 'El muon', 'CIENCIA', '¿Qué leptón es unos 200 veces más pesado que el electrón?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(523, 'Chien-Shiung Wu', 'CIENCIA', '¿Qué física demostró experimentalmente la violación de paridad?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(524, 'El centrosoma', 'CIENCIA', '¿Qué orgánulo organiza los microtúbulos y contiene los centriolos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(525, 'La constante de Boltzmann', 'CIENCIA', '¿Qué constante relaciona la energía térmica con la temperatura absoluta?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(526, 'El rodio', 'CIENCIA', '¿Qué metal del platino de símbolo Rh es de los más raros de la corteza?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(527, 'Vera Rubin', 'CIENCIA', '¿Qué astrónoma aportó pruebas clave de la materia oscura en galaxias?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(528, 'Los ribosomas', 'CIENCIA', '¿Qué complejos de ARN y proteína sintetizan las cadenas polipeptídicas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(529, 'El efecto Meissner', 'CIENCIA', '¿Qué fenómeno expulsa el campo magnético del interior de un superconductor?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(530, 'Amedeo Avogadro', 'CIENCIA', '¿Qué físico enunció que volúmenes iguales de gas contienen el mismo número de moléculas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(531, 'Duccio', 'ARTE', '¿Qué pintor sienés es autor de la Maestà de la catedral de Siena?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(532, 'El constructivismo', 'ARTE', '¿Qué vanguardia soviética de Tatlin y Rodchenko unió arte e industria?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(533, 'Alvar Aalto', 'ARTE', '¿Qué arquitecto finlandés diseñó el sanatorio de Paimio?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(534, 'Elisabetta Sirani', 'ARTE', '¿Qué pintora boloñesa del XVII dirigió un taller y formó a otras mujeres?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(535, 'El dadaísmo', 'ARTE', '¿Qué movimiento de Zúrich en 1916 lideraron Tzara y Ball en el Cabaret Voltaire?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(536, 'Nancy Spero', 'ARTE', '¿Qué artista feminista llenó frisos de figuras femeninas y textos políticos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(537, 'Sanchi', 'ARTE', '¿Qué gran estupa budista de la India mandó ampliar Ashoka?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(538, 'Helen Frankenthaler', 'ARTE', '¿Qué pintora desarrolló la técnica soak stain del color field?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(539, 'Juan van der Hamen', 'ARTE', '¿Qué bodegonista madrileño del XVII es famoso por sus estantes de dulces?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(540, 'San Apolinar in Classe', 'ARTE', '¿Qué basílica de Rávena muestra mosaicos de procesiones de corderos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(541, 'Garcilaso de la Vega', 'LITERATURA', '¿Qué poeta toledano naturalizó el soneto petrarquista en castellano?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(542, 'El Beato de Liébana', 'LITERATURA', '¿Qué comentario al Apocalipsis se copió en célebres manuscritos mozárabes?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(543, 'João Guimarães Rosa', 'LITERATURA', '¿Qué escritor brasileño es autor de Gran sertón: veredas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(544, 'Las Avispas', 'LITERATURA', '¿En qué comedia de Aristófanes un hijo encierra a su padre adicto a los jurados?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(545, 'Robert Musil', 'LITERATURA', '¿Quién escribió El hombre sin atributos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(546, 'Las Coplas a la muerte de su padre', 'LITERATURA', '¿Qué elegía de Jorge Manrique recuerda que nuestras vidas son los ríos?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(547, 'Octavio Paz', 'LITERATURA', '¿Qué poeta mexicano ganó el Nobel y escribió El laberinto de la soledad?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(548, 'Nazarín', 'LITERATURA', '¿Qué novela de Galdós sigue a un sacerdote quijotesco por Castilla?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(549, 'Czesław Miłosz', 'LITERATURA', '¿Qué poeta polaco ganó el Nobel en 1980?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(550, 'Reivindicación del conde don Julián', 'LITERATURA', '¿Qué novela de Goytisolo ataca el mito de la traición de la Cava?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(551, 'El birdie', 'DEPORTES', '¿Cómo se llama en golf un resultado de un golpe bajo par?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(552, 'Gino Bartali', 'DEPORTES', '¿Qué ciclista toscano rivalizó con Coppi y ayudó a la Resistencia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(553, 'Josh Gibson', 'DEPORTES', '¿Qué catcher de las Ligas Negras era apodado el Babe Ruth negro?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(554, 'El tetratlón', 'DEPORTES', '¿Qué prueba combina cuatro disciplinas, a menudo natación y carrera?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(555, 'Gilles Villeneuve', 'DEPORTES', '¿Qué piloto canadiense de Ferrari murió en Zolder en 1982?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(556, 'El ruck', 'DEPORTES', '¿Cómo se llama en rugby la disputa del balón en el suelo tras el placaje?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(557, 'Vera Čáslavská', 'DEPORTES', '¿Qué gimnasta checa deslumbró en México 68 y protestó contra la invasión soviética?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(558, 'Ibrox', 'DEPORTES', '¿Cuál es el estadio del Rangers de Glasgow?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(559, 'El luge', 'DEPORTES', '¿Qué deporte olímpico desciende en trineo tumbado boca arriba?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(560, 'Margaret Court', 'DEPORTES', '¿Qué tenista australiana ostenta 24 títulos de Grand Slam individuales?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(561, 'Kenji Mizoguchi', 'CINE', '¿Qué director japonés rodó Cuentos de la luna pálida después de la lluvia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(562, 'Nouakchott', 'GEOGRAFÍA', '¿Cuál es la capital de Mauritania?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(563, 'El pico Communism', 'GEOGRAFÍA', '¿Cómo se llamó en la URSS el actual pico Ismail Samani, techo de Tayikistán?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(564, 'El golfo de Bothnia', 'GEOGRAFÍA', '¿Qué golfo separa Suecia de Finlandia en el Báltico?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(565, 'Honiara', 'GEOGRAFÍA', '¿Cuál es la capital de las Islas Salomón?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(566, 'El desierto de Taklamakán', 'GEOGRAFÍA', '¿Qué desierto de Xinjiang atraviesa la Ruta de la Seda?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(567, 'Yibuti', 'GEOGRAFÍA', '¿Qué pequeño país del Cuerno de África controla la entrada al mar Rojo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(568, 'El río Ubangi', 'GEOGRAFÍA', '¿Qué afluente del Congo sirve de frontera a la RDC y la República Centroafricana?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(569, 'Osetia del Sur', 'GEOGRAFÍA', '¿Qué región caucásica tiene capital en Tsjinvali?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(570, 'Castries', 'GEOGRAFÍA', '¿Cuál es la capital de Santa Lucía?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(571, 'El monte Cook', 'GEOGRAFÍA', '¿Cuál es el pico más alto de Nueva Zelanda, Aoraki?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(572, 'La Paz de Teschen', 'HISTORIA', '¿Qué tratado de 1779 cerró la Guerra de Sucesión bávara?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(573, 'Darío I', 'HISTORIA', '¿Qué rey persa reorganizó el imperio en sátrapas y fue vencido en Maratón?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(574, 'El concilio de Calcedonia', 'HISTORIA', '¿Qué concilio de 451 definió las dos naturalezas de Cristo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(575, 'Caupolicán', 'HISTORIA', '¿Qué toqui mapuche sucedió a Lautaro según Alonso de Ercilla?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(576, 'La Liga Etolia', 'HISTORIA', '¿Qué confederación griega del norte rivalizó con la Liga Aquea?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(577, 'Artemisia I', 'HISTORIA', '¿Qué reina de Halicarnaso combatió con Jerjes en Salamina?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(578, 'El Tratado de Küçük Kaynarca', 'HISTORIA', '¿Qué paz de 1774 dio a Rusia acceso al mar Negro frente a los otomanos?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(579, 'Shaka Zulu', 'HISTORIA', '¿Qué rey zulú reorganizó el ejército con el impi y el iklwa en el XIX?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(580, 'La rebelión de Heiji', 'HISTORIA', '¿Qué guerra de 1160 en Kioto enfrentó a los Taira y los Minamoto?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(581, 'Kangxi', 'HISTORIA', '¿Qué emperador Qing gobernó 61 años y consolidó el imperio en el XVII-XVIII?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(582, 'Sophie Germain', 'CIENCIA', '¿Qué matemática francesa trabajó la elasticidad y los números primos que llevan su nombre?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(583, 'El tauón', 'CIENCIA', '¿Qué leptón, el más pesado, descubrió Martin Perl?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(584, 'Maria Goeppert Mayer', 'CIENCIA', '¿Qué física compartió el Nobel por el modelo de capas del núcleo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(585, 'El citoesqueleto', 'CIENCIA', '¿Qué red de filamentos da forma a la célula y dirige el tráfico interno?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(586, 'La constante de estructura fina', 'CIENCIA', '¿Qué constante adimensional vale unos 1/137 y regula el electromagnetismo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(587, 'El rutenio', 'CIENCIA', '¿Qué metal del platino tiene el símbolo Ru?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(588, 'Jocelyn Bell Burnell', 'CIENCIA', '¿Qué astrónoma descubrió los púlsares, aunque el Nobel fue a su director?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(589, 'Los cloroplastos', 'CIENCIA', '¿En qué orgánulos de las plantas ocurre la fase lumínica de la fotosíntesis?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(590, 'El efecto Josephson', 'CIENCIA', '¿Qué túnel de pares de Cooper entre superconductores se usa en SQUID?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(591, 'Svante Arrhenius', 'CIENCIA', '¿Qué químico sueco relacionó la velocidad de reacción con la temperatura?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(592, 'Simone Martini', 'ARTE', '¿Qué pintor gótico sienés es autor de la Anunciación de los Uffizi?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(593, 'El productivismo', 'ARTE', '¿Qué deriva del constructivismo soviético quiso disolver el arte en la fábrica?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(594, 'Gunnar Asplund', 'ARTE', '¿Qué arquitecto sueco diseñó el cementerio Woodland de Estocolmo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(595, 'Giovanna Garzoni', 'ARTE', '¿Qué miniaturista italiana del XVII pintó bodegones sobre pergamino?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(596, 'El merz', 'ARTE', '¿Cómo llamó Kurt Schwitters a su collage y a su merzbau de Hannover?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(597, 'Carolee Schneemann', 'ARTE', '¿Qué artista de performance es autora de Interior Scroll?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(598, 'Ellora', 'ARTE', '¿Qué conjunto rupestre de la India mezcla templos hindúes, jainas y budistas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(599, 'Morris Louis', 'ARTE', '¿Qué pintor del Washington Color School es famoso por sus veils?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(600, 'Antonio de Pereda', 'ARTE', '¿Qué pintor madrileño es autor de El sueño del caballero?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(601, 'San Apolinar Nuevo', 'ARTE', '¿Qué basílica de Rávena muestra el palacio de Teodorico en mosaico?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(602, 'Fernando de Herrera', 'LITERATURA', '¿Qué poeta sevillano del XVI fue apodado el Divino?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(603, 'Las Siete Partidas', 'LITERATURA', '¿Qué código de Alfonso X mezcla derecho y literatura didáctica?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(604, 'Graciliano Ramos', 'LITERATURA', '¿Qué escritor brasileño es autor de Vidas secas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(605, 'Los Caballeros', 'LITERATURA', '¿En qué comedia de Aristófanes un embutidero vence a Cleón?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(606, 'Hermann Broch', 'LITERATURA', '¿Quién escribió La muerte de Virgilio?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(607, 'El Laberinto de Fortuna', 'LITERATURA', '¿Qué poema de Juan de Mena alegoriza la rueda de la fortuna?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(608, 'Carlos Fuentes', 'LITERATURA', '¿Qué escritor mexicano es autor de La región más transparente?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(609, 'Ángel Guerra', 'LITERATURA', '¿Qué novela de Galdós ambientada en Toledo sigue a un revolucionario místico?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(610, 'Tadeusz Różewicz', 'LITERATURA', '¿Qué poeta polaco de la posguerra escribió La supervivencia?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(611, 'Juan sin Tierra', 'LITERATURA', '¿Qué novela de Goytisolo cierra la trilogía iniciada con Señas de identidad?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(612, 'El eagle', 'DEPORTES', '¿Cómo se llama en golf un resultado de dos golpes bajo par?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(613, 'Jacques Anquetil', 'DEPORTES', '¿Qué ciclista francés fue el primero en ganar cinco Tours?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(614, 'Cool Papa Bell', 'DEPORTES', '¿Qué jardinero de las Ligas Negras era legendario por su velocidad?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(615, 'El duatlón', 'DEPORTES', '¿Qué prueba combina carrera, ciclismo y otra carrera, sin natación?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(616, 'Ronnie Peterson', 'DEPORTES', '¿Qué piloto sueco de F1 falleció tras el accidente de Monza 1978?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(617, 'El maul', 'DEPORTES', '¿Cómo se llama en rugby el avance conjunto con el balón en mano, en pie?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(618, 'Larisa Latynina', 'DEPORTES', '¿Qué gimnasta soviética ostentó el récord de medallas olímpicas durante décadas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(619, 'Celtic Park', 'DEPORTES', '¿Cuál es el estadio del Celtic de Glasgow?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(620, 'El skeleton', 'DEPORTES', '¿Qué deporte olímpico desciende en trineo tumbado boca abajo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(621, 'Maureen Connolly', 'DEPORTES', '¿Qué tenista estadounidense, Little Mo, logró el Grand Slam en 1953?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(622, 'Mikio Naruse', 'CINE', '¿Qué director japonés rodó Nubes flotantes y El rumor de la montaña?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(623, 'La regla del juego', 'CINE', '¿Qué filme de Renoir de 1939 disecciona a la burguesía francesa?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(624, 'José Val del Omar', 'CINE', '¿Qué cineasta granadino concibió el desbordamiento apanorámico?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(625, 'Tierra sin pan', 'CINE', '¿Qué documental de Buñuel retrata Las Hurdes en 1933?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0);

-- ------------------------------------------------------------
-- 50 PREGUNTAS LIBRES DE NIVEL 5 (aprobadas, para crear combos nuevos)
-- 626-650 _5LS (25) + 651-675 _5NLS (25)
-- ------------------------------------------------------------
INSERT INTO preguntas (id, respuesta, tematica, pregunta, estado, estado_disponibilidad, nivel, fecha_creacion, creacion_usuario_id, version) VALUES
(626, 'Asmara', 'GEOGRAFÍA', '¿Cuál es la capital de Eritrea?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(627, 'El monte Roraima', 'GEOGRAFÍA', '¿Qué tepuy marca la triple frontera de Venezuela, Brasil y Guyana?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(628, 'El mar de Andamán', 'GEOGRAFÍA', '¿Qué mar baña la costa oeste de Tailandia y el sur de Myanmar?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(629, 'Bandar Seri Begawan', 'GEOGRAFÍA', '¿Cuál es la capital de Brunéi?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(630, 'El desierto de Namib', 'GEOGRAFÍA', '¿Qué desierto costero africano es uno de los más antiguos del mundo?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(631, 'La Paz de Westfalia', 'HISTORIA', '¿Qué tratados de 1648 pusieron fin a la Guerra de los Treinta Años?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(632, 'Hatshepsut', 'HISTORIA', '¿Qué faraona de la XVIII dinastía gobernó Egipto disfrazada de hombre?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(633, 'El edicto de Nantes', 'HISTORIA', '¿Qué edicto de 1598 concedió libertad de culto a los hugonotes?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(634, 'Toussaint Louverture', 'HISTORIA', '¿Qué líder encabezó la revolución haitiana contra Francia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(635, 'La Liga Hanseática', 'HISTORIA', '¿Qué liga comercial de ciudades del Báltico dominó el norte de Europa medieval?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(636, 'Lise Meitner', 'CIENCIA', '¿Qué física austriaca explicó la fisión nuclear junto a Otto Hahn?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(637, 'El neutrino', 'CIENCIA', '¿Qué partícula casi sin masa predijo Pauli y atraviesa la materia con facilidad?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(638, 'Rosalind Franklin', 'CIENCIA', '¿Qué científica obtuvo la foto 51 del ADN que usaron Watson y Crick?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(639, 'El retículo endoplasmático', 'CIENCIA', '¿Qué orgánulo celular sintetiza proteínas y lípidos en una red de membranas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(640, 'La constante de Planck', 'CIENCIA', '¿Qué constante relaciona la energía de un fotón con su frecuencia?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(641, 'Piero della Francesca', 'ARTE', '¿Qué pintor de Sansepolcro es autor de La leyenda de la Vera Cruz?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(642, 'El neoplasticismo', 'ARTE', '¿Qué movimiento de Mondrian reduce el arte a líneas y colores primarios?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(643, 'Alvar Aalto', 'ARTE', '¿Qué arquitecto finlandés diseñó el sanatorio de Paimio?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(644, 'Artemisia Gentileschi', 'ARTE', '¿Qué pintora barroca es autora de Judit decapitando a Holofernes?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(645, 'El constructivismo', 'ARTE', '¿Qué vanguardia soviética de Tatlin quiso unir arte e ingeniería?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(646, 'Santa Teresa de Jesús', 'LITERATURA', '¿Qué mística abulense escribió El Castillo interior o Las moradas?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(647, 'El Cantar de Mio Cid', 'LITERATURA', '¿Qué cantar de gesta castellano narra el destierro de Rodrigo Díaz de Vivar?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(648, 'Clarice Lispector', 'LITERATURA', '¿Qué escritora brasileña es autora de La pasión según G.H.?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(649, 'Las Ranas', 'LITERATURA', '¿En qué comedia de Aristófanes Dioniso baja al Hades a buscar un poeta?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(650, 'W. G. Sebald', 'LITERATURA', '¿Qué escritor alemán es autor de Austerlitz y Los anillos de Saturno?', 'aprobada', 'disponible', '_5LS', NOW(6), 1, 0),
(651, 'Dili', 'GEOGRAFÍA', '¿Cuál es la capital de Timor Oriental?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(652, 'El monte Kailash', 'GEOGRAFÍA', '¿Qué pico del Tíbet es sagrado para hinduistas, budistas y jainas?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(653, 'El mar de Aral', 'GEOGRAFÍA', '¿Qué lago de Asia Central se redujo drásticamente por el riego del algodón soviético?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(654, 'Naypyidaw', 'GEOGRAFÍA', '¿Cuál es la capital administrativa de Myanmar desde 2005?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(655, 'El desierto de Gobi', 'GEOGRAFÍA', '¿Qué desierto se extiende por Mongolia y el norte de China?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(656, 'La Paz de Cateau-Cambrésis', 'HISTORIA', '¿Qué tratado de 1559 cerró las guerras entre España y Francia en Italia?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(657, 'Akhenatón', 'HISTORIA', '¿Qué faraón impuso el culto a Atón y fundó Amarna?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(658, 'El edicto de Milán', 'HISTORIA', '¿Qué edicto de 313 legalizó el cristianismo en el Imperio romano?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(659, 'Nzinga de Ndongo', 'HISTORIA', '¿Qué reina angoleña resistió la expansión portuguesa en el siglo XVII?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(660, 'La Liga Aquea', 'HISTORIA', '¿Qué confederación del Peloponeso rivalizó con Macedonia en el siglo II a.C.?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(661, 'Ada Lovelace', 'CIENCIA', '¿Qué matemática escribió las notas al motor analítico de Babbage, consideradas el primer algoritmo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(662, 'El bosón de Higgs', 'CIENCIA', '¿Qué partícula da masa a otras partículas según el modelo estándar?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(663, 'Barbara McClintock', 'CIENCIA', '¿Qué genetista descubrió los transposones o genes saltarines en el maíz?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(664, 'El aparato de Golgi', 'CIENCIA', '¿Qué orgánulo empaqueta y modifica proteínas para su secreción?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(665, 'La constante de Avogadro', 'CIENCIA', '¿Qué constante indica el número de partículas en un mol?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(666, 'Duccio di Buoninsegna', 'ARTE', '¿Qué pintor sienés es autor de la Maestà de la catedral de Siena?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(667, 'El orfismo', 'ARTE', '¿Qué deriva del cubismo de Delaunay se centra en círculos de color y luz?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(668, 'Carlo Scarpa', 'ARTE', '¿Qué arquitecto veneciano reformó el Museo Castelvecchio de Verona?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(669, 'Élisabeth Vigée Le Brun', 'ARTE', '¿Qué retratista francesa fue pintora de María Antonieta?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(670, 'El dadaísmo', 'ARTE', '¿Qué movimiento de Cabaret Voltaire en Zúrich usó el absurdo contra la guerra?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(671, 'San Juan de la Cruz', 'LITERATURA', '¿Qué místico carmelita escribió Noche oscura y Cántico espiritual?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(672, 'El Libro de Alexandre', 'LITERATURA', '¿Qué poema de clerecía narra la vida de Alejandro Magno en cuaderna vía?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(673, 'Jorge Amado', 'LITERATURA', '¿Qué novelista bahiano es autor de Gabriela, clavo y canela?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(674, 'Las Aves', 'LITERATURA', '¿En qué comedia de Aristófanes dos atenienses fundan Nefelococcia en el cielo?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0),
(675, 'Elfriede Jelinek', 'LITERATURA', '¿Qué escritora austriaca ganó el Nobel en 2004 y es autora de La pianista?', 'aprobada', 'disponible', '_5NLS', NOW(6), 1, 0);

-- ------------------------------------------------------------
-- 75 CUESTIONARIOS
-- Cada uno lleva 4 preguntas: n, 100+n, 200+n, 300+n (niveles 1-4).
-- Estados: 1-25 grabado, 26-55 aprobado, 56-63 revisar, 64-68 corregir, 69-75 borrador.
-- ------------------------------------------------------------
INSERT INTO cuestionarios (id, creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version)
SELECT
    n,
    1,
    NOW(6),
    CASE
        WHEN n <= 25 THEN 'grabado'
        WHEN n <= 55 THEN 'aprobado'
        WHEN n <= 63 THEN 'revisar'
        WHEN n <= 68 THEN 'corregir'
        ELSE 'borrador'
    END,
    'NORMAL',
    CASE MOD(n, 10)
        WHEN 1 THEN 'GEOGRAFÍA'
        WHEN 2 THEN 'HISTORIA'
        WHEN 3 THEN 'CIENCIA'
        WHEN 4 THEN 'ARTE'
        WHEN 5 THEN 'LITERATURA'
        WHEN 6 THEN 'DEPORTES'
        WHEN 7 THEN 'CINE'
        WHEN 8 THEN 'MÚSICA'
        WHEN 9 THEN 'NATURALEZA'
        ELSE 'GASTRONOMÍA'
    END,
    CONCAT('Cuestionario de prueba ', n),
    0
FROM tmp_seq
WHERE n <= 75;

INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion)
SELECT s.n, o.base + s.n, 1
FROM tmp_seq s
CROSS JOIN (
    SELECT 0 AS base UNION ALL SELECT 100 UNION ALL SELECT 200 UNION ALL SELECT 300
) o
WHERE s.n <= 75;

-- ------------------------------------------------------------
-- 75 COMBOS LLENOS (3 preguntas cada uno)
-- 1-37  nivel _5LS  : preguntas 401-450 y 501-561
-- 38-75 nivel _5NLS : preguntas 451-500 y 562-625
-- Estados: 1-25 grabado, 26-75 aprobado
-- ------------------------------------------------------------
INSERT INTO combos (id, creacion_usuario_id, fecha_creacion, estado, nivel, tipo, tematica, notas_direccion, version)
SELECT
    n,
    1,
    NOW(6),
    CASE
        WHEN n <= 25 THEN 'grabado'
        ELSE 'aprobado'
    END,
    CASE
        WHEN n <= 37 THEN '_5LS'
        ELSE '_5NLS'
    END,
    CASE MOD(n, 4)
        WHEN 1 THEN 'P'
        WHEN 2 THEN 'A'
        WHEN 3 THEN 'D'
        ELSE 'R'
    END,
    CASE MOD(n, 10)
        WHEN 1 THEN 'GEOGRAFÍA'
        WHEN 2 THEN 'HISTORIA'
        WHEN 3 THEN 'CIENCIA'
        WHEN 4 THEN 'ARTE'
        WHEN 5 THEN 'LITERATURA'
        WHEN 6 THEN 'DEPORTES'
        WHEN 7 THEN 'CINE'
        WHEN 8 THEN 'MÚSICA'
        WHEN 9 THEN 'NATURALEZA'
        ELSE 'GASTRONOMÍA'
    END,
    CONCAT('Combo de prueba ', n),
    0
FROM tmp_seq
WHERE n <= 75;

-- Combos 1-37 (_5LS): idx 1-111 -> 401-450, luego 501-561
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion, posicion)
SELECT
    n,
    CASE
        WHEN ((n - 1) * 3 + pos) <= 50 THEN 400 + ((n - 1) * 3 + pos)
        ELSE 500 + (((n - 1) * 3 + pos) - 50)
    END,
    CASE pos WHEN 1 THEN '2' WHEN 2 THEN '3' ELSE 'X' END,
    pos
FROM tmp_seq
CROSS JOIN (SELECT 1 AS pos UNION ALL SELECT 2 UNION ALL SELECT 3) p
WHERE n BETWEEN 1 AND 37;

-- Combos 38-75 (_5NLS): idx 1-114 -> 451-500, luego 562-625
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion, posicion)
SELECT
    n,
    CASE
        WHEN (((n - 38) * 3) + pos) <= 50 THEN 450 + (((n - 38) * 3) + pos)
        ELSE 561 + ((((n - 38) * 3) + pos) - 50)
    END,
    CASE pos WHEN 1 THEN '2' WHEN 2 THEN '3' ELSE 'X' END,
    pos
FROM tmp_seq
CROSS JOIN (SELECT 1 AS pos UNION ALL SELECT 2 UNION ALL SELECT 3) p
WHERE n BETWEEN 38 AND 75;

-- ------------------------------------------------------------
-- 5 JORNADAS (5 concursantes por jornada)
-- ------------------------------------------------------------
INSERT INTO jornadas (id, nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas, version)
VALUES
(1, 'Jornada Madrid', DATE_SUB(CURDATE(), INTERVAL 8 DAY), 'Madrid', 'en_grabacion', 1, NOW(6), 'Grabación en plató de Madrid', 0),
(2, 'Jornada Barcelona', DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'Barcelona', 'en_grabacion', 1, NOW(6), 'Grabación en Barcelona', 0),
(3, 'Jornada Valencia', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'Valencia', 'en_grabacion', 1, NOW(6), 'Grabación en Valencia', 0),
(4, 'Jornada Sevilla', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Sevilla', 'en_grabacion', 1, NOW(6), 'Grabación en Sevilla', 0),
(5, 'Jornada Bilbao', CURDATE(), 'Bilbao', 'en_grabacion', 1, NOW(6), 'Grabación en Bilbao', 0);

-- Cuestionarios y combos 1-25 (grabados / asignados a concursante) van a las 5 jornadas
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id)
SELECT CEIL(n / 5), n FROM tmp_seq WHERE n <= 25;

INSERT INTO jornadas_combos (jornada_id, combo_id)
SELECT CEIL(n / 5), n FROM tmp_seq WHERE n <= 25;

-- ------------------------------------------------------------
-- 25 CONCURSANTES (cuestionario y combo del mismo número)
-- Estado inicial: grabado (ya no existe borrador).
-- ------------------------------------------------------------
INSERT INTO concursantes (
    id, numero_concursante, jornada_id, dia_grabacion, lugar, nombre, edad, ocupacion, redes_sociales,
    cuestionario_id, combo_id, xusoker, resultado, notas_grabacion, guionista, valoracion_guionista,
    estado, momentos_destacados, duracion, valoracion_final, version
)
SELECT
    n,
    n,
    CEIL(n / 5),
    DATE_SUB(CURDATE(), INTERVAL (5 - CEIL(n / 5)) DAY),
    CASE CEIL(n / 5)
        WHEN 1 THEN 'Madrid'
        WHEN 2 THEN 'Barcelona'
        WHEN 3 THEN 'Valencia'
        WHEN 4 THEN 'Sevilla'
        ELSE 'Bilbao'
    END,
    CASE n
        WHEN 1 THEN 'Laura Méndez'
        WHEN 2 THEN 'Carlos Ruiz'
        WHEN 3 THEN 'Marta Soler'
        WHEN 4 THEN 'Andrés Vega'
        WHEN 5 THEN 'Nuria Campos'
        WHEN 6 THEN 'Pablo Ortega'
        WHEN 7 THEN 'Elena Navarro'
        WHEN 8 THEN 'Javier Molina'
        WHEN 9 THEN 'Sara Iglesias'
        WHEN 10 THEN 'Diego Herrera'
        WHEN 11 THEN 'Lucía Romero'
        WHEN 12 THEN 'Hugo Castillo'
        WHEN 13 THEN 'Irene Pascual'
        WHEN 14 THEN 'Álvaro Gil'
        WHEN 15 THEN 'Carmen Soto'
        WHEN 16 THEN 'Raúl Delgado'
        WHEN 17 THEN 'Patricia León'
        WHEN 18 THEN 'Sergio Blanco'
        WHEN 19 THEN 'Inés Prieto'
        WHEN 20 THEN 'Manuel Cruz'
        WHEN 21 THEN 'Alicia Ferrer'
        WHEN 22 THEN 'Tomás Aguilar'
        WHEN 23 THEN 'Beatriz Lozano'
        WHEN 24 THEN 'Iván Cabrera'
        ELSE 'Rocío Peña'
    END,
    22 + n,
    CASE MOD(n, 5)
        WHEN 1 THEN 'Profesora'
        WHEN 2 THEN 'Ingeniero'
        WHEN 3 THEN 'Periodista'
        WHEN 4 THEN 'Enfermera'
        ELSE 'Arquitecto'
    END,
    CONCAT('@concursante', n),
    n,
    n,
    CASE MOD(n, 5)
        WHEN 1 THEN 'NO USÓ'
        WHEN 2 THEN 'CONTINÚE'
        WHEN 3 THEN 'AL VERRÉS'
        WHEN 4 THEN 'RECICLA'
        ELSE 'LLAMADA'
    END,
    0,
    CONCAT('Notas de grabación del concursante ', n),
    'admin',
    '2',
    'grabado',
    CONCAT('Momento destacado ', n),
    CONCAT(
        LPAD(10 + FLOOR(RAND() * 11), 2, '0'),
        ':',
        LPAD(FLOOR(RAND() * 60), 2, '0')
    ),
    '2',
    0
FROM tmp_seq
WHERE n <= 25;

-- ------------------------------------------------------------
-- Coherencia de estados de preguntas
-- ------------------------------------------------------------
UPDATE preguntas p
JOIN (
    SELECT pregunta_id FROM cuestionarios_preguntas
    UNION
    SELECT pregunta_id FROM combos_preguntas
) u ON p.id = u.pregunta_id
SET p.estado = 'usada',
    p.estado_disponibilidad = 'usada'
WHERE p.id BETWEEN 1 AND 675;

UPDATE preguntas p
LEFT JOIN (
    SELECT pregunta_id FROM cuestionarios_preguntas
    UNION
    SELECT pregunta_id FROM combos_preguntas
) u ON p.id = u.pregunta_id
SET p.estado = 'aprobada',
    p.estado_disponibilidad = 'disponible'
WHERE p.id BETWEEN 1 AND 675
  AND u.pregunta_id IS NULL;

ALTER TABLE preguntas AUTO_INCREMENT = 676;
ALTER TABLE cuestionarios AUTO_INCREMENT = 76;
ALTER TABLE combos AUTO_INCREMENT = 76;
ALTER TABLE jornadas AUTO_INCREMENT = 6;
ALTER TABLE concursantes AUTO_INCREMENT = 26;
ALTER TABLE usuarios AUTO_INCREMENT = 2;

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
