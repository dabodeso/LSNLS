-- Ejecutar antes de que Spring Boot cree las tablas

SET FOREIGN_KEY_CHECKS = 0;
-- Cambiar a una BD neutral para poder eliminar la BD actual sin quedar "en uso"
USE mysql;
DROP DATABASE IF EXISTS lsnls;
SET FOREIGN_KEY_CHECKS = 1;

-- Crear la base de datos con UTF-8 si no existe
CREATE DATABASE IF NOT EXISTS lsnls 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- Usar la base de datos
USE lsnls;

-- Configurar las variables de sesión para UTF-8
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_results = utf8mb4;
SET character_set_connection = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 0;
-- Borrado explícito ordenado (por si la BD existiera tras recreación)
DROP TABLE IF EXISTS combos_preguntas;
DROP TABLE IF EXISTS cuestionarios_preguntas;
DROP TABLE IF EXISTS concursantes;
DROP TABLE IF EXISTS jornadas_combos;
DROP TABLE IF EXISTS jornadas_cuestionarios;
DROP TABLE IF EXISTS jornadas;
DROP TABLE IF EXISTS combos;
DROP TABLE IF EXISTS cuestionarios;
DROP TABLE IF EXISTS preguntas;
DROP TABLE IF EXISTS tematicas;
DROP TABLE IF EXISTS programas;
DROP TABLE IF EXISTS configuracion_global;
DROP TABLE IF EXISTS usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- Las tablas se crean solo si no existen para mantener los datos entre ejecuciones

-- Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION') NOT NULL,
    version BIGINT DEFAULT 0
);

-- Crear tabla de temáticas
CREATE TABLE IF NOT EXISTS tematicas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    fecha_creacion datetime(6),
    creacion_usuario_id BIGINT,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (creacion_usuario_id) REFERENCES usuarios(id)
);

-- Crear tabla de preguntas
CREATE TABLE IF NOT EXISTS preguntas (
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
    autor VARCHAR(100),
    notas TEXT,
    notas_verificacion TEXT,
    notas_direccion TEXT,
    verificacion VARCHAR(500),
    estado ENUM('borrador', 'para_verificar', 'verificada', 'revisar', 'corregir', 'rechazada', 'aprobada') NOT NULL,
    estado_disponibilidad ENUM('disponible', 'usada', 'liberada', 'descartada'),
    factor ENUM('X', 'X2', 'X3'),
    nivel ENUM('_0', '_1LS', '_2NLS', '_3LS', '_4NLS', '_5LS', '_5NLS') NOT NULL,
    version BIGINT DEFAULT 0
);

-- Crear tabla de cuestionarios
CREATE TABLE IF NOT EXISTS cuestionarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    estado ENUM('borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado') NOT NULL,
    nivel ENUM('_1LS', '_2NLS', '_3LS', '_4NLS', 'PM1', 'PM2', 'PM3', 'NORMAL') NOT NULL,
    tematica VARCHAR(100),
    notas_direccion TEXT,
    version BIGINT DEFAULT 0
);

-- Crear tabla de combos
CREATE TABLE IF NOT EXISTS combos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    estado ENUM('borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado') NOT NULL,
    nivel ENUM('_5LS', '_5NLS', 'NORMAL') NOT NULL,
    tipo ENUM('P', 'A', 'D', 'R'),
    version BIGINT DEFAULT 0
);

-- Crear tabla de relación cuestionarios-preguntas
CREATE TABLE IF NOT EXISTS cuestionarios_preguntas (
    cuestionario_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    factor_multiplicacion INTEGER,
    PRIMARY KEY (cuestionario_id, pregunta_id)
);

-- Crear tabla de relación combos-preguntas
CREATE TABLE IF NOT EXISTS combos_preguntas (
    combo_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    factor_multiplicacion VARCHAR(10),
    PRIMARY KEY (combo_id, pregunta_id)
);

-- Crear tabla de configuración global
CREATE TABLE IF NOT EXISTS configuracion_global (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clave VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255),
    valor VARCHAR(255) NOT NULL,
    version BIGINT DEFAULT 0
);

-- Crear tabla de programas
CREATE TABLE IF NOT EXISTS programas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    temporada INTEGER NOT NULL,
    fecha_emision DATE,
    duracion_acumulada TIME(6),
    duracion_objetivo VARCHAR(255) DEFAULT '1h 5m',
    resultado_acumulado DECIMAL(10,2),
    dato_audiencia_share DECIMAL(5,2),
    dato_audiencia_target DECIMAL(5,2),
    estado ENUM('borrador', 'grabado', 'editado', 'programado', 'emitido') NOT NULL,
    total_premios DECIMAL(10,2),
    gap VARCHAR(255),
    total_concursantes INTEGER,
    creditos_especiales TEXT,
    version BIGINT DEFAULT 0
);

