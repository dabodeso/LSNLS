package com.lsnls.service;

import com.lsnls.entity.*;
import com.lsnls.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio centralizado para validaciones de integridad de datos
 * Implementa validaciones adicionales que van más allá de las validaciones básicas
 */
@Service
public class ValidationService {

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private CuestionarioRepository cuestionarioRepository;
    
    @Autowired
    private ComboRepository comboRepository;
    
    @Autowired
    private ConcursanteRepository concursanteRepository;
    
    @Autowired
    private JornadaRepository jornadaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ProgramaRepository programaRepository;

    /**
     * Resultado de validación con detalles específicos
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public ValidationResult() {
            this.valid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }
        
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
        
        public String getWarningsAsString() {
            return String.join("; ", warnings);
        }
    }

    /**
     * Valida la integridad completa de una pregunta
     */
    public ValidationResult validarIntegridadPregunta(Pregunta pregunta) {
        ValidationResult result = new ValidationResult();
        
        // Validar contenido de la pregunta
        if (pregunta.getPregunta() == null || pregunta.getPregunta().trim().length() < 10) {
            result.addError("La pregunta debe tener al menos 10 caracteres");
        }
        
        if (pregunta.getPregunta() != null && pregunta.getPregunta().length() > 2000) {
            result.addError("La pregunta no puede exceder 2000 caracteres");
        }
        
        // Validar respuesta
        if (pregunta.getRespuesta() == null || pregunta.getRespuesta().trim().length() < 3) {
            result.addError("La respuesta debe tener al menos 3 caracteres");
        }
        
        if (pregunta.getRespuesta() != null && pregunta.getRespuesta().length() > 500) {
            result.addError("La respuesta no puede exceder 500 caracteres");
        }
        
        // Validar temática
        if (pregunta.getTematica() == null || pregunta.getTematica().trim().length() < 3) {
            result.addError("La temática debe tener al menos 3 caracteres");
        }
        
        // Validar consistencia de fechas
        if (pregunta.getFechaCreacion() != null && pregunta.getFechaVerificacion() != null) {
            if (pregunta.getFechaVerificacion().isBefore(pregunta.getFechaCreacion())) {
                result.addError("La fecha de verificación no puede ser anterior a la fecha de creación");
            }
        }
        
        // Validar estado y disponibilidad consistentes
        if (pregunta.getEstado() == Pregunta.EstadoPregunta.aprobada) {
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.usada &&
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                result.addError("Una pregunta aprobada debe estar disponible, usada o liberada");
            }
        }
        
        // Validar límites por usuario
        if (pregunta.getCreacionUsuario() != null) {
            Long preguntasDelUsuario = preguntaRepository.countByCreacionUsuario(pregunta.getCreacionUsuario());
            if (preguntasDelUsuario > 1000) {
                result.addWarning("El usuario tiene más de 1000 preguntas creadas. Considerar revisión");
            }
        }
        
