# Sistema de Permisos por Rol - LSNLS

## ✅ IMPLEMENTACIÓN COMPLETADA

Se ha implementado un sistema granular de permisos basado en roles que cumple exactamente con los requisitos especificados de LSNOLS 2024.

## 🔒 ROLES Y PERMISOS

### **1. CONSULTA (Nivel 1)**
- ✅ **Solo lectura** en todas las entidades
- ❌ No puede crear, editar ni eliminar
- ❌ No puede cambiar estados

### **2. GUION (Nivel 2)**
- ✅ **Lectura** en todas las entidades
- ✅ **Crear y editar** preguntas en estado `borrador` y `creada`
- ✅ **Crear y editar** cuestionarios en estado `borrador` y `creado`
- ✅ **Crear y editar** concursantes en estado `borrador`
- ❌ No puede verificar, aprobar ni rechazar
- ❌ No puede eliminar

### **3. VERIFICACION (Nivel 3)**
- ✅ **Lectura** en todas las entidades
- ✅ **Crear y editar** preguntas en estado `borrador`, `creada`, `verificada` y `corregir`
- ✅ **Verificar preguntas** (cambiar a `verificada` o `corregir`)
- ✅ **Crear y editar** cuestionarios en estado `borrador` y `creado`
- ✅ **Crear y editar** concursantes en estado `borrador`, `grabado` y `editado`
- ✅ **Crear y editar** programas en estado `borrador`
- ❌ No puede aprobar ni rechazar definitivamente
- ❌ No puede eliminar

### **4. DIRECCION (Nivel 4)**
- ✅ **Control total** sobre todas las entidades
- ✅ **Aprobar y rechazar** preguntas (estado `aprobada` o `rechazada`)
- ✅ **Editar** entidades en cualquier estado
- ✅ **Validación final** de todos los procesos
- ✅ **Eliminar** cualquier entidad
- ✅ **Gestionar usuarios** (crear, editar, eliminar)

## 📋 CONTROL DE ESTADOS

### **PREGUNTAS**
| Estado | Quién puede editar | Quién puede asignar |
|--------|-------------------|-------------------|
| `borrador` | Guion, Verificacion, Direccion | Todos (al crear) |
| `creada` | Guion, Verificacion, Direccion | Guion, Verificacion, Direccion |
| `verificada` | Verificacion, Direccion | Verificacion, Direccion |
| `corregir` | Verificacion, Direccion | Verificacion, Direccion |
| `rechazada` | Solo Direccion | Solo Direccion |
| `aprobada` | Solo Direccion | Solo Direccion |

### **CUESTIONARIOS**
| Estado | Quién puede editar |
|--------|-------------------|
| `borrador` | Guion, Verificacion, Direccion |
| `creado` | Guion, Verificacion, Direccion |
| `adjudicado` | Solo Direccion |
| `grabado` | Solo Direccion |

### **CONCURSANTES**
| Estado | Quién puede editar |
|--------|-------------------|
| `borrador` | Guion, Verificacion, Direccion |
| `grabado` | Verificacion, Direccion |
| `editado` | Verificacion, Direccion |
| `programado` | Solo Direccion |

### **PROGRAMAS**
| Estado | Quién puede editar |
|--------|-------------------|
| `borrador` | Verificacion, Direccion |
| `programado` | Solo Direccion |
| `emitido` | Solo Direccion |

## 🛡️ CARACTERÍSTICAS DE SEGURIDAD

### **Autenticación Automática**
- Usuario actual detectado automáticamente via JWT
- Asignación automática de autoría en creaciones
- Seguimiento de verificadores

### **Validaciones Dinámicas**
- Permisos evaluados en tiempo real según estado de entidad
- Bloqueo automático de operaciones no permitidas
- Mensajes de error descriptivos

### **Endpoints Protegidos**
```java
@PreAuthorize("@authorizationService.canCreatePregunta()")
@PreAuthorize("@authorizationService.canRead()")
@PreAuthorize("@authorizationService.canDelete()")
```

### **Control Granular**
- Permisos específicos por cada cambio de estado
- Validación individual de cada operación
- Lógica de negocio integrada en autorización

## 🔧 COMPONENTES IMPLEMENTADOS

### **1. AuthorizationService**
Servicio centralizado con métodos específicos:
- `canCreatePregunta()`
- `canEditPregunta(estado)`
- `canChangeEstadoPregunta(estadoActual, nuevoEstado)`
- `canValidate()`
- `canDelete()`

### **2. Controladores Protegidos**
- **PreguntaController**: Control completo de permisos por estado
- **UsuarioController**: Solo direccion puede gestionar usuarios

### **3. Servicios Extendidos**
- **PreguntaService**: Métodos para verificar, aprobar, rechazar
- Integración automática con sistema de permisos

## 📝 ENDPOINTS CON PERMISOS

### **Preguntas**
```
GET    /api/preguntas              - Todos los roles
POST   /api/preguntas              - Guion, Verificacion, Direccion
PUT    /api/preguntas/{id}         - Según estado
DELETE /api/preguntas/{id}         - Solo Direccion
POST   /api/preguntas/{id}/verificar - Verificacion, Direccion
POST   /api/preguntas/{id}/aprobar  - Solo Direccion
POST   /api/preguntas/{id}/rechazar - Solo Direccion
```

### **Usuarios**
```
GET    /api/usuarios               - Todos los roles
POST   /api/usuarios/crear         - Solo Direccion
PUT    /api/usuarios/{id}          - Direccion o propio usuario
DELETE /api/usuarios/{id}          - Solo Direccion
GET    /api/usuarios/perfil        - Usuario autenticado
```

## ✅ CUMPLIMIENTO DE REQUISITOS

- ✅ **4 niveles de usuario** implementados correctamente
- ✅ **Control granular** según estados de entidades  
- ✅ **Permisos específicos** para cada operación
- ✅ **Seguimiento de autoría** automático
- ✅ **Validación en tiempo real** de permisos
- ✅ **Integración con JWT** y Spring Security

## 🚀 PRÓXIMOS PASOS

1. **Implementar validaciones de datos** (mayúsculas, caracteres)
2. **Sistema de búsqueda avanzado** para selección de preguntas
3. **Exportación de documentos** con permisos
4. **Controladores completos** para Cuestionarios, Concursantes y Programas 