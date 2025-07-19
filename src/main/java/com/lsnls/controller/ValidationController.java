package com.lsnls.controller;

import com.lsnls.service.ValidationService;
import com.lsnls.service.ValidationService.ValidationResult;
import com.lsnls.entity.*;
import com.lsnls.service.*;
import com.lsnls.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para validaciones de integridad del sistema
 * Solo accesible para usuarios con rol ADMIN o DIRECCION
 */
@RestController
@RequestMapping("/api/validation")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DIRECCION')")
public class ValidationController {

    @Autowired
    private ValidationService validationService;
    
    @Autowired
    private PreguntaService preguntaService;
    
    @Autowired
    private CuestionarioService cuestionarioService;
    
    @Autowired
    private ComboService comboService;
    
    @Autowired
    private ConcursanteRepository concursanteRepository;
    
    @Autowired
    private JornadaRepository jornadaRepository;
    
    @Autowired
    private ProgramaService programaService;

    /**
     * Valida la integridad de una pregunta específica
     */
    @GetMapping("/pregunta/{id}")
    public ResponseEntity<?> validarPregunta(@PathVariable Long id) {
        try {
            Optional<Pregunta> preguntaOpt = preguntaService.obtenerPorId(id);
            if (preguntaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadPregunta(preguntaOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Pregunta");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar pregunta: " + e.getMessage());
        }
    }

    /**
     * Valida la integridad de un cuestionario específico
     */
    @GetMapping("/cuestionario/{id}")
    public ResponseEntity<?> validarCuestionario(@PathVariable Long id) {
        try {
            Optional<Cuestionario> cuestionarioOpt = cuestionarioService.obtenerConPreguntas(id);
            if (cuestionarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadCuestionario(cuestionarioOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Cuestionario");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar cuestionario: " + e.getMessage());
        }
    }

    /**
     * Valida la integridad de un combo específico
     */
    @GetMapping("/combo/{id}")
    public ResponseEntity<?> validarCombo(@PathVariable Long id) {
        try {
            Optional<Combo> comboOpt = comboService.obtenerConPreguntas(id);
            if (comboOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadCombo(comboOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Combo");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar combo: " + e.getMessage());
        }
    }

    /**
     * Valida la integridad de un concursante específico
     */
    @GetMapping("/concursante/{id}")
    public ResponseEntity<?> validarConcursante(@PathVariable Long id) {
        try {
            Optional<Concursante> concursanteOpt = concursanteRepository.findById(id);
            if (concursanteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadConcursante(concursanteOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Concursante");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar concursante: " + e.getMessage());
        }
    }

    /**
     * Valida la integridad de una jornada específica
     */
    @GetMapping("/jornada/{id}")
    public ResponseEntity<?> validarJornada(@PathVariable Long id) {
        try {
            Optional<Jornada> jornadaOpt = jornadaRepository.findById(id);
            if (jornadaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadJornada(jornadaOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Jornada");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar jornada: " + e.getMessage());
        }
    }

    /**
     * Valida la integridad de un programa específico
     */
    @GetMapping("/programa/{id}")
    public ResponseEntity<?> validarPrograma(@PathVariable Long id) {
        try {
            Optional<Programa> programaOpt = programaService.findById(id);
            if (programaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ValidationResult result = validationService.validarIntegridadPrograma(programaOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Programa");
            response.put("id", id);
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar programa: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una validación completa del sistema
     */
    @GetMapping("/sistema")
    public ResponseEntity<?> validarSistemaCompleto() {
        try {
            ValidationResult result = validationService.validarSistemaCompleto();
            
            Map<String, Object> response = new HashMap<>();
            response.put("entidad", "Sistema Completo");
            response.put("valida", result.isValid());
            response.put("errores", result.getErrors());
            response.put("advertencias", result.getWarnings());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            if (result.isValid()) {
                response.put("mensaje", "Sistema validado correctamente sin errores críticos");
            } else {
                response.put("mensaje", "Se encontraron " + result.getErrors().size() + " errores críticos que requieren atención");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al validar sistema: " + e.getMessage());
        }
    }

    /**
     * Obtiene un resumen del estado de validación del sistema
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumenValidacion() {
        try {
            Map<String, Object> resumen = new HashMap<>();
            
            // Estadísticas generales
            resumen.put("timestamp", java.time.LocalDateTime.now());
            resumen.put("descripcion", "Resumen del estado de validación del sistema LSNLS");
            
            // Ejecutar validación completa
            ValidationResult resultadoSistema = validationService.validarSistemaCompleto();
            resumen.put("errores_criticos", resultadoSistema.getErrors().size());
            resumen.put("advertencias", resultadoSistema.getWarnings().size());
            resumen.put("sistema_valido", resultadoSistema.isValid());
            
            if (resultadoSistema.getErrors().isEmpty()) {
                resumen.put("estado_general", "SALUDABLE");
                resumen.put("recomendacion", "El sistema está funcionando correctamente");
            } else {
                resumen.put("estado_general", "REQUIERE_ATENCION");
                resumen.put("recomendacion", "Revisar errores críticos identificados");
            }
            
            return ResponseEntity.ok(resumen);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al generar resumen: " + e.getMessage());
        }
    }
} 