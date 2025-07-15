package com.lsnls.controller;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.service.ConcursanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<List<ConcursanteDTO>> findAll() {
        return ResponseEntity.ok(concursanteService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConcursanteDTO> findById(@PathVariable Long id) {
        ConcursanteDTO concursante = concursanteService.findById(id);
        if (concursante == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(concursante);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ConcursanteDTO>> findByEstado(@PathVariable String estado) {
        return ResponseEntity.ok(concursanteService.findByEstado(estado));
    }

    @GetMapping("/programa/{programaId}")
    public ResponseEntity<List<ConcursanteDTO>> findByProgramaId(@PathVariable Long programaId) {
        return ResponseEntity.ok(concursanteService.findByProgramaId(programaId));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<ConcursanteDTO>> findConcursantesDisponibles() {
        return ResponseEntity.ok(concursanteService.findConcursantesSinPrograma());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<ConcursanteDTO> create(@RequestBody ConcursanteDTO concursanteDTO) {
        return ResponseEntity.ok(concursanteService.create(concursanteDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ConcursanteDTO concursanteDTO) {
        try {
            ConcursanteDTO concursanteActualizado = concursanteService.update(id, concursanteDTO);
            return ResponseEntity.ok(concursanteActualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar concursante: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/campo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
    public ResponseEntity<ConcursanteDTO> updateCampo(@PathVariable Long id, @RequestBody Map<String, Object> campo) {
        return ResponseEntity.ok(concursanteService.updateCampo(id, campo));
    }

    @PostMapping("/{concursanteId}/asignar-programa/{programaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<ConcursanteDTO> asignarAPrograma(@PathVariable Long concursanteId, @PathVariable Long programaId) {
        return ResponseEntity.ok(concursanteService.asignarAPrograma(concursanteId, programaId));
    }

    @DeleteMapping("/{concursanteId}/desasignar-programa")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION', 'ROLE_DIRECCION')")
    public ResponseEntity<ConcursanteDTO> desasignarDePrograma(@PathVariable Long concursanteId) {
        return ResponseEntity.ok(concursanteService.desasignarDePrograma(concursanteId));
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        concursanteService.delete(id);
        return ResponseEntity.ok().build();
    }
} 