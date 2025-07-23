# 🌟 LSNLS - Sistema de Gestión de Preguntas y Concursantes

## 📋 Descripción General

LSNLS es una aplicación web completa para la gestión de preguntas, cuestionarios, combos y concursantes. Diseñada para sistemas de concursos y programas de televisión, permite gestionar todo el ciclo de vida de las preguntas desde su creación hasta su uso en jornadas de grabación.

## 🎯 Historias de Usuario

### 👤 **ROLE: ADMIN**
**Como administrador del sistema, quiero gestionar usuarios y tener acceso completo a todas las funcionalidades.**

#### Funcionalidades:
- ✅ **Gestión completa de usuarios**: Crear, editar, eliminar usuarios con diferentes roles
- ✅ **Acceso total**: Puede realizar todas las operaciones sin restricciones
- ✅ **Gestión de estados**: Cambiar estados de cualquier entidad sin limitaciones
- ✅ **Auditoría**: Acceso completo a logs y estadísticas del sistema

---

### 👤 **ROLE: DIRECCIÓN**
**Como director del programa, quiero gestionar programas, jornadas y tener control total sobre el contenido.**

#### Funcionalidades:
- ✅ **Gestión de programas**: Crear y gestionar programas de televisión
- ✅ **Gestión de jornadas**: Crear jornadas de grabación con cuestionarios y combos
- ✅ **Control de estados**: Cambiar estados de cualquier entidad
- ✅ **Exportación Excel**: Generar plantillas Excel para jornadas
- ✅ **Gestión de concursantes**: Asignar concursantes a jornadas

---

### 👤 **ROLE: VERIFICACIÓN**
**Como verificador, quiero revisar y aprobar preguntas antes de su uso.**

#### Funcionalidades:
- ✅ **Revisión de preguntas**: Ver preguntas en estado 'borrador' y 'revisar'
- ✅ **Aprobación/Rechazo**: Cambiar estado a 'aprobada' o 'rechazada'
- ✅ **Notas de verificación**: Agregar comentarios y observaciones
- ✅ **Gestión de temas/subtemas**: Agregar y eliminar temas y subtemas
- ✅ **Verificación de contenido**: Revisar respuestas y datos extra

---

### 👤 **ROLE: GUIÓN**
**Como guionista, quiero crear y gestionar preguntas, cuestionarios y combos.**

#### Funcionalidades:
- ✅ **Creación de preguntas**: Crear preguntas con diferentes niveles y temas
- ✅ **Gestión de cuestionarios**: Crear cuestionarios con múltiples preguntas
- ✅ **Gestión de combos**: Crear combos con preguntas multiplicadoras
- ✅ **Edición de contenido**: Modificar preguntas en estado 'borrador' y 'creado'
- ✅ **Gestión de temas**: Agregar nuevos temas y subtemas

---

### 👤 **ROLE: CONSULTA**
**Como usuario de consulta, quiero visualizar información sin poder modificarla.**

#### Funcionalidades:
- ✅ **Visualización completa**: Ver todas las entidades del sistema
- ✅ **Búsquedas y filtros**: Filtrar preguntas, cuestionarios, combos
- ✅ **Estadísticas**: Ver estadísticas y reportes
- ✅ **Sin modificaciones**: Solo permisos de lectura

---

## 🏗️ Arquitectura del Sistema

### **Entidades Principales**

#### 📝 **PREGUNTAS**
- **Estados**: `borrador` → `revisar` → `aprobada`/`rechazada` → `disponible`/`usada`
- **Niveles**: `_1LS`, `_2LS`, `_3LS`, `_4LS`, `_5LS`
- **Temas**: Arte, Historia, Ciencia, Tecnología, Deportes, etc.
- **Campos**: pregunta, respuesta, datos extra, autor, verificador, notas

#### 📋 **CUESTIONARIOS**
- **Estados**: `borrador` → `creado` → `adjudicado` → `grabado`
- **Composición**: Múltiples preguntas con factores multiplicadores
- **Gestión**: Agregar/quitar preguntas, cambiar estados

#### 🎯 **COMBOS**
- **Estados**: `borrador` → `creado` → `adjudicado` → `grabado`
- **Composición**: Preguntas con factores multiplicadores (x2, x3, x)
- **Gestión**: Agregar/quitar preguntas, cambiar estados

#### 👥 **CONCURSANTES**
- **Asignación**: Cuestionario y combo por concursante
- **Gestión**: Asignar/desasignar, cambiar asignaciones
- **Estados**: Los cuestionarios/combos cambian automáticamente de estado

#### 📅 **JORNADAS**
- **Composición**: Múltiples cuestionarios y combos
- **Estados**: `preparacion` → `en_grabacion` → `completada`
- **Exportación**: Genera plantillas Excel para grabación

