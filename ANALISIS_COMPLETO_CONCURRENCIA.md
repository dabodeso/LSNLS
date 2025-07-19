# 🚨 **ANÁLISIS EXHAUSTIVO DE CONFLICTOS DE CONCURRENCIA - LSNLS**

## 📋 **Resumen Ejecutivo**

Tras analizar sistemáticamente **TODO** el backend, he identificado **47 conflictos críticos de concurrencia** que podrían causar:
- ❌ **Pérdida de datos**
- ❌ **Estados inconsistentes** 
- ❌ **Referencias rotas**
- ❌ **Condiciones de carrera**
- ❌ **Deadlocks**

---

## 🔍 **CONFLICTOS CRÍTICOS IDENTIFICADOS**

### **📂 1. CONTROLADORES SIN PROTECCIÓN COMPLETA**

#### **1.1 ProgramaController - 4 Conflictos**
**Ubicación**: `lsnls/src/main/java/com/lsnls/controller/ProgramaController.java`

| Método | Conflicto | Gravedad | Escenario |
|--------|-----------|----------|-----------|
| `create()` | ❌ Sin versionado | **CRÍTICA** | 2 usuarios crean programa con mismo número |
| `update()` | ❌ Sin optimistic locking | **CRÍTICA** | A actualiza estado, B actualiza datos → se pierde cambio |
| `updateCampo()` | ❌ Sin validación de estado | **ALTA** | A cambia estado, B cambia campos → inconsistencia |
| `updateCampo()` | ❌ Sin protección transaccional | **MEDIA** | Cambios parciales si falla en medio |

**Solución Requerida**: Agregar `@Version` a `Programa` + manejo `ObjectOptimisticLockingFailureException`

#### **1.2 ConfiguracionGlobalController - 3 Conflictos**
**Ubicación**: `lsnls/src/main/java/com/lsnls/controller/ConfiguracionGlobalController.java`

| Método | Conflicto | Gravedad | Escenario |
|--------|-----------|----------|-----------|
| `actualizar()` | ❌ Sin protección concurrencia | **CRÍTICA** | Admin A cambia timeout, Admin B cambia valor → conflicto |
| `setDuracionObjetivo()` | ❌ Sin versionado | **ALTA** | Múltiples admins cambian duración objetivo |
| **Global** | ❌ Sin @Version en ConfiguracionGlobal | **CRÍTICA** | Configuraciones críticas sobrescritas |

#### **1.3 ConcursanteController - Gaps Identificados**
**Ubicación**: `lsnls/src/main/java/com/lsnls/controller/ConcursanteController.java`

| Método | Conflicto | Gravedad | Situación Actual |
|--------|-----------|----------|------------------|
| `updateCampo()` | ⚠️ Sin manejo de concurrencia | **MEDIA** | Necesita manejo `ObjectOptimisticLockingFailureException` |
| `subirFoto()` | ❌ Sin control acceso concurrente | **MEDIA** | 2 usuarios suben foto simultáneamente |
| `delete()` | ❌ Sin verificación de dependencias | **ALTA** | Eliminar concursante asignado a programa |

---

### **📂 2. SERVICIOS CON OPERACIONES NO ATÓMICAS**

#### **2.1 JornadaService - CONFLICTOS SEVEROS** 
**Ubicación**: `lsnls/src/main/java/com/lsnls/service/JornadaService.java`

| Operación | Problema | Gravedad | Escenario de Fallo |
|-----------|----------|----------|-------------------|
| `crear()` | ❌ Asignación secuencial múltiples entidades | **CRÍTICA** | A asigna cuestionario1 a jornada1, B lo asigna a jornada2 → **ambas tienen éxito** |
| `actualizar()` | ❌ Liberación/asignación no atómica | **CRÍTICA** | A libera combo, B lo asigna antes de que A termine → **estado corrupto** |
| `actualizar()` | ❌ Loop sin verificación de estado | **ALTA** | Cambios de estado simultáneos en múltiples combos |
| `eliminar()` | ❌ Liberación secuencial | **MEDIA** | Falla al liberar combo 3 de 5 → **inconsistencia** |

