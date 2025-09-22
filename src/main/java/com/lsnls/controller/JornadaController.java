package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.JornadaDTO;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.JornadaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/jornadas")
@CrossOrigin(origins = "*")
public class JornadaController {

    @Autowired
    private JornadaService jornadaService;

    @Autowired
    private AuthorizationService authService;

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<Page<JornadaDTO>>> obtenerTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String buscar) {
        try {
            // Crear objeto de ordenamiento
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<JornadaDTO> jornadas = jornadaService.obtenerTodasPaginadasConFiltros(
                pageable, estado, fechaDesde, fechaHasta, buscar);
            return ResponseEntity.ok(ApiResponse.exitoso("Jornadas obtenidas exitosamente", jornadas));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener jornadas: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<JornadaDTO>> obtenerPorId(@PathVariable Long id) {
        try {
            Optional<JornadaDTO> jornada = jornadaService.obtenerPorId(id);
            if (jornada.isPresent()) {
                return ResponseEntity.ok(ApiResponse.exitoso("Jornada encontrada", jornada.get()));
            } else {
                return ResponseEntity.status(404)
                    .body(ApiResponse.error("Jornada con ID " + id + " no encontrada"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error interno al obtener jornada: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canCreate()")
    public ResponseEntity<ApiResponse<JornadaDTO>> crear(@RequestBody JornadaDTO jornadaDTO) {
        try {
            // Validaciones específicas de campos requeridos
            if (jornadaDTO.getNombre() == null || jornadaDTO.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El campo 'nombre' es obligatorio para crear una jornada"));
            }

            // Validar límites de cuestionarios y combos
            if (jornadaDTO.getCuestionarioIds() != null && jornadaDTO.getCuestionarioIds().size() > 5) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Una jornada puede tener máximo 5 cuestionarios. Has seleccionado " + jornadaDTO.getCuestionarioIds().size() + " cuestionarios. Deselecciona " + (jornadaDTO.getCuestionarioIds().size() - 5) + " cuestionarios."));
            }
            if (jornadaDTO.getComboIds() != null && jornadaDTO.getComboIds().size() > 5) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Una jornada puede tener máximo 5 combos. Has seleccionado " + jornadaDTO.getComboIds().size() + " combos. Deselecciona " + (jornadaDTO.getComboIds().size() - 5) + " combos."));
            }

            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            JornadaDTO nuevaJornada = jornadaService.crear(jornadaDTO, currentUser.getId());
            
            String mensaje = "Jornada '" + nuevaJornada.getNombre() + "' creada exitosamente";
            if (jornadaDTO.getCuestionarioIds() != null && !jornadaDTO.getCuestionarioIds().isEmpty()) {
                mensaje += " con " + jornadaDTO.getCuestionarioIds().size() + " cuestionarios";
            }
            if (jornadaDTO.getComboIds() != null && !jornadaDTO.getComboIds().isEmpty()) {
                mensaje += " y " + jornadaDTO.getComboIds().size() + " combos";
            }
            
            return ResponseEntity.ok(ApiResponse.exitoso(mensaje, nuevaJornada));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error interno al crear jornada: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<JornadaDTO>> actualizar(@PathVariable Long id, @RequestBody JornadaDTO jornadaDTO) {
        try {
            JornadaDTO jornadaActualizada = jornadaService.actualizar(id, jornadaDTO);
            return ResponseEntity.ok(ApiResponse.exitoso("Jornada actualizada exitosamente", jornadaActualizada));
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409)
                .body(ApiResponse.error("La jornada ha sido modificada por otro usuario. Por favor, recarga e intenta nuevamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al actualizar jornada: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        try {
            jornadaService.eliminar(id);
            return ResponseEntity.ok(ApiResponse.exitoso("Jornada eliminada exitosamente", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al eliminar jornada: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<JornadaDTO>> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String nuevoEstado = request.get("estado");
            if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El estado es requerido"));
            }
            
            JornadaDTO jornadaActualizada = jornadaService.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(ApiResponse.exitoso("Estado actualizado exitosamente", jornadaActualizada));
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409)
                .body(ApiResponse.error("La jornada ha sido modificada por otro usuario. Por favor, recarga e intenta nuevamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al cambiar estado: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/exportar-excel")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<byte[]> exportarExcel(
            @PathVariable Long id, 
            @RequestHeader(value = "X-Excel-Cambiar-Columna-ID-PREGUNTA", required = false) String cambiarColumnaID,
            @RequestHeader(value = "X-Excel-Mostrar-Factor-Multiplicacion", required = false) String mostrarFactor,
            @RequestHeader(value = "X-Excel-Ordenar-Cuestionarios-Por-Nivel", required = false) String ordenarCuestionarios,
            @RequestHeader(value = "X-Excel-Ordenar-Combos-Por-Factor", required = false) String ordenarCombos) {
        try {
            // Configurar opciones de exportación basadas en las cabeceras
            Map<String, Object> opcionesExcel = new HashMap<>();
            
            // Si se solicita cambiar la columna ID PREGUNTA por otro valor
            if (cambiarColumnaID != null && !cambiarColumnaID.isEmpty()) {
                opcionesExcel.put("cambiarColumnaID", cambiarColumnaID);
            }
            
            // Si se solicita mostrar el factor de multiplicación
            if ("true".equalsIgnoreCase(mostrarFactor)) {
                opcionesExcel.put("mostrarFactorMultiplicacion", true);
            }
            
            // Si se solicita ordenar cuestionarios por nivel
            if ("true".equalsIgnoreCase(ordenarCuestionarios)) {
                opcionesExcel.put("ordenarCuestionariosPorNivel", true);
            }
            
            // Si se solicita ordenar combos por factor de multiplicación
            if ("true".equalsIgnoreCase(ordenarCombos)) {
                opcionesExcel.put("ordenarCombosPorFactor", true);
            }
            
            // Generar el Excel con las opciones especificadas
            byte[] excelData = jornadaService.exportarExcel(id, opcionesExcel);
            
            // Obtener información de la jornada para el nombre del archivo
            Optional<JornadaDTO> jornada = jornadaService.obtenerPorId(id);
            String nombreArchivo = "jornada_" + id;
            if (jornada.isPresent()) {
                nombreArchivo = "jornada_" + jornada.get().getNombre().replaceAll("[^a-zA-Z0-9]", "_");
                if (jornada.get().getFechaJornada() != null) {
                    nombreArchivo += "_" + jornada.get().getFechaJornada().toString();
                }
            }
            nombreArchivo += ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", nombreArchivo);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cuestionarios-disponibles")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerCuestionariosDisponibles() {
        try {
            List<Map<String, Object>> cuestionarios = jornadaService.obtenerCuestionariosDisponibles();
            return ResponseEntity.ok(ApiResponse.exitoso("Cuestionarios disponibles obtenidos", cuestionarios));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener cuestionarios disponibles: " + e.getMessage()));
        }
    }

    @GetMapping("/combos-disponibles")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerCombosDisponibles() {
        try {
            List<Map<String, Object>> combos = jornadaService.obtenerCombosDisponibles();
            return ResponseEntity.ok(ApiResponse.exitoso("Combos disponibles obtenidos", combos));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener combos disponibles: " + e.getMessage()));
        }
    }

    @PostMapping("/{jornadaId}/reutilizar-cuestionario/{cuestionarioId}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<String>> reutilizarCuestionario(
            @PathVariable Long jornadaId, 
            @PathVariable Long cuestionarioId) {
        try {
            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            jornadaService.reutilizarCuestionario(jornadaId, cuestionarioId, currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Cuestionario " + cuestionarioId + " reutilizado correctamente. Ahora está disponible para usar en otras jornadas.", 
                "Cuestionario reutilizado"));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al reutilizar cuestionario: " + e.getMessage()));
        }
    }

    @PostMapping("/{jornadaId}/reutilizar-combo/{comboId}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<String>> reutilizarCombo(
            @PathVariable Long jornadaId, 
            @PathVariable Long comboId) {
        try {
            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            jornadaService.reutilizarCombo(jornadaId, comboId, currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Combo " + comboId + " reutilizado correctamente. Ahora está disponible para usar en otras jornadas.", 
                "Combo reutilizado"));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al reutilizar combo: " + e.getMessage()));
        }
    }

    @PostMapping("/{jornadaId}/reciclar-combo-entero/{comboId}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<String>> reciclarComboEntero(
            @PathVariable Long jornadaId, 
            @PathVariable Long comboId) {
        try {
            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            jornadaService.reciclarComboEntero(jornadaId, comboId, currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Combo " + comboId + " reciclado completamente. Marcado como liberado.", 
                "Combo reciclado"));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al reciclar combo: " + e.getMessage()));
        }
    }

    @PostMapping("/{jornadaId}/reciclar-combo-parcial/{comboId}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<String>> reciclarComboParcial(
            @PathVariable Long jornadaId, 
            @PathVariable Long comboId,
            @RequestBody Map<String, Object> request) {
        try {
            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            // Obtener la pregunta usada del request
            Long preguntaUsadaId = null;
            if (request.containsKey("preguntaUsadaId")) {
                Object preguntaIdObj = request.get("preguntaUsadaId");
                if (preguntaIdObj instanceof Number) {
                    preguntaUsadaId = ((Number) preguntaIdObj).longValue();
                } else if (preguntaIdObj instanceof String) {
                    preguntaUsadaId = Long.valueOf((String) preguntaIdObj);
                }
            }
            
            if (preguntaUsadaId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Se debe especificar la pregunta usada"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            jornadaService.reciclarComboParcial(jornadaId, comboId, preguntaUsadaId, currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Combo reciclado parcialmente. Se creó un nuevo combo con las preguntas restantes.", 
                "Combo reciclado parcialmente"));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al reciclar combo parcialmente: " + e.getMessage()));
        }
    }
} 