#### 📺 **PROGRAMAS**
- **Gestión**: Crear y gestionar programas de televisión
- **Estados**: `preparacion` → `programado` → `emitido`

---

## 🔄 Flujos de Trabajo

### **Flujo de Creación de Preguntas**
```
1. GUIÓN crea pregunta → estado 'borrador'
2. VERIFICACIÓN revisa → estado 'revisar' o 'aprobada'
3. Si aprobada → estado 'disponible'
4. Si usada en cuestionario/combo → estado 'usada'
```

### **Flujo de Cuestionarios/Combos**
```
1. GUIÓN crea → estado 'borrador'
2. GUIÓN completa → estado 'creado'
3. DIRECCIÓN asigna a jornada → estado 'adjudicado'
4. Después de grabación → estado 'grabado'
```

### **Flujo de Concursantes**
```
1. DIRECCIÓN asigna concursante a jornada
2. Sistema asigna cuestionario y combo automáticamente
3. Estados cambian: 'creado' → 'adjudicado'
4. Al cambiar asignación: anterior vuelve a 'creado'
```

---

## 🛠️ Tecnologías y Configuración

### **Stack Tecnológico**
- **Backend**: Spring Boot 2.7.18 (Java 11)
- **Base de Datos**: MySQL 8.0+
- **Frontend**: HTML5, CSS3, JavaScript (Bootstrap, Toastify.js)
- **Seguridad**: JWT (JSON Web Tokens) + HTTPS
- **Red**: HTTPS en puerto 8080 (interno) / 8088 (externo)

### **Requisitos del Sistema**
- **Java 11** (obligatorio)
- **Maven 3.6+**
- **MySQL 8.0+**
- **Navegador web moderno**

---

## 🚀 Instalación y Configuración

### **1. Configuración Automática (Recomendado)**
```bash
# Ejecutar script de configuración completa
setup-server-https.bat
```

### **2. Configuración Manual**

#### Paso 1: Preparar Base de Datos
```sql
CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Paso 2: Compilar y Ejecutar
```bash
mvn clean package -DskipTests
java -jar target/lsnls-1.0-SNAPSHOT.jar
```

### **3. Acceso a la Aplicación**

#### **Para desarrollo local:**
```
https://localhost:8080
```

#### **Para acceso desde red local (oficina):**
```
https://[IP-DEL-SERVIDOR]:8080
```

#### **Para acceso desde fuera (externo):**
```
https://[IP-PUBLICA]:8088
```

**📋 Para encontrar las IPs:**
```bash
# Obtener IP local
ipconfig

