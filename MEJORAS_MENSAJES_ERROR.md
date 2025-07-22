# 🚨 **MEJORAS EN MENSAJES DE ERROR - LSNLS**

## 📋 **Resumen Ejecutivo**

Se han mejorado significativamente los mensajes de error en toda la aplicación para que sean más claros, específicos y útiles para el usuario. Los cambios incluyen:

- ✅ **Mensajes específicos de dependencias** para cada entidad
- ✅ **Validaciones mejoradas** antes de intentar eliminar
- ✅ **Manejo diferenciado** de diferentes tipos de errores
- ✅ **Información cuantitativa** (número de elementos dependientes)
- ✅ **Instrucciones claras** sobre cómo resolver el problema

---

## 🔧 **MEJORAS IMPLEMENTADAS**

### **📂 1. SERVICIO DE PREGUNTAS**

#### **Antes:**
```java
throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en cuestionarios. Quítala de los cuestionarios primero.");
```

#### **Después:**
```java
Long cuestionariosCount = entityManager.createQuery(
    "SELECT COUNT(pc) FROM PreguntaCuestionario pc WHERE pc.pregunta.id = :preguntaId", Long.class)
    .setParameter("preguntaId", id)
    .getSingleResult();

if (cuestionariosCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en " + 
        cuestionariosCount + " cuestionario(s). Quítala de los cuestionarios primero.");
}
```

**Beneficios:**
- ✅ Muestra el número exacto de cuestionarios que usan la pregunta
- ✅ Mensaje más específico y útil
- ✅ Usuario sabe exactamente cuántos elementos debe revisar

---

### **📂 2. SERVICIO DE CONCURSANTES**

#### **Nuevas Validaciones:**
```java
// Verificar si está asignado a un programa
if (concursante.getNumeroPrograma() != null) {
    throw new IllegalArgumentException("No se puede eliminar el concursante porque está asignado al programa " + 
        concursante.getNumeroPrograma() + ". Desasígnalo del programa primero.");
}

// Verificar estado del concursante
if (concursante.getEstado() == "grabado") {
    throw new IllegalArgumentException("No se puede eliminar el concursante porque ya está grabado. " +
        "Los concursantes grabados no pueden ser eliminados.");
}
```

**Beneficios:**
- ✅ Previene eliminación de concursantes en programas
- ✅ Protege concursantes ya grabados
- ✅ Mensajes específicos sobre el estado actual

---

### **📂 3. SERVICIO DE CUESTIONARIOS**

#### **Validaciones Mejoradas:**
```java
// Verificar si hay concursantes usando este cuestionario
Long concursantesCount = entityManager.createQuery(
    "SELECT COUNT(c) FROM Concursante c WHERE c.cuestionario.id = :cuestionarioId", Long.class)
    .setParameter("cuestionarioId", id)
    .getSingleResult();

if (concursantesCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está siendo usado por " + 
        concursantesCount + " concursante(s). Desasígnalo primero.");
}

// Verificar si está en alguna jornada
Long jornadasCount = entityManager.createQuery(
    "SELECT COUNT(j) FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :cuestionarioId", Long.class)
    .setParameter("cuestionarioId", id)
    .getSingleResult();
    
if (jornadasCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está asignado a " + 
        jornadasCount + " jornada(s). Desasígnalo primero.");
}
```

**Beneficios:**
- ✅ Cuenta exacta de concursantes y jornadas
- ✅ Mensajes específicos sobre cada tipo de dependencia
- ✅ Instrucciones claras sobre qué hacer

---

### **📂 4. SERVICIO DE COMBOS**

#### **Validaciones Similares a Cuestionarios:**
```java
// Verificar si hay concursantes usando este combo
Long concursantesCount = entityManager.createQuery(
    "SELECT COUNT(c) FROM Concursante c WHERE c.combo.id = :comboId", Long.class)
    .setParameter("comboId", id)
    .getSingleResult();

if (concursantesCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar el combo porque está siendo usado por " + 
        concursantesCount + " concursante(s). Desasígnalo primero.");
}

// Verificar si está en alguna jornada
Long jornadasCount = entityManager.createQuery(
    "SELECT COUNT(j) FROM Jornada j JOIN j.combos c WHERE c.id = :comboId", Long.class)
    .setParameter("comboId", id)
    .getSingleResult();
    
if (jornadasCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar el combo porque está asignado a " + 
        jornadasCount + " jornada(s). Desasígnalo primero.");
}
```

---

### **📂 5. SERVICIO DE PROGRAMAS**

#### **Validaciones Mejoradas:**
```java
// Verificar dependencias - no se puede eliminar si hay concursantes asignados
Long concursantesCount = entityManager.createQuery(
    "SELECT COUNT(c) FROM Concursante c WHERE c.numeroPrograma = :programaId", Long.class)
    .setParameter("programaId", programa.getTemporada())
    .getSingleResult();

if (concursantesCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar el programa temporada " + 
        programa.getTemporada() + " porque tiene " + concursantesCount + 
        " concursante(s) asignado(s). Desasigna los concursantes primero.");
}

// Verificar estado del programa
if (programa.getEstado() == Programa.EstadoPrograma.emitido) {
    throw new IllegalArgumentException("No se puede eliminar un programa que ya ha sido emitido. " +
        "Los programas emitidos no pueden ser eliminados.");
}
```

