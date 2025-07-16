# Gestión de Jornadas - LSNLS

## 🎯 Funcionalidad Implementada

Se ha implementado una **funcionalidad completa de gestión de jornadas** que permite al equipo de producción preparar las jornadas de grabación con exactamente **5 cuestionarios y 5 combos por jornada**, y exportar toda la información en formato Excel para el equipo de grabación.

## 📋 Características Principales

### ✅ **Gestión de Jornadas**
- ✨ **Crear nuevas jornadas** con información completa
- 📝 **Editar jornadas existentes** (excepto completadas/archivadas)
- 🗑️ **Eliminar jornadas** (excepto en grabación)
- 👁️ **Ver detalles completos** de cada jornada
- 🔄 **Cambiar estados** del flujo de trabajo

### ✅ **Selección de Contenido**
- 📊 **Seleccionar exactamente 5 cuestionarios** por jornada
- 🎯 **Seleccionar exactamente 5 combos** por jornada
- 🔍 **Búsqueda y filtrado** de cuestionarios/combos disponibles
- ✋ **Validación automática** de límites (5 máximo)
- 🔗 **Solo elementos en estado "creado"** disponibles

### ✅ **Exportación a Excel**
- 📊 **Formato profesional** con dos hojas separadas
- 📋 **Hoja "CUESTIONARIOS"** con 5 cuestionarios
- 🎯 **Hoja "COMBOS"** con 5 combos
- ✏️ **Campos editables** para el equipo de grabación
- 📝 **Estructura específica** según requerimientos

### ✅ **Estados del Flujo de Trabajo**
- 🔧 **Preparación**: Jornada en proceso de creación
- ✅ **Lista**: Jornada completa y lista para grabar
- 🎬 **En Grabación**: Grabación en curso
- ✔️ **Completada**: Grabación finalizada
- 📁 **Archivada**: Jornada archivada

## 🗂️ Estructura del Excel Exportado

### 📋 **Hoja "CUESTIONARIOS"**

Para cada uno de los 5 cuestionarios:

```
CUESTIONARIO X (ID: xxx)
┌─────────────┬───────┬──────────┬──────────┬─────────────┬─────┐
│ ID PREGUNTA │ NIVEL │ PREGUNTA │ RESPUESTA│ DATOS EXTRA │ REC │
├─────────────┼───────┼──────────┼──────────┼─────────────┼─────┤
│ 123         │ 1LS   │ Pregunta │ Respuesta│ Info extra  │     │
│ 124         │ 2NLS  │ Pregunta │ Respuesta│ Info extra  │     │
│ 125         │ 3LS   │ Pregunta │ Respuesta│ Info extra  │     │
│ 126         │ 4NLS  │ Pregunta │ Respuesta│ Info extra  │     │
└─────────────┴───────┴──────────┴──────────┴─────────────┴─────┘

CONCURSANTE: [____]    RESULTADO: [____]
GRABACIÓN: [____]      NOTAS GUIÓN: [____]
```

### 🎯 **Hoja "COMBOS"**

Para cada uno de los 5 combos:

```
COMBO X (ID: xxx)
┌─────────────┬───────┬──────────┬──────────┬─────────────┬─────┐
│ ID PREGUNTA │ NIVEL │ PREGUNTA │ RESPUESTA│ DATOS EXTRA │ REC │
├─────────────┼───────┼──────────┼──────────┼─────────────┼─────┤
│ 127         │ 5LS   │ Pregunta │ Respuesta│ Info extra  │     │
│ 128         │ 5NLS  │ Pregunta │ Respuesta│ Info extra  │     │
│ 129         │ 5LS   │ Pregunta │ Respuesta│ Info extra  │     │
└─────────────┴───────┴──────────┴──────────┴─────────────┴─────┘

CONCURSANTE: [____]    RESULTADO: [____]
GRABACIÓN: [____]      NOTAS GUIÓN: [____]
```

## 🖥️ Interfaz de Usuario

