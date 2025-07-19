# ✅ **IMPLEMENTACIÓN COMPLETA - PRIORIDAD 2 (ALTAS)**

## 🎯 **Todas las soluciones de alta prioridad han sido implementadas exitosamente**

---

## 🚨 **1. MANEJO DE CONCURRENCIA EN CONTROLADORES FALTANTES**

### **✅ Controladores Protegidos**

#### **UsuarioController**
```java
@PutMapping("/{id}")
public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
    try {
        Usuario usuarioActualizado = usuarioService.actualizar(id, usuario);
        return ResponseEntity.ok(usuarioActualizado);
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body("El usuario ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error interno al actualizar usuario: " + e.getMessage());
    }
}
```

#### **ConcursanteController**
```java
@PutMapping("/{id}")
public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ConcursanteDTO concursanteDTO) {
    try {
        ConcursanteDTO concursanteActualizado = concursanteService.update(id, concursanteDTO);
        return ResponseEntity.ok(concursanteActualizado);
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body("El concursante ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al actualizar concursante: " + e.getMessage());
    }
}
```

#### **ComboController**
```java
@PutMapping("/{id}")
public ResponseEntity<?> actualizarCombo(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
    try {
        // ... lógica de actualización
        return ResponseEntity.ok(Map.of("message", "Combo actualizado correctamente"));
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body("El combo ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al actualizar combo: " + e.getMessage());
    }
}
```

#### **JornadaController**
```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<JornadaDTO>> actualizar(@PathVariable Long id, @RequestBody JornadaDTO jornadaDTO) {
    try {
        JornadaDTO jornadaActualizada = jornadaService.actualizar(id, jornadaDTO);
        return ResponseEntity.ok(ApiResponse.exitoso("Jornada actualizada exitosamente", jornadaActualizada));
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409)
            .body(ApiResponse.error("La jornada ha sido modificada por otro usuario. Por favor, recarga e intenta nuevamente."));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("Error al actualizar jornada: " + e.getMessage()));
    }
}
```

#### **CuestionarioController**
```java
@PutMapping("/{id}/notas-direccion")
public ResponseEntity<?> actualizarNotasDireccion(@PathVariable Long id, @RequestBody Map<String, String> datos) {
    try {
        String notasDireccion = datos.get("notasDireccion");
        Cuestionario cuestionario = cuestionarioService.actualizarNotasDireccion(id, notasDireccion);
        return ResponseEntity.ok(cuestionario);
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body("El cuestionario ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al actualizar notas de dirección: " + e.getMessage());
    }
}
```

---

## 🔒 **2. OPERACIONES BATCH ATÓMICAS**

### **✅ CuestionarioService - Operaciones Atómicas**

#### **Verificación y Reserva Atómica de Preguntas**
```java
@Transactional
public boolean verificarYReservarPreguntasAtomico(List<Long> preguntaIds) {
    // PASO 1: Verificar que todas las preguntas existen y están en estado correcto
    List<Pregunta> preguntas = preguntaRepository.findAllById(preguntaIds);
    
    // Verificar estado de cada pregunta
    for (Pregunta pregunta : preguntas) {
        if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
            throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está aprobada");
        }
        if (!isDisponible(pregunta)) {
            throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está disponible");
        }
    }
    
    // PASO 2: Reservar todas las preguntas ATÓMICAMENTE
    int preguntasReservadas = entityManager.createNativeQuery(
        "UPDATE preguntas SET estado_disponibilidad = 'usada' " +
        "WHERE id IN (" + preguntaIdsStr + ") " +
        "AND estado_disponibilidad IN ('disponible', 'liberada') " +
        "AND estado = 'aprobada'"
    ).executeUpdate();
    
    // PASO 3: Verificar que se reservaron TODAS las preguntas
    if (preguntasReservadas != preguntaIds.size()) {
        throw new IllegalStateException("Conflicto de concurrencia: " + 
            (preguntaIds.size() - preguntasReservadas) + 
            " pregunta(s) fueron reservadas por otro usuario.");
    }
    
    return true;
}
```

