-- Datos de prueba para LSNLS
-- Crea: 1 usuario admin, catálogos básicos, 50 preguntas, 5 cuestionarios, 5 combos, 1 jornada y 1 concursante

USE lsnls;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection=utf8mb4;
SET character_set_client=utf8mb4;
SET character_set_results=utf8mb4;
SET collation_connection=utf8mb4_unicode_ci;

-- Limpiar todas las tablas (reinicia AUTO_INCREMENT)
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
TRUNCATE TABLE configuracion_global;
TRUNCATE TABLE tematicas;
TRUNCATE TABLE usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- Usuario admin (texto plano, encoder NoOp)
INSERT INTO usuarios (id, nombre, password, rol, version)
VALUES (1, 'admin', '123456', 'ROLE_ADMIN', 0)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), password = VALUES(password), rol = VALUES(rol);

-- Catálogos de temáticas y subtemas (preguntas)
INSERT INTO tematicas_preguntas (nombre, fecha_creacion, creacion_usuario_id)
VALUES ('GEOGRAFÍA', NOW(6), 1), ('HISTORIA', NOW(6), 1), ('CIENCIA', NOW(6), 1), ('ARTE', NOW(6), 1), ('DEPORTES', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO subtemas_preguntas (nombre, fecha_creacion, creacion_usuario_id)
VALUES ('GEOGRAFÍA FÍSICA', NOW(6), 1), ('HISTORIA MODERNA', NOW(6), 1), ('FÍSICA CLÁSICA', NOW(6), 1), ('PINTURA', NOW(6), 1), ('FÚTBOL', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- 50 preguntas mínimas
-- Campos obligatorios: respuesta, tematica, pregunta, estado, nivel
INSERT INTO preguntas (respuesta, tematica, pregunta, subtema, estado, nivel, fecha_creacion, version)
VALUES
('El Nilo', 'GEOGRAFÍA', '¿Cuál es el río más largo de África?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_1LS', NOW(6), 0),
('1914', 'HISTORIA', '¿En qué año comenzó la Primera Guerra Mundial?', 'HISTORIA MODERNA', 'aprobada', '_2NLS', NOW(6), 0),
('Isaac Newton', 'CIENCIA', '¿Quién formuló las leyes del movimiento?', 'FÍSICA CLÁSICA', 'aprobada', '_3LS', NOW(6), 0),
('Leonardo da Vinci', 'ARTE', '¿Qué artista pintó La Última Cena?', 'PINTURA', 'aprobada', '_4NLS', NOW(6), 0),
('Once', 'DEPORTES', '¿Cuántos jugadores por equipo hay en un partido de fútbol?', 'FÚTBOL', 'aprobada', '_5LS', NOW(6), 0),
('Everest', 'GEOGRAFÍA', '¿Cuál es la montaña más alta del mundo sobre el nivel del mar?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_5NLS', NOW(6), 0),
('1789', 'HISTORIA', '¿En qué año comenzó la Revolución Francesa?', 'HISTORIA MODERNA', 'aprobada', '_0', NOW(6), 0),
('La inercia', 'CIENCIA', '¿Cómo se llama la tendencia de un cuerpo a mantener su estado de movimiento?', 'FÍSICA CLÁSICA', 'aprobada', '_1LS', NOW(6), 0),
('Velázquez', 'ARTE', '¿Quién pintó Las Meninas?', 'PINTURA', 'aprobada', '_2NLS', NOW(6), 0),
('Brasil', 'DEPORTES', '¿Qué país ha ganado más Copas del Mundo de fútbol?', 'FÚTBOL', 'aprobada', '_3LS', NOW(6), 0),
('Sáhara', 'GEOGRAFÍA', '¿Cuál es el desierto cálido más grande del mundo?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_4NLS', NOW(6), 0),
('1945', 'HISTORIA', '¿En qué año terminó la Segunda Guerra Mundial?', 'HISTORIA MODERNA', 'aprobada', '_5LS', NOW(6), 0),
('Galileo Galilei', 'CIENCIA', '¿Quién perfeccionó el telescopio y apoyó el heliocentrismo?', 'FÍSICA CLÁSICA', 'aprobada', '_5NLS', NOW(6), 0),
('Goya', 'ARTE', '¿Qué pintor español es autor de Los fusilamientos del 3 de mayo?', 'PINTURA', 'aprobada', '_0', NOW(6), 0),
('Fuera de juego', 'DEPORTES', '¿Cómo se llama la infracción cuando un atacante recibe el balón por detrás de la defensa?', 'FÚTBOL', 'aprobada', '_1LS', NOW(6), 0),
('Andes', 'GEOGRAFÍA', '¿Cómo se llama la cordillera que recorre Sudamérica de norte a sur?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_2NLS', NOW(6), 0),
('Napoleón', 'HISTORIA', '¿Qué militar francés se coronó emperador en 1804?', 'HISTORIA MODERNA', 'aprobada', '_3LS', NOW(6), 0),
('Gravedad', 'CIENCIA', '¿Qué fuerza mantiene a los planetas en órbita alrededor del Sol?', 'FÍSICA CLÁSICA', 'aprobada', '_4NLS', NOW(6), 0),
('Impresionismo', 'ARTE', '¿A qué movimiento pertenecen Monet y Renoir?', 'PINTURA', 'aprobada', '_5LS', NOW(6), 0),
('Penalty', 'DEPORTES', '¿Cómo se llama el tiro directo desde el punto de castigo en fútbol?', 'FÚTBOL', 'aprobada', '_5NLS', NOW(6), 0),
('Amazonas', 'GEOGRAFÍA', '¿Qué río descarga el mayor caudal de agua del mundo?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_0', NOW(6), 0),
('Guerra Fría', 'HISTORIA', '¿Cómo se llamó la tensión política entre EEUU y la URSS tras la Segunda Guerra Mundial?', 'HISTORIA MODERNA', 'aprobada', '_1LS', NOW(6), 0),
('Óptica', 'CIENCIA', '¿Qué rama de la física estudia la luz?', 'FÍSICA CLÁSICA', 'aprobada', '_2NLS', NOW(6), 0),
('El Prado', 'ARTE', '¿Qué museo madrileño alberga obras de Velázquez y Goya?', 'PINTURA', 'aprobada', '_3LS', NOW(6), 0),
('La mano de Dios', 'DEPORTES', '¿Cómo se apoda el famoso gol de Maradona a Inglaterra en 1986?', 'FÚTBOL', 'aprobada', '_4NLS', NOW(6), 0),
('Himalaya', 'GEOGRAFÍA', '¿En qué cordillera se encuentra el Everest?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_5LS', NOW(6), 0),
('ONU', 'HISTORIA', '¿Qué organización internacional se fundó en 1945 para promover la paz?', 'HISTORIA MODERNA', 'aprobada', '_5NLS', NOW(6), 0),
('Segunda ley de Newton', 'CIENCIA', '¿Cómo se llama la ley F = m · a?', 'FÍSICA CLÁSICA', 'aprobada', '_0', NOW(6), 0),
('Capilla Sixtina', 'ARTE', '¿Qué obra pintó Miguel Ángel en el Vaticano?', 'PINTURA', 'aprobada', '_1LS', NOW(6), 0),
('VAR', 'DEPORTES', '¿Qué tecnología de videoasistencia se usa para revisar jugadas?', 'FÚTBOL', 'aprobada', '_2NLS', NOW(6), 0),
('Trópico de Cáncer', 'GEOGRAFÍA', '¿Cómo se llama el paralelo situado al norte a 23,5°?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_3LS', NOW(6), 0),
('Muro de Berlín', 'HISTORIA', '¿Qué barrera cayó en 1989 simbolizando el fin de una era?', 'HISTORIA MODERNA', 'aprobada', '_4NLS', NOW(6), 0),
('Cinemática', 'CIENCIA', '¿Qué parte de la mecánica estudia el movimiento sin considerar sus causas?', 'FÍSICA CLÁSICA', 'aprobada', '_5LS', NOW(6), 0),
('El Guernica', 'ARTE', '¿Qué cuadro de Picasso denuncia los horrores de la guerra?', 'PINTURA', 'aprobada', '_5NLS', NOW(6), 0),
('Hat-trick', 'DEPORTES', '¿Cómo se llama cuando un jugador marca tres goles en un partido?', 'FÚTBOL', 'aprobada', '_0', NOW(6), 0),
('Mar Muerto', 'GEOGRAFÍA', '¿Qué mar tiene una salinidad tan alta que facilita flotar?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_1LS', NOW(6), 0),
('Plan Marshall', 'HISTORIA', '¿Cómo se llamó el programa de ayuda económica de EEUU a Europa tras la guerra?', 'HISTORIA MODERNA', 'aprobada', '_2NLS', NOW(6), 0),
('Péndulo', 'CIENCIA', '¿Qué sistema oscilante usó Foucault para demostrar la rotación terrestre?', 'FÍSICA CLÁSICA', 'aprobada', '_3LS', NOW(6), 0),
('Sorolla', 'ARTE', '¿Qué pintor valenciano es reconocido por sus escenas de playa y luz?', 'PINTURA', 'aprobada', '_4NLS', NOW(6), 0),
('Fuera de banda', 'DEPORTES', '¿Cómo se denomina cuando el balón sale por las líneas laterales?', 'FÚTBOL', 'aprobada', '_5LS', NOW(6), 0),
('Paso del Noroeste', 'GEOGRAFÍA', '¿Cómo se llama la ruta marítima que conecta Atlántico y Pacífico por el Ártico?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_5NLS', NOW(6), 0),
('OTAN', 'HISTORIA', '¿Qué alianza militar se creó en 1949 encabezada por EEUU?', 'HISTORIA MODERNA', 'aprobada', '_0', NOW(6), 0),
('Paradoja de Zenón', 'CIENCIA', '¿Cómo se llaman los famosos problemas de movimiento de Zenón, como Aquiles y la tortuga?', 'FÍSICA CLÁSICA', 'aprobada', '_1LS', NOW(6), 0),
('El Jardín de las Delicias', 'ARTE', '¿Qué tríptico del Bosco es una de las obras más enigmáticas del Prado?', 'PINTURA', 'aprobada', '_2NLS', NOW(6), 0),
('Bicicleta', 'DEPORTES', '¿Qué acrobacia consiste en golpear el balón con una patada en el aire hacia atrás?', 'FÚTBOL', 'aprobada', '_3LS', NOW(6), 0),
('Círculo Polar Ártico', 'GEOGRAFÍA', '¿Cómo se llama el paralelo más al norte antes del polo?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_4NLS', NOW(6), 0),
('Tratado de Versalles', 'HISTORIA', '¿Qué tratado puso fin oficialmente a la Primera Guerra Mundial?', 'HISTORIA MODERNA', 'aprobada', '_5LS', NOW(6), 0),
('Trabajo', 'CIENCIA', 'En física clásica, ¿cómo se llama al producto de fuerza por desplazamiento?', 'FÍSICA CLÁSICA', 'aprobada', '_5NLS', NOW(6), 0),
('El Grito', 'ARTE', '¿Qué obra de Munch muestra una figura angustiada sobre un puente?', 'PINTURA', 'aprobada', '_0', NOW(6), 0),
('Tiro de esquina', 'DEPORTES', '¿Cómo se llama el saque que se realiza desde el banderín?', 'FÚTBOL', 'aprobada', '_1LS', NOW(6), 0),
('Aconcagua', 'GEOGRAFÍA', '¿Cuál es la cima más alta de América del Sur?', 'GEOGRAFÍA FÍSICA', 'aprobada', '_2NLS', NOW(6), 0),
('Ilustración', 'HISTORIA', '¿Cómo se llama el movimiento intelectual del siglo XVIII que reivindica la razón?', 'HISTORIA MODERNA', 'aprobada', '_3LS', NOW(6), 0),
('Tercera ley de Newton', 'CIENCIA', '¿Cómo se llama la ley que dice “a toda acción, una reacción”?', 'FÍSICA CLÁSICA', 'aprobada', '_4NLS', NOW(6), 0),
('Retrato', 'ARTE', '¿Cómo se llama la representación pictórica de una persona?', 'PINTURA', 'aprobada', '_5LS', NOW(6), 0),
('Zidane', 'DEPORTES', '¿Qué jugador marcó de volea en la final de la Champions 2002 para el Real Madrid?', 'FÚTBOL', 'aprobada', '_5NLS', NOW(6), 0);

-- Asegurar disponibilidad para poder crear combos (las PM deben estar disponibles o liberadas)
UPDATE preguntas
SET estado_disponibilidad = 'disponible'
WHERE estado = 'aprobada' AND (estado_disponibilidad IS NULL OR estado_disponibilidad = '');

-- 5 cuestionarios
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version)
VALUES
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q1', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q2', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q3', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q4', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q5', 0);

-- 5 combos
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, tipo, tematica, notas_direccion, version)
VALUES
(1, NOW(6), 'borrador', 'NORMAL', 'P', 'GENERAL', 'Notas dirección C1', 0),
(1, NOW(6), 'borrador', '_5LS',   'A', 'GENERAL', 'Notas dirección C2', 0),
(1, NOW(6), 'borrador', '_5NLS',  'D', 'GENERAL', 'Notas dirección C3', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'R', 'GENERAL', 'Notas dirección C4', 0),
(1, NOW(6), 'borrador', '_5LS',   'P', 'GENERAL', 'Notas dirección C5', 0),
(1, NOW(6), 'borrador', '_5NLS',  'A', 'GENERAL', 'Notas dirección C6', 0);