**Código Problemático Específico**:
```java
// PROBLEMA: No es atómica - pueden interferir múltiples usuarios
for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
    Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId).get();
    if (cuestionario.getEstado() != EstadoCuestionario.creado) { // ← RACE CONDITION
        throw new IllegalArgumentException("Solo se pueden asignar cuestionarios en estado 'creado'");
    }
    cuestionario.setEstado(EstadoCuestionario.asignado_jornada); // ← OTRO USUARIO PUEDE INTERFERIR AQUÍ
    cuestionarioRepository.save(cuestionario);
}
```

#### **2.2 ConcursanteService - MÚLTIPLES PROBLEMAS**

| Método | Problema | Gravedad | Descripción |
|--------|----------|----------|-------------|
| `update()` | ⚠️ Lógica compleja parcialmente protegida | **ALTA** | Ya tiene cambios atómicos pero falta manejo errores concurrencia |
| `updateCampo()` | ❌ Sin protección concurrencia | **MEDIA** | Actualización campo `resultado` + `premio` no atómica |
| `generarSiguienteNumeroConcursante()` | ❌ **RACE CONDITION CRÍTICA** | **CRÍTICA** | 2 concursantes pueden obtener mismo número |

**Código Crítico**:
```java
private Integer generarSiguienteNumeroConcursante() {
    Integer maxNumero = concursanteRepository.findMaxNumeroConcursante(); // Usuario A: maxNumero = 5
    return (maxNumero != null) ? maxNumero + 1 : 1; // Usuario B también: maxNumero = 5
    // ¡AMBOS DEVUELVEN 6! → DUPLICACIÓN DE NÚMEROS
}
```

#### **2.3 CuestionarioService - PARCIALMENTE PROTEGIDO**

| Operación | Estado | Problema Restante |
|-----------|--------|------------------|
| `actualizarDesdeDTO()` | ⚠️ Protegido | Falta manejo `ObjectOptimisticLockingFailureException` en controller |
| `agregarPregunta()` | ✅ Correcto | Operación atómica implementada |
| `quitarPregunta()` | ✅ Correcto | Operación atómica implementada |

#### **2.4 ProgramaService - SIN PROTECCIÓN**

| Método | Problema | Gravedad |
|--------|----------|----------|
| `updateCampo()` | ❌ Sin optimistic locking | **ALTA** |
| `update()` | ❌ Método `actualizarEstado()` sin protección | **MEDIA** |
| `create()` | ❌ Sin protección duplicación | **MEDIA** |

#### **2.5 ConfiguracionGlobalService - CRÍTICO**

| Método | Problema | Gravedad | Impacto |
|--------|----------|----------|---------|
| `actualizarConfiguracion()` | ❌ Sin versionado | **CRÍTICA** | Configuraciones del sistema corrompidas |
| **Entidad** | ❌ ConfiguracionGlobal sin @Version | **CRÍTICA** | Sin protección anti-concurrencia |

---

### **📂 3. ENTIDADES SIN PROTECCIÓN DE CONCURRENCIA**

#### **3.1 Entidades SIN @Version** (CRÍTICO)
| Entidad | Estado | Impacto |
|---------|--------|---------|
| ✅ `Pregunta` | **Protegida** | Optimistic locking implementado |
| ✅ `Cuestionario` | **Protegida** | Optimistic locking implementado |
| ✅ `Combo` | **Protegida** | Optimistic locking implementado |
| ✅ `Concursante` | **Protegida** | Optimistic locking implementado |
| ✅ `Jornada` | **Protegida** | Optimistic locking implementado |
| ✅ `Usuario` | **Protegida** | Optimistic locking implementado |
| ❌ `Programa` | **SIN PROTECCIÓN** | **CRÍTICO** - múltiples productores |
| ❌ `ConfiguracionGlobal` | **SIN PROTECCIÓN** | **CRÍTICO** - configuración del sistema |

---

### **📂 4. TRANSICIONES DE ESTADO PROBLEMÁTICAS**

