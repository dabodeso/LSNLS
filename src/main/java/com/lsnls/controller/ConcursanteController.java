package com.lsnls.controller;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.entity.EstadoConcursante;
import com.lsnls.service.ConcursanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<ConcursanteDTO>> findByEstado(@PathVariable EstadoConcursante estado) {
        return ResponseEntity.ok(concursanteService.findByEstado(estado));
    }

    @GetMapping("/programa/{programaId}")
    public ResponseEntity<List<ConcursanteDTO>> findByProgramaId(@PathVariable Long programaId) {
        return ResponseEntity.ok(concursanteService.findByProgramaId(programaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<?> create(@RequestBody ConcursanteDTO concursanteDTO) {
        // El campo 'imagen' se espera como base64 en el DTO
        try {
            return ResponseEntity.ok(concursanteService.create(concursanteDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ConcursanteDTO concursanteDTO) {
        // El campo 'imagen' se espera como base64 en el DTO
        try {
            return ResponseEntity.ok(concursanteService.update(id, concursanteDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        concursanteService.delete(id);
        return ResponseEntity.ok().build();
    }
} 