-- 1 jornada
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas, version)
VALUES ('Jornada Prueba 1', CURDATE(), 'SEGOVIA', 'preparacion', 1, NOW(6), 'Notas jornada de prueba', 0);

-- ------------------------------
-- Vínculos y datos adicionales
-- ------------------------------

-- Capturar IDs útiles
SET @jid = (SELECT id FROM jornadas WHERE nombre = 'Jornada Prueba 1' LIMIT 1);

SET @q1_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q1' LIMIT 1);
SET @q2_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q2' LIMIT 1);
SET @q3_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q3' LIMIT 1);
SET @q4_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q4' LIMIT 1);
SET @q5_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q5' LIMIT 1);
-- Crear un 6º cuestionario
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version)
VALUES (1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q6', 0);
SET @q6_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q6' LIMIT 1);

SET @c1_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C1' LIMIT 1);
SET @c2_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C2' LIMIT 1);
SET @c3_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C3' LIMIT 1);
SET @c4_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C4' LIMIT 1);
SET @c5_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C5' LIMIT 1);
SET @c6_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C6' LIMIT 1);

-- Seleccionar preguntas por nivel para cuestionarios
SET @p1ls_1 = (SELECT id FROM preguntas WHERE nivel = '_1LS' ORDER BY id LIMIT 1);
SET @p2nls_1 = (SELECT id FROM preguntas WHERE nivel = '_2NLS' ORDER BY id LIMIT 1);
SET @p3ls_1 = (SELECT id FROM preguntas WHERE nivel = '_3LS' ORDER BY id LIMIT 1);
SET @p4nls_1 = (SELECT id FROM preguntas WHERE nivel = '_4NLS' ORDER BY id LIMIT 1);

SET @p1ls_2 = (SELECT id FROM preguntas WHERE nivel = '_1LS' ORDER BY id LIMIT 1 OFFSET 1);
SET @p2nls_2 = (SELECT id FROM preguntas WHERE nivel = '_2NLS' ORDER BY id LIMIT 1 OFFSET 1);
SET @p3ls_2 = (SELECT id FROM preguntas WHERE nivel = '_3LS' ORDER BY id LIMIT 1 OFFSET 1);
SET @p4nls_2 = (SELECT id FROM preguntas WHERE nivel = '_4NLS' ORDER BY id LIMIT 1 OFFSET 1);

SET @p1ls_3 = (SELECT id FROM preguntas WHERE nivel = '_1LS' ORDER BY id LIMIT 1 OFFSET 2);
SET @p2nls_3 = (SELECT id FROM preguntas WHERE nivel = '_2NLS' ORDER BY id LIMIT 1 OFFSET 2);
SET @p3ls_3 = (SELECT id FROM preguntas WHERE nivel = '_3LS' ORDER BY id LIMIT 1 OFFSET 2);
SET @p4nls_3 = (SELECT id FROM preguntas WHERE nivel = '_4NLS' ORDER BY id LIMIT 1 OFFSET 2);

-- Asignar 4 preguntas a cada cuestionario (niveles 1-4)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
(@q1_id, @p1ls_1, 1), (@q1_id, @p2nls_1, 1), (@q1_id, @p3ls_1, 1), (@q1_id, @p4nls_1, 1),
(@q2_id, @p1ls_2, 1), (@q2_id, @p2nls_2, 1), (@q2_id, @p3ls_2, 1), (@q2_id, @p4nls_2, 1),
(@q3_id, @p1ls_3, 1), (@q3_id, @p2nls_3, 1), (@q3_id, @p3ls_3, 1), (@q3_id, @p4nls_3, 1),
(@q4_id, @p1ls_1, 1), (@q4_id, @p2nls_2, 1), (@q4_id, @p3ls_3, 1), (@q4_id, @p4nls_1, 1),
(@q5_id, @p1ls_2, 1), (@q5_id, @p2nls_3, 1), (@q5_id, @p3ls_1, 1), (@q5_id, @p4nls_2, 1),
(@q6_id, @p1ls_3, 1), (@q6_id, @p2nls_1, 1), (@q6_id, @p3ls_2, 1), (@q6_id, @p4nls_3, 1);

-- Seleccionar preguntas de nivel 5 para combos (PM)
SET @p5ls_1 = (SELECT id FROM preguntas WHERE nivel = '_5LS' ORDER BY id LIMIT 1);
SET @p5ls_2 = (SELECT id FROM preguntas WHERE nivel = '_5LS' ORDER BY id LIMIT 1 OFFSET 1);
SET @p5ls_3 = (SELECT id FROM preguntas WHERE nivel = '_5LS' ORDER BY id LIMIT 1 OFFSET 2);
SET @p5nls_1 = (SELECT id FROM preguntas WHERE nivel = '_5NLS' ORDER BY id LIMIT 1);
SET @p5nls_2 = (SELECT id FROM preguntas WHERE nivel = '_5NLS' ORDER BY id LIMIT 1 OFFSET 1);
SET @p5nls_3 = (SELECT id FROM preguntas WHERE nivel = '_5NLS' ORDER BY id LIMIT 1 OFFSET 2);

-- Asignar 3 PM a cada combo (X2, X3, X)
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
(@c1_id, @p5ls_1, 'X2'), (@c1_id, @p5nls_1, 'X3'), (@c1_id, @p5ls_2, 'X'),
(@c2_id, @p5ls_2, 'X2'), (@c2_id, @p5nls_2, 'X3'), (@c2_id, @p5ls_3, 'X'),
(@c3_id, @p5ls_3, 'X2'), (@c3_id, @p5nls_3, 'X3'), (@c3_id, @p5ls_1, 'X'),
(@c4_id, @p5ls_1, 'X2'), (@c4_id, @p5nls_2, 'X3'), (@c4_id, @p5ls_3, 'X'),
(@c5_id, @p5ls_2, 'X2'), (@c5_id, @p5nls_3, 'X3'), (@c5_id, @p5ls_1, 'X'),
(@c6_id, @p5ls_3, 'X2'), (@c6_id, @p5nls_1, 'X3'), (@c6_id, @p5ls_2, 'X');

-- Asignar los 6 cuestionarios y 6 combos a la jornada
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id) VALUES
(@jid, @q1_id), (@jid, @q2_id), (@jid, @q3_id), (@jid, @q4_id), (@jid, @q5_id), (@jid, @q6_id);