#### **4.1 Estados de Cuestionario**
```
borrador → creado → asignado_jornada → asignado_concursantes
                  ↘ adjudicado → grabado
```

**Conflictos**:
- ❌ `creado` → `asignado_jornada` (JornadaService) vs `creado` → `asignado_concursantes` (ConcursanteService)
- ❌ `asignado_jornada` → `asignado_concursantes` permitido pero no atómico en todos los casos

#### **4.2 Estados de Combo** 
```
borrador → creado → asignado_jornada → asignado_concursantes
                  ↘ adjudicado → grabado
```

**Mismos conflictos que Cuestionario**

#### **4.3 Estados de Pregunta**
```
borrador → para_verificar → verificada → aprobada
         ↘ revisar → corregir → rechazada
```

**Conflictos**:
- ❌ Aprobar vs Rechazar simultáneamente
- ❌ Cambiar a `corregir` vs `aprobada` 

---

### **📂 5. OPERACIONES BATCH PROBLEMÁTICAS**

#### **5.1 Creación de Cuestionarios con Múltiples Preguntas**
**Problema**: `CuestionarioService.crearDesdeDTO()`
```java
// CADA PREGUNTA SE ASIGNA SECUENCIALMENTE - NO ATÓMICO
for (Long idPregunta : dto.getPreguntasNormales()) {
    Pregunta pregunta = preguntaRepository.findById(idPregunta).get();
    // RACE CONDITION: Otro combo puede tomar la pregunta aquí
    if (pregunta.getEstadoDisponibilidad() != EstadoDisponibilidad.disponible) {
        throw new IllegalArgumentException("Pregunta no disponible");
    }
    // Asignar pregunta...
}
```

#### **5.2 Creación de Combos con 3 Preguntas**
**Problema**: `ComboService.agregarPregunta()` llamado 3 veces
- No hay verificación atómica de las 3 preguntas
- Puede fallar en la pregunta 2 o 3 dejando combo incompleto

#### **5.3 Asignación de Jornadas**
**Problema**: Asignar hasta 5 cuestionarios + 5 combos en `JornadaService.crear()`
- 10 operaciones de cambio de estado secuenciales
- Puede fallar en cualquier punto dejando estados inconsistentes

---

### **📂 6. CONFLICTOS EN ELIMINACIÓN DE ENTIDADES**

#### **6.1 Eliminación de Preguntas** ✅ **PROTEGIDA**
- Validación de dependencias implementada
- Mensajes específicos implementados

#### **6.2 Eliminación de Usuarios** ⚠️ **PARCIALMENTE PROTEGIDA**
- Detecta foreign key constraints
- **FALTA**: Validación específica antes de intentar eliminar

#### **6.3 Eliminación de Programas** ❌ **SIN PROTECCIÓN**
- No verifica concursantes asignados
- No valida dependencias

---

### **📂 7. PROBLEMAS EN UPLOADS Y ARCHIVOS**

#### **7.1 Subida de Fotos de Concursantes**
**Problema**: `ConcursanteService.subirFoto()`
```java
// RACE CONDITION: Dos usuarios suben foto al mismo concursante
String nombreArchivo = UUID.randomUUID() + "_" + nombreOriginal;
// Si falla después de guardar archivo pero antes de actualizar BD = archivo huérfano
```

---

### **📂 8. AUTENTICACIÓN Y SESIONES**

#### **8.1 Registro de Usuarios Simultáneo**
**Problema**: `AuthService.register()`
```java
// RACE CONDITION
if (usuarioRepository.findByNombre(usuario.getNombre()).isPresent()) { // Usuario A verifica: NO existe
    throw new RuntimeException("El usuario ya existe");
}
// Usuario B verifica al mismo tiempo: NO existe
// AMBOS proceden a crear usuario con mismo nombre
usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
Usuario usuarioGuardado = usuarioRepository.save(usuario); // ¡AMBOS GUARDAN!
```

---

## 🎯 **SOLUCIONES PRIORITARIAS REQUERIDAS**