-- Crear tabla de jornadas
CREATE TABLE IF NOT EXISTS jornadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    fecha_jornada DATE,
    lugar VARCHAR(255),
    estado ENUM('preparacion', 'lista', 'en_grabacion', 'completada', 'archivada') NOT NULL DEFAULT 'preparacion',
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    notas TEXT,
    version BIGINT DEFAULT 0
);

-- Crear tabla de relación jornadas-cuestionarios
CREATE TABLE IF NOT EXISTS jornadas_cuestionarios (
    jornada_id BIGINT NOT NULL,
    cuestionario_id BIGINT NOT NULL,
    PRIMARY KEY (jornada_id, cuestionario_id)
);

-- Crear tabla de relación jornadas-combos
CREATE TABLE IF NOT EXISTS jornadas_combos (
    jornada_id BIGINT NOT NULL,
    combo_id BIGINT NOT NULL,
    PRIMARY KEY (jornada_id, combo_id)
);

-- Crear tabla de concursantes
CREATE TABLE IF NOT EXISTS concursantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_concursante INTEGER,
    jornada_id BIGINT,
    dia_grabacion DATE,
    lugar VARCHAR(255),
    nombre VARCHAR(255),
    edad INTEGER,
    ocupacion VARCHAR(255),
    redes_sociales VARCHAR(255),
    cuestionario_id BIGINT,
    combo_id BIGINT,
    factor_x VARCHAR(255),
    resultado INTEGER,
    notas_grabacion TEXT,
    guionista VARCHAR(255),
    valoracion_guionista TEXT,
    estado VARCHAR(255),
    momentos_destacados TEXT,
    duracion VARCHAR(255),
    duracion_direccion VARCHAR(255),
    duracion_final VARCHAR(255),
    valoracion_final TEXT,
    numero_programa INTEGER,
    orden_escaleta INTEGER,
    bonico VARCHAR(255),
    premio DECIMAL(10,2),
    foto VARCHAR(255),
    creditos_especiales TEXT,
    version BIGINT DEFAULT 0
);

-- Añadir restricción única para configuracion_global
ALTER TABLE configuracion_global
    ADD CONSTRAINT UK_n1f89pcjsk127q2qekw84p9wt UNIQUE (clave);

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

ALTER TABLE combos
    ADD CONSTRAINT FK6qmwif52lh67cai38n9tlruw1
    FOREIGN KEY (creacion_usuario_id) REFERENCES usuarios (id);

ALTER TABLE cuestionarios_preguntas
    ADD CONSTRAINT FKth10ov6gek3qugxd2n14oaeuu
    FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios (id);

ALTER TABLE cuestionarios_preguntas
    ADD CONSTRAINT FKdraip2y9m64wkhhspa0twca35
    FOREIGN KEY (pregunta_id) REFERENCES preguntas (id);

ALTER TABLE combos_preguntas
    ADD CONSTRAINT FKrl83qy6m69m8tcpxioo3o5ohq
    FOREIGN KEY (combo_id) REFERENCES combos (id);

ALTER TABLE combos_preguntas
    ADD CONSTRAINT FKqpwe8mb1twqui4hebcnj1j815
    FOREIGN KEY (pregunta_id) REFERENCES preguntas (id);

ALTER TABLE concursantes
    ADD CONSTRAINT FKjfi334ngdgfl0ungoi4mtrvmm
    FOREIGN KEY (combo_id) REFERENCES combos (id);

ALTER TABLE concursantes
    ADD CONSTRAINT FKe46vd5w3bblq8doneuo3ibant
    FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios (id);

-- Claves foráneas para jornadas
ALTER TABLE jornadas
    ADD CONSTRAINT FK_jornada_creacion_usuario
    FOREIGN KEY (creacion_usuario_id) REFERENCES usuarios (id);

ALTER TABLE jornadas_cuestionarios
    ADD CONSTRAINT FK_jornada_cuestionario_jornada
    FOREIGN KEY (jornada_id) REFERENCES jornadas (id);

ALTER TABLE jornadas_cuestionarios
    ADD CONSTRAINT FK_jornada_cuestionario_cuestionario
    FOREIGN KEY (cuestionario_id) REFERENCES cuestionarios (id);

ALTER TABLE jornadas_combos
    ADD CONSTRAINT FK_jornada_combo_jornada
    FOREIGN KEY (jornada_id) REFERENCES jornadas (id);

ALTER TABLE jornadas_combos
    ADD CONSTRAINT FK_jornada_combo_combo
    FOREIGN KEY (combo_id) REFERENCES combos (id);

-- Clave foránea para concursantes-jornada
ALTER TABLE concursantes
    ADD CONSTRAINT FK_concursante_jornada
    FOREIGN KEY (jornada_id) REFERENCES jornadas (id); 