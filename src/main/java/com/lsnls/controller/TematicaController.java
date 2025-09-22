package com.lsnls.controller;

import com.lsnls.entity.Tematica;
import com.lsnls.entity.Usuario;
import com.lsnls.service.TematicaService;
import com.lsnls.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tematicas")
@CrossOrigin(origins = "*")
public class TematicaController {

    private static final Logger log = LoggerFactory.getLogger(TematicaController.class);

    @Autowired
    private TematicaService tematicaService;

    @Autowired
    private AuthorizationService authService;

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Tematica>> obtenerTodas() {
        try {
            List<Tematica> tematicas = tematicaService.obtenerTodas();
            return ResponseEntity.ok(tematicas);
        } catch (Exception e) {
            log.error("Error al obtener temáticas: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/buscar")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Tematica>> buscarPorTexto(@RequestParam String texto) {
        try {
            List<Tematica> tematicas = tematicaService.buscarPorTexto(texto);
            return ResponseEntity.ok(tematicas);
        } catch (Exception e) {
            log.error("Error al buscar temáticas: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canWrite()")
    public ResponseEntity<Map<String, Object>> crearTematica(@RequestBody Map<String, String> request) {
        try {
            String nombre = request.get("nombre");
            if (nombre == null || nombre.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "El nombre de la temática es obligatorio");
                return ResponseEntity.badRequest().body(error);
            }

            // Obtener el usuario actual (esto debería venir del contexto de seguridad)
            // Por ahora, asumimos que existe un usuario con ID 1
            Usuario usuario = new Usuario();
            usuario.setId(1L);

            Tematica tematica = tematicaService.crearTematica(nombre.trim(), usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tematica", tematica);
            response.put("mensaje", "Temática creada exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al crear temática: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al crear temática: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canWrite()")
    public ResponseEntity<Map<String, Object>> actualizarTematica(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String nuevoNombre = request.get("nombre");
            if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "El nombre de la temática es obligatorio");
                return ResponseEntity.badRequest().body(error);
            }

            Tematica tematica = tematicaService.actualizarTematica(id, nuevoNombre.trim());
            if (tematica == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Temática no encontrada");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("tematica", tematica);
            response.put("mensaje", "Temática actualizada exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al actualizar temática: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al actualizar temática: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canWrite()")
    public ResponseEntity<Map<String, Object>> eliminarTematica(@PathVariable Long id) {
        try {
            boolean eliminada = tematicaService.eliminarTematica(id);
            if (!eliminada) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Temática no encontrada");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Temática eliminada exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al eliminar temática: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al eliminar temática: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
