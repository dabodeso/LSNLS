package com.lsnls.controller;

import com.lsnls.dto.ProgramaDTO;
import com.lsnls.entity.Programa;
import com.lsnls.service.ProgramaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}")
    public ResponseEntity<ProgramaDTO> findById(@PathVariable Long id) {
        return programaService.findByIdDTO(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<ProgramaDTO> create(@RequestBody ProgramaDTO programaDTO) {
        return ResponseEntity.ok(programaService.createFromDTO(programaDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<ProgramaDTO> update(@PathVariable Long id, @RequestBody ProgramaDTO programaDTO) {
        return ResponseEntity.ok(programaService.updateFromDTO(id, programaDTO));
    }

    @PatchMapping("/{id}/campo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<ProgramaDTO> updateCampo(@PathVariable Long id, @RequestBody Map<String, Object> campo) {
        return ResponseEntity.ok(programaService.updateCampo(id, campo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programaService.delete(id);
        return ResponseEntity.ok().build();
    }
} 