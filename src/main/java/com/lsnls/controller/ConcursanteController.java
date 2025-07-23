package com.lsnls.controller;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.service.ConcursanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/concursantes")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
public class ConcursanteController {

    @Autowired
    private ConcursanteService concursanteService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            List<ConcursanteDTO> concursantes = concursanteService.findAll();
            return ResponseEntity.ok(concursantes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener concursantes: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            ConcursanteDTO concursante = concursanteService.findById(id);
            if (concursante == null) {
                return ResponseEntity.status(404).body("Concursante con ID " + id + " no encontrado");
            }
            return ResponseEntity.ok(concursante);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al buscar concursante: " + e.getMessage());
        }
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> findByEstado(@PathVariable String estado) {
        try {
            if (estado == null || estado.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El estado es requerido");
            }
            List<ConcursanteDTO> concursantes = concursanteService.findByEstado(estado);
            return ResponseEntity.ok(concursantes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al buscar concursantes por estado: " + e.getMessage());
        }
    }

    @GetMapping("/programa/{programaId}")
    public ResponseEntity<?> findByProgramaId(@PathVariable Long programaId) {
        try {
            if (programaId == null) {
                return ResponseEntity.badRequest().body("El ID del programa es requerido");
            }
            List<ConcursanteDTO> concursantes = concursanteService.findByProgramaId(programaId);
            return ResponseEntity.ok(concursantes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al buscar concursantes por programa: " + e.getMessage());
        }
    }

    @GetMapping("/disponibles")
    public ResponseEntity<?> findConcursantesDisponibles() {
        try {
            List<ConcursanteDTO> concursantes = concursanteService.findConcursantesSinPrograma();
            return ResponseEntity.ok(concursantes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener concursantes disponibles: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> create(@RequestBody ConcursanteDTO concursanteDTO) {
        try {
            // Validaciones específicas de campos requeridos
            if (concursanteDTO.getNombre() == null || concursanteDTO.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'nombre' es obligatorio para crear un concursante");
            }

            // Validar edad si se proporciona
            if (concursanteDTO.getEdad() != null && (concursanteDTO.getEdad() < 18 || concursanteDTO.getEdad() > 99)) {
                return ResponseEntity.badRequest().body("La edad debe estar entre 18 y 99 años");
            }

            // Validar duración si se proporciona
            if (concursanteDTO.getDuracion() != null && !concursanteDTO.getDuracion().trim().isEmpty()) {
                if (!concursanteDTO.getDuracion().matches("^\\d{1,2}:\\d{2}$")) {
                    return ResponseEntity.badRequest().body("La duración debe tener formato MM:SS (ejemplo: 25:30)");
                }
            }

            ConcursanteDTO nuevoConcursante = concursanteService.create(concursanteDTO);
            return ResponseEntity.ok(nuevoConcursante);
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje.contains("cuestionario") && mensaje.contains("estado")) {
                return ResponseEntity.badRequest().body("Error de asignación: " + mensaje);
            }
            if (mensaje.contains("combo") && mensaje.contains("estado")) {
                return ResponseEntity.badRequest().body("Error de asignación: " + mensaje);
            }
            return ResponseEntity.badRequest().body("Error al crear concursante: " + mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al crear concursante: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
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

    @PatchMapping("/{id}/campo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> updateCampo(@PathVariable Long id, @RequestBody Map<String, Object> campo) {
        try {
            return ResponseEntity.ok(concursanteService.updateCampo(id, campo));
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El concursante ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar campo: " + e.getMessage());
        }
    }

    @PostMapping("/{concursanteId}/asignar-programa/{programaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> asignarAPrograma(@PathVariable Long concursanteId, @PathVariable Long programaId) {
        try {
            ConcursanteDTO concursante = concursanteService.asignarAPrograma(concursanteId, programaId);
            return ResponseEntity.ok(concursante);
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje.contains("no encontrado")) {
                return ResponseEntity.status(404).body(mensaje);
            }
            return ResponseEntity.badRequest().body("Error al asignar concursante a programa: " + mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al asignar concursante a programa: " + e.getMessage());
        }
    }

    @DeleteMapping("/{concursanteId}/desasignar-programa")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> desasignarDePrograma(@PathVariable Long concursanteId) {
        try {
            ConcursanteDTO concursante = concursanteService.desasignarDePrograma(concursanteId);
            return ResponseEntity.ok(concursante);
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje.contains("no encontrado")) {
                return ResponseEntity.status(404).body(mensaje);
            }
            return ResponseEntity.badRequest().body("Error al desasignar concursante del programa: " + mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al desasignar concursante del programa: " + e.getMessage());
        }
    }

    @PostMapping("/{concursanteId}/asignar-jornada/{jornadaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> asignarAJornada(@PathVariable Long concursanteId, @PathVariable Long jornadaId) {
        try {
            ConcursanteDTO concursante = concursanteService.asignarAJornada(concursanteId, jornadaId);
            return ResponseEntity.ok(concursante);
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje.contains("no encontrado")) {
                return ResponseEntity.status(404).body(mensaje);
            }
            return ResponseEntity.badRequest().body("Error al asignar concursante a jornada: " + mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al asignar concursante a jornada: " + e.getMessage());
        }
    }

    @DeleteMapping("/{concursanteId}/desasignar-jornada")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> desasignarDeJornada(@PathVariable Long concursanteId) {
        try {
            ConcursanteDTO concursante = concursanteService.desasignarDeJornada(concursanteId);
            return ResponseEntity.ok(concursante);
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje.contains("no encontrado")) {
                return ResponseEntity.status(404).body(mensaje);
            }
            return ResponseEntity.badRequest().body("Error al desasignar concursante de la jornada: " + mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al desasignar concursante de la jornada: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> subirFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        try {
            String urlFoto = concursanteService.subirFoto(id, foto);
            Map<String, String> response = new HashMap<>();
            response.put("url", urlFoto);
            response.put("mensaje", "Foto subida correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al subir la foto: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            // Verificar que el concursante existe
            ConcursanteDTO concursanteExistente = concursanteService.findById(id);
            if (concursanteExistente == null) {
                return ResponseEntity.status(404).body("Concursante con ID " + id + " no encontrado");
            }

            concursanteService.delete(id);
            return ResponseEntity.ok().body("Concursante eliminado exitosamente");
        } catch (IllegalArgumentException e) {
            // Mensajes específicos de validación
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("No se puede eliminar el concursante porque tiene datos asociados. Revisa sus asignaciones primero.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al eliminar concursante: " + e.getMessage());
        }
    }
} 