### 📱 **Página Principal de Jornadas**
- 🎨 **Diseño responsive** y moderno
- 🏷️ **Cards visuales** para cada jornada
- 🔍 **Filtros avanzados** (estado, fecha, búsqueda)
- ⚡ **Acciones rápidas** (ver, editar, exportar, eliminar)

### 🛠️ **Modal de Creación/Edición**
- 📝 **Formulario completo** con validaciones
- 🔄 **Selección visual** de cuestionarios y combos
- 📊 **Vista previa** de slots (5 cuestionarios + 5 combos)
- 🔍 **Búsqueda en tiempo real** de elementos

### 📋 **Selectores de Contenido**
- 📋 **Lista de cuestionarios disponibles**
- 🎯 **Lista de combos disponibles**
- ✅ **Selección múltiple** con límites
- 🔍 **Búsqueda y filtrado**

## 🔧 Implementación Técnica

### 🏗️ **Backend (Spring Boot)**

#### **Entidades Nuevas:**
- `Jornada.java` - Entidad principal de jornadas
- `JornadaDTO.java` - DTO para transferencia de datos

#### **Servicios:**
- `JornadaService.java` - Lógica de negocio completa
- `ExcelExportService.java` - Generación de archivos Excel

#### **Controlador:**
- `JornadaController.java` - Endpoints REST completos

#### **Repositorio:**
- `JornadaRepository.java` - Acceso a datos

### 🎨 **Frontend (JavaScript + Bootstrap)**

#### **Interfaz:**
- `jornadas.html` - Página principal
- `jornadas.js` - Lógica JavaScript completa

#### **Funcionalidades:**
- Gestión completa de jornadas
- Selección visual de contenido
- Exportación de Excel
- Filtros y búsquedas

### 🗃️ **Base de Datos**

#### **Nuevas Tablas:**
```sql
-- Tabla principal
CREATE TABLE jornadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    fecha_jornada DATE,
    lugar VARCHAR(255),
    estado ENUM('preparacion', 'lista', 'en_grabacion', 'completada', 'archivada'),
    creacion_usuario_id BIGINT NOT NULL,
    fecha_creacion datetime(6),
    notas TEXT
);

-- Relación jornadas-cuestionarios (muchos a muchos)
CREATE TABLE jornadas_cuestionarios (
    jornada_id BIGINT NOT NULL,
    cuestionario_id BIGINT NOT NULL,
    PRIMARY KEY (jornada_id, cuestionario_id)
);

-- Relación jornadas-combos (muchos a muchos)
CREATE TABLE jornadas_combos (
    jornada_id BIGINT NOT NULL,
    combo_id BIGINT NOT NULL,
    PRIMARY KEY (jornada_id, combo_id)
);
```

## 🚀 Endpoints API

### **Gestión de Jornadas**
```
GET    /api/jornadas                    # Obtener todas las jornadas
GET    /api/jornadas/{id}               # Obtener jornada específica
POST   /api/jornadas                    # Crear nueva jornada
PUT    /api/jornadas/{id}               # Actualizar jornada
DELETE /api/jornadas/{id}               # Eliminar jornada
PUT    /api/jornadas/{id}/estado        # Cambiar estado
```

### **Exportación**
```
GET    /api/jornadas/{id}/exportar-excel # Exportar jornada a Excel
```

### **Datos de Apoyo**
```
GET    /api/jornadas/cuestionarios-disponibles # Cuestionarios disponibles
GET    /api/jornadas/combos-disponibles        # Combos disponibles
```

## 🔐 Permisos y Seguridad

- ✅ **Autenticación JWT** requerida para todos los endpoints
- 🔒 **Autorización por roles** integrada con el sistema existente
- 👥 **Control de acceso** según permisos del usuario
- 🛡️ **Validaciones** de datos en backend y frontend

## 📊 Validaciones Implementadas

### **Jornadas:**
- ✅ Nombre obligatorio y único
- ✅ Máximo 5 cuestionarios por jornada
- ✅ Máximo 5 combos por jornada
- ✅ Solo cuestionarios/combos en estado "creado"

