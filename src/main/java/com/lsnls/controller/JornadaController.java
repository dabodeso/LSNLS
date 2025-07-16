package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.JornadaDTO;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.JornadaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public ResponseEntity<ApiResponse<List<JornadaDTO>>> obtenerTodas() {
        try {
            List<JornadaDTO> jornadas = jornadaService.obtenerTodas();
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
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener jornada: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canCreate()")
    public ResponseEntity<ApiResponse<JornadaDTO>> crear(@RequestBody JornadaDTO jornadaDTO) {
        try {
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Usuario no autenticado"));
            }
            
            Usuario currentUser = currentUserOpt.get();
            JornadaDTO nuevaJornada = jornadaService.crear(jornadaDTO, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.exitoso("Jornada creada exitosamente", nuevaJornada));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al crear jornada: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canEdit()")
    public ResponseEntity<ApiResponse<JornadaDTO>> actualizar(@PathVariable Long id, @RequestBody JornadaDTO jornadaDTO) {
        try {
            JornadaDTO jornadaActualizada = jornadaService.actualizar(id, jornadaDTO);
            return ResponseEntity.ok(ApiResponse.exitoso("Jornada actualizada exitosamente", jornadaActualizada));
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
    public ResponseEntity<byte[]> exportarExcel(@PathVariable Long id) {
        try {
            byte[] excelData = jornadaService.exportarExcel(id);
            
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
} 