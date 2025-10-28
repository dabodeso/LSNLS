-- Vacía todas las tablas de la base de datos LSNLS (MySQL)
-- Atención: esto elimina todos los datos y resetea los AUTO_INCREMENT
USE lsnls;

SET FOREIGN_KEY_CHECKS = 0;

-- Hijo primero (por si acaso), aunque FOREIGN_KEY_CHECKS está desactivado
TRUNCATE TABLE jornadas_combos;
TRUNCATE TABLE jornadas_cuestionarios;
TRUNCATE TABLE cuestionarios_preguntas;
TRUNCATE TABLE combos_preguntas;
TRUNCATE TABLE historial_jornadas;
TRUNCATE TABLE concursantes;
TRUNCATE TABLE programas;
TRUNCATE TABLE jornadas;
TRUNCATE TABLE combos;
TRUNCATE TABLE cuestionarios;
TRUNCATE TABLE preguntas;
TRUNCATE TABLE tematicas;
TRUNCATE TABLE configuracion_global;
TRUNCATE TABLE usuarios;

SET FOREIGN_KEY_CHECKS = 1;