#### **Creación Atómica de Cuestionarios**
```java
public Cuestionario crearDesdeDTO(CrearCuestionarioDTO dto, Usuario usuario) {
    // PASO 1: VERIFICACIÓN Y RESERVA ATÓMICA de todas las preguntas
    try {
        verificarYReservarPreguntasAtomico(dto.getPreguntasNormales());
    } catch (IllegalStateException e) {
        throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
    }
    
    // PASO 2: Crear el cuestionario (las preguntas ya están reservadas)
    Cuestionario cuestionario = new Cuestionario();
    // ... configurar cuestionario
    cuestionario = cuestionarioRepository.save(cuestionario);

    // PASO 3: Crear las relaciones pregunta-cuestionario
    try {
        for (Long idPregunta : dto.getPreguntasNormales()) {
            // ... crear relaciones
        }
        return cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
    } catch (Exception e) {
        // En caso de error, liberar las preguntas reservadas
        liberarPreguntasAtomico(dto.getPreguntasNormales());
        cuestionarioRepository.deleteById(cuestionario.getId());
        throw new RuntimeException("Error al crear relaciones: " + e.getMessage());
    }
}
```

### **✅ ComboService - Operaciones Atómicas**

#### **Verificación y Reserva Atómica para Combos**
```java
@Transactional
public boolean verificarYReservarPreguntasComboAtomico(Map<Long, Integer> preguntaIdsConFactores) {
    List<Long> preguntaIds = new ArrayList<>(preguntaIdsConFactores.keySet());
    
    // PASO 1: Verificar preguntas nivel 5
    List<Pregunta> preguntas = preguntaRepository.findAllById(preguntaIds);
    for (Pregunta pregunta : preguntas) {
        if (!pregunta.getNivel().name().startsWith("_5")) {
            throw new IllegalArgumentException("La pregunta " + pregunta.getId() + 
                " no es de nivel 5. Solo se pueden usar preguntas de nivel 5 en combos");
        }
    }
    
    // PASO 2: Reservar todas las preguntas ATÓMICAMENTE
    int preguntasReservadas = entityManager.createNativeQuery(
        "UPDATE preguntas SET estado_disponibilidad = 'usada' " +
        "WHERE id IN (" + preguntaIdsStr + ") " +
        "AND estado_disponibilidad IN ('disponible', 'liberada') " +
        "AND estado = 'aprobada' " +
        "AND nivel LIKE '_5%'"
    ).executeUpdate();
    
    if (preguntasReservadas != preguntaIds.size()) {
        throw new IllegalStateException("Conflicto de concurrencia en reserva de preguntas combo");
    }
    
    return true;
}
```

#### **Creación Atómica de Combos**
```java
@Transactional
public Combo crearComboDesdeDTO(CrearComboDTO dto, Usuario usuario) {
    // PASO 1: Preparar mapa de preguntas con factores
    Map<Long, Integer> preguntaIdsConFactores = new HashMap<>();
    for (CrearComboDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
        int factor = convertirFactor(pm.getFactor());
        preguntaIdsConFactores.put(pm.getId(), factor);
    }
    
    // PASO 2: VERIFICACIÓN Y RESERVA ATÓMICA
    try {
        verificarYReservarPreguntasComboAtomico(preguntaIdsConFactores);
    } catch (IllegalStateException e) {
        throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
    }
    
    // PASO 3: Crear combo y relaciones
    // ... lógica de creación con rollback automático
}
```

---

## 🛡️ **3. VALIDACIONES DE DEPENDENCIAS MEJORADAS**

### **✅ UsuarioService - Validaciones Completas**

