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