# Obtener IP pública
get-public-ip.bat
```

**🌐 Configuración del Router:**
- Puerto interno: 8080
- Puerto público: 8088
- Protocolo: TCP
- IP destino: IP del servidor

**⚠️ Certificado Autofirmado:**
El certificado SSL es autofirmado. Al acceder por primera vez:
1. Haz clic en "Avanzado"
2. Haz clic en "Continuar a [IP] (no seguro)"
3. La aplicación funcionará normalmente

---

## 🧪 Tests y Validaciones

### **Tests de Funcionalidad**

#### **Tests de Autenticación**
- ✅ Login con credenciales válidas
- ✅ Login con credenciales inválidas
- ✅ Acceso a rutas protegidas
- ✅ Validación de roles y permisos

#### **Tests de Gestión de Preguntas**
- ✅ Crear pregunta (GUIÓN)
- ✅ Revisar pregunta (VERIFICACIÓN)
- ✅ Aprobar/rechazar pregunta (VERIFICACIÓN)
- ✅ Editar pregunta (GUIÓN)
- ✅ Eliminar pregunta (solo borrador/rechazada)

#### **Tests de Gestión de Cuestionarios**
- ✅ Crear cuestionario (GUIÓN)
- ✅ Agregar/quitar preguntas (GUIÓN)
- ✅ Cambiar estados (según rol)
- ✅ Asignar a jornada (DIRECCIÓN)

#### **Tests de Gestión de Combos**
- ✅ Crear combo (GUIÓN)
- ✅ Agregar preguntas con factores (GUIÓN)
- ✅ Cambiar estados (según rol)
- ✅ Asignar a jornada (DIRECCIÓN)

#### **Tests de Gestión de Concursantes**
- ✅ Asignar concursante a jornada (DIRECCIÓN)
- ✅ Cambiar asignaciones (DIRECCIÓN)
- ✅ Estados automáticos de cuestionarios/combos

#### **Tests de Exportación**
- ✅ Exportar jornada a Excel
- ✅ Formato correcto de plantillas
- ✅ Datos completos en exportación

### **Tests de Concurrencia**
- ✅ Múltiples usuarios simultáneos
- ✅ Control de versiones optimista
- ✅ Prevención de conflictos de datos

### **Tests de Seguridad**
- ✅ Validación de roles
- ✅ Protección de rutas
- ✅ Validación de datos de entrada
- ✅ Prevención de inyección SQL

---

## 🔐 Seguridad y Permisos

### **Sistema de Roles**
| Rol | Preguntas | Cuestionarios | Combos | Concursantes | Jornadas | Usuarios |
|-----|-----------|---------------|--------|--------------|----------|----------|
| **ADMIN** | ✅ Total | ✅ Total | ✅ Total | ✅ Total | ✅ Total | ✅ Total |
| **DIRECCIÓN** | ✅ Total | ✅ Total | ✅ Total | ✅ Total | ✅ Total | ❌ Solo lectura |
| **VERIFICACIÓN** | ✅ Revisar/Aprobar | ✅ Total | ✅ Total | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura |
| **GUIÓN** | ✅ Crear/Editar | ✅ Crear/Editar | ✅ Crear/Editar | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura |
| **CONSULTA** | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura | ❌ Solo lectura |

### **Estados y Permisos**
- **borrador**: Solo GUIÓN puede editar
- **creado**: GUIÓN, VERIFICACIÓN, DIRECCIÓN pueden editar
- **adjudicado**: Solo DIRECCIÓN puede cambiar
- **grabado**: Solo ADMIN puede cambiar

---

## 📊 Funcionalidades Avanzadas

### **Sistema de Auditoría**
- ✅ Logs automáticos de todas las operaciones
- ✅ Registro de usuarios y timestamps
- ✅ Historial de cambios de estado
- ✅ Reportes de actividad

### **Gestión de Temas y Subtemas**
- ✅ Agregar/eliminar temas dinámicamente
- ✅ Agregar/eliminar subtemas dinámicamente
- ✅ Filtrado por temas y subtemas
- ✅ Estadísticas de uso

### **Exportación Excel**
- ✅ Plantillas para jornadas
- ✅ Formato profesional
- ✅ Datos completos y organizados
- ✅ Campos editables para grabación

### **Sistema de Notificaciones**
- ✅ Mensajes de éxito/error
- ✅ Validaciones en tiempo real
- ✅ Confirmaciones de acciones críticas

---

## 🛠️ Mantenimiento y Soporte

### **Backup de Base de Datos**
```bash
# Backup automático
mysqldump -u root -p lsnls > backup_lsnls_$(date +%Y%m%d).sql

# Restaurar backup
mysql -u root -p lsnls < backup_lsnls_20240723.sql
```

### **Logs del Sistema**
- **Ubicación**: `logs/` (si configurado)
- **Nivel**: DEBUG para desarrollo, INFO para producción
- **Rotación**: Automática por Spring Boot

### **Monitoreo**
- ✅ Estado de la aplicación
- ✅ Conexión a base de datos
- ✅ Uso de memoria y CPU
- ✅ Errores y excepciones

---

## 🐛 Solución de Problemas

### **Problemas Comunes**

#### **Error: "Unknown database 'lsnls'"**
```sql
CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### **Error: "Port 8080 already in use"**
```properties
# Cambiar puerto en application.properties
server.port=8081
```

#### **Error: "Class file version 61.0"**
```bash
# Limpiar y recompilar con Java 11
mvn clean compile
```

#### **No se puede acceder desde fuera**
```bash
# Verificar configuración del router
# Puerto 8080 → 8088 debe estar redirigido
# Verificar firewall del servidor
```

### **Verificación de Instalación**
1. ✅ Java 11 instalado: `java -version`
2. ✅ MySQL ejecutándose: `mysql -u root -p`
3. ✅ Base de datos creada: `SHOW DATABASES;`
4. ✅ Aplicación compilada: `ls target/lsnls-*.jar`
5. ✅ Puerto 8080 abierto: `netstat -an | findstr 8080`

---

## 📞 Contacto y Soporte

### **Información del Proyecto**
- **Versión**: 1.0-SNAPSHOT
- **Última actualización**: Julio 2024
- **Compatibilidad**: Java 11, Spring Boot 2.7.18

### **Soporte Técnico**
- **Documentación**: Este README
- **Logs**: Revisar logs de la aplicación
- **Base de datos**: Verificar conexión y datos

---

## 📝 Changelog

### **v1.0-SNAPSHOT (Julio 2024)**
- ✅ Migración completa a Java 11
- ✅ Configuración para acceso externo (puerto 8080/8088)
- ✅ Sistema de roles y permisos completo
- ✅ Gestión de preguntas, cuestionarios, combos
- ✅ Sistema de concursantes y jornadas
- ✅ Exportación Excel
- ✅ Auditoría completa
- ✅ Tests de funcionalidad
- ✅ Documentación unificada 