**Beneficios:**
- ✅ Protección de programas emitidos
- ✅ Validación de concursantes asignados
- ✅ Mensajes específicos por estado

---

### **📂 6. SERVICIO DE JORNADAS**

#### **Nuevas Validaciones:**
```java
// Verificar estado de la jornada
if (jornada.getEstado() == Jornada.EstadoJornada.en_grabacion) {
    throw new IllegalArgumentException("No se puede eliminar una jornada que está en grabación. " +
        "Finaliza la grabación antes de eliminar la jornada.");
}

if (jornada.getEstado() == Jornada.EstadoJornada.completada) {
    throw new IllegalArgumentException("No se puede eliminar una jornada que ya está completada. " +
        "Las jornadas completadas no pueden ser eliminadas.");
}

// Verificar si hay concursantes asignados
Long concursantesCount = entityManager.createQuery(
    "SELECT COUNT(c) FROM Concursante c WHERE c.jornada.id = :jornadaId", Long.class)
    .setParameter("jornadaId", id)
    .getSingleResult();

if (concursantesCount > 0) {
    throw new IllegalArgumentException("No se puede eliminar la jornada porque tiene " + 
        concursantesCount + " concursante(s) asignado(s). Desasigna los concursantes primero.");
}
```

---

## 🎯 **CONTROLADORES MEJORADOS**

### **Manejo Diferenciado de Errores:**

Todos los controladores ahora manejan específicamente `IllegalArgumentException` para mostrar mensajes de validación claros:

```java
} catch (IllegalArgumentException e) {
    // Mensajes específicos de validación
    return ResponseEntity.badRequest().body(e.getMessage());
} catch (org.springframework.dao.DataIntegrityViolationException e) {
    return ResponseEntity.badRequest().body("No se puede eliminar porque tiene datos asociados.");
} catch (Exception e) {
    return ResponseEntity.badRequest().body("Error interno: " + e.getMessage());
}
```

**Beneficios:**
- ✅ Mensajes de validación se muestran directamente
- ✅ Errores de base de datos se manejan por separado
- ✅ Errores internos se capturan apropiadamente

---

## 📊 **TIPOS DE ERRORES CUBIERTOS**

### **1. Errores de Dependencias**
- ✅ Preguntas en cuestionarios/combos
- ✅ Cuestionarios en jornadas/concursantes
- ✅ Combos en jornadas/concursantes
- ✅ Concursantes en programas
- ✅ Programas con concursantes asignados
- ✅ Jornadas con concursantes asignados

### **2. Errores de Estado**
- ✅ Cuestionarios adjudicados/grabados
- ✅ Combos adjudicados/grabados
- ✅ Concursantes grabados
- ✅ Programas programados/emitidos
- ✅ Jornadas en grabación/completadas

### **3. Errores de Permisos**
- ✅ Validación de roles específicos
- ✅ Mensajes claros sobre permisos requeridos

### **4. Errores de Concurrencia**
- ✅ Optimistic locking
- ✅ Mensajes específicos sobre conflictos

---

## 🚀 **BENEFICIOS PARA EL USUARIO**

### **Antes:**
```
Error: No se pudo borrar el cuestionario
```

### **Después:**
```
No se puede eliminar el cuestionario porque está asignado a 2 jornada(s). Desasígnalo primero.
```

### **Mejoras Específicas:**
1. **Información Cuantitativa**: El usuario sabe exactamente cuántos elementos dependen
2. **Instrucciones Claras**: Se le dice exactamente qué hacer para resolver el problema
3. **Contexto Específico**: Se menciona el tipo de dependencia (jornadas, concursantes, etc.)
4. **Prevención de Errores**: Se validan los estados antes de intentar eliminar

---

## 🔍 **ERRORES ADICIONALES IDENTIFICADOS**

### **Errores que Podrían Ocurrir:**

1. **Errores de Concurrencia**
   - Múltiples usuarios modificando el mismo elemento
   - Cambios de estado simultáneos

2. **Errores de Validación**
   - Campos requeridos faltantes
   - Formatos de datos incorrectos
   - Estados inválidos

3. **Errores de Sistema**
   - Problemas de conectividad
   - Errores de base de datos
   - Problemas de permisos

### **Recomendaciones:**
- ✅ Implementar logging detallado para debugging
- ✅ Agregar validaciones de formato en frontend
- ✅ Implementar retry automático para errores transitorios
- ✅ Agregar tooltips explicativos en la interfaz

---

## 📝 **CONCLUSIÓN**

Los mensajes de error ahora son significativamente más claros y útiles. Los usuarios pueden:

- ✅ **Entender exactamente** qué está causando el problema
- ✅ **Saber cuántos elementos** están involucrados
- ✅ **Recibir instrucciones claras** sobre cómo resolver el problema
- ✅ **Evitar errores** antes de que ocurran

Esto mejora significativamente la experiencia del usuario y reduce la frustración al trabajar con el sistema. 