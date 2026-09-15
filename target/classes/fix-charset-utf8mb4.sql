-- ============================================================
-- Pasar la base de datos y todas sus tablas a utf8mb4
-- ============================================================
-- Para qué sirve:
--   Si la base de datos se creó con el juego de caracteres por defecto de
--   MySQL (latin1 en instalaciones antiguas), las tablas heredan ese juego y
--   rechazan cualquier letra que latin1 no sepa representar. Al cargar los
--   datos de prueba salta:
--
--     Error Code: 1366. Incorrect string value: '\xC4\x83neci'
--     for column 'respuesta' at row 57
--
--   Esa 'ă' es de 'Nadia Comăneci'. El fichero de datos y la conexión ya van
--   en utf8mb4; lo que falla es la columna de destino.
--
-- Cuándo ejecutarlo:
--   Una sola vez por servidor, ANTES de cargar dataPrueba.sql o dataFinal.sql.
--   Es idempotente: volver a ejecutarlo no rompe nada.
--
-- Ojo:
--   CONVERT TO CHARACTER SET reescribe cada tabla. Con las tablas llenas puede
--   tardar un rato y conviene tener copia de seguridad antes.
-- ============================================================

USE lsnls;

-- Que las tablas que se creen a partir de ahora nazcan ya en utf8mb4
ALTER DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Convertir las tablas que ya existen, con sus datos
ALTER TABLE usuarios               CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE tematicas              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE tematicas_preguntas    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE tematicas_combos       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE subtemas_preguntas     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE preguntas              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE cuestionarios          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE combos                 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE cuestionarios_preguntas CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE combos_preguntas       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE configuracion_global   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE programas              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE jornadas               CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE jornadas_cuestionarios CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE jornadas_combos        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE concursantes           CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE historial_jornadas     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE edit_locks             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE operaciones_undo       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE audit_logs             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Comprobación: después de ejecutar esto no debería salir ninguna fila
-- ------------------------------------------------------------
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'lsnls'
  AND TABLE_COLLATION NOT LIKE 'utf8mb4%';

SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'lsnls'
  AND CHARACTER_SET_NAME IS NOT NULL
  AND CHARACTER_SET_NAME <> 'utf8mb4';
