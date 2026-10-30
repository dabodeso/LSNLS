package com.lsnls.controller;

import com.lsnls.config.MensajesUsuario;
import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.service.ProgramaRepetidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/programas/repetidos")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
public class ProgramaRepetidoController {

    @Autowired
    private ProgramaRepetidoService programaRepetidoService;

    @GetMapping
    public ResponseEntity<List<ProgramaRepetidoDTO>> findAll() {
        return ResponseEntity.ok(programaRepetidoService.findAll());
    }

    @GetMapping("/{id}/concursantes")
    public ResponseEntity<?> findConcursantes(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(programaRepetidoService.findConcursantes(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/disponibles")
    public ResponseEntity<?> findDisponibles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<ConcursanteDTO> concursantes = programaRepetidoService.findDisponibles(id, pageable, busqueda, estado);
            return ResponseEntity.ok(concursantes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error interno al obtener concursantes: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @PostMapping("/{id}/concursantes/{concursanteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> asignar(
            @PathVariable Long id,
            @PathVariable Long concursanteId,
            @RequestParam(required = false) Integer posicion) {
        try {
            return ResponseEntity.ok(programaRepetidoService.asignar(id, concursanteId, posicion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/concursantes/{concursanteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> quitar(@PathVariable Long id, @PathVariable Long concursanteId) {
        try {
            programaRepetidoService.quitar(id, concursanteId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
