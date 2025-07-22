package com.lsnls.controller;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Usuario;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.service.ComboService;
import com.lsnls.service.AuthorizationService;
import com.lsnls.repository.ComboRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lsnls.dto.CrearComboDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;

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

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodos() {
        try {
            List<Combo> combos = comboService.obtenerTodos();
            List<Map<String, Object>> dtos = new java.util.ArrayList<>();
            for (Combo c : combos) {
                Map<String, Object> dto = comboService.obtenerComboConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
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
            if (dto.getPreguntasMultiplicadoras().size() != 3) {
                return ResponseEntity.badRequest().body("Un combo debe tener exactamente 3 preguntas multiplicadoras (PM1, PM2, PM3)");
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
            if (idsPreguntas.size() != 3) {
                return ResponseEntity.badRequest().body("No se puede usar la misma pregunta para diferentes multiplicadores (PM1, PM2, PM3)");
            }
            
            // Validar factores únicos
            java.util.HashSet<String> factores = new java.util.HashSet<>();
            for (CrearComboDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
                if (!factores.add(pm.getFactor())) {
                    return ResponseEntity.badRequest().body("No se puede asignar el mismo factor (" + pm.getFactor() + ") a múltiples preguntas");
                }
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
                "message", "Combo tipo " + dto.getTipo() + " creado correctamente con 3 preguntas multiplicadoras", 
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
            
            Long preguntaId = Long.valueOf(request.get("preguntaId").toString());
            Integer factorMultiplicacion = request.get("factorMultiplicacion") != null ? 
                Integer.valueOf(request.get("factorMultiplicacion").toString()) : 1;
            
            boolean exito = comboService.agregarPregunta(comboId, preguntaId, factorMultiplicacion);
            
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
                
                // Obtener preguntas con texto para búsqueda
                List<Map<String, Object>> preguntasInfo = new ArrayList<>();
                if (c.getPreguntas() != null) {
                    for (PreguntaCombo pc : c.getPreguntas()) {
                        if (pc.getPregunta() != null) {
                            Map<String, Object> preguntaInfo = new HashMap<>();
                            preguntaInfo.put("id", pc.getPregunta().getId());
                            preguntaInfo.put("pregunta", pc.getPregunta().getPregunta());
                            preguntaInfo.put("respuesta", pc.getPregunta().getRespuesta());
                            preguntaInfo.put("tematica", pc.getPregunta().getTematica());
                            preguntaInfo.put("factor", pc.getFactorMultiplicacion());
                            preguntasInfo.add(preguntaInfo);
                        }
                    }
                }
                dto.put("preguntas", preguntasInfo);
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

    @PutMapping("/{id}/estado")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Combo.EstadoCombo nuevoEstado) {
        try {
            Optional<Combo> comboOpt = comboRepository.findById(id);
            if (comboOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Combo combo = comboOpt.get();
            
            // Verificar permisos para cambiar estado
            if (!authService.canEditCombo(combo.getEstado())) {
                String estadoDescripcion = combo.getEstado().toString();
                return ResponseEntity.status(403).body("No tienes permisos para cambiar el estado de este combo. Tu rol actual no permite editar combos en estado '" + estadoDescripcion + "'.");
            }

            Combo comboActualizado = comboService.cambiarEstado(id, nuevoEstado);
            if (comboActualizado != null) {
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
                    combo.setEstado(Combo.EstadoCombo.valueOf(estadoStr));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Estado de combo inválido: " + estadoStr);
                }
            }
            
            Combo comboActualizado = comboService.actualizar(id, combo);
            if (comboActualizado != null) {
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
            return ResponseEntity.badRequest().body("Error interno al eliminar combo: " + e.getMessage());
        }
    }
} 