# 🛡️ Sistema de Control de Concurrencia - LSNLS

## 🎯 **Propósito**

Este sistema previene conflictos cuando múltiples usuarios trabajan simultáneamente con los mismos datos, especialmente:
- Edición simultánea de preguntas
- Asignación simultánea de cuestionarios/combos
- Cambios de estado concurrentes
- Eliminación durante uso

## 🔧 **Soluciones Implementadas**

### **1. Optimistic Locking (@Version)**
**Ubicación**: Todas las entidades principales
**Funcionamiento**: Detecta automáticamente cuando dos usuarios editan la misma entidad

**Entidades con versionado:**
- ✅ `Pregunta` - Protege contra edición simultánea
- ✅ `Cuestionario` - Protege modificaciones de estructura  
- ✅ `Combo` - Protege cambios en preguntas multiplicadoras
- ✅ `Concursante` - Protege asignaciones
- ✅ `Jornada` - Protege configuración de jornadas
- ✅ `Usuario` - Protege cambios de perfil/roles

**Ejemplo de uso:**
```java
@Entity
public class Pregunta {
    @Id
    private Long id;
    
    @Version  // ← Esta anotación detecta conflictos automáticamente
    private Long version;
}
```

### **2. Validaciones de Concurrencia Específicas**
**Ubicación**: Todos los controladores principales

**Mensajes de error específicos:**
```http
HTTP 409 - "La pregunta ha sido modificada por otro usuario. Por favor, recarga la página y vuelve a intentarlo."
HTTP 409 - "El cuestionario ha sido modificado por otro usuario. Por favor, recarga la página y vuelve a intentarlo."
```

**Controladores protegidos:**
- ✅ `PreguntaController` - Métodos: actualizar, aprobar, rechazar
- ✅ `CuestionarioController` - Métodos: actualizar, cambiarEstado
- ✅ `ComboController` - Métodos: actualizar, eliminar
- ✅ `ConcursanteController` - Métodos: update, asignarPrograma
- ✅ `JornadaController` - Métodos: crear, actualizar

### **3. Operaciones Atómicas**
**Ubicación**: Servicios críticos
**Funcionamiento**: Cambios de estado con verificación en una sola operación SQL

**Métodos implementados:**
```java
// CuestionarioService
public boolean cambiarEstadoAtomico(Long id, EstadoCuestionario estadoEsperado, EstadoCuestionario nuevoEstado)

// ComboService  
public boolean cambiarEstadoAtomico(Long id, EstadoCombo estadoEsperado, EstadoCombo nuevoEstado)
```

**Query SQL atómica:**
```sql
UPDATE cuestionarios SET estado = ? WHERE id = ? AND estado = ?
-- ↑ Solo cambia si el estado actual coincide con el esperado
```

### **4. Validaciones de Dependencias Robustas**
**Ubicación**: Métodos eliminar() en servicios

**Validaciones implementadas:**
- ✅ Verificar existencia antes de eliminar
- ✅ Verificar estados (no eliminar si asignado)
- ✅ Verificar dependencias (cuestionarios usados por concursantes)
- ✅ Contar referencias activas
- ✅ Mensajes específicos por tipo de conflicto

## 🚨 **Escenarios Protegidos**

### **Escenario 1: Edición Simultánea**
```
Usuario A: Edita pregunta (version=1) → pregunta.texto = "Nueva pregunta"
Usuario B: Edita pregunta (version=1) → pregunta.respuesta = "Nueva respuesta"

❌ ANTES: Se perdía un cambio
✅ AHORA: B recibe HTTP 409 - "Pregunta modificada por otro usuario"
```

### **Escenario 2: Asignación Simultánea**
```
Usuario A: Asigna cuestionario X a jornada 1
Usuario B: Asigna cuestionario X a concursante Y

❌ ANTES: Estado inconsistente  
✅ AHORA: Operación atómica - solo uno tiene éxito
```

### **Escenario 3: Eliminación Durante Uso**
```
Usuario A: Elimina pregunta P
Usuario B: Asigna pregunta P a combo

❌ ANTES: Referencia rota
✅ AHORA: A recibe error - "Pregunta usada en combos"
```

## 📊 **Códigos de Estado HTTP**

| Código | Significado | Cuándo ocurre |
|--------|-------------|---------------|
| `409 Conflict` | Conflicto de concurrencia | Optimistic locking detectó cambio simultáneo |
| `400 Bad Request` | Validación falló | Estado incorrecto para operación |
| `404 Not Found` | Entidad no existe | ID inexistente |
| `403 Forbidden` | Sin permisos | Rol insuficiente |

## 🔧 **Instalación y Migración**

### **1. Ejecutar Migración de BD**
```sql
-- Ejecutar este archivo:
migracion_versionado_concurrencia.sql
```

### **2. Verificar Migración**
```sql
-- Verificar columnas version agregadas
DESCRIBE preguntas;
DESCRIBE cuestionarios;
-- etc.
```

### **3. Reiniciar Aplicación**
- Las nuevas anotaciones @Version se cargan automáticamente
- No requiere cambios de configuración

## 🧪 **Testing de Concurrencia**

### **Test Manual 1: Edición Simultánea**
1. Abrir la misma pregunta en 2 navegadores
2. Editar en navegador A → Guardar ✅
3. Editar en navegador B → Error 409 ✅

### **Test Manual 2: Asignación Simultánea**  
1. Intentar asignar mismo cuestionario a 2 entidades diferentes
2. Solo una asignación debería tener éxito ✅

### **Test Manual 3: Eliminación Protegida**
1. Asignar pregunta a cuestionario
2. Intentar eliminar pregunta → Error con mensaje específico ✅

## 🚀 **Beneficios Inmediatos**

### **Para Usuarios:**
- ✅ No más pérdida de cambios
- ✅ Mensajes claros sobre conflictos  
- ✅ Guía sobre qué hacer (recargar página)

### **Para Administradores:**
- ✅ Datos consistentes siempre
- ✅ No más estados corruptos
- ✅ Trazabilidad de conflictos en logs

### **Para Desarrolladores:**
- ✅ Sistema robusto y mantenible
- ✅ Errores específicos facilitan debugging
- ✅ Extensible a nuevas entidades

## ⚙️ **Configuración Avanzada**

### **Timeout de Optimistic Locking**
```properties
# application.properties
spring.jpa.properties.javax.persistence.lock.timeout=10000
```

### **Logging de Concurrencia**
```properties
# Activar logs de conflictos
logging.level.org.springframework.orm=DEBUG
logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=DEBUG
```

## 🔍 **Monitoreo**

### **Métricas a Monitorear:**
- Frecuencia de errores 409
- Tiempo de respuesta en operaciones críticas
- Volumen de operaciones simultáneas

### **Alertas Recomendadas:**
- Si errores 409 > 10% de operaciones
- Si tiempo de respuesta > 2 segundos en updates

---

**🎯 Con este sistema, la aplicación LSNLS es ahora completamente segura para uso multi-usuario simultáneo.** 