INSERT INTO jornadas_combos (jornada_id, combo_id) VALUES
(@jid, @c1_id), (@jid, @c2_id), (@jid, @c3_id), (@jid, @c4_id), (@jid, @c5_id), (@jid, @c6_id);

-- 6 concursantes con datos, todos en la misma jornada y asignados a Q/C correspondientes
INSERT INTO concursantes (numero_concursante, jornada_id, dia_grabacion, lugar, nombre, edad, ocupacion, redes_sociales, cuestionario_id, combo_id, xusoker, resultado, notas_grabacion, guionista, valoracion_guionista, momentos_destacados, duracion, valoracion_final, version)
VALUES
(1, @jid, CURDATE(), 'SEGOVIA', 'ALICIA', 28, 'INGENIERA', '@alicia', @q1_id, @c1_id, 'CONTINÚE', 0, 'Muy resolutiva y simpática', 'admin', 'Bien', 'Gran presentación', '12:30', '1', 0),
(2, @jid, CURDATE(), 'SEGOVIA', 'BRUNO', 35, 'ABOGADO', '@bruno', @q2_id, @c2_id, 'NO USÓ', 0, 'Seguro en cámara', 'admin', 'Muy bien', 'Buen humor', '11:15', '1', 0),
(3, @jid, CURDATE(), 'SEGOVIA', 'CARLA', 32, 'MÉDICA', '@carla', @q3_id, @c3_id, 'AL VERRÉS', 0, 'Energía alta todo el rato', 'admin', 'Excelente', 'Momentazos', '13:05', '1', 0),
(4, @jid, CURDATE(), 'SEGOVIA', 'DIEGO', 29, 'DOCENTE', '@diego', @q4_id, @c4_id, 'RECICLA', 0, 'Muy natural', 'admin', 'OK', 'Improvisación', '10:45', '1', 0),
(5, @jid, CURDATE(), 'SEGOVIA', 'ELENA', 27, 'PERIODISTA', '@elena', @q5_id, @c5_id, 'LLAMADA', 0, 'Comunicación clara', 'admin', 'Notable', 'Reacciones', '09:55', '1', 0),
(6, @jid, CURDATE(), 'SEGOVIA', 'FERNANDO', 41, 'EMPRESARIO', '@fernando', @q6_id, @c6_id, 'CONTINÚE', 0, 'Carisma alto', 'admin', 'Top', 'Risas', '14:10', '1', 0);

