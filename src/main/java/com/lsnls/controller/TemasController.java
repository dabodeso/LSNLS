package com.lsnls.controller;

import com.lsnls.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/temas")
@CrossOrigin(origins = "*")
public class TemasController {

    @Autowired
    private AuthorizationService authService;

    // Lista estática de temas (se puede expandir a base de datos en el futuro)
    private static final Set<String> TEMAS_DISPONIBLES = new HashSet<>(Arrays.asList(
        "GEOGRAFÍA", "HISTORIA", "DEPORTES", "CIENCIA", "ARTE", "LITERATURA", 
        "MÚSICA", "CINE", "TELEVISIÓN", "TECNOLOGÍA", "POLÍTICA", "ECONOMÍA",
        "MEDICINA", "MATEMÁTICAS", "FÍSICA", "QUÍMICA", "BIOLOGÍA", "ASTRONOMÍA"
    ));

    // Lista estática de subtemas (se puede expandir a base de datos en el futuro)
    private static final Set<String> SUBTEMAS_DISPONIBLES = new HashSet<>(Arrays.asList(
        "GEOGRAFÍA FÍSICA", "GEOGRAFÍA HUMANA", "GEOGRAFÍA POLÍTICA", "GEOGRAFÍA ECONÓMICA",
        "HISTORIA ANTIGUA", "HISTORIA MEDIEVAL", "HISTORIA MODERNA", "HISTORIA CONTEMPORÁNEA",
        "FÚTBOL", "BALONCESTO", "TENIS", "ATLETISMO", "NATACIÓN", "CICLISMO",
        "FÍSICA CUÁNTICA", "FÍSICA CLÁSICA", "QUÍMICA ORGÁNICA", "QUÍMICA INORGÁNICA",
        "PINTURA", "ESCULTURA", "ARQUITECTURA", "FOTOGRAFÍA", "CINE",
        "LITERATURA CLÁSICA", "LITERATURA CONTEMPORÁNEA", "POESÍA", "NOVELA",
        "MÚSICA CLÁSICA", "MÚSICA POP", "MÚSICA ROCK", "JAZZ", "ÓPERA",
        "PROGRAMACIÓN", "INTELIGENCIA ARTIFICIAL", "ROBÓTICA", "INTERNET",
        "POLÍTICA NACIONAL", "POLÍTICA INTERNACIONAL", "DERECHO", "DIPLOMACIA",
        "MACROECONOMÍA", "MICROECONOMÍA", "FINANZAS", "COMERCIO INTERNACIONAL",
        "ANATOMÍA", "FISIOLOGÍA", "FARMACOLOGÍA", "CIRUGÍA", "PEDIATRÍA",
        "ÁLGEBRA", "GEOMETRÍA", "CÁLCULO", "ESTADÍSTICA", "TRIGONOMETRÍA",
        "GENÉTICA", "ECOLOGÍA", "EVOLUCIÓN", "BOTÁNICA", "ZOOLOGÍA",
        "SISTEMA SOLAR", "GALAXIAS", "ESTRELLAS", "PLANETAS", "EXPLORACIÓN ESPACIAL"
    ));

    /**
     * Obtener todos los temas disponibles
     */
    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> obtenerTemas() {
        try {
            List<String> temas = TEMAS_DISPONIBLES.stream()
                .sorted()
                .collect(Collectors.toList());
            return ResponseEntity.ok(temas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener temas: " + e.getMessage());
        }
    }

    /**
     * Obtener todos los subtemas disponibles
     */
    @GetMapping("/subtemas")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<?> obtenerSubtemas() {
        try {
            List<String> subtemas = SUBTEMAS_DISPONIBLES.stream()
                .sorted()
                .collect(Collectors.toList());
            return ResponseEntity.ok(subtemas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener subtemas: " + e.getMessage());
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

            // Normalizar el tema (mayúsculas, sin espacios extra)
            String temaNormalizado = nuevoTema.trim().toUpperCase();
            
            if (TEMAS_DISPONIBLES.contains(temaNormalizado)) {
                return ResponseEntity.badRequest().body("El tema '" + temaNormalizado + "' ya existe. Por favor, elige otro nombre para el tema.");
            }

            TEMAS_DISPONIBLES.add(temaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Tema añadido correctamente");
            response.put("tema", temaNormalizado);
            response.put("totalTemas", TEMAS_DISPONIBLES.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al añadir tema: " + e.getMessage());
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

            // Normalizar el subtema (mayúsculas, sin espacios extra)
            String subtemaNormalizado = nuevoSubtema.trim().toUpperCase();
            
            if (SUBTEMAS_DISPONIBLES.contains(subtemaNormalizado)) {
                return ResponseEntity.badRequest().body("El subtema '" + subtemaNormalizado + "' ya existe. Por favor, elige otro nombre para el subtema.");
            }

            SUBTEMAS_DISPONIBLES.add(subtemaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Subtema añadido correctamente");
            response.put("subtema", subtemaNormalizado);
            response.put("totalSubtemas", SUBTEMAS_DISPONIBLES.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al añadir subtema: " + e.getMessage());
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
            
            if (!TEMAS_DISPONIBLES.contains(temaNormalizado)) {
                return ResponseEntity.badRequest().body("El tema '" + temaNormalizado + "' no existe. Verifica el nombre del tema que intentas eliminar.");
            }

            // Verificar si el tema está en uso (aquí podrías hacer una consulta a la base de datos)
            // Por ahora, permitimos eliminar cualquier tema
            
            TEMAS_DISPONIBLES.remove(temaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Tema eliminado correctamente");
            response.put("tema", temaNormalizado);
            response.put("totalTemas", TEMAS_DISPONIBLES.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar tema: " + e.getMessage());
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
            
            if (!SUBTEMAS_DISPONIBLES.contains(subtemaNormalizado)) {
                return ResponseEntity.badRequest().body("El subtema '" + subtemaNormalizado + "' no existe. Verifica el nombre del subtema que intentas eliminar.");
            }

            // Verificar si el subtema está en uso (aquí podrías hacer una consulta a la base de datos)
            // Por ahora, permitimos eliminar cualquier subtema
            
            SUBTEMAS_DISPONIBLES.remove(subtemaNormalizado);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Subtema eliminado correctamente");
            response.put("subtema", subtemaNormalizado);
            response.put("totalSubtemas", SUBTEMAS_DISPONIBLES.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar subtema: " + e.getMessage());
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
            estadisticas.put("totalTemas", TEMAS_DISPONIBLES.size());
            estadisticas.put("totalSubtemas", SUBTEMAS_DISPONIBLES.size());
            estadisticas.put("temas", TEMAS_DISPONIBLES.stream().sorted().collect(Collectors.toList()));
            estadisticas.put("subtemas", SUBTEMAS_DISPONIBLES.stream().sorted().collect(Collectors.toList()));
            
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al obtener estadísticas: " + e.getMessage());
        }
    }
} 