package com.lsnls.controller;

import com.lsnls.config.MensajesUsuario;
import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.ConcursanteEmitidoOpcionDTO;
import com.lsnls.dto.CrearProgramaRepetidoDTO;
import com.lsnls.dto.ProgramaEmitidoOpcionDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.dto.ResultadoBusquedaDTO;
import com.lsnls.service.ProgramaRepetidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/siguiente-codigo")
    public ResponseEntity<?> siguienteCodigo(
            @RequestParam(defaultValue = "REPE") String tipo,
            @RequestParam(required = false) Integer temporada) {
        try {
            return ResponseEntity.ok(Map.of("codigo", programaRepetidoService.siguienteCodigoSugerido(tipo, temporada)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @GetMapping("/origen/programas")
    public ResponseEntity<?> buscarProgramasEmitidos(
            @RequestParam(required = false) String fechaEmision,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String valoracionMinima,
            @RequestParam(defaultValue = "0") int page) {
        try {
            ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> resultado =
                    programaRepetidoService.buscarProgramasEmitidos(fechaEmision, codigo, valoracionMinima, page);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @GetMapping("/origen/concursantes")
    public ResponseEntity<?> buscarConcursantesEmitidos(
            @RequestParam(required = false) String valoracionMinima,
            @RequestParam(required = false) String tiempo,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> resultado =
                    programaRepetidoService.buscarConcursantesEmitidos(
                            valoracionMinima, tiempo, numero, ciudad, estado, page, size);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> crear(@RequestBody CrearProgramaRepetidoDTO dto) {
        try {
            return ResponseEntity.ok(programaRepetidoService.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(
                    "Ya existe un repetido con el mismo tipo, código, temporada y fecha de emisión.");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            programaRepetidoService.delete(id);
            return ResponseEntity.ok().body("Programa repetido eliminado");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(programaRepetidoService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/campo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
    public ResponseEntity<?> updateCampo(@PathVariable Long id, @RequestBody Map<String, Object> campo) {
        try {
            if (campo == null || campo.isEmpty()) {
                return ResponseEntity.badRequest().body("Debe proporcionar al menos un campo para actualizar.");
            }
            return ResponseEntity.ok(programaRepetidoService.updateCampo(id, campo));
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body(
                    "El programa ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MensajesUsuario.sanitizar(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(
                    "Ya existe un repetido con el mismo tipo, código, temporada y fecha de emisión.");
        }
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