-- Marcar como USADAS las preguntas que están en cuestionarios o combos
UPDATE preguntas
SET estado = 'usada', estado_disponibilidad = 'usada'
WHERE id IN (
    SELECT pregunta_id FROM cuestionarios_preguntas
    UNION
    SELECT pregunta_id FROM combos_preguntas
);

-- ===========================================
-- Bloque adicional: +50 preguntas, +5 cuestionarios, +5 combos y nueva jornada
-- ===========================================

-- 50 preguntas adicionales
INSERT INTO preguntas (respuesta, tematica, pregunta, subtema, estado, nivel, fecha_creacion, version)
VALUES
('Respuesta extra 1',  'GEOGRAFÍA', 'Pregunta extra 1 sobre geografía',    'GEOGRAFÍA FÍSICA', 'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 2',  'HISTORIA',  'Pregunta extra 2 sobre historia',     'HISTORIA MODERNA', 'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 3',  'CIENCIA',   'Pregunta extra 3 sobre ciencia',      'FÍSICA CLÁSICA',   'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 4',  'ARTE',      'Pregunta extra 4 sobre arte',         'PINTURA',          'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 5',  'DEPORTES',  'Pregunta extra 5 sobre deportes',     'FÚTBOL',           'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 6',  'GEOGRAFÍA', 'Pregunta extra 6 sobre geografía',    'GEOGRAFÍA FÍSICA', 'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 7',  'HISTORIA',  'Pregunta extra 7 sobre historia',     'HISTORIA MODERNA', 'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 8',  'CIENCIA',   'Pregunta extra 8 sobre ciencia',      'FÍSICA CLÁSICA',   'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 9',  'ARTE',      'Pregunta extra 9 sobre arte',         'PINTURA',          'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 10', 'DEPORTES',  'Pregunta extra 10 sobre deportes',    'FÚTBOL',           'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 11', 'GEOGRAFÍA', 'Pregunta extra 11 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 12', 'HISTORIA',  'Pregunta extra 12 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 13', 'CIENCIA',   'Pregunta extra 13 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 14', 'ARTE',      'Pregunta extra 14 sobre arte',        'PINTURA',          'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 15', 'DEPORTES',  'Pregunta extra 15 sobre deportes',    'FÚTBOL',           'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 16', 'GEOGRAFÍA', 'Pregunta extra 16 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 17', 'HISTORIA',  'Pregunta extra 17 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 18', 'CIENCIA',   'Pregunta extra 18 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 19', 'ARTE',      'Pregunta extra 19 sobre arte',        'PINTURA',          'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 20', 'DEPORTES',  'Pregunta extra 20 sobre deportes',    'FÚTBOL',           'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 21', 'GEOGRAFÍA', 'Pregunta extra 21 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 22', 'HISTORIA',  'Pregunta extra 22 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 23', 'CIENCIA',   'Pregunta extra 23 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 24', 'ARTE',      'Pregunta extra 24 sobre arte',        'PINTURA',          'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 25', 'DEPORTES',  'Pregunta extra 25 sobre deportes',    'FÚTBOL',           'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 26', 'GEOGRAFÍA', 'Pregunta extra 26 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 27', 'HISTORIA',  'Pregunta extra 27 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 28', 'CIENCIA',   'Pregunta extra 28 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 29', 'ARTE',      'Pregunta extra 29 sobre arte',        'PINTURA',          'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 30', 'DEPORTES',  'Pregunta extra 30 sobre deportes',    'FÚTBOL',           'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 31', 'GEOGRAFÍA', 'Pregunta extra 31 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 32', 'HISTORIA',  'Pregunta extra 32 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 33', 'CIENCIA',   'Pregunta extra 33 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 34', 'ARTE',      'Pregunta extra 34 sobre arte',        'PINTURA',          'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 35', 'DEPORTES',  'Pregunta extra 35 sobre deportes',    'FÚTBOL',           'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 36', 'GEOGRAFÍA', 'Pregunta extra 36 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 37', 'HISTORIA',  'Pregunta extra 37 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 38', 'CIENCIA',   'Pregunta extra 38 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 39', 'ARTE',      'Pregunta extra 39 sobre arte',        'PINTURA',          'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 40', 'DEPORTES',  'Pregunta extra 40 sobre deportes',    'FÚTBOL',           'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 41', 'GEOGRAFÍA', 'Pregunta extra 41 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 42', 'HISTORIA',  'Pregunta extra 42 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 43', 'CIENCIA',   'Pregunta extra 43 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_1LS',  NOW(6), 0),
('Respuesta extra 44', 'ARTE',      'Pregunta extra 44 sobre arte',        'PINTURA',          'aprobada', '_2NLS', NOW(6), 0),
('Respuesta extra 45', 'DEPORTES',  'Pregunta extra 45 sobre deportes',    'FÚTBOL',           'aprobada', '_3LS',  NOW(6), 0),
('Respuesta extra 46', 'GEOGRAFÍA', 'Pregunta extra 46 sobre geografía',   'GEOGRAFÍA FÍSICA', 'aprobada', '_4NLS', NOW(6), 0),
('Respuesta extra 47', 'HISTORIA',  'Pregunta extra 47 sobre historia',    'HISTORIA MODERNA', 'aprobada', '_5LS',  NOW(6), 0),
('Respuesta extra 48', 'CIENCIA',   'Pregunta extra 48 sobre ciencia',     'FÍSICA CLÁSICA',   'aprobada', '_5NLS', NOW(6), 0),
('Respuesta extra 49', 'ARTE',      'Pregunta extra 49 sobre arte',        'PINTURA',          'aprobada', '_0',    NOW(6), 0),
('Respuesta extra 50', 'DEPORTES',  'Pregunta extra 50 sobre deportes',    'FÚTBOL',           'aprobada', '_1LS',  NOW(6), 0);

