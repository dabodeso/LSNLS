-- Datos de prueba LSNLS (consistentes y sin reutilizar preguntas)
-- Crea 100 preguntas, 10 combos, 10 cuestionarios, 10 jornadas y 10 concursantes.
-- Regla de asignación de preguntas:
--   - Combo i usa 5 preguntas únicas del bloque i (posiciones 1..5)
--   - Cuestionario i usa otras 5 preguntas únicas del bloque i (posiciones 6..10)

USE lsnls;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection=utf8mb4;
SET character_set_client=utf8mb4;
SET character_set_results=utf8mb4;
SET collation_connection=utf8mb4_unicode_ci;

-- Limpieza total
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

-- Usuario base
INSERT INTO usuarios (id, nombre, password, rol, version)
VALUES (1, 'admin', '123456', 'ROLE_ADMIN', 0)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  password = VALUES(password),
  rol = VALUES(rol);

-- Catálogos mínimos
INSERT INTO tematicas_preguntas (nombre, fecha_creacion, creacion_usuario_id)
VALUES
('GEOGRAFÍA', NOW(6), 1),
('HISTORIA', NOW(6), 1),
('CIENCIA', NOW(6), 1),
('ARTE', NOW(6), 1),
('DEPORTES', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO subtemas_preguntas (nombre, fecha_creacion, creacion_usuario_id)
VALUES
('GEOGRAFÍA FÍSICA', NOW(6), 1),
('HISTORIA MODERNA', NOW(6), 1),
('FÍSICA CLÁSICA', NOW(6), 1),
('PINTURA', NOW(6), 1),
('FÚTBOL', NOW(6), 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);

-- Secuencia 1..100 (compatible con MySQL sin CTE recursivo)
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

-- 100 preguntas base
INSERT INTO preguntas (respuesta, tematica, pregunta, subtema, estado, nivel, fecha_creacion, version)
SELECT
    CONCAT('Respuesta ', n),
    CASE MOD(n, 5)
        WHEN 1 THEN 'GEOGRAFÍA'
        WHEN 2 THEN 'HISTORIA'
        WHEN 3 THEN 'CIENCIA'
        WHEN 4 THEN 'ARTE'
        ELSE 'DEPORTES'
    END AS tematica,
    CONCAT('Pregunta de prueba ', n),
    CASE MOD(n, 5)
        WHEN 1 THEN 'GEOGRAFÍA FÍSICA'
        WHEN 2 THEN 'HISTORIA MODERNA'
        WHEN 3 THEN 'FÍSICA CLÁSICA'
        WHEN 4 THEN 'PINTURA'
        ELSE 'FÚTBOL'
    END AS subtema,
    'aprobada' AS estado,
    CASE MOD(n, 6)
        WHEN 1 THEN '_1LS'
        WHEN 2 THEN '_2NLS'
        WHEN 3 THEN '_3LS'
        WHEN 4 THEN '_4NLS'
        WHEN 5 THEN '_5LS'
        ELSE '_5NLS'
    END AS nivel,
    NOW(6),
    0
FROM tmp_seq
WHERE n <= 100;

-- Disponibilidad inicial
UPDATE preguntas
SET estado_disponibilidad = 'disponible'
WHERE estado = 'aprobada';

-- 10 cuestionarios (inicialmente borrador)
INSERT INTO cuestionarios (creacion_usuario_id, fecha_creacion, estado, nivel, tematica, notas_direccion, version)
SELECT
    1,
    NOW(6),
    'borrador',
    'NORMAL',
    'GENERAL',
    CONCAT('Notas dirección Q', n),
    0
FROM tmp_seq
WHERE n <= 10;

-- 10 combos (inicialmente borrador)
INSERT INTO combos (creacion_usuario_id, fecha_creacion, estado, nivel, tipo, tematica, notas_direccion, version)
SELECT
    1,
    NOW(6),
    'borrador',
    'NORMAL',
    CASE MOD(n, 4)
        WHEN 1 THEN 'P'
        WHEN 2 THEN 'A'
        WHEN 3 THEN 'D'
        ELSE 'R'
    END,
    'GENERAL',
    CONCAT('Notas dirección C', n),
    0
FROM tmp_seq
WHERE n <= 10;

-- 2 jornadas (inicialmente preparacion), para repartir 5 bloques en cada una
INSERT INTO jornadas (nombre, fecha_jornada, lugar, estado, creacion_usuario_id, fecha_creacion, notas, version)
SELECT
    CONCAT('Jornada Prueba ', n),
    DATE_ADD(CURDATE(), INTERVAL n - 1 DAY),
    CONCAT('SEDE ', n),
    'preparacion',
    1,
    NOW(6),
    CONCAT('Notas jornada ', n),
    0
FROM tmp_seq
WHERE n <= 2;

DROP TEMPORARY TABLE IF EXISTS tmp_pregunta_idx;
CREATE TEMPORARY TABLE tmp_pregunta_idx AS
SELECT (@rp := @rp + 1) AS idx, p.id
FROM (SELECT id FROM preguntas ORDER BY id) p
CROSS JOIN (SELECT @rp := 0) init;

DROP TEMPORARY TABLE IF EXISTS tmp_cuestionario_idx;
CREATE TEMPORARY TABLE tmp_cuestionario_idx AS
SELECT (@rq := @rq + 1) AS idx, q.id
FROM (SELECT id FROM cuestionarios ORDER BY id) q
CROSS JOIN (SELECT @rq := 0) init;

DROP TEMPORARY TABLE IF EXISTS tmp_combo_idx;
CREATE TEMPORARY TABLE tmp_combo_idx AS
SELECT (@rc := @rc + 1) AS idx, c.id
FROM (SELECT id FROM combos ORDER BY id) c
CROSS JOIN (SELECT @rc := 0) init;

DROP TEMPORARY TABLE IF EXISTS tmp_jornada_idx;
CREATE TEMPORARY TABLE tmp_jornada_idx AS
SELECT (@rj := @rj + 1) AS idx, j.id
FROM (SELECT id FROM jornadas ORDER BY id) j
CROSS JOIN (SELECT @rj := 0) init;

DROP TEMPORARY TABLE IF EXISTS tmp_pos5;
CREATE TEMPORARY TABLE tmp_pos5 (pos INT PRIMARY KEY);
INSERT INTO tmp_pos5 (pos) VALUES (1), (2), (3), (4), (5);

-- Asignar 5 preguntas únicas a cada combo (bloques 1..5 de cada decena)
INSERT INTO combos_preguntas (combo_id, pregunta_id, factor_multiplicacion)
SELECT
    c.id,
    p.id,
    CASE s.pos
        WHEN 1 THEN 'X2'
        WHEN 2 THEN 'X3'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'X4'
        ELSE 'X5'
    END AS factor_multiplicacion
FROM tmp_combo_idx c
JOIN tmp_pos5 s
JOIN tmp_pregunta_idx p
  ON p.idx = ((c.idx - 1) * 10) + s.pos;

-- Asignar otras 5 preguntas únicas a cada cuestionario (bloques 6..10 de cada decena)
INSERT INTO cuestionarios_preguntas (cuestionario_id, pregunta_id, factor_multiplicacion)
SELECT
    q.id,
    p.id,
    1
FROM tmp_cuestionario_idx q
JOIN tmp_pos5 s
JOIN tmp_pregunta_idx p
  ON p.idx = ((q.idx - 1) * 10) + 5 + s.pos;

-- Asignar 10 cuestionarios en 2 jornadas (5 por jornada)
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id)
SELECT j.id, q.id
FROM tmp_cuestionario_idx q
JOIN tmp_jornada_idx j ON j.idx = CEIL(q.idx / 5);

