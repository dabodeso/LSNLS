package com.lsnls.controller;

import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.service.PreguntaService;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.DataTransformationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.lsnls.dto.PreguntaCreateDTO;
import com.lsnls.dto.PreguntaDTO;

@RestController
@RequestMapping("/api/preguntas")
@CrossOrigin(origins = "*")
public class PreguntaController {

    private static final Logger log = LoggerFactory.getLogger(PreguntaController.class);

    @Autowired
    private PreguntaService preguntaService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private DataTransformationService dataTransformationService;

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<PreguntaDTO>> obtenerTodas() {
        try {
            List<PreguntaDTO> preguntas = preguntaService.obtenerTodasDTO();
            log.info("[PREGUNTAS] Total encontradas: {}", preguntas.size());
            for (PreguntaDTO p : preguntas) {
                log.info("[PREGUNTA] id={}, tematica={}, subtema={}", p.getId(), p.getTematica(), p.getSubtema());
            }
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            log.error("[ERROR] Al serializar preguntas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/paged")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Page<Pregunta>> obtenerPaginadas(Pageable pageable) {
        try {
            Page<Pregunta> preguntas = preguntaService.obtenerPaginadas(pageable);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<PreguntaDTO> obtenerPorId(@PathVariable Long id) {
        try {
            Optional<PreguntaDTO> pregunta = preguntaService.obtenerPorIdDTO(id);
            return pregunta.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canCreatePregunta()")
    public ResponseEntity<?> crear(@Valid @RequestBody PreguntaCreateDTO dto) {
        try {
            if (!authService.canCreatePregunta()) {
                return ResponseEntity.status(403).body("No tienes permisos para crear preguntas");
            }
            return authService.getCurrentUser()
                .map(currentUser -> {
                    Pregunta pregunta = new Pregunta();
                    pregunta.setNivel(Pregunta.NivelPregunta.valueOf(dto.nivel));
                    pregunta.setTematica(dto.tematica);
                    pregunta.setPregunta(dto.pregunta);
                    pregunta.setRespuesta(dto.respuesta);
                    pregunta.setDatosExtra(dto.datosExtra);
                    pregunta.setFuentes(dto.fuentes);
                    pregunta.setCreacionUsuario(currentUser);
                    pregunta.setEstado(Pregunta.EstadoPregunta.borrador);
                    pregunta.setNotasVerificacion(dto.notasVerificacion);
                    pregunta.setNotasDireccion(dto.notasDireccion);
                    pregunta.setSubtema(dto.subtema);
                    try {
                        Pregunta nuevaPregunta = preguntaService.crear(pregunta);
                        return ResponseEntity.ok(nuevaPregunta);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Error al crear pregunta: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.status(401).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear pregunta: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody PreguntaDTO dto) {
        try {
            Optional<Pregunta> preguntaExistente = preguntaService.obtenerPorId(id);
            if (preguntaExistente.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Pregunta preguntaActual = preguntaExistente.get();

            if (!authService.canEditPregunta(preguntaActual.getEstado())) {
                return ResponseEntity.status(403).body("No tienes permisos para editar esta pregunta en su estado actual");
            }

            Pregunta preguntaActualizada = preguntaService.actualizarDesdeDTO(id, dto);
            return preguntaActualizada != null ?
                    ResponseEntity.ok(preguntaActualizada) :
                    ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar pregunta: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Pregunta.EstadoPregunta nuevoEstado) {
        try {
            Optional<Pregunta> preguntaExistente = preguntaService.obtenerPorId(id);
            if (preguntaExistente.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Pregunta pregunta = preguntaExistente.get();

            // Verificar permisos para cambiar estado
            if (!authService.canChangeEstadoPregunta(pregunta.getEstado(), nuevoEstado)) {
                return ResponseEntity.status(403).body("No tienes permisos para cambiar a este estado");
            }

            Optional<Usuario> usuarioActualOpt = authService.getCurrentUser();
            Usuario usuarioActual = usuarioActualOpt.orElse(null);
            Pregunta preguntaActualizada = preguntaService.cambiarEstado(id, nuevoEstado, usuarioActual);
            return ResponseEntity.ok(preguntaActualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cambiar estado: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminarPregunta(@PathVariable Long id) {
        try {
            preguntaService.eliminarPorId(id);
            return ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("No se puede borrar la pregunta porque está asociada a un cuestionario.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al borrar la pregunta: " + e.getMessage());
        }
    }

    @GetMapping("/por-nivel/{nivel}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Pregunta>> obtenerPorNivel(@PathVariable Pregunta.NivelPregunta nivel) {
        try {
            List<Pregunta> preguntas = preguntaService.obtenerPorNivel(nivel);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/por-estado/{estado}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Pregunta>> obtenerPorEstado(@PathVariable Pregunta.EstadoPregunta estado) {
        try {
            List<Pregunta> preguntas = preguntaService.obtenerPorEstado(estado);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/disponibles")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Pregunta>> obtenerDisponibles() {
        try {
            List<Pregunta> preguntas = preguntaService.obtenerDisponibles();
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/verificar")
    public ResponseEntity<?> verificarPregunta(@PathVariable Long id, @RequestParam boolean aprobada, 
                                              @RequestParam(required = false) String notas) {
        try {
            return authService.getCurrentUser()
                .map(currentUser -> {
                    if (currentUser.getRol() != Usuario.RolUsuario.ROLE_VERIFICACION && 
                        currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION) {
                        return ResponseEntity.status(403).body("No tienes permisos para verificar preguntas");
                    }

                    Pregunta.EstadoPregunta nuevoEstado = aprobada ? 
                        Pregunta.EstadoPregunta.verificada : Pregunta.EstadoPregunta.rechazada;

                    try {
                        Pregunta pregunta = preguntaService.verificar(id, nuevoEstado, notas, currentUser);
                        return ResponseEntity.ok(pregunta);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Error al verificar pregunta: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.status(401).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al verificar pregunta: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<?> aprobarPregunta(@PathVariable Long id) {
        try {
            if (!authService.canValidate()) {
                return ResponseEntity.status(403).body("No tienes permisos para aprobar preguntas");
            }

            Pregunta pregunta = preguntaService.cambiarEstado(id, Pregunta.EstadoPregunta.aprobada);
            return ResponseEntity.ok(pregunta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al aprobar pregunta: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<?> rechazarPregunta(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        try {
            if (!authService.canValidate()) {
                return ResponseEntity.status(403).body("No tienes permisos para rechazar preguntas");
            }

            Pregunta pregunta = preguntaService.rechazar(id, motivo);
            return ResponseEntity.ok(pregunta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al rechazar pregunta: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/revisar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> marcarParaRevisar(@PathVariable Long id, @RequestParam(required = false) String notas) {
        try {
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            Usuario currentUser = currentUserOpt.get();
            
            // Solo niveles 3 y 4 (VERIFICACION y DIRECCION) pueden marcar para revisar
            if (currentUser.getRol() != Usuario.RolUsuario.ROLE_VERIFICACION && 
                currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION &&
                currentUser.getRol() != Usuario.RolUsuario.ROLE_ADMIN) {
                return ResponseEntity.status(403).body("No tienes permisos para marcar preguntas para revisar");
            }
            
            Pregunta pregunta = preguntaService.marcarParaRevisar(id, notas, currentUser);
            if (pregunta != null) {
                return ResponseEntity.ok(pregunta);
            }
            return ResponseEntity.badRequest().body("Error al marcar pregunta para revisar");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al marcar pregunta para revisar: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/corregir")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> marcarParaCorregir(@PathVariable Long id, @RequestParam(required = false) String notas) {
        try {
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            Usuario currentUser = currentUserOpt.get();
            
            // Solo nivel 4 (DIRECCION) puede marcar para corregir
            if (currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION &&
                currentUser.getRol() != Usuario.RolUsuario.ROLE_ADMIN) {
                return ResponseEntity.status(403).body("No tienes permisos para marcar preguntas para corregir");
            }
            
            Pregunta pregunta = preguntaService.marcarParaCorregir(id, notas, currentUser);
            if (pregunta != null) {
                return ResponseEntity.ok(pregunta);
            }
            return ResponseEntity.badRequest().body("Error al marcar pregunta para corregir");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al marcar pregunta para corregir: " + e.getMessage());
        }
    }

    @PostMapping("/validar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> validarPregunta(@Valid @RequestBody Pregunta pregunta) {
        try {
            var validationResult = preguntaService.validarPregunta(pregunta);
            
            if (validationResult.isValid()) {
                return ResponseEntity.ok().body(Map.of(
                    "valid", true,
                    "message", "La pregunta cumple con todos los requisitos",
                    "transformedData", Map.of(
                        "pregunta", pregunta.getPregunta(),
                        "respuesta", pregunta.getRespuesta(),
                        "tematica", pregunta.getTematica()
                    )
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "errors", validationResult.getErrors(),
                    "message", validationResult.getErrorsAsString()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", "Error al validar pregunta: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/transformar")
    @PreAuthorize("@authorizationService.canCreatePregunta()")
    public ResponseEntity<?> transformarTexto(@RequestBody Map<String, String> datos) {
        try {
            String pregunta = datos.get("pregunta");
            String respuesta = datos.get("respuesta");
            String tematica = datos.get("tematica");

            Map<String, String> transformados = new HashMap<>();
            
            if (pregunta != null) {
                transformados.put("pregunta", dataTransformationService.normalizarPregunta(pregunta));
            }
            if (respuesta != null) {
                transformados.put("respuesta", dataTransformationService.normalizarRespuesta(respuesta));
            }
            if (tematica != null) {
                transformados.put("tematica", dataTransformationService.normalizarTematica(tematica));
            }

            return ResponseEntity.ok(Map.of(
                "transformados", transformados,
                "message", "Textos transformados correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al transformar textos: " + e.getMessage());
        }
    }

    @GetMapping("/debug/permisos")
    public ResponseEntity<Map<String, Object>> debugPermisos() {
        Map<String, Object> permisos = new HashMap<>();
        Optional<Usuario> currentUserOpt = authService.getCurrentUser();
        
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        Usuario currentUser = currentUserOpt.get();
        permisos.put("usuario", currentUser);
        permisos.put("canRead", authService.canRead());
        permisos.put("canCreatePregunta", authService.canCreatePregunta());
        permisos.put("canDelete", authService.canDelete());
        permisos.put("canValidate", authService.canValidate());
        
        return ResponseEntity.ok(permisos);
    }

    @GetMapping("/buscar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Page<Pregunta>> buscarPreguntas(
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String factor,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String pregunta,
            @RequestParam(required = false) String respuesta,
            @RequestParam(required = false) String tematica,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Pregunta> preguntas = preguntaService.buscarPreguntasPaginadas(nivel, factor, id, pregunta, respuesta, tematica, pageable);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/filtrar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<PreguntaDTO>> filtrarPreguntasCompleto(
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String factor,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tematica,
            @RequestParam(required = false) String subtema,
            @RequestParam(required = false) String pregunta,
            @RequestParam(required = false) String respuesta
    ) {
        try {
            List<PreguntaDTO> preguntas = preguntaService.filtrarPreguntasCompleto(
                nivel, factor, estado, tematica, subtema, pregunta, respuesta);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            log.error("Error al filtrar preguntas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 