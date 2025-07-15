-- Configuración UTF-8 para la base de datos LSNLS
-- Ejecutar antes de que Spring Boot cree las tablas

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

-- Eliminar tablas existentes en orden correcto para evitar errores de foreign keys
DROP TABLE IF EXISTS concursantes;
DROP TABLE IF EXISTS cuestionarios_preguntas;
DROP TABLE IF EXISTS combos_preguntas;
DROP TABLE IF EXISTS cuestionarios;
DROP TABLE IF EXISTS combos;
DROP TABLE IF EXISTS preguntas;
DROP TABLE IF EXISTS programas;
DROP TABLE IF EXISTS configuracion_global;
DROP TABLE IF EXISTS usuarios;

-- Crear tabla de usuarios
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION') NOT NULL
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
    estado ENUM('borrador', 'para_verificar', 'verificada', 'revisar', 'corregir', 'rechazada', 'aprobada') NOT NULL,
    estado_disponibilidad ENUM('disponible', 'usada', 'liberada', 'descartada'),
    factor ENUM('X', 'X2', 'X3'),
    nivel ENUM('_0', '_1LS', '_2NLS', '_3LS', '_4NLS', '_5LS', '_5NLS') NOT NULL
);

-- Crear tabla de cuestionarios
CREATE TABLE cuestionarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    estado ENUM('borrador', 'creado', 'adjudicado', 'grabado') NOT NULL,
    nivel ENUM('_1LS', '_2NLS', '_3LS', '_4NLS', 'PM1', 'PM2', 'PM3', 'NORMAL') NOT NULL
);

-- Crear tabla de combos
CREATE TABLE combos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    estado ENUM('borrador', 'creado', 'adjudicado', 'grabado') NOT NULL,
    nivel ENUM('_5LS', '_5NLS', 'NORMAL') NOT NULL
);

-- Crear tabla de relación cuestionarios-preguntas
CREATE TABLE cuestionarios_preguntas (
    cuestionario_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    factor_multiplicacion INTEGER,
    PRIMARY KEY (cuestionario_id, pregunta_id)
);

-- Crear tabla de relación combos-preguntas
CREATE TABLE combos_preguntas (
    combo_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    factor_multiplicacion INTEGER,
    PRIMARY KEY (combo_id, pregunta_id)
);

-- Crear tabla de configuración global
CREATE TABLE configuracion_global (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clave VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255),
    valor VARCHAR(255) NOT NULL
);

-- Crear tabla de programas
CREATE TABLE programas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    temporada INTEGER NOT NULL,
    fecha_emision DATE,
    duracion_acumulada TIME(6),
    resultado_acumulado DECIMAL(10,2),
    dato_audiencia_share DECIMAL(5,2),
    dato_audiencia_target DECIMAL(5,2),
    estado ENUM('borrador', 'grabado', 'editado', 'programado', 'emitido') NOT NULL,
    total_premios DECIMAL(10,2),
    gap VARCHAR(255),
    total_concursantes INTEGER,
    creditos_especiales TEXT
);

-- Crear tabla de concursantes
CREATE TABLE concursantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_concursante INTEGER,
    jornada VARCHAR(255),
    dia_grabacion DATE,
    lugar VARCHAR(255),
    nombre VARCHAR(255),
    edad INTEGER,
    ocupacion VARCHAR(255),
    redes_sociales VARCHAR(255),
    cuestionario_id BIGINT,
    combo_id BIGINT,
    factor_x VARCHAR(255),
    resultado VARCHAR(255),
    notas_grabacion TEXT,
    guionista VARCHAR(255),
    valoracion_guionista TEXT,
    concursantes_por_jornada INTEGER,
    estado VARCHAR(255),
    momentos_destacados TEXT,
    duracion VARCHAR(255),
    valoracion_final TEXT,
    numero_programa INTEGER,
    orden_escaleta INTEGER,
    premio DECIMAL(10,2),
    foto VARCHAR(255),
    creditos_especiales TEXT
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