package com.lsnls.controller;

import com.lsnls.config.MensajesUsuario;

import com.lsnls.service.TemasCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/temas")
@CrossOrigin(origins = "*")
public class TemasController {


    @Autowired
    private TemasCatalogService temasCatalogService;

    // Catálogos ahora en BD (servicio)

    /**
     * Obtener todos los temas disponibles
     */
    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> obtenerTemas() {
        try {
            return ResponseEntity.ok(temasCatalogService.obtenerTematicasPreguntas());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener temas: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Obtener todos los subtemas disponibles
     */
    @GetMapping("/subtemas")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> obtenerSubtemas() {
        try {
            return ResponseEntity.ok(temasCatalogService.obtenerSubtemasPreguntas());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener subtemas: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Añadir un nuevo tema
     */
    @PostMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> añadirTema(@RequestBody Map<String, String> request) {
        try {
            String nuevoTema = request.get("tema");
            if (nuevoTema == null || nuevoTema.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El tema no puede estar vacío. Por favor, introduce un nombre para el tema.");
            }
            String temaNormalizado = temasCatalogService.añadirTematicaPregunta(nuevoTema);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Tema añadido correctamente");
            response.put("tema", temaNormalizado);
            response.put("totalTemas", temasCatalogService.obtenerTematicasPreguntas().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al añadir tema: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Añadir un nuevo subtema
     */
    @PostMapping("/subtemas")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> añadirSubtema(@RequestBody Map<String, String> request) {
        try {
            String nuevoSubtema = request.get("subtema");
            if (nuevoSubtema == null || nuevoSubtema.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El subtema no puede estar vacío. Por favor, introduce un nombre para el subtema.");
            }
            String subtemaNormalizado = temasCatalogService.añadirSubtemaPregunta(nuevoSubtema);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Subtema añadido correctamente");
            response.put("subtema", subtemaNormalizado);
            response.put("totalSubtemas", temasCatalogService.obtenerSubtemasPreguntas().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al añadir subtema: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Eliminar un tema
     */
    @DeleteMapping("/{tema}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> eliminarTema(@PathVariable String tema) {
        try {
            String temaNormalizado = tema.trim().toUpperCase();
            temasCatalogService.eliminarTematicaPregunta(temaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Tema eliminado correctamente");
            response.put("tema", temaNormalizado);
            response.put("totalTemas", temasCatalogService.obtenerTematicasPreguntas().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar tema: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Eliminar un subtema
     */
    @DeleteMapping("/subtemas/{subtema}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> eliminarSubtema(@PathVariable String subtema) {
        try {
            String subtemaNormalizado = subtema.trim().toUpperCase();
            temasCatalogService.eliminarSubtemaPregunta(subtemaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Subtema eliminado correctamente");
            response.put("subtema", subtemaNormalizado);
            response.put("totalSubtemas", temasCatalogService.obtenerSubtemasPreguntas().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar subtema: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }

    /**
     * Obtener estadísticas de uso de temas y subtemas
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("totalTemas", temasCatalogService.obtenerTematicasPreguntas().size());
            estadisticas.put("totalSubtemas", temasCatalogService.obtenerSubtemasPreguntas().size());
            estadisticas.put("temas", temasCatalogService.obtenerTematicasPreguntas());
            estadisticas.put("subtemas", temasCatalogService.obtenerSubtemasPreguntas());
            
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener estadísticas: " + MensajesUsuario.sanitizar(e.getMessage()));
        }
    }
} 