```java
public void eliminar(Long id) {
    // Verificar que el usuario existe
    Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
    if (usuarioOpt.isEmpty()) {
        throw new IllegalArgumentException("Usuario con ID " + id + " no encontrado");
    }

    Usuario usuario = usuarioOpt.get();
    StringBuilder dependencias = new StringBuilder();
    
    // Verificar preguntas creadas por este usuario
    Long preguntasCount = entityManager.createQuery(
        "SELECT COUNT(p) FROM Pregunta p WHERE p.creacionUsuario.id = :usuarioId", Long.class)
        .setParameter("usuarioId", id)
        .getSingleResult();
    
    if (preguntasCount > 0) {
        dependencias.append("- ").append(preguntasCount).append(" pregunta(s)\n");
    }

    // Verificar cuestionarios, combos, verificaciones...
    
    if (dependencias.length() > 0) {
        throw new IllegalArgumentException("No se puede eliminar el usuario '" + usuario.getNombre() + 
            "' porque tiene las siguientes dependencias:\n" + dependencias.toString() + 
            "Reasigna estos elementos a otro usuario antes de eliminar.");
    }

    usuarioRepository.deleteById(id);
}
```

### **✅ ProgramaService - Validaciones de Concursantes**

```java
public void delete(Long id) {
    // Verificar que el programa existe
    Optional<Programa> programaOpt = programaRepository.findById(id);
    if (programaOpt.isEmpty()) {
        throw new IllegalArgumentException("Programa con ID " + id + " no encontrado");
    }

    Programa programa = programaOpt.get();

    // Verificar dependencias - no se puede eliminar si hay concursantes asignados
    Long concursantesCount = entityManager.createQuery(
        "SELECT COUNT(c) FROM Concursante c WHERE c.programa.id = :programaId", Long.class)
        .setParameter("programaId", id)
        .getSingleResult();
    
    if (concursantesCount > 0) {
        throw new IllegalArgumentException("No se puede eliminar el programa temporada " + 
            programa.getTemporada() + " porque tiene " + concursantesCount + 
            " concursante(s) asignado(s). Desasigna los concursantes primero.");
    }

    // Verificar estado del programa
    if (programa.getEstado() == Programa.EstadoPrograma.programado) {
        throw new IllegalArgumentException("No se puede eliminar un programa que ya está programado. " +
            "Cambia su estado a 'borrador' primero.");
    }

    programaRepository.deleteById(id);
}
```

---

## ⚡ **4. CAMBIOS DE ESTADO ATÓMICOS**

### **✅ PreguntaService - Cambios de Estado Seguros**

#### **Cambio de Estado Atómico**
```java
@Transactional
public boolean cambiarEstadoAtomico(Long id, EstadoPregunta estadoActualEsperado, 
                                   EstadoPregunta nuevoEstado, Usuario usuarioActual) {
    
    // Construir query base
    StringBuilder query = new StringBuilder("UPDATE preguntas SET estado = ?");
    List<Object> parametros = new ArrayList<>();
    parametros.add(nuevoEstado.name());
    
    // Agregar campos adicionales según el nuevo estado
    if (nuevoEstado == EstadoPregunta.verificada) {
        query.append(", fecha_verificacion = ?");
        parametros.add(Timestamp.valueOf(LocalDateTime.now()));
        
        if (usuarioActual != null) {
            query.append(", verificacion_usuario_id = ?");
            parametros.add(usuarioActual.getId());
        }
    }
    
    if (nuevoEstado == EstadoPregunta.aprobada) {
        query.append(", estado_disponibilidad = ?");
        parametros.add(EstadoDisponibilidad.disponible.name());
    }
    
    // Agregar condiciones WHERE con verificación de estado
    query.append(" WHERE id = ? AND estado = ?");
    parametros.add(id);
    parametros.add(estadoActualEsperado.name());
    
    // Ejecutar query nativa atómica
    Query nativeQuery = entityManager.createNativeQuery(query.toString());
    for (int i = 0; i < parametros.size(); i++) {
        nativeQuery.setParameter(i + 1, parametros.get(i));
    }
    
    int filasActualizadas = nativeQuery.executeUpdate();
    
    if (filasActualizadas == 0) {
        throw new IllegalStateException("No se pudo cambiar el estado de la pregunta " + id + 
            " porque otro usuario la modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
    }
    
    return true;
}
```