### **Estados:**
- ✅ Flujo de estados controlado
- ✅ Edición restringida según estado
- ✅ Eliminación restringida en grabación

### **Excel:**
- ✅ Manejo de errores en generación
- ✅ Nombres de archivo dinámicos
- ✅ Formato profesional consistente

## 🎯 Casos de Uso Principales

### 1️⃣ **Preparar Jornada de Grabación**
1. Crear nueva jornada con información básica
2. Seleccionar 5 cuestionarios de la lista disponible
3. Seleccionar 5 combos de la lista disponible
4. Marcar jornada como "lista"

### 2️⃣ **Exportar para Grabación**
1. Acceder a jornada lista
2. Hacer clic en "Exportar Excel"
3. Descargar archivo con formato específico
4. Usar en grabación con campos editables

### 3️⃣ **Gestionar Flujo de Trabajo**
1. Cambiar estado según progreso
2. Ver historial y detalles
3. Archivar jornadas completadas

## 🔄 Flujo de Trabajo Completo

```
Preparación → Lista → En Grabación → Completada → Archivada
    ↓           ↓          ↓            ↓          ↓
  Editable   Editable   Bloqueada   Bloqueada  Bloqueada
```

## 📁 Archivos Modificados/Creados

### **Nuevos Archivos:**
- `src/main/java/com/lsnls/entity/Jornada.java`
- `src/main/java/com/lsnls/dto/JornadaDTO.java`
- `src/main/java/com/lsnls/repository/JornadaRepository.java`
- `src/main/java/com/lsnls/service/JornadaService.java`
- `src/main/java/com/lsnls/service/ExcelExportService.java`
- `src/main/java/com/lsnls/controller/JornadaController.java`
- `src/main/resources/static/jornadas.html`
- `src/main/resources/static/js/jornadas.js`

### **Archivos Modificados:**
- `pom.xml` - Dependencias Apache POI
- `src/main/resources/schema.sql` - Nuevas tablas
- `src/main/resources/data.sql` - Datos de ejemplo
- `src/main/resources/static/dashboard.html` - Navegación

## 🎉 Beneficios Implementados

### ✅ **Para el Equipo de Producción:**
- 🎯 **Organización perfecta** de jornadas de grabación
- 📊 **Control total** sobre cuestionarios y combos
- 🔄 **Flujo de trabajo** estructurado y claro
- 📋 **Exportación automática** en formato requerido

### ✅ **Para el Equipo de Grabación:**
- 📝 **Excel listo para usar** con formato específico
- ✏️ **Campos editables** para anotaciones en vivo
- 📋 **Información completa** y organizada
- 🎯 **Separación clara** entre cuestionarios y combos

### ✅ **Para el Sistema:**
- 🔗 **Integración completa** con sistema existente
- 🔐 **Seguridad consistente** con roles y permisos
- 📊 **Base de datos optimizada** con relaciones apropiadas
- 🚀 **Rendimiento óptimo** con carga eficiente

## 🔮 Funcionalidades Futuras (Opcionales)

- 📅 **Calendario visual** de jornadas
- 📊 **Estadísticas** y reportes de jornadas
- 🔄 **Plantillas** de jornadas recurrentes
- 📱 **Notificaciones** de cambios de estado
- 🎯 **Asignación automática** de cuestionarios/combos
- 📈 **Dashboard** de seguimiento de grabaciones

---

## 🚀 ¡Funcionalidad Lista para Usar!

La gestión de jornadas está **completamente implementada y funcional**. El equipo puede comenzar a:

1. ✅ **Crear jornadas** con 5 cuestionarios y 5 combos
2. ✅ **Exportar Excel** con el formato exacto requerido
3. ✅ **Gestionar el flujo** completo de grabación
4. ✅ **Usar en producción** inmediatamente

**¡El sistema está listo para mejorar significativamente el proceso de grabación de LSNLS!** 🎬📺 