-- Asignar 10 combos en 2 jornadas (5 por jornada)
INSERT INTO jornadas_combos (jornada_id, combo_id)
SELECT j.id, c.id
FROM tmp_combo_idx c
JOIN tmp_jornada_idx j ON j.idx = CEIL(c.idx / 5);

-- 10 concursantes (5 por jornada), enlazados con su cuestionario/combo
INSERT INTO concursantes (
    numero_concursante, jornada_id, dia_grabacion, lugar, nombre, edad, ocupacion, redes_sociales,
    cuestionario_id, combo_id, xusoker, resultado, notas_grabacion, guionista, valoracion_guionista,
    momentos_destacados, duracion, valoracion_final, version
)
SELECT
    q.idx AS numero_concursante,
    j.id AS jornada_id,
    DATE_ADD(CURDATE(), INTERVAL j.idx - 1 DAY) AS dia_grabacion,
    CONCAT('SEDE ', j.idx) AS lugar,
    CONCAT('CONCURSANTE ', LPAD(q.idx, 2, '0')) AS nombre,
    20 + q.idx AS edad,
    'PROFESION' AS ocupacion,
    CONCAT('@concursante', q.idx) AS redes_sociales,
    q.id AS cuestionario_id,
    c.id AS combo_id,
    CASE MOD(q.idx, 5)
        WHEN 1 THEN 'NO USÓ'
        WHEN 2 THEN 'CONTINÚE'
        WHEN 3 THEN 'AL VERRÉS'
        WHEN 4 THEN 'RECICLA'
        ELSE 'LLAMADA'
    END AS xusoker,
    0 AS resultado,
    CONCAT('Notas concursante ', q.idx) AS notas_grabacion,
    'admin' AS guionista,
    '2' AS valoracion_guionista,
    CONCAT('Momento destacado ', q.idx) AS momentos_destacados,
    CONCAT(10 + q.idx, ':00') AS duracion,
    '2' AS valoracion_final,
    0 AS version