        return result;
    }

    /**
     * Valida la integridad de un cuestionario
     */
    public ValidationResult validarIntegridadCuestionario(Cuestionario cuestionario) {
        ValidationResult result = new ValidationResult();
        
        // Validar que tenga exactamente 4 preguntas
        if (cuestionario.getPreguntas() != null) {
            int totalPreguntas = cuestionario.getPreguntas().size();
            if (totalPreguntas != 4) {
                result.addError("Un cuestionario debe tener exactamente 4 preguntas, actual: " + totalPreguntas);
            }
            
            // Validar que las preguntas sean de niveles 1-4
            boolean[] nivelesPresentes = new boolean[4]; // _1LS, _2NLS, _3LS, _4NLS
            for (PreguntaCuestionario pc : cuestionario.getPreguntas()) {
                String nivel = pc.getPregunta().getNivel().name();
                switch (nivel) {
                    case "_1LS":
                        if (nivelesPresentes[0]) {
                            result.addError("Duplicate nivel _1LS en cuestionario");
                        }
                        nivelesPresentes[0] = true;
                        break;
                    case "_2NLS":
                        if (nivelesPresentes[1]) {
                            result.addError("Duplicate nivel _2NLS en cuestionario");
                        }
                        nivelesPresentes[1] = true;
                        break;
                    case "_3LS":
                        if (nivelesPresentes[2]) {
                            result.addError("Duplicate nivel _3LS en cuestionario");
                        }
                        nivelesPresentes[2] = true;
                        break;
                    case "_4NLS":
                        if (nivelesPresentes[3]) {
                            result.addError("Duplicate nivel _4NLS en cuestionario");
                        }
                        nivelesPresentes[3] = true;
                        break;
                    default:
                        result.addError("Nivel inválido para cuestionario: " + nivel + ". Solo se permiten niveles 1-4");
                }
            }
            
            // Validar que todos los niveles estén presentes
            String[] nombreNiveles = {"_1LS", "_2NLS", "_3LS", "_4NLS"};
            for (int i = 0; i < 4; i++) {
                if (!nivelesPresentes[i]) {
                    result.addError("Falta pregunta de nivel " + nombreNiveles[i]);
                }
            }
        }
        
        // Validar consistencia de estados
        if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
            // Verificar que realmente esté asignado a una jornada
            Long jornadasCount = entityManager.createQuery(
                "SELECT COUNT(j) FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :cuestionarioId", Long.class)
                .setParameter("cuestionarioId", cuestionario.getId())
                .getSingleResult();
            
            if (jornadasCount == 0) {
                result.addError("Cuestionario marcado como 'adjudicado' pero no está asignado a ninguna jornada");
            }
        }
        
        return result;
    }

    /**
     * Valida la integridad de un combo
     */
    public ValidationResult validarIntegridadCombo(Combo combo) {
        ValidationResult result = new ValidationResult();
        
        // Validar que tenga exactamente 3 preguntas
        if (combo.getPreguntas() != null) {
            int totalPreguntas = combo.getPreguntas().size();
            if (totalPreguntas != 3) {
                result.addError("Un combo debe tener exactamente 3 preguntas, actual: " + totalPreguntas);
            }
            
            // Validar factores únicos
            boolean[] factoresPresentes = new boolean[3]; // factor 2, 3, 0
            for (PreguntaCombo pc : combo.getPreguntas()) {
                Integer factor = pc.getFactorMultiplicacion();
                
                // Validar que la pregunta sea de nivel 5
                String nivel = pc.getPregunta().getNivel().name();
                if (!nivel.startsWith("_5")) {
                    result.addError("Combo contiene pregunta de nivel " + nivel + ". Solo se permiten preguntas de nivel 5");
                }
                
                // Validar factores únicos
                switch (factor) {
                    case 2:
                        if (factoresPresentes[0]) {
                            result.addError("Factor multiplicador X2 duplicado en combo");
                        }
                        factoresPresentes[0] = true;
                        break;
                    case 3:
                        if (factoresPresentes[1]) {
                            result.addError("Factor multiplicador X3 duplicado en combo");
                        }
                        factoresPresentes[1] = true;
                        break;
                    case 0:
                        if (factoresPresentes[2]) {
                            result.addError("Factor multiplicador X duplicado en combo");
                        }
                        factoresPresentes[2] = true;
                        break;
                    default:
                        result.addError("Factor multiplicador inválido: " + factor + ". Solo se permiten 0, 2, 3");
                }
            }
            
            // Validar que todos los factores estén presentes
            String[] nombreFactores = {"X2 (factor 2)", "X3 (factor 3)", "X (factor 0)"};
            for (int i = 0; i < 3; i++) {
                if (!factoresPresentes[i]) {
                    result.addError("Falta pregunta con " + nombreFactores[i]);
                }
            }
        }
        
        return result;
    }

    /**
     * Valida la integridad de un concursante
     */
    public ValidationResult validarIntegridadConcursante(Concursante concursante) {
        ValidationResult result = new ValidationResult();
        
        // Validar campos obligatorios
        if (concursante.getNombre() == null || concursante.getNombre().trim().length() < 2) {
            result.addError("El nombre del concursante debe tener al menos 2 caracteres");
        }
        
        // Validar número de concursante único
        if (concursante.getNumeroConcursante() != null) {
            Long duplicados = concursanteRepository.countByNumeroConcursante(concursante.getNumeroConcursante());
            if (concursante.getId() == null && duplicados > 0) {
                result.addError("Ya existe un concursante con el número " + concursante.getNumeroConcursante());
            } else if (concursante.getId() != null) {
                // Para actualizaciones, excluir el propio concursante
                Long duplicadosExcluyendo = entityManager.createQuery(
                    "SELECT COUNT(c) FROM Concursante c WHERE c.numeroConcursante = :numero AND c.id != :id", Long.class)
                    .setParameter("numero", concursante.getNumeroConcursante())
                    .setParameter("id", concursante.getId())
                    .getSingleResult();
                    
                if (duplicadosExcluyendo > 0) {
                    result.addError("Ya existe otro concursante con el número " + concursante.getNumeroConcursante());
                }
            }
        }
        
        // Validar asignaciones consistentes
        if (concursante.getCuestionario() != null) {
            if (concursante.getCuestionario().getEstado() != Cuestionario.EstadoCuestionario.creado &&
                concursante.getCuestionario().getEstado() != Cuestionario.EstadoCuestionario.adjudicado &&
                concursante.getCuestionario().getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                result.addError("El cuestionario asignado no está en un estado válido para concursantes");
            }
        }
        
        if (concursante.getCombo() != null) {
            if (concursante.getCombo().getEstado() != Combo.EstadoCombo.creado &&
                concursante.getCombo().getEstado() != Combo.EstadoCombo.adjudicado &&
                concursante.getCombo().getEstado() != Combo.EstadoCombo.grabado) {
                result.addError("El combo asignado no está en un estado válido para concursantes");
            }
        }
        
        return result;
    }

    /**
     * Valida la integridad de una jornada
     */
    public ValidationResult validarIntegridadJornada(Jornada jornada) {
        ValidationResult result = new ValidationResult();
        
        // Validar límites de cuestionarios y combos
        if (jornada.getCuestionarios() != null && jornada.getCuestionarios().size() > 5) {
            result.addError("Una jornada no puede tener más de 5 cuestionarios, actual: " + jornada.getCuestionarios().size());
        }
        
        if (jornada.getCombos() != null && jornada.getCombos().size() > 5) {
            result.addError("Una jornada no puede tener más de 5 combos, actual: " + jornada.getCombos().size());
        }
        
        // Validar fechas
        if (jornada.getFechaJornada() != null) {
            LocalDate hoy = LocalDate.now();
            LocalDate fechaLimite = hoy.plusYears(2); // No más de 2 años en el futuro
            
            if (jornada.getFechaJornada().isBefore(hoy.minusYears(1))) {
                result.addWarning("La fecha de la jornada es muy antigua (más de 1 año)");
            }
            
            if (jornada.getFechaJornada().isAfter(fechaLimite)) {
                result.addError("La fecha de la jornada no puede ser más de 2 años en el futuro");
            }
        }
        
        // Validar estados de cuestionarios y combos asignados
        if (jornada.getCuestionarios() != null) {
            for (Cuestionario cuestionario : jornada.getCuestionarios()) {
                if (cuestionario.getEstado() != Cuestionario.EstadoCuestionario.adjudicado) {
                    result.addWarning("Cuestionario " + cuestionario.getId() + " asignado a jornada pero no está en estado 'adjudicado'");
                }
            }
        }
        
        if (jornada.getCombos() != null) {
            for (Combo combo : jornada.getCombos()) {
                if (combo.getEstado() != Combo.EstadoCombo.adjudicado) {
                    result.addWarning("Combo " + combo.getId() + " asignado a jornada pero no está en estado 'adjudicado'");
                }
            }
        }
        
        return result;
    }

    /**
     * Valida la integridad de un programa
     */
    public ValidationResult validarIntegridadPrograma(Programa programa) {
        ValidationResult result = new ValidationResult();
        
        // Validar temporada única
        if (programa.getTemporada() != null) {
            Long duplicados = programaRepository.countByTemporada(programa.getTemporada());
            if (programa.getId() == null && duplicados > 0) {
                result.addError("Ya existe un programa para la temporada " + programa.getTemporada());
            } else if (programa.getId() != null) {
                Long duplicadosExcluyendo = entityManager.createQuery(
                    "SELECT COUNT(p) FROM Programa p WHERE p.temporada = :temporada AND p.id != :id", Long.class)
                    .setParameter("temporada", programa.getTemporada())
                    .setParameter("id", programa.getId())
                    .getSingleResult();
                    
                if (duplicadosExcluyendo > 0) {
                    result.addError("Ya existe otro programa para la temporada " + programa.getTemporada());
                }
            }
        }
        
        // Validar rangos de temporada
        if (programa.getTemporada() != null) {
            int currentYear = LocalDate.now().getYear();
            if (programa.getTemporada() < 2020 || programa.getTemporada() > currentYear + 5) {
                result.addError("La temporada debe estar entre 2020 y " + (currentYear + 5));
            }
        }
        
        // Validar fechas
        if (programa.getFechaEmision() != null) {
            LocalDate hoy = LocalDate.now();
            if (programa.getFechaEmision().isBefore(hoy.minusYears(5))) {
                result.addWarning("La fecha de emisión es muy antigua (más de 5 años)");
            }
            if (programa.getFechaEmision().isAfter(hoy.plusYears(2))) {
                result.addError("La fecha de emisión no puede ser más de 2 años en el futuro");
            }
        }
        
        // Validar duración acumulada
        if (programa.getDuracionAcumulada() != null) {
            LocalTime maxDuracion = LocalTime.of(4, 0); // 4 horas máximo
            if (programa.getDuracionAcumulada().isAfter(maxDuracion)) {
                result.addWarning("La duración acumulada es muy alta (más de 4 horas)");
            }
        }
        
        return result;
    }

    /**
     * Ejecuta una validación integral del sistema
     */
    public ValidationResult validarSistemaCompleto() {
        ValidationResult result = new ValidationResult();
        
        try {
            // Validar integridad referencial
            result.addWarning("Ejecutando validación completa del sistema...");
            
            // Contar inconsistencias
            Long preguntasOrfanas = entityManager.createQuery(
                "SELECT COUNT(pc) FROM PreguntaCuestionario pc WHERE pc.pregunta IS NULL OR pc.cuestionario IS NULL", Long.class)
                .getSingleResult();
            if (preguntasOrfanas > 0) {
                result.addError("Existen " + preguntasOrfanas + " relaciones pregunta-cuestionario con referencias nulas");
            }
            
            Long combosOrfanos = entityManager.createQuery(
                "SELECT COUNT(pc) FROM PreguntaCombo pc WHERE pc.pregunta IS NULL OR pc.combo IS NULL", Long.class)
                .getSingleResult();
            if (combosOrfanos > 0) {
                result.addError("Existen " + combosOrfanos + " relaciones pregunta-combo con referencias nulas");
            }
            
            // Validar estados inconsistentes
            Long cuestionariosInconsistentes = entityManager.createQuery(
                "SELECT COUNT(c) FROM Cuestionario c WHERE c.estado = 'adjudicado' AND " +
                "c.id NOT IN (SELECT cu.id FROM Jornada j JOIN j.cuestionarios cu)", Long.class)
                .getSingleResult();
            if (cuestionariosInconsistentes > 0) {
                result.addError("Existen " + cuestionariosInconsistentes + " cuestionarios marcados como 'adjudicado' pero no asignados a ninguna jornada");
            }
            
            result.addWarning("Validación del sistema completada");
            
        } catch (Exception e) {
            result.addError("Error durante la validación del sistema: " + e.getMessage());
        }
        
        return result;
    }
} 