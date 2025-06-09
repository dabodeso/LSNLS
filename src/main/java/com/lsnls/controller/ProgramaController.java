package com.lsnls.controller;

import com.lsnls.entity.Programa;
import com.lsnls.service.ProgramaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programas")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONSULTA', 'ROLE_GUION', 'ROLE_VERIFICACION', 'ROLE_DIRECCION')")
public class ProgramaController {

    @Autowired
    private ProgramaService programaService;

    @GetMapping
    public ResponseEntity<List<Programa>> findAll() {
        return ResponseEntity.ok(programaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Programa> findById(@PathVariable Long id) {
        return programaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<Programa> create(@RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.create(programa));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<Programa> update(@PathVariable Long id, @RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.update(id, programa));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GUION')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programaService.delete(id);
        return ResponseEntity.ok().build();
    }
} 