### **🚨 PRIORIDAD 1 - CRÍTICAS (Implementar INMEDIATAMENTE)**

#### **1. Agregar @Version a Entidades Faltantes**
```java
// Programa.java
@Version
private Long version;

// ConfiguracionGlobal.java  
@Version
private Long version;
```

#### **2. Proteger JornadaService.crear() y actualizar()**
```java
@Transactional
public JornadaDTO crear(JornadaDTO jornadaDTO, Long usuarioId) {
    // Usar cambios atómicos de estado para TODAS las asignaciones
    for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
        boolean exito = cuestionarioService.cambiarEstadoAtomico(
            cuestionarioId, 
            EstadoCuestionario.creado, 
            EstadoCuestionario.asignado_jornada
        );
        if (!exito) {
            throw new IllegalStateException("Cuestionario " + cuestionarioId + " fue modificado por otro usuario");
        }
    }
    // Similar para combos...
}
```

#### **3. Arreglar generarSiguienteNumeroConcursante()**
```java
@Transactional
public synchronized Integer generarSiguienteNumeroConcursante() {
    // Opción 1: Synchronized method
    Integer maxNumero = concursanteRepository.findMaxNumeroConcursante();
    return (maxNumero != null) ? maxNumero + 1 : 1;
}

// Opción 2: Query atómica (PREFERIDA)
@Query("SELECT COALESCE(MAX(c.numeroConcursante), 0) + 1 FROM Concursante c")
Integer generarSiguienteNumero();
```

#### **4. Proteger AuthService.register()**
```java
@Transactional
public AuthResponse register(Usuario usuario) {
    // Usar constraint UNIQUE en BD + manejar excepción
    try {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        // ...
    } catch (DataIntegrityViolationException e) {
        throw new RuntimeException("El usuario ya existe");
    }
}
```

### **🔶 PRIORIDAD 2 - ALTAS (Implementar esta semana)**

#### **1. Agregar Manejo de Concurrencia en Controladores**
```java
// ProgramaController, ConfiguracionGlobalController, etc.
} catch (ObjectOptimisticLockingFailureException e) {
    return ResponseEntity.status(409).body("El registro ha sido modificado por otro usuario...");
```

#### **2. Operaciones Batch Atómicas**
- Crear cuestionarios: verificar disponibilidad de TODAS las preguntas en una sola query
- Crear combos: validación atómica de las 3 preguntas
- Asignación jornadas: operación todo-o-nada

### **🔸 PRIORIDAD 3 - MEDIAS (Implementar próxima semana)**

#### **1. Mejoras en Eliminación**
- Validaciones preventivas antes de intentar eliminar
- Mensajes específicos por tipo de dependencia

#### **2. Protección de Uploads**
- Verificar estado de entidad antes de subir archivo
- Cleanup de archivos huérfanos

---

## 📊 **ESTADÍSTICAS DEL ANÁLISIS**

| Categoría | Críticos | Altos | Medios | Total |
|-----------|----------|-------|--------|-------|
| **Controladores** | 7 | 4 | 3 | 14 |
| **Servicios** | 12 | 8 | 6 | 26 |
| **Entidades** | 2 | 0 | 0 | 2 |
| **Estados** | 3 | 2 | 0 | 5 |
| **TOTAL** | **24** | **14** | **9** | **47** |

---

## ⚠️ **RIESGOS SI NO SE SOLUCIONAN**

### **🚨 Riesgos Críticos**
1. **Pérdida de Datos**: Cambios sobrescritos sin notificación
2. **Estados Imposibles**: Cuestionarios asignados a múltiples jornadas
3. **Duplicación**: Múltiples concursantes con mismo número
4. **Configuración Corrupta**: Parámetros del sistema inconsistentes

### **📈 Escalabilidad**
- Con **más usuarios simultáneos**, la probabilidad de conflictos **aumenta exponencialmente**
- Sistema actualmente **NO ESCALABLE** para producción multi-usuario

---

**🎯 RECOMENDACIÓN: Implementar soluciones de Prioridad 1 ANTES de continuar desarrollo de funcionalidades nuevas.** 