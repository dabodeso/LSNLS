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

-- Secuencia 1..100
INSERT INTO tmp_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT n FROM seq;

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

-- 10 jornadas (inicialmente preparacion)
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
WHERE n <= 10;

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

-- Asignar 1 cuestionario y 1 combo por jornada (índice a índice)
INSERT INTO jornadas_cuestionarios (jornada_id, cuestionario_id)
SELECT j.id, q.id
FROM tmp_jornada_idx j
JOIN tmp_cuestionario_idx q ON q.idx = j.idx;

INSERT INTO jornadas_combos (jornada_id, combo_id)
SELECT j.id, c.id
FROM tmp_jornada_idx j
JOIN tmp_combo_idx c ON c.idx = j.idx;

-- 10 concursantes (1 por jornada), enlazados con su cuestionario/combo
INSERT INTO concursantes (
    numero_concursante, jornada_id, dia_grabacion, lugar, nombre, edad, ocupacion, redes_sociales,
    cuestionario_id, combo_id, xusoker, resultado, notas_grabacion, guionista, valoracion_guionista,
    momentos_destacados, duracion, valoracion_final, version
)
SELECT
    j.idx AS numero_concursante,
    j.id AS jornada_id,
    DATE_ADD(CURDATE(), INTERVAL j.idx - 1 DAY) AS dia_grabacion,
    CONCAT('SEDE ', j.idx) AS lugar,
    CONCAT('CONCURSANTE ', LPAD(j.idx, 2, '0')) AS nombre,
    20 + j.idx AS edad,
    'PROFESION' AS ocupacion,
    CONCAT('@concursante', j.idx) AS redes_sociales,
    q.id AS cuestionario_id,
    c.id AS combo_id,
    CASE MOD(j.idx, 5)
        WHEN 1 THEN 'NO USÓ'
        WHEN 2 THEN 'CONTINÚE'
        WHEN 3 THEN 'AL VERRÉS'
        WHEN 4 THEN 'RECICLA'
        ELSE 'LLAMADA'
    END AS xusoker,
    0 AS resultado,
    CONCAT('Notas concursante ', j.idx) AS notas_grabacion,
    'admin' AS guionista,
    '2' AS valoracion_guionista,
    CONCAT('Momento destacado ', j.idx) AS momentos_destacados,
    CONCAT(10 + j.idx, ':00') AS duracion,
    '2' AS valoracion_final,
    0 AS version
FROM tmp_jornada_idx j
JOIN tmp_cuestionario_idx q ON q.idx = j.idx
JOIN tmp_combo_idx c ON c.idx = j.idx;

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