-- 5 cuestionarios adicionales
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version)
VALUES
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q7', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q8', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q9', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q10', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'GENERAL', 'Notas dirección Q11', 0);

SET @q7_id  = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q7'  LIMIT 1);
SET @q8_id  = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q8'  LIMIT 1);
SET @q9_id  = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q9'  LIMIT 1);
SET @q10_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q10' LIMIT 1);
SET @q11_id = (SELECT id FROM cuestionarios WHERE notas_direccion = 'Notas dirección Q11' LIMIT 1);

-- 5 combos adicionales
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, tipo, tematica, notas_direccion, version)
VALUES
(1, NOW(6), 'borrador', 'NORMAL', 'P', 'GENERAL', 'Notas dirección C7', 0),
(1, NOW(6), 'borrador', '_5LS',   'A', 'GENERAL', 'Notas dirección C8', 0),
(1, NOW(6), 'borrador', '_5NLS',  'D', 'GENERAL', 'Notas dirección C9', 0),
(1, NOW(6), 'borrador', 'NORMAL', 'R', 'GENERAL', 'Notas dirección C10', 0),
(1, NOW(6), 'borrador', '_5LS',   'P', 'GENERAL', 'Notas dirección C11', 0);

SET @c7_id  = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C7'  LIMIT 1);
SET @c8_id  = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C8'  LIMIT 1);
SET @c9_id  = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C9'  LIMIT 1);
SET @c10_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C10' LIMIT 1);
SET @c11_id = (SELECT id FROM combos WHERE notas_direccion = 'Notas dirección C11' LIMIT 1);