FROM tmp_cuestionario_idx q
JOIN tmp_combo_idx c ON c.idx = q.idx
JOIN tmp_jornada_idx j ON j.idx = CEIL(q.idx / 5);

-- ===========================================
-- Comprobación final de consistencia de estados
-- ===========================================

-- 1) Si una pregunta está en un cuestionario o combo -> usada
UPDATE preguntas
SET estado = 'usada',
    estado_disponibilidad = 'usada'
WHERE id IN (
    SELECT pregunta_id FROM cuestionarios_preguntas
    UNION
    SELECT pregunta_id FROM combos_preguntas
);

-- 2) Si un cuestionario/combo está en una jornada -> adjudicado (asignado)
UPDATE cuestionarios q
SET q.estado = 'adjudicado'
WHERE EXISTS (
    SELECT 1
    FROM jornadas_cuestionarios jc
    WHERE jc.cuestionario_id = q.id
)
AND q.estado <> 'grabado';

UPDATE combos c
SET c.estado = 'adjudicado'
WHERE EXISTS (
    SELECT 1
    FROM jornadas_combos jc
    WHERE jc.combo_id = c.id
)
AND c.estado <> 'grabado';

-- 3) Si una jornada tiene concursantes -> en_grabacion
UPDATE jornadas j
SET j.estado = 'en_grabacion'
WHERE EXISTS (
    SELECT 1
    FROM concursantes c
    WHERE c.jornada_id = j.id
);

-- Limpieza de temporales
DROP TEMPORARY TABLE IF EXISTS tmp_pos5;
DROP TEMPORARY TABLE IF EXISTS tmp_jornada_idx;
DROP TEMPORARY TABLE IF EXISTS tmp_combo_idx;
DROP TEMPORARY TABLE IF EXISTS tmp_cuestionario_idx;
DROP TEMPORARY TABLE IF EXISTS tmp_pregunta_idx;
DROP TEMPORARY TABLE IF EXISTS tmp_seq;