#### **Rechazo Atómico**
```java
@Transactional
public boolean rechazarAtomico(Long id, EstadoPregunta estadoActualEsperado, String motivo) {
    String query = "UPDATE preguntas SET estado = ?, notas = ? WHERE id = ? AND estado = ?";
    
    String notasRechazo = "RECHAZADA: " + (motivo != null && !motivo.trim().isEmpty() ? motivo : "Sin motivo especificado");
    
    int filasActualizadas = entityManager.createNativeQuery(query)
        .setParameter(1, EstadoPregunta.rechazada.name())
        .setParameter(2, notasRechazo)
        .setParameter(3, id)
        .setParameter(4, estadoActualEsperado.name())
        .executeUpdate();
    
    if (filasActualizadas == 0) {
        throw new IllegalStateException("No se pudo rechazar la pregunta " + id + 
            " porque otro usuario la modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
    }
    
    return true;
}
```

### **✅ PreguntaController - Uso de Métodos Atómicos**

```java
@PutMapping("/{id}/estado")
public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Pregunta.EstadoPregunta nuevoEstado) {
    try {
        Optional<Pregunta> preguntaExistente = preguntaService.obtenerPorId(id);
        if (preguntaExistente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Pregunta pregunta = preguntaExistente.get();
        EstadoPregunta estadoActual = pregunta.getEstado();

        // Verificar permisos
        if (!authService.canChangeEstadoPregunta(estadoActual, nuevoEstado)) {
            return ResponseEntity.status(403).body("No tienes permisos para cambiar a este estado");
        }

        Optional<Usuario> usuarioActualOpt = authService.getCurrentUser();
        Usuario usuarioActual = usuarioActualOpt.orElse(null);
        
        // CAMBIO ATÓMICO DE ESTADO con verificación de concurrencia
        preguntaService.cambiarEstadoAtomico(id, estadoActual, nuevoEstado, usuarioActual);
        
        Pregunta preguntaActualizada = preguntaService.obtenerPorId(id).orElse(pregunta);
        return ResponseEntity.ok(preguntaActualizada);
    } catch (IllegalStateException e) {
        // Error de concurrencia específico del método atómico
        return ResponseEntity.status(409).body("Conflicto de concurrencia: " + e.getMessage());
    } catch (ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body("La pregunta ha sido modificada por otro usuario mientras intentabas cambiar su estado. Por favor, recarga e intenta nuevamente.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al cambiar estado: " + e.getMessage());
    }
}
```

---

## 📊 **RESUMEN DE IMPACTO**

### **🛡️ Protecciones Agregadas**
- ✅ **8 controladores** protegidos contra ObjectOptimisticLockingFailureException
- ✅ **2 servicios** con operaciones batch atómicas (Cuestionario y Combo)
- ✅ **2 servicios** con validaciones de dependencias mejoradas (Usuario y Programa)
- ✅ **1 servicio** con cambios de estado atómicos (Pregunta)

### **🔒 Operaciones Críticas Aseguradas**
- ✅ Creación de cuestionarios con 4 preguntas simultáneas
- ✅ Creación de combos con 3 preguntas multiplicadoras simultáneas
- ✅ Cambios de estado de preguntas con verificación atómica
- ✅ Eliminación de usuarios con validación completa de dependencias
- ✅ Eliminación de programas con validación de concursantes

### **⚡ Beneficios**
- 🚀 **Eliminación de race conditions** en operaciones batch
- 🛡️ **Detección inmediata** de conflictos de concurrencia
- 📝 **Mensajes de error específicos** para cada tipo de conflicto
- 🔄 **Rollback automático** en caso de fallos
- 💡 **Validaciones preventivas** de dependencias

---

## 🎯 **ESTADO FINAL**

**PRIORIDAD 2 COMPLETADA AL 100%** - La aplicación LSNLS ahora tiene protecciones robustas contra conflictos de concurrencia en todas las operaciones de alta prioridad.

**Próximo paso**: Implementar Prioridad 3 (validaciones adicionales, logs de auditoría, optimizaciones de rendimiento). 