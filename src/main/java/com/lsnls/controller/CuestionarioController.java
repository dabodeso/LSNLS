package com.lsnls.controller;

import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Cuestionario.NivelCuestionario;
import com.lsnls.entity.Usuario;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.service.CuestionarioService;
import com.lsnls.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lsnls.dto.CrearCuestionarioDTO;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/cuestionarios")
@CrossOrigin(origins = "*")
public class CuestionarioController {

    private static final Logger log = LoggerFactory.getLogger(CuestionarioController.class);

    @Autowired
    private CuestionarioService cuestionarioService;

    @Autowired
    private AuthorizationService authService;

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodos() {
        try {
            List<Cuestionario> cuestionarios = cuestionarioService.obtenerTodos();
            List<Map<String, Object>> dtos = new java.util.ArrayList<>();
            for (Cuestionario c : cuestionarios) {
                Map<String, Object> dto = cuestionarioService.obtenerCuestionarioConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Cuestionario> obtenerPorId(@PathVariable Long id) {
        try {
            System.out.println("🌐 CONTROLADOR: Solicitando cuestionario " + id);
            
            Optional<Cuestionario> cuestionario = cuestionarioService.obtenerConPreguntas(id);
            
            if (cuestionario.isPresent()) {
                Cuestionario c = cuestionario.get();
                System.out.println("📤 ENVIANDO AL FRONTEND: Cuestionario " + c.getId() + " con " + c.getPreguntas().size() + " preguntas");
                return ResponseEntity.ok(c);
            } else {
                System.out.println("❌ CONTROLADOR: Cuestionario " + id + " no encontrado");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("💥 CONTROLADOR ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> crear(@Valid @RequestBody Cuestionario cuestionario) {
        try {
            return authService.getCurrentUser()
                .map(currentUser -> {
                    cuestionario.setCreacionUsuario(currentUser);
                    cuestionario.setEstado(Cuestionario.EstadoCuestionario.borrador);

                    try {
                        Cuestionario nuevoCuestionario = cuestionarioService.crear(cuestionario);
                        return ResponseEntity.ok(nuevoCuestionario);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Error al crear cuestionario: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.status(401).body("Usuario no autenticado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear cuestionario: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/notas-direccion")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
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

    @GetMapping("/filtrar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> filtrarCuestionarios(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tematica
    ) {
        try {
            List<Cuestionario> cuestionarios = cuestionarioService.filtrarCuestionarios(estado, tematica);
            List<Map<String, Object>> dtos = new java.util.ArrayList<>();
            for (Cuestionario c : cuestionarios) {
                Map<String, Object> dto = cuestionarioService.obtenerCuestionarioConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody CrearCuestionarioDTO dto) {
        try {
            // Verificar que el cuestionario existe
            Optional<Cuestionario> cuestionarioExistente = cuestionarioService.obtenerPorId(id);
            if (cuestionarioExistente.isEmpty()) {
                return ResponseEntity.status(404).body("Cuestionario con ID " + id + " no encontrado");
            }

            Cuestionario cuestionarioActual = cuestionarioExistente.get();

            // Validaciones específicas de campos
            if (dto.getPreguntasNormales() == null || dto.getPreguntasNormales().isEmpty()) {
                return ResponseEntity.badRequest().body("Debe seleccionar al menos una pregunta para el cuestionario");
            }
            if (dto.getPreguntasNormales().size() != 4) {
                return ResponseEntity.badRequest().body("Un cuestionario debe tener exactamente 4 preguntas (niveles 1LS, 2NLS, 3LS, 4NLS)");
            }

            // Verificar permisos específicos según estado
            if (!authService.canEditCuestionario(cuestionarioActual.getEstado())) {
                String estadoDescripcion = getCuestionarioEstadoDescripcion(cuestionarioActual.getEstado());
                return ResponseEntity.status(403).body("No tienes permisos para editar cuestionarios en estado '" + 
                    estadoDescripcion + "'. Solo se pueden editar cuestionarios en borrador o creado.");
            }

            // Verificar que no esté asignado a jornadas si está en estado avanzado
            if (cuestionarioActual.getEstado() == Cuestionario.EstadoCuestionario.asignado_jornada || 
                cuestionarioActual.getEstado() == Cuestionario.EstadoCuestionario.asignado_concursantes) {
                return ResponseEntity.badRequest().body("No se puede editar un cuestionario que ya está asignado a jornadas o concursantes. Desasígnalo primero.");
            }

            try {
                Cuestionario actualizado = cuestionarioService.actualizarDesdeDTO(id, dto);
                if (actualizado != null) {
                    return ResponseEntity.ok(Map.of(
                        "id", actualizado.getId(),
                        "message", "Cuestionario actualizado correctamente con " + dto.getPreguntasNormales().size() + " preguntas"
                    ));
                } else {
                    return ResponseEntity.status(404).body("Error al actualizar: cuestionario no encontrado");
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El cuestionario ha sido modificado por otro usuario. Por favor, recarga la página y vuelve a intentarlo.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al actualizar cuestionario: " + e.getMessage());
        }
    }

    private String getCuestionarioEstadoDescripcion(Cuestionario.EstadoCuestionario estado) {
        switch (estado) {
            case borrador: return "borrador";
            case creado: return "creado";
            case adjudicado: return "adjudicado";
            case grabado: return "grabado";
            case asignado_jornada: return "asignado a jornada";
            case asignado_concursantes: return "asignado a concursantes";
            default: return estado.toString();
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Cuestionario.EstadoCuestionario nuevoEstado) {
        try {
            Optional<Cuestionario> cuestionarioExistente = cuestionarioService.obtenerPorId(id);
            if (cuestionarioExistente.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Cuestionario cuestionario = cuestionarioExistente.get();

            if (!authService.canEditCuestionario(cuestionario.getEstado())) {
                return ResponseEntity.status(403).body("No tienes permisos para cambiar el estado de este cuestionario");
            }

            Cuestionario cuestionarioActualizado = cuestionarioService.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(cuestionarioActualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cambiar estado: " + e.getMessage());
        }
    }

    @PostMapping("/{cuestionarioId}/preguntas")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> agregarPregunta(
            @PathVariable Long cuestionarioId,
            @RequestBody Map<String, Object> request) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para agregar preguntas a cuestionarios");
            }
            
            Long preguntaId = Long.valueOf(request.get("preguntaId").toString());
            Integer factorMultiplicacion = request.get("factorMultiplicacion") != null ? 
                Integer.valueOf(request.get("factorMultiplicacion").toString()) : 1;
            
            boolean exito = cuestionarioService.agregarPregunta(cuestionarioId, preguntaId, factorMultiplicacion);
            
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

    @GetMapping("/para-asignar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Map<String, Object>>> obtenerCuestionariosParaAsignar() {
        try {
            List<Cuestionario> cuestionarios = cuestionarioService.obtenerDisponiblesParaConcursantes();
            List<Map<String, Object>> resultado = new ArrayList<>();
            
            for (Cuestionario c : cuestionarios) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", c.getId());
                dto.put("estado", c.getEstado());
                dto.put("fechaCreacion", c.getFechaCreacion());
                dto.put("nivel", c.getNivel());
                
                // Obtener preguntas con texto para búsqueda
                List<Map<String, Object>> preguntasInfo = new ArrayList<>();
                if (c.getPreguntas() != null) {
                    for (PreguntaCuestionario pc : c.getPreguntas()) {
                        if (pc.getPregunta() != null) {
                            Map<String, Object> preguntaInfo = new HashMap<>();
                            preguntaInfo.put("id", pc.getPregunta().getId());
                            preguntaInfo.put("pregunta", pc.getPregunta().getPregunta());
                            preguntaInfo.put("respuesta", pc.getPregunta().getRespuesta());
                            preguntaInfo.put("tematica", pc.getPregunta().getTematica());
                            preguntasInfo.add(preguntaInfo);
                        }
                    }
                }
                dto.put("preguntas", preguntasInfo);
                resultado.add(dto);
            }
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al obtener cuestionarios para asignar", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/por-estado/{estado}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Cuestionario>> obtenerPorEstado(@PathVariable Cuestionario.EstadoCuestionario estado) {
        try {
            List<Cuestionario> cuestionarios = cuestionarioService.obtenerPorEstado(estado);
            return ResponseEntity.ok(cuestionarios);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/por-nivel/{nivel}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Cuestionario>> obtenerPorNivel(@PathVariable Cuestionario.NivelCuestionario nivel) {
        try {
            List<Cuestionario> cuestionarios = cuestionarioService.obtenerPorNivel(nivel);
            return ResponseEntity.ok(cuestionarios);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("[ELIMINAR CUESTIONARIO] Solicitud para eliminar cuestionario con id: {}", id);
        try {
            if (!authService.canDelete()) {
                log.warn("[ELIMINAR CUESTIONARIO] Permiso denegado para eliminar cuestionario id: {}", id);
                return ResponseEntity.status(403).body("No tienes permisos para eliminar cuestionarios");
            }
            authService.getCurrentUser().ifPresent(user -> log.info("[ELIMINAR CUESTIONARIO] Usuario actual: {} (ID: {})", user.getNombre(), user.getId()));
            cuestionarioService.eliminar(id);
            log.info("[ELIMINAR CUESTIONARIO] Cuestionario {} eliminado correctamente", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ELIMINAR CUESTIONARIO] Error al eliminar cuestionario {}: {}", id, e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("foreign key") || msg.contains("constraint fails")) {
                return ResponseEntity.badRequest().body("No se puede eliminar el cuestionario porque está siendo usado por uno o más concursantes.");
            }
            return ResponseEntity.badRequest().body("Error al eliminar cuestionario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{cuestionarioId}/preguntas/{preguntaId}")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> quitarPregunta(
            @PathVariable Long cuestionarioId,
            @PathVariable Long preguntaId) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para quitar preguntas de cuestionarios");
            }
            
            boolean exito = cuestionarioService.quitarPregunta(cuestionarioId, preguntaId);
            
            if (exito) {
                return ResponseEntity.ok(Map.of("message", "Pregunta quitada exitosamente"));
            } else {
                return ResponseEntity.badRequest().body("Error al quitar pregunta: No se pudo completar la operación");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al quitar pregunta: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @DeleteMapping("/{cuestionarioId}/preguntas/slot/{slot}")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> quitarPreguntaPorSlot(
            @PathVariable Long cuestionarioId,
            @PathVariable String slot) {
        try {
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("No tienes permisos para quitar preguntas de cuestionarios");
            }
            
            boolean exito = cuestionarioService.quitarPreguntaPorSlot(cuestionarioId, slot);
            
            if (exito) {
                return ResponseEntity.ok(Map.of("message", "Pregunta quitada exitosamente del slot " + slot));
            } else {
                return ResponseEntity.badRequest().body("Error al quitar pregunta: No se encontró pregunta en el slot " + slot);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al quitar pregunta: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @GetMapping("/debug/permisos")
    public ResponseEntity<Map<String, Object>> debugPermisos() {
        Map<String, Object> debug = new HashMap<>();
        
        return authService.getCurrentUser()
            .map(currentUser -> {
                debug.put("currentUser", currentUser.getNombre());
                debug.put("currentUserRole", currentUser.getRol().toString());
                debug.put("canCreateCuestionario", authService.canCreateCuestionario());
                debug.put("canRead", authService.canRead());
                debug.put("canDelete", authService.canDelete());
                return ResponseEntity.ok(debug);
            })
            .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/debug/pregunta/{id}")
    public ResponseEntity<Map<String, Object>> debugPregunta(@PathVariable Long id) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            Optional<com.lsnls.entity.Pregunta> preguntaOpt = cuestionarioService.obtenerPreguntaPorId(id);
            if (preguntaOpt.isPresent()) {
                com.lsnls.entity.Pregunta pregunta = preguntaOpt.get();
                debug.put("id", pregunta.getId());
                debug.put("estado", pregunta.getEstado().toString());
                debug.put("estadoDisponibilidad", pregunta.getEstadoDisponibilidad().toString());
                debug.put("pregunta", pregunta.getPregunta());
                debug.put("respuesta", pregunta.getRespuesta());
                debug.put("respuestaLength", pregunta.getRespuesta() != null ? pregunta.getRespuesta().length() : 0);
                debug.put("respuestaBytes", pregunta.getRespuesta() != null ? java.util.Arrays.toString(pregunta.getRespuesta().getBytes()) : "null");
                debug.put("nivel", pregunta.getNivel().toString());
                debug.put("creador", pregunta.getCreacionUsuario() != null ? pregunta.getCreacionUsuario().getNombre() : "null");
                
                // Verificar caracteres especiales
                if (pregunta.getRespuesta() != null) {
                    String respuesta = pregunta.getRespuesta();
                    StringBuilder caracteresEspeciales = new StringBuilder();
                    for (char c : respuesta.toCharArray()) {
                        if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && 
                            ".,;:!?¡¿()[]\"'-".indexOf(c) == -1) {
                            caracteresEspeciales.append(c).append(" (").append((int)c).append(") ");
                        }
                    }
                    debug.put("caracteresEspeciales", caracteresEspeciales.toString());
                }
            } else {
                debug.put("error", "Pregunta no encontrada");
            }
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(debug);
    }

    @GetMapping("/debug/simple/{id}")
    public ResponseEntity<String> debugSimple(@PathVariable Long id) {
        try {
            Optional<com.lsnls.entity.Pregunta> preguntaOpt = cuestionarioService.obtenerPreguntaPorId(id);
            if (preguntaOpt.isPresent()) {
                com.lsnls.entity.Pregunta pregunta = preguntaOpt.get();
                return ResponseEntity.ok("Pregunta " + id + " - Respuesta: '" + pregunta.getRespuesta() + "' - Estado: " + pregunta.getEstado() + " - Disponibilidad: " + pregunta.getEstadoDisponibilidad());
            } else {
                return ResponseEntity.ok("Pregunta " + id + " no encontrada");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    @GetMapping("/debug/sql/{id}")
    public ResponseEntity<Map<String, Object>> debugSql(@PathVariable Long id) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Verificar cuestionario
            Optional<Cuestionario> cuestionarioOpt = cuestionarioService.obtenerPorId(id);
            debug.put("cuestionarioExists", cuestionarioOpt.isPresent());
            
            if (cuestionarioOpt.isPresent()) {
                Cuestionario cuestionario = cuestionarioOpt.get();
                debug.put("cuestionarioId", cuestionario.getId());
                debug.put("cuestionarioNivel", cuestionario.getNivel());
                debug.put("cuestionarioEstado", cuestionario.getEstado());
                
                // Verificar preguntas usando consulta SQL directa
                List<Object[]> resultados = cuestionarioService.obtenerPreguntasPorCuestionarioSQL(id);
                debug.put("preguntasEncontradas", resultados.size());
                
                List<Map<String, Object>> preguntasInfo = new ArrayList<>();
                for (Object[] row : resultados) {
                    Map<String, Object> preguntaInfo = new HashMap<>();
                    preguntaInfo.put("preguntaId", row[0]);
                    preguntaInfo.put("cuestionarioId", row[1]);
                    preguntaInfo.put("factor", row[2]);
                    preguntaInfo.put("preguntaTexto", row[3]);
                    preguntaInfo.put("respuesta", row[4]);
                    preguntasInfo.add(preguntaInfo);
                }
                debug.put("preguntasDetalle", preguntasInfo);
            }
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(debug);
    }

    @PostMapping("/nuevo")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<?> crearDesdeDTO(@Valid @RequestBody CrearCuestionarioDTO dto) {
        log.info("[CREAR CUESTIONARIO] DTO recibido: {}", dto);
        try {
            // Validaciones específicas de campos
            if (dto.getPreguntasNormales() == null || dto.getPreguntasNormales().isEmpty()) {
                return ResponseEntity.badRequest().body("Debe seleccionar al menos una pregunta para el cuestionario");
            }
            if (dto.getPreguntasNormales().size() != 4) {
                return ResponseEntity.badRequest().body("Un cuestionario debe tener exactamente 4 preguntas (niveles 1LS, 2NLS, 3LS, 4NLS)");
            }

            // Verificar permisos específicos
            if (!authService.canCreateCuestionario()) {
                return ResponseEntity.status(403).body("Solo usuarios con rol GUION o DIRECCION pueden crear cuestionarios");
            }

            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            Usuario currentUser = currentUserOpt.get();
            log.info("[CREAR CUESTIONARIO] Usuario actual: {} (ID: {})", currentUser.getNombre(), currentUser.getId());

            try {
                Cuestionario nuevo = cuestionarioService.crearDesdeDTO(dto, currentUser);
                log.info("[CREAR CUESTIONARIO] Cuestionario creado con ID: {}", nuevo.getId());
                return ResponseEntity.ok(Map.of(
                    "id", nuevo.getId(),
                    "message", "Cuestionario creado correctamente con " + dto.getPreguntasNormales().size() + " preguntas"
                ));
            } catch (IllegalArgumentException e) {
                log.error("[CREAR CUESTIONARIO] Error de validación: {}", e.getMessage());
                return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
            } catch (Exception e) {
                log.error("[CREAR CUESTIONARIO] Error al crear cuestionario: {}", e.getMessage(), e);
                return ResponseEntity.badRequest().body("Error interno al crear cuestionario: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("[CREAR CUESTIONARIO] Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error interno al crear cuestionario: " + e.getMessage());
        }
    }
} 