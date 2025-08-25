package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.HistorialJornadaDTO;
import com.lsnls.dto.MarcarNoUsadoDTO;
import com.lsnls.dto.ReaprovecharComboDTO;
import com.lsnls.entity.Combo;
import com.lsnls.service.HistorialJornadaService;
import com.lsnls.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial-jornadas")
@CrossOrigin(origins = "*")
public class HistorialJornadaController {

    @Autowired
    private HistorialJornadaService historialService;

    @Autowired
    private AuthorizationService authService;

    /**
     * Obtener historial de un cuestionario
     */
    @GetMapping("/cuestionario/{cuestionarioId}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> obtenerHistorialCuestionario(@PathVariable Long cuestionarioId) {
        try {
            List<HistorialJornadaDTO> historial = historialService.obtenerHistorialCuestionario(cuestionarioId);
            return ResponseEntity.ok(ApiResponse.exitoso("Historial del cuestionario obtenido", historial));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener historial: " + e.getMessage()));
        }
    }

    /**
     * Obtener historial de un combo
     */
    @GetMapping("/combo/{comboId}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> obtenerHistorialCombo(@PathVariable Long comboId) {
        try {
            List<HistorialJornadaDTO> historial = historialService.obtenerHistorialCombo(comboId);
            return ResponseEntity.ok(ApiResponse.exitoso("Historial del combo obtenido", historial));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener historial: " + e.getMessage()));
        }
    }

    /**
     * Obtener elementos no usados de una jornada
     */
    @GetMapping("/jornada/{jornadaId}/no-usados")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<ApiResponse<List<HistorialJornadaDTO>>> obtenerNoUsados(@PathVariable Long jornadaId) {
        try {
            List<HistorialJornadaDTO> noUsados = historialService.obtenerNoUsados(jornadaId);
            return ResponseEntity.ok(ApiResponse.exitoso("Elementos no usados obtenidos", noUsados));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener elementos no usados: " + e.getMessage()));
        }
    }

    /**
     * Marcar elementos como no usados
     */
    @PostMapping("/marcar-no-usados")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<ApiResponse<String>> marcarNoUsados(@RequestBody MarcarNoUsadoDTO dto) {
        try {
            // Validaciones
            if (dto.getJornadaId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El ID de la jornada es obligatorio"));
            }
            
            if ((dto.getCuestionarioIds() == null || dto.getCuestionarioIds().isEmpty()) &&
                (dto.getComboIds() == null || dto.getComboIds().isEmpty())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Debe seleccionar al menos un cuestionario o combo"));
            }

            historialService.marcarNoUsados(dto);
            
            String mensaje = "Elementos marcados como no usados correctamente";
            if (dto.getCuestionarioIds() != null && !dto.getCuestionarioIds().isEmpty()) {
                mensaje += ". Cuestionarios: " + dto.getCuestionarioIds().size();
            }
            if (dto.getComboIds() != null && !dto.getComboIds().isEmpty()) {
                mensaje += ". Combos: " + dto.getComboIds().size();
            }
            
            return ResponseEntity.ok(ApiResponse.exitoso(mensaje, null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al marcar como no usados: " + e.getMessage()));
        }
    }

    /**
     * Reaprovechar un combo
     */
    @PostMapping("/reaprovechar-combo")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<ApiResponse<Combo>> reaprovecharCombo(@RequestBody ReaprovecharComboDTO dto) {
        try {
            // Validaciones
            if (dto.getComboOriginalId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El ID del combo original es obligatorio"));
            }
            
            if (dto.getPreguntaUsadaId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El ID de la pregunta usada es obligatorio"));
            }
            
            if (dto.getNuevaPreguntaId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El ID de la nueva pregunta es obligatorio"));
            }

            Combo nuevoCombo = historialService.reaprovecharCombo(dto);
            
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Combo reaprovechado correctamente. Nuevo combo ID: " + nuevoCombo.getId(), 
                nuevoCombo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al reaprovechar combo: " + e.getMessage()));
        }
    }

    /**
     * Registrar asignación de cuestionario a jornada
     */
    @PostMapping("/asignar-cuestionario")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<ApiResponse<String>> asignarCuestionario(
            @RequestParam Long jornadaId, 
            @RequestParam Long cuestionarioId) {
        try {
            historialService.registrarAsignacionCuestionario(jornadaId, cuestionarioId);
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Cuestionario asignado a jornada correctamente", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al asignar cuestionario: " + e.getMessage()));
        }
    }

    /**
     * Registrar asignación de combo a jornada
     */
    @PostMapping("/asignar-combo")
    @PreAuthorize("@authorizationService.canCreateCuestionario()")
    public ResponseEntity<ApiResponse<String>> asignarCombo(
            @RequestParam Long jornadaId, 
            @RequestParam Long comboId) {
        try {
            historialService.registrarAsignacionCombo(jornadaId, comboId);
            return ResponseEntity.ok(ApiResponse.exitoso(
                "Combo asignado a jornada correctamente", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al asignar combo: " + e.getMessage()));
        }
    }
}