-- Nueva jornada
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas, version)
VALUES ('Jornada Prueba 2', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'VALENCIA', 'preparacion', 1, NOW(6), 'Segunda jornada de prueba', 0);

SET @jid2 = (SELECT id FROM jornadas WHERE nombre = 'Jornada Prueba 2' LIMIT 1);

-- Asignar preguntas a los nuevos cuestionarios (reutilizamos las ya seleccionadas por nivel)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion) VALUES
(@q7_id,  @p1ls_1, 1), (@q7_id,  @p2nls_1, 1), (@q7_id,  @p3ls_1, 1), (@q7_id,  @p4nls_1, 1),
(@q8_id,  @p1ls_2, 1), (@q8_id,  @p2nls_2, 1), (@q8_id,  @p3ls_2, 1), (@q8_id,  @p4nls_2, 1),
(@q9_id,  @p1ls_3, 1), (@q9_id,  @p2nls_3, 1), (@q9_id,  @p3ls_3, 1), (@q9_id,  @p4nls_3, 1),
(@q10_id, @p1ls_1, 1), (@q10_id, @p2nls_2, 1), (@q10_id, @p3ls_3, 1), (@q10_id, @p4nls_1, 1),
(@q11_id, @p1ls_2, 1), (@q11_id, @p2nls_3, 1), (@q11_id, @p3ls_1, 1), (@q11_id, @p4nls_2, 1);

