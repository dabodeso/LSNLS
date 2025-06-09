# LSNLS - Sistema de Gestión de Preguntas y Concursantes

Sistema de gestión para el control de preguntas, cuestionarios, concursantes y programas.

## Requisitos

- Java 17
- Maven 3.8+
- MySQL 8.0+

## Configuración de la Base de Datos

1. Asegúrate de tener MySQL instalado y ejecutándose
2. Crea la base de datos y el usuario ejecutando el script `src/main/resources/schema.sql`

## Configuración del Proyecto

1. Clona el repositorio
2. Configura las credenciales de la base de datos en `src/main/resources/application.properties`
3. Ejecuta `mvn clean install` para construir el proyecto

## Estructura del Proyecto

### Entidades

- **Usuario**: Gestión de usuarios del sistema con roles (consulta, guion, verificacion, direccion)
- **Subtema**: Categorización de preguntas
- **Pregunta**: Preguntas con diferentes niveles y estados
- **Cuestionario**: Agrupación de preguntas para concursantes
- **Concursante**: Información de participantes
- **Programa**: Gestión de programas y resultados

### Características Principales

- Gestión completa de preguntas con estados y niveles
- Control de cuestionarios y su asignación a concursantes
- Seguimiento de programas y resultados
- Sistema de roles para diferentes tipos de usuarios

## Ejecución

```bash
mvn spring-boot:run
```

El servidor se iniciará en `http://localhost:8080` 