-- 20 preguntas reales extra, en estado aprobada y disponibles (no asignadas)
INSERT INTO preguntas (respuesta, tematica, pregunta, subtema, estado, estado_disponibilidad, nivel, fecha_creacion, version)
VALUES
('Madrid', 'GEOGRAFÍA', '¿Cuál es la capital de España?', 'GEOGRAFÍA FÍSICA', 'aprobada', 'disponible', '_1LS', NOW(6), 0),
('Nilo', 'GEOGRAFÍA', '¿Cuál es uno de los ríos más largos del mundo?', 'GEOGRAFÍA FÍSICA', 'aprobada', 'disponible', '_2NLS', NOW(6), 0),
('Océano Pacífico', 'GEOGRAFÍA', '¿Cuál es el océano más grande de la Tierra?', 'GEOGRAFÍA FÍSICA', 'aprobada', 'disponible', '_3LS', NOW(6), 0),
('Sáhara', 'GEOGRAFÍA', '¿Cuál es el desierto cálido más grande del mundo?', 'GEOGRAFÍA FÍSICA', 'aprobada', 'disponible', '_4NLS', NOW(6), 0),

('1914', 'HISTORIA', '¿En qué año comenzó la Primera Guerra Mundial?', 'HISTORIA MODERNA', 'aprobada', 'disponible', '_1LS', NOW(6), 0),
('1789', 'HISTORIA', '¿En qué año comenzó la Revolución Francesa?', 'HISTORIA MODERNA', 'aprobada', 'disponible', '_2NLS', NOW(6), 0),
('Muro de Berlín', 'HISTORIA', '¿Qué símbolo cayó en 1989 en Alemania?', 'HISTORIA MODERNA', 'aprobada', 'disponible', '_3LS', NOW(6), 0),
('Napoleón Bonaparte', 'HISTORIA', '¿Qué líder francés fue emperador en 1804?', 'HISTORIA MODERNA', 'aprobada', 'disponible', '_4NLS', NOW(6), 0),

('H2O', 'CIENCIA', '¿Cuál es la fórmula química del agua?', 'FÍSICA CLÁSICA', 'aprobada', 'disponible', '_1LS', NOW(6), 0),
('Gravedad', 'CIENCIA', '¿Qué fuerza atrae los cuerpos hacia la Tierra?', 'FÍSICA CLÁSICA', 'aprobada', 'disponible', '_2NLS', NOW(6), 0),
('Newton', 'CIENCIA', '¿Quién formuló las leyes del movimiento clásico?', 'FÍSICA CLÁSICA', 'aprobada', 'disponible', '_3LS', NOW(6), 0),
('299.792.458 m/s', 'CIENCIA', '¿Cuál es la velocidad de la luz en el vacío?', 'FÍSICA CLÁSICA', 'aprobada', 'disponible', '_4NLS', NOW(6), 0),

('Velázquez', 'ARTE', '¿Qué pintor español es autor de Las Meninas?', 'PINTURA', 'aprobada', 'disponible', '_1LS', NOW(6), 0),
('Picasso', 'ARTE', '¿Qué artista pintó el Guernica?', 'PINTURA', 'aprobada', 'disponible', '_2NLS', NOW(6), 0),
('Museo del Prado', 'ARTE', '¿En qué museo se encuentra Las Meninas?', 'PINTURA', 'aprobada', 'disponible', '_3LS', NOW(6), 0),
('Impresionismo', 'ARTE', '¿A qué movimiento pertenecen Monet y Renoir?', 'PINTURA', 'aprobada', 'disponible', '_4NLS', NOW(6), 0),

('11', 'DEPORTES', '¿Cuántos jugadores por equipo hay en un partido de fútbol?', 'FÚTBOL', 'aprobada', 'disponible', '_1LS', NOW(6), 0),
('90', 'DEPORTES', '¿Cuántos minutos dura un partido reglamentario de fútbol?', 'FÚTBOL', 'aprobada', 'disponible', '_2NLS', NOW(6), 0),
('Tarjeta roja', 'DEPORTES', '¿Qué tarjeta implica expulsión directa?', 'FÚTBOL', 'aprobada', 'disponible', '_3LS', NOW(6), 0),
('Pelé', 'DEPORTES', '¿Qué futbolista brasileño fue apodado O Rei?', 'FÚTBOL', 'aprobada', 'disponible', '_4NLS', NOW(6), 0);