-- Asignar preguntas de nivel 5 a los nuevos combos (reutilizamos las ya seleccionadas)
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion) VALUES
(@c7_id,  @p5ls_1,  'X2'), (@c7_id,  @p5nls_1, 'X3'), (@c7_id,  @p5ls_2, 'X'),
(@c8_id,  @p5ls_2,  'X2'), (@c8_id,  @p5nls_2, 'X3'), (@c8_id,  @p5ls_3, 'X'),
(@c9_id,  @p5ls_3,  'X2'), (@c9_id,  @p5nls_3, 'X3'), (@c9_id,  @p5ls_1, 'X'),
(@c10_id, @p5ls_1,  'X2'), (@c10_id, @p5nls_2, 'X3'), (@c10_id, @p5ls_3, 'X'),
(@c11_id, @p5ls_2,  'X2'), (@c11_id, @p5nls_3, 'X3'), (@c11_id, @p5ls_1, 'X');

-- Asignar nuevos cuestionarios y combos a la nueva jornada
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id) VALUES
(@jid2, @q7_id), (@jid2, @q8_id), (@jid2, @q9_id), (@jid2, @q10_id), (@jid2, @q11_id);

INSERT INTO jornadas_combos (jornada_id, combo_id) VALUES
(@jid2, @c7_id), (@jid2, @c8_id), (@jid2, @c9_id), (@jid2, @c10_id), (@jid2, @c11_id);

-- Asegurar de nuevo que todas las preguntas usadas en cuestionarios o combos estén en estado 'usada'
UPDATE preguntas
SET estado = 'usada', estado_disponibilidad = 'usada'
WHERE id IN (
    SELECT pregunta_id FROM cuestionarios_preguntas
    UNION
    SELECT pregunta_id FROM combos_preguntas
);

