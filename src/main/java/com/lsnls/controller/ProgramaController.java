package com.lsnls.controller;

import com.lsnls.dto.ProgramaDTO;
import com.lsnls.service.ProgramaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/programas")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
public class ProgramaController {

    @Autowired
    private ProgramaService programaService;

    @GetMapping
    public ResponseEntity<List<ProgramaDTO>> findAll() {
        return ResponseEntity.ok(programaService.findAllDTO());
    }
    
    @GetMapping("/pagina")
    public ResponseEntity<Map<String, Object>> findAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        return ResponseEntity.ok(programaService.findAllPaginated(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            Optional<ProgramaDTO> programa = programaService.findByIdDTO(id);
            if (programa.isPresent()) {
                return ResponseEntity.ok(programa.get());
            } else {
                return ResponseEntity.status(404).body("Programa con ID " + id + " no encontrado");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al buscar programa: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> create(@RequestBody ProgramaDTO programaDTO) {
        try {
            // Validaciones específicas de campos requeridos
            if (programaDTO.getTemporada() == null) {
                return ResponseEntity.badRequest().body("El campo 'temporada' es obligatorio para crear un programa");
            }
            // fechaEmision puede ser null

            ProgramaDTO nuevoPrograma = programaService.createFromDTO(programaDTO);
            return ResponseEntity.ok(nuevoPrograma);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El programa ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al crear programa: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProgramaDTO programaDTO) {
        try {
            // Verificar que el programa existe
            Optional<ProgramaDTO> programaExistente = programaService.findByIdDTO(id);
            if (programaExistente.isEmpty()) {
                return ResponseEntity.status(404).body("Programa con ID " + id + " no encontrado");
            }

            // Validaciones específicas de campos requeridos
            if (programaDTO.getTemporada() == null) {
                return ResponseEntity.badRequest().body("El campo 'temporada' es obligatorio");
            }
            // fechaEmision puede ser null

            ProgramaDTO programaActualizado = programaService.updateFromDTO(id, programaDTO);
            return ResponseEntity.ok(programaActualizado);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El programa ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al actualizar programa: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/duracion-objetivo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> updateDuracionObjetivo(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String duracionObjetivo = request.get("duracionObjetivo");
            if (duracionObjetivo == null || duracionObjetivo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("La duración objetivo es obligatoria");
            }
            
            programaService.updateDuracionObjetivo(id, duracionObjetivo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar duración objetivo: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/campo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> updateCampo(@PathVariable Long id, @RequestBody Map<String, Object> campo) {
        try {
            // Verificar que el programa existe
            Optional<ProgramaDTO> programaExistente = programaService.findByIdDTO(id);
            if (programaExistente.isEmpty()) {
                return ResponseEntity.status(404).body("Programa con ID " + id + " no encontrado");
            }

            // Validar que el campo no esté vacío
            if (campo == null || campo.isEmpty()) {
                return ResponseEntity.badRequest().body("Debe proporcionar al menos un campo para actualizar");
            }

            Object resultado = programaService.updateCampo(id, campo);
            return ResponseEntity.ok(resultado);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("El programa ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al actualizar campo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            // Verificar que el programa existe
            Optional<ProgramaDTO> programaExistente = programaService.findByIdDTO(id);
            if (programaExistente.isEmpty()) {
                return ResponseEntity.status(404).body("Programa con ID " + id + " no encontrado");
            }

            programaService.delete(id);
            return ResponseEntity.ok().body("Programa eliminado exitosamente");
        } catch (IllegalArgumentException e) {
            // Mensajes específicos de validación
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("No se puede eliminar el programa porque tiene concursantes asociados. Desasigna los concursantes primero.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al eliminar programa: " + e.getMessage());
        }
    }
} 