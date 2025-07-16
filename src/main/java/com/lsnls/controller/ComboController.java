package com.lsnls.controller;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Usuario;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.service.ComboService;
import com.lsnls.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para crear combos");
            }
            
            Optional<Usuario> usuarioOpt = authService.getCurrentUser();
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            
            // Validar que se proporcionen exactamente 3 preguntas multiplicadoras
            if (dto.getPreguntasMultiplicadoras() == null || dto.getPreguntasMultiplicadoras().size() != 3) {
                return ResponseEntity.badRequest().body("Debes proporcionar exactamente 3 preguntas multiplicadoras");
            }
            
            Combo combo = new Combo();
            combo.setCreacionUsuario(usuarioOpt.get());
            combo.setEstado(EstadoCombo.borrador);
            combo.setNivel(NivelCombo.NORMAL);
            
            // Establecer tipo
            if (dto.getTipo() != null && !dto.getTipo().isEmpty()) {
                try {
                    combo.setTipo(Combo.TipoCombo.valueOf(dto.getTipo()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Tipo de combo inválido: " + dto.getTipo());
                }
            }
            
            combo = comboService.crear(combo);
            
            // Asociar preguntas multiplicadoras
            for (CrearComboDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
                int factor = 1;
                if ("X2".equalsIgnoreCase(pm.getFactor())) factor = 2;
                else if ("X3".equalsIgnoreCase(pm.getFactor())) factor = 3;
                else if ("X".equalsIgnoreCase(pm.getFactor())) factor = 0;
                
                comboService.agregarPregunta(combo.getId(), pm.getId(), factor);
            }
            
            return ResponseEntity.ok(Map.of("message", "Combo creado correctamente", "id", combo.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear combo: " + e.getMessage());
        }
    }

    @PostMapping("/{comboId}/preguntas")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> agregarPregunta(
            @PathVariable Long comboId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para agregar preguntas a combos");
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
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para quitar preguntas de combos");
            }
            
            boolean exito = comboService.quitarPregunta(comboId, preguntaId);
            
            if (exito) {
                return ResponseEntity.ok(Map.of("message", "Pregunta quitada exitosamente"));
            } else {
                return ResponseEntity.badRequest().body("Error al quitar pregunta: No se pudo completar la operación");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @GetMapping("/para-asignar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> obtenerCombosParaAsignar() {
        try {
            List<Combo> combos = comboService.obtenerPorEstado(Combo.EstadoCombo.creado);
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

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("[ELIMINAR COMBO] Solicitud para eliminar combo con id: {}", id);
        try {
            if (!authService.canDelete()) {
                log.warn("[ELIMINAR COMBO] Permiso denegado para eliminar combo id: {}", id);
                return ResponseEntity.status(403).body("No tienes permisos para eliminar combos");
            }
            authService.getCurrentUser().ifPresent(user -> log.info("[ELIMINAR COMBO] Usuario actual: {} (ID: {})", user.getNombre(), user.getId()));
            comboService.eliminar(id);
            log.info("[ELIMINAR COMBO] Combo {} eliminado correctamente", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ELIMINAR COMBO] Error al eliminar combo {}: {}", id, e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("foreign key") || msg.contains("constraint fails")) {
                return ResponseEntity.badRequest().body("No se puede eliminar el combo porque está siendo usado por uno o más concursantes.");
            }
            return ResponseEntity.badRequest().body("Error al eliminar combo: " + e.getMessage());
        }
    }
} 