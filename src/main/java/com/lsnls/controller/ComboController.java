package com.lsnls.controller;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
// import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Usuario;
// import com.lsnls.entity.PreguntaCombo;
import com.lsnls.service.ComboService;
import com.lsnls.service.AuthorizationService;
import com.lsnls.repository.ComboRepository;
// import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lsnls.dto.CrearComboDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.service.EditLockService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;
import com.lsnls.dto.ApiResponse;

@RestController
@RequestMapping("/api/combos")
@CrossOrigin(origins = "*")
public class ComboController {

    private static final Logger log = LoggerFactory.getLogger(ComboController.class);

    @Autowired
    private ComboService comboService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private EditLockService editLockService;

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> obtenerTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            Map<String, Object> response = comboService.obtenerTodosPaginados(page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener combos paginados: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> obtenerPorId(@PathVariable Long id) {
        try {
            Map<String, Object> dto = comboService.obtenerComboConSlots(id);
            if (dto != null) {
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/preguntas")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerPreguntas(@PathVariable Long id) {
        try {
            List<Map<String, Object>> preguntas = comboService.obtenerPreguntasCombo(id);
            return ResponseEntity.ok(ApiResponse.exitoso("Preguntas obtenidas correctamente", preguntas));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener preguntas del combo: " + e.getMessage()));
        }
    }
    

    @PostMapping("/nuevo")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> crearCombo(@RequestBody CrearComboDTO dto) {
        try {
            // Verificar permisos específicos
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para crear combos. Solo usuarios con rol GUION o DIRECCION pueden crear combos.");
            }
            
            // Verificar autenticación
            Optional<Usuario> usuarioOpt = authService.getCurrentUser();
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            
            // Validaciones específicas de campos requeridos
            if (dto.getPreguntasMultiplicadoras() == null || dto.getPreguntasMultiplicadoras().isEmpty()) {
                return ResponseEntity.badRequest().body("Debe seleccionar las preguntas multiplicadoras para el combo");
            }
            
            // Obtener el estado del combo (por defecto "borrador" si no se especifica)
            String estadoCombo = dto.getEstado();
            if (estadoCombo == null || estadoCombo.trim().isEmpty()) {
                estadoCombo = "borrador";
            }

            if ("aprobado".equalsIgnoreCase(estadoCombo) && dto.getPreguntasMultiplicadoras().size() != 3) {
                return ResponseEntity.badRequest().body(
                    "Un combo debe tener exactamente 3 preguntas multiplicadoras (PM1, PM2, PM3) para pasar a aprobado");
            }

            // Validar tipo de combo
            if (dto.getTipo() == null || dto.getTipo().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'tipo' es obligatorio. Tipos permitidos: P (Premio), A (Asequible), D (Difícil)");
            }
            
            // Validar que no se repitan preguntas
            java.util.HashSet<Long> idsPreguntas = new java.util.HashSet<>();
            for (CrearComboDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
                if (pm.getId() == null) {
                    return ResponseEntity.badRequest().body("Todas las preguntas multiplicadoras deben tener un ID válido");
                }
                if (pm.getFactor() == null || pm.getFactor().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Todas las preguntas multiplicadoras deben tener un factor asignado (X2, X3, X)");
                }
                idsPreguntas.add(pm.getId());
            }
            // Validar que no haya IDs duplicados comparando con el tamaño real de la lista
            if (idsPreguntas.size() != dto.getPreguntasMultiplicadoras().size()) {
                return ResponseEntity.badRequest().body("No se puede usar la misma pregunta para diferentes multiplicadores (PM1, PM2, PM3)");
            }
            
            // Validar tipo con validación anticipada
            try {
                Combo.TipoCombo.valueOf(dto.getTipo());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Tipo de combo '" + dto.getTipo() + "' no válido. Tipos permitidos: P (Premio), A (Asequible), D (Difícil)");
            }
            
            // CREAR COMBO DE FORMA ATÓMICA con todas las preguntas
            Combo combo;
            try {
                combo = comboService.crearComboDesdeDTO(dto, usuarioOpt.get());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Error de concurrencia")) {
                    return ResponseEntity.status(409).body("Conflicto de concurrencia: " + e.getMessage());
                }
                return ResponseEntity.badRequest().body(e.getMessage());
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body("Error al crear combo: " + e.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Combo tipo " + dto.getTipo() + " creado correctamente",
                "id", combo.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al crear combo: " + e.getMessage());
        }
    }

    @PostMapping("/{comboId}/preguntas")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> agregarPregunta(
            @PathVariable Long comboId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para agregar preguntas a combos. Solo usuarios con rol GUION o DIRECCION pueden agregar preguntas a combos.");
            }
            
            // Bloquear si el combo está asignado a una jornada
            if (comboService.estaAsignadoAJornada(comboId)) {
                return ResponseEntity.status(409).body("Este combo está asignado a una jornada y no se puede modificar.");
            }
            
            Long preguntaId = Long.valueOf(request.get("preguntaId").toString());
            Integer factorMultiplicacion = request.get("factorMultiplicacion") != null ? 
                Integer.valueOf(request.get("factorMultiplicacion").toString()) : 1;
            Integer posicion = request.get("posicion") != null ?
                Integer.valueOf(request.get("posicion").toString()) : null;
            
            boolean exito = comboService.agregarPregunta(comboId, preguntaId, factorMultiplicacion, posicion);
            
            if (exito) {
                return ResponseEntity.ok(Map.of("message", "Pregunta agregada exitosamente"));
            } else {
                return ResponseEntity.badRequest().body("Error al agregar pregunta: No se pudo completar la operación");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al agregar pregunta: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @DeleteMapping("/{comboId}/preguntas/{preguntaId}")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> quitarPregunta(@PathVariable Long comboId, @PathVariable Long preguntaId) {
        try {
            log.info("[QUITAR PREGUNTA] Intentando quitar pregunta {} del combo {}", preguntaId, comboId);
            
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para quitar preguntas de combos. Solo usuarios con rol GUION o DIRECCION pueden quitar preguntas de combos.");
            }
            
            // Bloquear si el combo está asignado a una jornada
            if (comboService.estaAsignadoAJornada(comboId)) {
                return ResponseEntity.status(409).body("Este combo está asignado a una jornada y no se puede modificar.");
            }
            
            boolean exito = comboService.quitarPregunta(comboId, preguntaId);
            
            if (exito) {
                log.info("[QUITAR PREGUNTA] Pregunta {} quitada exitosamente del combo {}", preguntaId, comboId);
                return ResponseEntity.ok(Map.of("message", "Pregunta quitada exitosamente"));
            } else {
                log.warn("[QUITAR PREGUNTA] No se pudo quitar pregunta {} del combo {}", preguntaId, comboId);
                return ResponseEntity.badRequest().body("Error al quitar pregunta: No se pudo completar la operación");
            }
        } catch (Exception e) {
            log.error("[QUITAR PREGUNTA] Error al quitar pregunta {} del combo {}: {}", preguntaId, comboId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @PostMapping("/{comboId}/limpiar-preguntas-invalidas")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> limpiarPreguntasInvalidas(@PathVariable Long comboId) {
        try {
            log.info("[LIMPIAR PREGUNTAS] Limpiando preguntas inválidas del combo {}", comboId);
            
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para limpiar combos. Solo usuarios con rol GUION o DIRECCION pueden limpiar combos.");
            }
            
            int preguntasEliminadas = comboService.limpiarPreguntasInvalidas(comboId);
            
            log.info("[LIMPIAR PREGUNTAS] {} preguntas inválidas eliminadas del combo {}", preguntasEliminadas, comboId);
            return ResponseEntity.ok(Map.of(
                "message", "Preguntas inválidas limpiadas exitosamente",
                "preguntasEliminadas", preguntasEliminadas
            ));
        } catch (Exception e) {
            log.error("[LIMPIAR PREGUNTAS] Error al limpiar preguntas del combo {}: {}", comboId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @GetMapping("/para-asignar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> obtenerCombosParaAsignar() {
        try {
            List<Combo> combos = comboService.obtenerDisponiblesParaConcursantes();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Combo c : combos) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", c.getId());
                dto.put("estado", c.getEstado());
                dto.put("fechaCreacion", c.getFechaCreacion());
                dto.put("nivel", c.getNivel());
                resultado.add(dto);
            }
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al obtener combos para asignar", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/por-estado/{estado}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Combo>> obtenerPorEstado(@PathVariable Combo.EstadoCombo estado) {
        try {
            List<Combo> combos = comboService.obtenerPorEstado(estado);
            return ResponseEntity.ok(combos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/por-nivel/{nivel}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Combo>> obtenerPorNivel(@PathVariable Combo.NivelCombo nivel) {
        try {
            List<Combo> combos = comboService.obtenerPorNivel(nivel);
            return ResponseEntity.ok(combos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/filtrar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> filtrarCombos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String tematica,
            @RequestParam(required = false) String subtema,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        try {
            Map<String, Object> response;
            
            // Si se proporciona un ID, buscar por ID
            if (id != null && !id.isEmpty()) {
                response = comboService.filtrarCombosPorId(id, page, size);
            } else {
                response = comboService.filtrarCombos(estado, tipo, tematica, subtema, texto, page, size);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al filtrar combos: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Combo.EstadoCombo nuevoEstado) {
        try {
            Optional<Combo> comboOpt = comboRepository.findById(id);
            if (comboOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Combo combo = comboOpt.get();
            
            // Bloquear cambio si está asignado a una jornada
            if (comboService.estaAsignadoAJornada(id)) {
                return ResponseEntity.status(409).body("Este combo está asignado a una jornada y su estado está bloqueado en 'adjudicado'.");
            }

            // Verificar permisos para cambiar estado
            if (!authService.canEditCombo(combo.getEstado())) {
                String estadoDescripcion = combo.getEstado().toString();
                return ResponseEntity.status(403).body("No tienes permisos para cambiar el estado de este combo. Tu rol actual no permite editar combos en estado '" + estadoDescripcion + "'.");
            }

            editLockService.assertCanEdit(AuditLog.EntityType.COMBO, id);

            Combo comboActualizado;
            try {
                comboActualizado = comboService.cambiarEstado(id, nuevoEstado);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            if (comboActualizado != null) {
                editLockService.logEntityUpdate(AuditLog.EntityType.COMBO, id, "Cambio de estado de combo");
                return ResponseEntity.ok(Map.of(
                    "message", "Estado del combo cambiado exitosamente",
                    "estado", nuevoEstado
                ));
            } else {
                return ResponseEntity.badRequest().body("Error al cambiar el estado del combo");
            }
        } catch (Exception e) {
            log.error("Error al cambiar estado del combo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error interno del servidor: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> actualizarCombo(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para editar combos. Solo usuarios con rol GUION o DIRECCION pueden editar combos.");
            }
            
            Optional<Combo> comboOpt = comboService.obtenerPorId(id);
            if (comboOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Combo no encontrado");
            }
            
            Combo combo = comboOpt.get();

            if (datos.containsKey("version") && datos.get("version") != null) {
                combo.setVersion(Long.parseLong(datos.get("version").toString()));
            }

            // Actualizar tipo si se proporciona
            if (datos.containsKey("tipo") && datos.get("tipo") != null) {
                String tipoStr = datos.get("tipo").toString();
                try {
                    combo.setTipo(Combo.TipoCombo.valueOf(tipoStr));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Tipo de combo inválido: " + tipoStr);
                }
            }
            
            // Actualizar estado si se proporciona
            if (datos.containsKey("estado") && datos.get("estado") != null) {
                String estadoStr = datos.get("estado").toString();
                try {
                    Combo.EstadoCombo nuevoEstado = Combo.EstadoCombo.valueOf(estadoStr);
                    if (nuevoEstado == Combo.EstadoCombo.aprobado) {
                        Combo comboConPreguntas = comboService.obtenerConPreguntas(id)
                            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado"));
                        comboService.validarCompletoParaAprobar(comboConPreguntas);
                    }
                    combo.setEstado(nuevoEstado);
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && !e.getMessage().startsWith("No enum constant")) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                    }
                    return ResponseEntity.badRequest().body("Estado de combo inválido: " + estadoStr);
                }
            }
            
            // Actualizar temática si se proporciona
            if (datos.containsKey("tematica")) {
                String tematica = datos.get("tematica") != null ? datos.get("tematica").toString() : null;
                combo.setTematica(tematica);
            }

            // Actualizar notas de dirección si se proporciona
            if (datos.containsKey("notasDireccion")) {
                String notas = datos.get("notasDireccion") != null ? datos.get("notasDireccion").toString() : null;
                combo.setNotasDireccion(notas);
            }

            editLockService.assertCanEdit(AuditLog.EntityType.COMBO, id);
            
            Combo comboActualizado = comboService.actualizar(id, combo);
            if (comboActualizado != null) {
                editLockService.logEntityUpdate(AuditLog.EntityType.COMBO, id, "Actualización de combo");
                return ResponseEntity.ok(Map.of("message", "Combo actualizado correctamente"));
            } else {
                return ResponseEntity.badRequest().body("Error al actualizar combo");
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El combo ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar combo: " + e.getMessage());
        }
    }

        @PutMapping("/{comboId}/preguntas/{preguntaId}/factor")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> actualizarFactorPregunta(
            @PathVariable Long comboId,
            @PathVariable Long preguntaId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para modificar factores de preguntas en combos.");
            }

            // Verificar que el factor existe en la solicitud
            if (!request.containsKey("factorMultiplicacion")) {
                log.warn("[ACTUALIZAR FACTOR] Falta el parámetro factorMultiplicacion en la solicitud");
                return ResponseEntity.badRequest().body("Error: Falta el parámetro factorMultiplicacion");
            }
            
            // Convertir el factor a String de manera segura
            Object factorObj = request.get("factorMultiplicacion");
            String factorMultiplicacion = factorObj != null ? factorObj.toString() : "";

            log.info("[ACTUALIZAR FACTOR] Actualizando factor de pregunta {} en combo {} a: {}",
                    preguntaId, comboId, factorMultiplicacion);

            boolean exito = comboService.actualizarFactorPregunta(comboId, preguntaId, factorMultiplicacion);

            if (exito) {
                log.info("[ACTUALIZAR FACTOR] Factor actualizado exitosamente");
                return ResponseEntity.ok(Map.of("message", "Factor actualizado exitosamente", 
                                               "comboId", comboId,
                                               "preguntaId", preguntaId,
                                               "factor", factorMultiplicacion));
            } else {
                log.warn("[ACTUALIZAR FACTOR] No se pudo actualizar el factor");
                return ResponseEntity.badRequest().body("Error al actualizar factor: No se pudo completar la operación");
            }
        } catch (Exception e) {
            log.error("Error al actualizar factor de pregunta {} en combo {}: {}", preguntaId, comboId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("[ELIMINAR COMBO] Solicitud para eliminar combo con id: {}", id);
        try {
            // Verificar permisos específicos
            if (!authService.canDelete()) {
                log.warn("[ELIMINAR COMBO] Permiso denegado para eliminar combo id: {}", id);
                return ResponseEntity.status(403).body("No tienes permisos para eliminar combos. Solo usuarios con rol ADMIN o DIRECCION pueden eliminar combos.");
            }

            // Verificar que el combo existe
            Optional<Combo> comboOpt = comboService.obtenerPorId(id);
            if (comboOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Combo con ID " + id + " no encontrado");
            }

            Combo combo = comboOpt.get();
            
            // Verificar estado del combo
            if (combo.getEstado() == EstadoCombo.adjudicado) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está adjudicado. Cámbialo a un estado anterior primero.");
            }
            if (combo.getEstado() == EstadoCombo.grabado) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está grabado. Cámbialo a un estado anterior primero.");
            }

            authService.getCurrentUser().ifPresent(user -> log.info("[ELIMINAR COMBO] Usuario actual: {} (ID: {})", user.getNombre(), user.getId()));
            
            comboService.eliminar(id);
            log.info("[ELIMINAR COMBO] Combo {} eliminado correctamente", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // Mensajes específicos de validación
            log.warn("[ELIMINAR COMBO] Validación fallida para combo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[ELIMINAR COMBO] Error al eliminar combo {}: {}", id, e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("foreign key") || msg.contains("constraint")) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está siendo usado por concursantes o jornadas. Desasígnalo primero.");
            }
            if (msg.contains("jornada")) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está asignado a una jornada.");
            }
            if (msg.contains("concursante")) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está asignado a concursantes.");
            }
            // Mensaje genérico para errores internos
            return ResponseEntity.badRequest().body("No se pudo eliminar el combo. Verifica que no esté siendo usado por otros elementos del sistema.");
        }
    }
} 